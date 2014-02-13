/**
 * 
 */

package code.dws.ontology;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import code.dws.dao.Pair;
import code.dws.dbConnectivity.DBWrapper;
import code.dws.query.SPARQLEndPointQueryAPI;
import code.dws.utils.Constants;
import code.dws.utils.Constants.OIE;
import code.dws.utils.Utilities;

/**
 * This class is converter class for converting specially owl/csv files into
 * markov logic networks formulae
 * 
 * @author Arnab Dutta
 */
public class GenericConverter {

    // define Logger
    public static Logger logger = Logger.getLogger(GenericConverter.class.getName());

    // data structure to hold the pairs of NELL categories and relations and its
    // hierarchy
    static Map<String, List<Pair<String, String>>> NELL_CATG_RELTNS = new HashMap<String, List<Pair<String, String>>>();

    private static int cntr = 0;

    // how domain property is defined in NELL
    private static final String DOMAIN_DEFN = "domain";

    // how range is defined in NELL
    private static final String RANGE_DEFN = "range";

    // disjointness property definition
    private static final String DISJOINT_DEFN = "mutexpredicates";

    // inverse property definition
    private static final String INVERSE_DEFN = "inverse";

    // is it a concept or predicate is defined by this relation ..
    private static final String MEMBER_TYPE_DEFN = "memberofsets";

    // subsumption definition
    private static final String SUBSUMP_DEFN = "generalizations";

    public static long ENTITY_COUNTER = 0;

    public static Map<String, Long> MAP_COUNTER = new HashMap<String, Long>();

    private static final Map<String, String> URI_2_ENTITY_MAP = new HashMap<String, String>();

    public enum TYPE {
        NELL_ONTO, NELL_PRED_ANNO, NELL_CLASS_ANNO, NELL_ABOX; // ; is optional
    }

    // ..and by these four values
    // private static final String CATG_TYPE_DEFN_I = "concept:rtwcategory";
    private static final String CATG_TYPE_DEFN_II = "rtwcategory";
    // private static final String REL_TYPE_DEFN_I = "concept:rtwrelation";
    private static final String REL_TYPE_DEFN_II = "rtwrelation";

    public static final String URI_CANONICAL_FILE = "resources/uri2CanonMapping.tsv";

    private static Map<Pair<String, String>, Double> NELL_CLASS_ASSERT_MAP = new HashMap<Pair<String, String>, Double>();
    private static Set<String> uniqueEntities = new HashSet<String>();

    public static Set<String> SUB_SET_TYPES = new HashSet<String>();
    public static Set<String> OBJ_SET_TYPES = new HashSet<String>();

    // assertions of each NELL entities and its types
    private static final String NELL_CLASS_ASSERTIONS_FILE = "resource/input/NellClassAsserions.csv";

    /**
     * converts a csv file to owl file
     * 
     * @param inputCsvFile input file
     * @param delimit delimiter of input file
     * @param outputOwlFile
     * @param
     */
    public static void convertCsvToOwl(String inputCsvFile, String delimit, TYPE type,
            String outputOwlFile) {
        // If we are processing the baseline vs nell triple, no need to load it
        // in memory
        // since, we can readily convert the rows to a set of assertion
        // statements

        if (!type.equals(TYPE.NELL_ABOX)) {
            loadCsvInMemory(inputCsvFile, delimit);

            try {
                createOwlFile(type, outputOwlFile);
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
            }
        }
        else { // deal NELL ABox stuff separately
            System.out.println("Loading confidence values of the NELL class assertions ...");
            loadNELLClassAssertConfidences();

            try {
                readCsvAndCreateOwlFile(inputCsvFile, delimit,
                        outputOwlFile);
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * creates the owl file reading the contents of the csv file.
     * 
     * @param inputCsvFile input csv file
     * @param delimit delimiter of csv file
     * @param outputOwlFile output owl file path
     * @throws OWLOntologyCreationException
     */
    private static void readCsvAndCreateOwlFile(String inputCsvFile, String delimit,
            String outputOwlFile)
            throws OWLOntologyCreationException {

        String line = null;
        BufferedReader tupleReader;
        String[] elements = null;

        // Nell specific variables
        String nellSub = null;
        String nellSubPFxd = null;
        String nellSubType = null;

        String nellPred = null;

        String nellObj = null;
        String nellObjPFxd = null;
        String nellObjType = null;

        String blSubjInst = null;
        String blObjInst = null;

        String goldSub = null;
        String goldObj = null;

        List<String> listTypes = null;

        OWLCreator owlCreator = new OWLCreator(Constants.OIE_ONTOLOGY_NAMESPACE);

        try {
            tupleReader = new BufferedReader(new FileReader(inputCsvFile));
            logger.info("Reading " + inputCsvFile + " ... Please wait !!");

            // the file where the evidences for the MLN are written out
            BufferedWriter goldEvidenceWriter = new BufferedWriter(new FileWriter(
                    Constants.GOLD_MLN_EVIDENCE));

            BufferedWriter isOfTypeEvidenceWriter = new BufferedWriter(new FileWriter(
                    Constants.IS_OF_TYPE_CONF_NELL_EVIDENCE));

            // initiate DB
            DBWrapper.init(Constants.GET_WIKI_TITLES_SQL);

            if (tupleReader != null) {
                while ((line = tupleReader.readLine()) != null) {
                    elements = line.split(delimit);

                    if (elements.length >= 5) {

                        // get the individual NELL subject, predicate and object
                        nellSub = getInst(elements[0]);
                        nellPred = elements[1];
                        nellObj = getInst(elements[2]);

                        // go on adding to a set of types
                        // used later for relative ranking
                        // addToSet(nellSub, nellObj, OIE.NELL);

                        if (nellObj.indexOf("_israel") != -1)
                            System.out.println("");

                        goldSub = elements[5].trim();
                        goldObj = elements[6].trim();

                        nellSubType = getType(elements[0]);
                        nellObjType = getType(elements[2]);

                        if (!nellSubType.equals(nellSub))
                            nellSubPFxd = generateUniqueURI(nellSub, elements[0], elements[1],
                                    elements[2]);

                        if (!nellObjType.equals(nellObj))
                            nellObjPFxd = generateUniqueURI(nellObj, elements[0], elements[1],
                                    elements[2]);

                        if (nellSubPFxd == null)
                            System.out.println();

                        // create a property assertion on the nell triple after
                        // entities are postfixed
                        if (nellPred != null && nellSub != null && nellObj != null
                                && !nellPred.equals("generalizations"))
                            owlCreator.createPropertyAssertion(nellPred, nellSubPFxd, nellObjPFxd);

                        // check for the types of the instances, if any
                        if (!nellSubType.equals(nellSub))
                            createTypeOfMLN(nellSub, nellSubPFxd, isOfTypeEvidenceWriter);

                        if (!nellObjType.equals(nellObj))
                            createTypeOfMLN(nellObj, nellObjPFxd, isOfTypeEvidenceWriter);

                        blSubjInst = Utilities.cleanDBpediaURI(elements[3]);

                        blObjInst = Utilities.cleanDBpediaURI(elements[4]);

                        // get types of subject instances
                        getDBpediaTypes(blSubjInst, owlCreator, Constants.DOMAIN);

                        blSubjInst = Utilities.characterToUTF8(blSubjInst);
                        //
                        // type assertion of NELL instances as subjects
                        // if (!nellSubType.equals(nellSub))
                        // owlCreator.createIsTypeOf(nellSubPFxd, nellSubType);

                        // same as between NELL and DBpedia instance as
                        // // subjects
                        if (!nellSubType.equals(nellSub))
                            owlCreator.createSameAs(nellSubPFxd, blSubjInst);

                        // get types of subject instances
                        getDBpediaTypes(blObjInst, owlCreator, Constants.RANGE);

                        blObjInst = Utilities.characterToUTF8(blObjInst);

                        // type assertion of NELL instances as objects
                        // if (!nellObjType.equals(nellObj))
                        // owlCreator.createIsTypeOf(nellObj, nellObjType);

                        // same as between NELL and DBpedia instance as
                        // objects
                        if (!nellObjType.equals(nellObj))
                            owlCreator.createSameAs(nellObjPFxd, blObjInst);

                        // Create Gold MLN
                        if (goldSub.indexOf("?") == -1)
                            createGoldSameAsMLN(goldSub, nellSubPFxd, goldEvidenceWriter);

                        if (!nellPred.equals("generalizations") && goldObj.indexOf("?") == -1) {
                            createGoldSameAsMLN(goldObj, nellObjPFxd, goldEvidenceWriter);
                        }

                        if (elements[7].trim().equals("C"))
                            createGoldPropAsstMLN(nellPred, nellSubPFxd, nellObjPFxd,
                                    goldEvidenceWriter);

                    }
                }

                goldEvidenceWriter.close();
                isOfTypeEvidenceWriter.close();

                // dumpToLocalFile(GenericConverter.URI_2_ENTITY_MAP,
                // URI_CANONICAL_FILE);

                // flush to file
                owlCreator.createOutput(outputOwlFile);
            }

            DBWrapper.saveResidualDBPTypes();
            DBWrapper.shutDown();

        } catch (IOException e) {
            System.out.println("Error processing " + line + " " + e.getMessage());
        }
    }

    /**
     * overriden method.
     * 
     * @param sub subject
     * @param prop property
     * @param obj object
     * @param oieType the type of OIE project, NELL or Reverb, different types
     *            have diffeent format
     */
    public static void addToSet(String sub, String obj, OIE oieType) {

        // fetch the most probable mathced entities both for subject and object
        List<String> subjConcepts = DBWrapper
                .fetchWikiTitles((oieType == Constants.OIE.NELL) ? Utilities
                        .cleanse(sub).replaceAll("\\_+", " ") : Utilities.cleanse(sub).replaceAll(
                        "\\_+",
                        " "));

        List<String> objConcepts = DBWrapper
                .fetchWikiTitles((oieType == Constants.OIE.NELL) ? Utilities
                        .cleanse(obj).replaceAll("\\_+", " ") : Utilities.cleanse(obj).replaceAll(
                        "\\_+",
                        " "));

        getTypes(subjConcepts, objConcepts);

    }

    /**
     * iterates the DBPEdia Entities and finds their types
     * 
     * @param subEntities list of subject candidates
     * @param objEntities list of object candidates
     */
    private static void getTypes(List<String> subEntities, List<String> objEntities) {
        List<String> entityTypes = null;

        // iterate over the subject entities
        for (String dbpEntity : subEntities) {
            dbpEntity = dbpEntity.replaceAll("\\s+", "_");

            // if(dbpEntity.indexOf("House_of_the_Seven_Gables") != -1)
            // System.out.println();

            entityTypes = SPARQLEndPointQueryAPI.getInstanceTypes(dbpEntity);
            entityTypes = SPARQLEndPointQueryAPI.getLowestType(entityTypes);

            if (entityTypes.size() != 0) {
                for (String type : entityTypes) {
                    SUB_SET_TYPES.add(type);
                }
            }
        }
        // iterate over the object entities
        for (String dbpEntity : objEntities) {
            dbpEntity = dbpEntity.replaceAll("\\s+", "_");

            entityTypes = SPARQLEndPointQueryAPI.getInstanceTypes(dbpEntity);
            entityTypes = SPARQLEndPointQueryAPI.getLowestType(entityTypes);

            if (entityTypes.size() != 0) {
                for (String entityType : entityTypes) {
                    OBJ_SET_TYPES.add(entityType);
                }
            }
        }
    }

    private static void createInstanceSourceType(String blInst, String nellPFxd,
            BufferedWriter instanceSourceTypeEvidenceWriter) throws IOException {

        blInst = Utilities.characterToUTF8(blInst);
        blInst = blInst.replaceAll("%", "~");
        blInst = blInst.replaceAll("~28", "[");
        blInst = blInst.replaceAll("~29", "]");
        blInst = blInst.replaceAll("~27", "*");

        instanceSourceTypeEvidenceWriter.write("instanceFromDBPedia(\"DBP#resource/" + blInst
                + "\")\n");
        instanceSourceTypeEvidenceWriter.write("instanceFromNell(\"NELL#Instance/" + nellPFxd
                + "\")\n");

    }

    /**
     * looks into the assertions of NELL entity types from the in-memory
     * collection and creates the isOfType axioms
     * 
     * @param originalNELLEntity
     * @param nellEntityPrefixed
     * @param isOfTypeEvidenceWriter
     * @return
     */
    private static String createTypeOfMLN(String originalNELLEntity,
            String nellEntityPrefixed, BufferedWriter isOfTypeEvidenceWriter) {

        String type;

        // String tempEntity = originalNELLEntity.replaceAll(Constants.POST_FIX
        // + "[0-9]", "");

        for (Map.Entry<Pair<String, String>, Double> entry : NELL_CLASS_ASSERT_MAP.entrySet()) {
            if (entry.getKey().getFirst().equals(originalNELLEntity)) {
                type = entry.getKey().getSecond();
                try {
                    if (!uniqueEntities.contains(nellEntityPrefixed + type)) {
                        isOfTypeEvidenceWriter.write("isOfTypeConf(\"NELL#Concept/"
                                + type + "\", \"NELL#Instance/" + nellEntityPrefixed
                                + "\", " + Utilities.convertProbabilityToWeight(entry.getValue())
                                + ")\n");

                        uniqueEntities.add(nellEntityPrefixed + type);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static void createGoldSameAsMLN(String gold, String nell, BufferedWriter bw)
            throws IOException {

        String nellFiltered = nell.substring(nell.indexOf(":") + 1, nell.length());
        String goldFiltered = gold.replaceAll("http://dbpedia.org/resource/", "");

        // if (gold.indexOf("Carrie_") != -1)
        // System.out.println("");

        goldFiltered = Utilities.characterToUTF8(goldFiltered);
        goldFiltered = goldFiltered.replaceAll("%", "~");
        goldFiltered = goldFiltered.replaceAll("~28", "[");
        goldFiltered = goldFiltered.replaceAll("~29", "]");
        goldFiltered = goldFiltered.replaceAll("~27", "*");

        bw.write("sameAs(\"DBP#resource/" + goldFiltered + "\", \"NELL#Instance/" + nellFiltered
                + "\")\n");

    }

    public static void createGoldPropAsstMLN(String nellPred, String nellSubPFxd,
            String nellObjPFxd, BufferedWriter goldEvidenceWriter) throws IOException {

        goldEvidenceWriter.write("propAsst(\"NELL#Predicate/" + nellPred + "\", \"NELL#Instance/"
                + nellSubPFxd + "\", \"NELL#Instance/" + nellObjPFxd + "\")\n");

    }

    /**
     * dump the uri to canonical form mappings to some local file
     * 
     * @param uri2EntityMap
     * @param path
     * @throws IOException
     */
    private static void dumpToLocalFile(Map<String, String> uri2EntityMap, String path)
            throws IOException {
        String key = null;
        String value = null;

        FileWriter fw = new FileWriter(path);
        BufferedWriter bw = new BufferedWriter(fw);

        for (Map.Entry<String, String> entry : uri2EntityMap.entrySet()) {
            key = entry.getKey();
            value = entry.getValue();
            bw.write("OIE#Instance/" + key + "\t" + value + "\n");
        }

        // clear the counter map
        GenericConverter.MAP_COUNTER.clear();

        // close the writer stream
        bw.close();
    }

    /**
     * takes a nell/reverb instance and creates an unique URI out of it. So if
     * multiple times an entity occurs, each one will have different uris.
     * 
     * @param nellInst
     * @param classInstance
     * @param elements2
     * @param elements
     * @return
     */
    private static String generateUniqueURI(String nellInst, String sub, String pred, String obj) {
        // check if this URI is already there
        if (GenericConverter.MAP_COUNTER.containsKey(nellInst)) {
            long value = GenericConverter.MAP_COUNTER.get(nellInst);
            GenericConverter.MAP_COUNTER.put(nellInst, value + 1);

            // create an unique URI because same entity already has been
            // encountered before
            nellInst = nellInst + Constants.POST_FIX + String.valueOf(value + 1);

        } else {
            GenericConverter.MAP_COUNTER.put(nellInst, 1L);
        }

        GenericConverter.URI_2_ENTITY_MAP.put(nellInst, sub + "\t" + pred + "\t" + obj);
        return nellInst;
    }

    /**
     * @param blInst
     * @param owlCreator
     * @param identifier
     */
    public static void getDBpediaTypes(String blInst, OWLCreator owlCreator, String identifier) {
        List<String> listTypes;

        if (blInst.indexOf("The_Inferno") != -1)
            System.out.println("");

        // get DBPedia types
        if (Constants.RELOAD_DBPEDIA_TYPES)
            listTypes = SPARQLEndPointQueryAPI.getInstanceTypes(Utilities.utf8ToCharacter(blInst));
        else{
            if(blInst.indexOf("Ring") != -1)
                System.out.println();

            listTypes = DBWrapper.getDBPInstanceType(Utilities.characterToUTF8(blInst));
        }
        if (listTypes.size() > 0) {

            // get the most specific type
            if (Constants.RELOAD_DBPEDIA_TYPES)
                listTypes = SPARQLEndPointQueryAPI.getLowestType(listTypes);

            // type assertion of DBPedia instances occurring
            // as subjects
            // before adding to owl convert special characters to UTF-8
            blInst = Utilities.characterToUTF8(blInst);
            owlCreator.createIsTypeOf(blInst, listTypes);

        }
        // else {
        // System.out.println(" No TYPE found for = " + blInst);
        // }
    }

    private static void addToSet(List<String> listTypes, String identifier) {
        for (String type : listTypes) {
            if (identifier.equals(Constants.DOMAIN))
                SUB_SET_TYPES.add(type);
            else
                OBJ_SET_TYPES.add(type);
        }
    }

    /**
     * get the actual nell instance, following the ":" if any
     * 
     * @param arg
     * @param identifier
     * @return
     */
    public static String getInst(String arg) {

        if (arg.indexOf(":") != -1)
            return arg.substring(arg.indexOf(":") + 1, arg.length());
        else
            return arg;
    }

    private static String getType(String arg) {
        if (arg.indexOf(":") != -1)
            return arg.substring(0, arg.indexOf(":"));
        else
            return arg;
    }

    /**
     * creates the owl file reading the contents of the csv file loaded in
     * memory. Basically, the csv input file can be the NELL ontology or the
     * predicate mapping annotated file, so we require slightly different way of
     * parsing the files
     * 
     * @param type type of inpput file, NELL or annotated file
     * @param outputOwlFile
     * @throws OWLOntologyCreationException
     */
    private static void createOwlFile(TYPE type, String outputOwlFile)
            throws OWLOntologyCreationException {
        String key = null;
        String domain = null;
        String range = null;
        String inverse = null;

        boolean isConcept = false;

        // for NELL ontology
        List<String> listDisjClasses = null;
        List<String> supCls = null;

        // for Nell predicate matches
        List<String> subClasses = null;
        List<String> supClasses = null;
        List<String> equivClasses = null;

        OWLCreator owlCreator = new OWLCreator(Constants.OIE_ONTOLOGY_NAMESPACE);

        for (Map.Entry<String, List<Pair<String, String>>> entry : NELL_CATG_RELTNS.entrySet()) {
            // get the key
            key = entry.getKey();

            if (type.equals(TYPE.NELL_ONTO)) {
                // check if this is a concept or predicate
                isConcept = isConcept(key);
                // get its super type
                supCls = isTypeOf(key);
                // ******* FOR RELATIONS
                // **********************************************
                // only predicates will have domain range restrictions and
                // inverses
                if (!isConcept) {
                    // get domain and range
                    domain = getDomain(key);
                    range = getRange(key);

                    if (domain != null && range != null) {
                        logger.info(domain + "  " + key + "  " + range);

                        // create an ontology with these values
                        // domain range restriction
                        owlCreator.creatDomainRangeRestriction(key.replaceAll(":", "_"),
                                domain.replaceAll(":", "_"), range.replaceAll(":", "_"));
                    }

                    // get the inverse
                    inverse = getInverse(key);

                    // NO INVERSE AS OF NOW
                    // if (inverse != null) {
                    // // inverse
                    // owlCreator.createInverseRelations(key.replaceAll(":",
                    // "_"),
                    // inverse.replaceAll(":", "_"));
                    //
                    // }

                    if (supCls != null) {
                        owlCreator.createSubsumption(key.replaceAll(":", "_"),
                                supCls, 0);
                    }
                }
                // ******* FOR CONCEPTS
                // ***********************************************
                if (isConcept) {
                    listDisjClasses = getDisjointClasses(key);

                    // disjoint
                    owlCreator.createDisjointClasses(key.replaceAll(":", "_"), listDisjClasses);

                    if (supCls != null) {
                        owlCreator.createSubsumption(key.replaceAll(":", "_"),
                                supCls, 1);
                    }
                }
            }

            // ****** FOR THE NELL PREDICATES MATCHINGS
            // *****************************
            if (type.equals(TYPE.NELL_PRED_ANNO)) {
                // idea is get the set of predicates with subsumption relations
                // or equivalence relation
                subClasses = getSubClasses(key);
                if (subClasses.size() > 0)
                    logger.info(key + "  " + subClasses);

                supClasses = getSupClasses(key);
                if (supClasses.size() > 0)
                    logger.info(key + "  " + supClasses);

                equivClasses = getEquivClasses(key);
                if (equivClasses.size() > 0)
                    logger.info(key + "  " + equivClasses);

                // with these bunch of sub and super classes, create subsumption
                // relations as an owl ontology
                owlCreator.createCrossDomainSubsumption(key, supClasses, 0, 0);
                owlCreator.createCrossDomainSubsumption(key, subClasses, 1, 0);

                // add create the equivalent properties
                owlCreator.createEquivalentProperties(key, equivClasses);
            }
        }

        // flush to file
        owlCreator.createOutput(outputOwlFile);
    }

    /**
     * takes a csv file and converts to a an owl ontology
     * 
     * @param filePath path of the input file
     * @param delimiter file delimiter
     * @param dataType
     */
    private static void loadCsvInMemory(String filePath, String delimiter) {

        String line = null;
        String[] elements = null;

        Pair<String, String> pair = null;
        List<Pair<String, String>> listPairs = null;

        BufferedReader tupleReader;

        try {
            // get the reader
            tupleReader = new BufferedReader(new FileReader(filePath));

            // swipe through the file and read lines
            if (tupleReader != null) {
                while ((line = tupleReader.readLine()) != null) {
                    elements = line.split(delimiter);

                    // processing logic for NELL
                    if (elements.length == 3) {

                        // create a custom data structure with these
                        // elements
                        if (!elements[2].equals("INCORRECT")
                                && elements[0].indexOf("concept:") == -1) {
                            pair = new Pair<String, String>(elements[1], elements[2]);

                            if (NELL_CATG_RELTNS.containsKey(elements[0])) {
                                // get the list for this key
                                listPairs = NELL_CATG_RELTNS.get(elements[0]);

                                // check if the pair exists in the list,
                                // else add it
                                if (!listPairs.contains(pair)) {
                                    listPairs.add(pair);
                                }
                            } else {
                                listPairs = new ArrayList<Pair<String, String>>();
                                listPairs.add(pair);
                                NELL_CATG_RELTNS.put(elements[0], listPairs);
                            }
                        }
                    }
                }
            }

        } catch (IOException e) {
            logger.info("Error processing " + line + " " + e.getMessage());
        }
    }

    // ****************** FOR NELL ONTOLOGY *****************************

    /**
     * get super concept/property of a given property
     * 
     * @param arg property
     * @return immediate super concept
     */
    private static List<String> isTypeOf(String arg) {

        List<String> retList = new ArrayList<String>();

        List<Pair<String, String>> list = NELL_CATG_RELTNS.get(arg);
        for (Pair<String, String> pair : list) {
            if (pair.getFirst().equals(SUBSUMP_DEFN))
                retList.add(pair.getSecond().replaceAll(":", "_"));
        }
        return retList;
    }

    /**
     * get inverse property of a given property
     * 
     * @param arg property
     * @return inverse
     */
    private static String getInverse(String arg) {
        List<Pair<String, String>> list = NELL_CATG_RELTNS.get(arg);
        for (Pair<String, String> pair : list) {
            if (pair.getFirst().equals(INVERSE_DEFN))
                return pair.getSecond();
        }
        // if there is no inverse definition,
        return null;
    }

    /**
     * returns the range of the given predicate
     * 
     * @param arg predicate
     * @return range
     */
    public static String getRange(String arg) {

        List<Pair<String, String>> list = NELL_CATG_RELTNS.get(arg);
        for (Pair<String, String> pair : list) {
            if (pair.getFirst().equals(RANGE_DEFN))
                return pair.getSecond();
        }
        // if there is no range definition, occurs for classes
        return null;
    }

    /**
     * returns the domain of the given predicate
     * 
     * @param arg predicate
     * @return domain
     */
    public static String getDomain(String arg) {

        List<Pair<String, String>> list = NELL_CATG_RELTNS.get(arg);
        for (Pair<String, String> pair : list) {
            if (pair.getFirst().equals(DOMAIN_DEFN))
                return pair.getSecond();
        }
        // if there is no domain definition, occurs for classes
        return null;
    }

    /**
     * returns is it is a concept or relation
     * 
     * @param arg class or predicate
     * @return true if concept, false if predicate
     */
    public static boolean isConcept(String arg) {

        List<Pair<String, String>> list = NELL_CATG_RELTNS.get(arg);
        for (Pair<String, String> pair : list) {
            if (pair.getFirst().equals(MEMBER_TYPE_DEFN)
                    &&
                    pair.getSecond().equals(
                            CATG_TYPE_DEFN_II))
                return true;
        }
        return false;
    }

    /**
     * returns list of disjoint classes/predicates
     * 
     * @param arg class or predicate
     * @return List of disjoint classes or predicates
     */
    public static List<String> getDisjointClasses(String arg) {

        List<String> retList = new ArrayList<String>();

        List<Pair<String, String>> list = NELL_CATG_RELTNS.get(arg);
        for (Pair<String, String> pair : list) {
            if (pair.getFirst().equals(DISJOINT_DEFN))
                retList.add(pair.getSecond());
        }
        return retList;
    }

    // ****************** FOR NELL PREDICATE MATCHES
    // *****************************

    /**
     * get super classes(in DBPedia) of the nell property
     * 
     * @param arg nell predicate
     * @return list of super classes
     */
    private static List<String> getSubClasses(String arg) {
        List<String> retList = new ArrayList<String>();

        List<Pair<String, String>> list = NELL_CATG_RELTNS.get(arg);
        for (Pair<String, String> pair : list) {
            if (pair.getSecond().equals("SUPPROP"))
                retList.add(Utilities.cleanDBpediaURI(pair.getFirst()));
        }
        return retList;
    }

    /**
     * get sub classes(in DBPedia) of the nell property
     * 
     * @param arg nell predicate
     * @return list of sub classes
     */
    private static List<String> getSupClasses(String arg) {
        List<String> retList = new ArrayList<String>();

        List<Pair<String, String>> list = NELL_CATG_RELTNS.get(arg);
        for (Pair<String, String> pair : list) {
            if (pair.getSecond().equals("SUBPROP"))
                retList.add(Utilities.cleanDBpediaURI(pair.getFirst()));
        }
        return retList;
    }

    /**
     * get quivalent classes for a given class
     * 
     * @param arg provide class
     * @return equivalent class
     */
    private static List<String> getEquivClasses(String arg) {
        List<String> retList = new ArrayList<String>();

        List<Pair<String, String>> list = NELL_CATG_RELTNS.get(arg);
        for (Pair<String, String> pair : list) {
            if (pair.getSecond().equals("EQUIV"))
                retList.add(Utilities.cleanDBpediaURI(pair.getFirst()));
        }
        return retList;
    }

    public static void print() {
        for (Map.Entry<String, List<Pair<String, String>>> entry : NELL_CATG_RELTNS.entrySet()) {
            String key = entry.getKey();
            List<Pair<String, String>> list = entry.getValue();

            for (Pair<String, String> pair : list) {
                String first = pair.getFirst();
                String secnd = pair.getSecond();

                logger.info(key + "  " + first + "  " + secnd);
            }
        }
    }

    /**
     * loads the assertions of the NELL entities in main memory. This helps to
     * form is of Type assertions on the NELL entities
     */
    private static void loadNELLClassAssertConfidences() {

        String strLine = null;

        String sub = null;
        String pred = null;
        String obj = null;
        double conf = 0D;
        try {
            FileInputStream file = new FileInputStream(NELL_CLASS_ASSERTIONS_FILE);
            BufferedReader input = new BufferedReader
                    (new InputStreamReader(file));

            Pair<String, String> pairEntity = null;

            while ((strLine = input.readLine()) != null) {

                sub = (strLine.split(",")[0]);
                sub = sub.substring(sub.indexOf(":") + 1, sub.length());

                pred = (strLine.split(",")[1]);
                obj = (strLine.split(",")[2]);
                conf = Double.valueOf((strLine.split(",")[3]));

                pairEntity = new Pair<String, String>(sub, obj);

                // if (NELL_CLASS_ASSERT_MAP.containsKey(pairEntity))
                // System.out.println(pairEntity + " " +
                // NELL_CLASS_ASSERT_MAP.get(pairEntity));
                // else
                NELL_CLASS_ASSERT_MAP.put(pairEntity, conf);

            }
        } catch (IOException ex) {
            System.out.println("Exception with line = " + strLine + "  " + ex.getMessage());
        }
    }
}
