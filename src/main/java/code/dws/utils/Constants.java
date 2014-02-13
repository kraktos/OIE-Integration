/**
 * 
 */

package code.dws.utils;

import code.dws.experiment.ExperimentAutomation;

/**
 * This class stores a set of constants required for the application
 * 
 * @author Arnab Dutta
 */
public class Constants
{
    public static enum OIE
    {
        NELL,
        REVERB
    }

    public static final String DELIMIT = "\",";

    /**
     * delimiter for the the CSV file coming as input from extraction engines' output
     */
    public static final String DELIMIT_IE_FILE = ",";

    public static String VIRTUSO_ENDPOINT = "http://wifo5-38.informatik.uni-mannheim.de:8890/sparql";

    public static String YAGO_KB = "http://yago-knowledge.org";

    public static String YAGO_NS = "http://yago-knowledge.org/resource/";

    /**
     * DBPedia End point URL
     */
    public static String DBPEDIA_SPARQL_ENDPOINT = "http://dbpedia.org/sparql";

    // ExperimentAutomation.DBPEDIA_SPARQL_ENDPOINT;
    // "http://wifo5-32.informatik.uni-mannheim.de:8890/sparql";
    // "http://wifo5-32.informatik.uni-mannheim.de:8890/sparql";

    // "http://dbpedia.org/sparql";
    // "http://live.dbpedia.org/sparql";

    // *****************DIRECTORY LOCATIONS
    // ************************************************

    /**
     * location for storing the predicate distribution patterns based on some integer values of the subjects and objects
     * of the instances
     */
    public static final String DBPEDIA_PREDICATE_DISTRIBUTION = "/home/arnab/Work/data/DBPedia/property";

    /**
     * location of all the raw CSV files dumped from DBPedia SPARQL endpoint
     */
    public static final String DBPEDIA_DATA_DIR = "/home/arnab/Work/data/DBPedia/data";

    /**
     * location of the directory where the DBPEdia indices for entities are stored
     */
    public static final String DBPEDIA_ENT_INDEX_DIR = "/home/arnab/Work/data/DBPedia/indexFiles";

    /**
     * location of the directory where the NELL indices for entities are stored. Gold Standard creation
     */
    public static final String NELL_ENT_INDEX_DIR = "/home/arnab/Work/data/NELL/indexFiles";

    /**
     * location of the directory where the DBPedia SO indices for entities are stored. Baseline creation
     */
    public static final String DBPEDIA_INFO_INDEX_DIR = "/home/arnab/Work/data/DBPedia/infoIndex";

    /**
     * location of the directory where the ReVerb indices for entities are stored. Gold Standard creation
     */
    public static final String REVERB_ENT_INDEX_DIR = "/home/arnab/Work/data/ReVerb/index";

    /**
     * location of the directory where the indices for predicates are stored
     */
    public static final String DBPEDIA_PROP_INDEX_DIR = "/home/arnab/Work/data/DBPedia/propIndexFiles";

    /**
     * Delimiter to separate the URI and the lable of DBPedia entries
     */
    public static final String DBPEDIA_DATA_DELIMIT = "~!~";

    /**
     * only the URIs with the following header will be used for indexing
     */
    public static final String DBPEDIA_HEADER = "http://dbpedia.org/";

    /**
     * Filter out the YAGO links
     */
    public static final String YAGO_HEADER = "http://dbpedia.org/class/yago";

    // *****************INDEXING STRATEGIES
    // ************************************************

    /**
     * allowable text for indexing, do not index Chinese, Japanese, Korean, Russian etc labels
     */
    public static final String ALLOWED_ENGLISH_TEXT = "[^\\w_\\s()'.:,]";

    /**
     * Filter to remove certain punctuations from the uri
     */
    public static final String URI_FILTER = "[():,.\\s'-]";

    /**
     * Filter to remove certain punctuations from the labels
     */
    public static final String LABEL_FILTER = "[():,']";

    /**
     * change here to use different analyzers
     */
    // public static final Analyzer LUCENE_ANALYZER = new StandardAnalyzer(Version.LUCENE_40);

    /**
     * flag to determine whether to concat to old indices or recreate all from scratch
     */
    public static final boolean EMPTY_INDICES = true;

    /**
     * Flag to denote if indexing is to be done or query on old indices
     */
    public static final boolean INDEX_AGAIN = false;

    // *****************FETCH STRATEGIES
    // ***************************************************
    // tweaking these can dramatically effect the query response time

    /**
     * percentage length of common (non-fuzzy) prefix in the user query you want to match, higher value makes it to
     * search over smaller data matches not on all of them. Here it means 80% of the query term should contain in the
     * result sets
     */
    public static final float PREFIX_LENGTH_PERCENT = 0.1F;

    /**
     * default 50% similarity and above, lower this value to fetch even lesser similar items
     */
    public static final double SIMILARITY = 100.00;

    /**
     * change the value to fetch these many records, Lucene uses this to fetch maximum these many matching documents
     */
    public static final int MAX_RESULTS = 50;

    /**
     * Number of top k matching elements you wish to retrieve
     */
    // public static final int TOPK = 5;

    /**
     * Sample query to test the indexed DBPedia data
     */
    public static final String SAMPLE_QUERY = "shaw";

    // *****************IE Engines output locations
    // ***************************************************

    /**
     * location of the output file generated the IE Engine ReVerb
     */
    public static final String NELL_DATA_PATH = "/home/arnab/Work/data/NELL/Nell.csv";

    /**
     * Delimiter used to parse the ReVerb extracted tuples
     */
    public static final String REVERB_IE_DELIMIT = ";";

    /**
     * Delimiter used to parse the ReVerb extracted tuples
     */
    public static final String NELL_IE_DELIMIT = ",";

    /**
     * output location of the predicate list after calculating jaccard score for each
     */
    public static final String PREDICATE_FREQ_FILEPATH = "/home/arnab/Work/data/NELL/predFreq_2.txt";

    // *****************WEB INTERFACE
    // PARAMETES***************************************************

    /**
     * If this is turned on the then the system performs a predictive search else just a simple search based on the
     * input terms
     */
    public static final boolean PREDICTIVE_SEARCH_MODE = true;

    /**
     * only those entities with a match of value higher than this will be taken into consideration for further
     * processing.
     */
    public static final double THRESHOLD_SCORE = 80;

    // *****************Database Parameters
    // PARAMETES***************************************************
    public static final String INSERT_FACT_SQL =
        "INSERT INTO \"UNCERTAIN_KB\"(\"SUB\", \"PRED\", \"OBJ\", \"CONFIDENCE\") VALUES (?, ?, ?, ?)";

    public static final String INSERT_PROPERTY_DOMAIN_RANGE_SQL =
        "INSERT INTO \"PREDICATE_DOMAIN_RANGE\"(\"PREDICATE\", \"DOMAIN\", \"RANGE\") VALUES (?, ?, ?)";

    public static final String GET_WIKI_STAT = "select distinct entity from stats where anchor=?";

    public static final String GET_LINK_COUNT =
        "select count(*) as cnt  from link_anchors l, title_2_id t where t.title = ? and l.anchor=? and l.target = t.id";

    /**
     * SQL to insert a gold standard instance for NELL
     */
    public static final String INSERT_GOLD_STANDARD =
        "INSERT INTO goldStandard (E_SUB, E_PRED, E_OBJ, D_SUB, D_PRED, D_OBJ, SUB_LINK_CNT, OBJ_LINK_CNT ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    /**
     * SQL to insert a gold standard instance for ReVerb
     */
    public static final String INSERT_GOLD_STANDARD_REVERB =
        "INSERT INTO goldStandard_2 (E_SUB, E_PRED, E_OBJ, D_SUB, D_PRED, D_OBJ, SUB_LINK_CNT, OBJ_LINK_CNT ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    /**
     * SQL to insert a baseline instance for NELL
     */
    public static final String INSERT_BASE_LINE =
        "INSERT INTO baseLine (E_SUB, E_PRED, E_OBJ, D_SUB, D_PRED, D_OBJ ) VALUES (?, ?, ?, ?, ?, ?)";

    /**
     * SQL to insert a baseline instance for REVERB
     */
    public static final String INSERT_BASE_LINE_REVERB =
        "INSERT INTO baseLine_2 (E_SUB, E_PRED, E_OBJ, D_SUB, D_PRED, D_OBJ ) VALUES (?, ?, ?, ?, ?, ?)";

    /**
     * SQL to insert an axiom before running inference
     */
    public static final String INSERT_AXIOM_SQL =
        "INSERT INTO axioms (E_ENTITY, CANDIDATE, APRIORI, APOSTERIORI) VALUES (?, ?, ?, ?)";

    /**
     * SQL to update an axiom after running inference
     */
    public static final String UPDATE_AXIOM_SQL = "UPDATE axioms SET APOSTERIORI=? WHERE  E_ENTITY=? AND CANDIDATE=?";

    /**
     * given a title, fetch top surface forms
     */
    // public static final String GET_WIKI_SURFACE_FORMS_SQL =
    // "select l.anchor as anchor, count(*) as cnt from link_anchors l, title_2_id t where t.title=? and t.id=l.target group by l.anchor having cnt > ? order by cnt desc limit ?";

    public static final String GET_WIKI_SURFACE_FORMS_SQL = "select SF, PROB from surfaceForms where URI = ? ";

    /**
     * given a surface form, fetch top titles it refers to
     */
    public static final String GET_WIKI_TITLES_SQL =
        "select URI, SUM(COUNT) as cnt from wikiPrep where SF = ? group by BINARY URI order by cnt desc limit ?";

    public static final String GET_NELL_CONF =
        "select confidence from nell where subject = ? and predicate = ? and object = ?";

    // "select  t.title, count(*) as cnt from link_anchors l, title_2_id t where l.anchor=? and l.target=t.id group by t.title order by cnt desc limit 2";

    // "select URI from surfaceForms where SF =? order by PROB desc" ;
    // "select  t.title, count(*) as cnt from link_anchors l, title_2_id t where l.anchor=? and l.target=t.id group by t.title order by cnt desc limit ?";

    public static final String INSERT_SURFACE_FORMS_SQL =
        "INSERT INTO surfaceForms_2_uri (uri, surface, count) VALUES (?, ?, ?)";

    public static final String INSERT_DB_SURFACE_FORMS_SQL =
        "INSERT INTO surfaceForms (URI, SF, PROB) VALUES (?, ?, ?)";

    /**
     * fetch the top matching DBPedia predicates co-occurring with a given NELL predicate
     */
    // "select *, count(*) as cnt from goldStandardClean where E_PRED =? group by D_PRED order by cnt desc"
    // ;
    // "select count(*) as cnt, D_PRED from goldStandardClean where E_PRED =? group by D_PRED order by cnt desc";

    /**
     * fetch all the NELL predicates matched with the DBPedia dataset
     */
    public static final String GET_NELL_PREDICATES =
        "select E_PRED, count(*) as cnt from goldStandardClean group by E_PRED order by cnt desc";

    // "select predicate, count(*) as cnt from nell group by predicate order by cnt desc";

    // *****************OWL
    // PARAMETES***************************************************

    /**
     * namespace of the ontology to be used for creation of the axiom files
     */
    public static String ONTOLOGY_NAMESPACE = "http://dbpedia.org/ontology/";

    public static String DBPEDIA_NAMESPACE = "http://dbpedia.org/";

    public static String OIE_ONTOLOGY_NAMESPACE = "http://dws/OIE#";

    /**
     * DBPedia namespace
     */
    public static String ONTOLOGY_DBP_NS = ONTOLOGY_NAMESPACE; // + "Dbp#";

    /**
     * extraction engine namespace
     */
    public static String ONTOLOGY_EXTRACTION_NS = ONTOLOGY_NAMESPACE + "Extract#";

    public static String ONTOLOGY_EXTRACTION_CONCEPT_NS = OIE_ONTOLOGY_NAMESPACE + "Concept/";

    public static String ONTOLOGY_EXTRACTION_PREDICATE_NS = OIE_ONTOLOGY_NAMESPACE + "Predicate/";

    public static String ONTOLOGY_EXTRACTION_INSTANCE_NS = OIE_ONTOLOGY_NAMESPACE + "Instance/";

    public static String DBPEDIA_CONCEPT_NS = DBPEDIA_NAMESPACE + "ontology/";

    public static String DBPEDIA_PREDICATE_NS = DBPEDIA_NAMESPACE + "ontology/";

    public static String DBPEDIA_INSTANCE_NS = DBPEDIA_NAMESPACE + "resource/";

    /**
     * DBPedia TBOX info file
     */
    public static final String OWL_INPUT_FILE_PATH =
        "/home/arnab/Workspaces/SchemaMapping/EntityLinker/data/ontology/input/dbpediaGold.owl";

    /**
     * defines the confidence value namespace for the owl files
     */
    public static final String CONFIDENCE_VALUE_DEFINITION = "http://reasoner#confidence";

    /**
     * place where generated owl files are dumped. This file contains all the axioms on which reasoner runs
     */
    public static final String OWLFILE_CREATED_FROM_FACTS_OUTPUT_PATH =
        "/home/arnab/Workspaces/SchemaMapping/EntityLinker/data/ontology/output/assertions.owl";

    /**
     * place where generated owl files are dumped. This file contains all the axioms on which reasoner runs
     */
    public static final String OWLFILE_CREATED_FROM_ELOG_REASONER_OUTPUT_PATH =
        "/home/arnab/Workspaces/SchemaMapping/EntityLinker/data/ontology/output/aposteriori.owl";

    /**
     * Max weight an Axiom can have, basically recomputing the weights, w from probability, p using the formula [p =
     * (exp(w))/1+(exp(w))]. Assuming maximum probability an axiom to be 0.999999999
     */
    public static final double AXIOM_MAX_WEIGHT = 20.7232;

    /**
     * Min weight an Axiom can have, basically recomputing the weights, w from probability, p using the formula [p =
     * (exp(w))/1+(exp(w))]. Assuming minimum probability an axiom to be 0.000000001
     */
    public static final double AXIOM_MIN_WEIGHT = -20.7232;

    // from the UI you can run to create gold standard, as well as perform
    // inference. Setting it false will make it run as gold standard creation
    // mode
    public static final boolean INFERENCE_MODE = false;

    // ********** Experiments
    // *************************************************************
    /**
     * input set of data from NELL, with no intersection across triples
     */
    // public static final String NELL_DOMAIN_INPUT_FILE_PATH =
    // "/home/arnab/Work/data/NELL/all.csv";

    /**
     * input set of data from NELL, with some intersection across triples
     */
    public static final String NELL_DOMAIN_INPUT_FILE_PATH = "/home/arnab/Work/data/NELL/portion.csv";

    /**
     * read the above file randomly or make it false to do sequential read
     */
    public static final boolean RANDOM_READ = false;

    /**
     * input set of random triples from NELL
     */
    public static final String NELL_RANDOM_TRIPLE_DATA_SET = "/home/arnab/Work/data/NELL/randomTriples.csv";

    /**
     * number of nell triples to be considered.
     */
    public static final int RANDOM_TRIPLES_LIMIT = 8;

    // **************** WIKIPEDIA PARAMS ******************************

    public static final String WIKI_PAGE_HEADER = "http://en.wikipedia.org/wiki/";

    // anchors with atleast these many occurrence in wikipedia pointing to the
    // page
    public static final int ATLEAST_LINKS = 5;

    // take sentences with atmost these many words between them
    public static final int WORD_GAP = 5;

    public static final boolean IS_NELL = true;

    /**
     * flag to determine if to use weights or probabilities
     */

    /**
     * file I/O location
     */
    public static final String sample_dumps = "/output/ds_";

    public static final String DIRECTORY = "/home/arnab/Work/data/experiments/reasoning/newBL/ds_"
        + ExperimentAutomation.PREDICATE + "/";

    // public static final String INPUT_CSV_FILE = DIRECTORY + "goldBL_" +
    // PREDICATE + ".tsv";

    public static final String DOMCONF = DIRECTORY + "domConf.nell.dbpedia.db";

    public static final String RANCONF = DIRECTORY + "ranConf.nell.dbpedia.db";

    public static final String DELIMIT_INPUT_CSV = "\t";

    public static final String POST_FIX = "_";

    public static final String BL = "/home/arnab/Work/data/experiments/reasoning/newBL/blData.tsv";

    public static final String PSUBCONF_FILE = DIRECTORY + "psubConf.nell.dbpedia.db";

    /**
     * defines the batch size for the Data base operations
     */
    public static final int BATCH_SIZE = ExperimentAutomation.BATCH_SIZE;

    // select URI, SF, SUM(COUNT), (SUM(COUNT)/(select SUM(COUNT) from wikiPrep
    // where SF = 'satun')) as prob from wikiPrep where SF = 'satun' group by
    // URI order by prob asc;

    public static final String SILVER_STANDARD_DUMP =
        "/home/arnab/Work/data/NELL/ontology/GoldStandardPredicateTypes.tsv";

    public static final String AIRPEDIA_DUMP = "/home/arnab/Work/data/airpedia/airpedia-classes-en.nt";

    /**
     * annotated gold standard file of NELL triples
     */
    public static final String INPUT_CSV_FILE = sample_dumps + ExperimentAutomation.PREDICATE + "/goldBL_"
        + ExperimentAutomation.PREDICATE + ".tsv";

    /**
     * output owl file of the NELL input triples
     */
    public static final String OUTPUT_OWL_FILE = sample_dumps + ExperimentAutomation.PREDICATE + "/data.owl";

    /**
     * gold standard evidence file, post-fixed
     */
    public static final String GOLD_MLN_EVIDENCE = sample_dumps + ExperimentAutomation.PREDICATE
        + "/goldEvidencePostFixed.db";

    /**
     * gold standard evidence file, post-fixed
     */
    public static final String GOLD_MLN_EVIDENCE_ALL = sample_dumps + ExperimentAutomation.PREDICATE
        + "/goldEvidencePostFixedAll.db";

    /**
     * isTypeOf evidences of NELL entities
     */
    public static final String IS_OF_TYPE_CONF_NELL_EVIDENCE = sample_dumps + ExperimentAutomation.PREDICATE
        + "/isOfTypeConf.nell.db";

    /**
     * location of the term-concept pair probabilities file
     */
    public static final String APRIORI_PROB_FILE = sample_dumps + ExperimentAutomation.PREDICATE
        + "/sameAsLinksPrior.tsv";

    /**
     * SQL to fetch the probabilities of the same as links from terms to concepts
     */
    public static final String GET_WIKI_LINKS_APRIORI_SQL =
        "select  URI, (SUM(COUNT)/(select  SUM(COUNT) from wikiPrep  where SF =?)) as p from wikiPrep  where SF =? group by URI order by p desc limit ?";

    /**
     * TOPK candidates for the same as links probabilities
     */
    public static final int SAMEAS_TOPK = ExperimentAutomation.TOP_K_MATCHES;

    /**
     * nell assertions with confidences
     */
    public static final String NELL_CONFIDENCE_FILE = sample_dumps + ExperimentAutomation.PREDICATE
        + "/NELLTripleConfidences." + ExperimentAutomation.PREDICATE + ".csv";

    /**
     * location of different from MLN evidences
     */
    public static final String DIFFERENTFROM_DBPEDIA_EVIDENCE = sample_dumps + ExperimentAutomation.PREDICATE
        + "/differentFrom.dbpedia";

    /**
     * isOfType MLN evidence location
     */
    public static final String IS_OF_TYPE_DBPEDIA_EVIDENCE = sample_dumps + ExperimentAutomation.PREDICATE
        + "/isOfType.dbpedia";

    /**
     * Substitute placeholder for missing type information
     */
    public static final String UNTYPED = "UNTYPED";

    /**
     * base line created for the predicate
     */
    public static final String BL_DUMP = "resource/output/blSample." + ExperimentAutomation.PREDICATE + ".tsv";

    /**
     * annotated gold standard dump of the predicated. should contain 100 rows always
     */
    public static String FULL_GS_DUMP = "resource/input/ALL." + ExperimentAutomation.PREDICATE + ".tsv";

    /**
     * gold standard evidence file, post-fixed
     */
    public static final String SOURCE_TYPE_EVIDENCE = sample_dumps + ExperimentAutomation.PREDICATE + "/sourceType.db";

    /**
     * dom range evidence writer
     */
    public static final String DOM_RAN_EVIDENCE = sample_dumps + ExperimentAutomation.PREDICATE + "/domRanEvid.db";

    public static final Double DOMRAN_CONFIDENCE_THRESHOLD = 0.500;

    /**
     * value used to scale up and down the node values while working with the DBPedia tree
     */
    public static double TREE_PROPAGATION_FACTOR = ExperimentAutomation.PROPGTN_FACTOR;

    /**
     * flag to determine if to use weights or probabilities
     */
    public static boolean USE_LOGIT_FUNC = ExperimentAutomation.USE_LOGIT;

    public static boolean BOOTSTRAP = ExperimentAutomation.BOOTSTRAP;

    public static final String DOMAIN = "Domain";

    public static final String RANGE = "Range";

    public static final String DOMAIN_RANGE_PREFERENCE_FILE = sample_dumps + ExperimentAutomation.PREDICATE
        + "/domRanAlpha" + TREE_PROPAGATION_FACTOR + "." + String.valueOf(USE_LOGIT_FUNC) + ".out";

    public static final String DOMAIN_RANGE_BS_PREFERENCE_FILE = sample_dumps + ExperimentAutomation.PREDICATE
        + "/domRanAlphaBS" + TREE_PROPAGATION_FACTOR + "." + String.valueOf(USE_LOGIT_FUNC) + ".out";

    public static final String DOMAIN_RANGE_EVIDENCE_FILE = sample_dumps + ExperimentAutomation.PREDICATE
        + "/domRanEvidence.db";

    public static final String DOMAIN_RANGE_BS_EVIDENCE_FILE = sample_dumps + ExperimentAutomation.PREDICATE
        + "/domRanEvidenceBS.db";

    // Top surface forms for a given wikipedia page
    public static int TOP_ANCHORS = ExperimentAutomation.TOP_K_MATCHES;

    /**
     * Kernel Density estimates evidence file, post-fixed
     */
    public static final String KDE_EVIDENCE_ALL = sample_dumps + ExperimentAutomation.PREDICATE + "/KDEEvidenceAll.db";

    /**
     * gold standard evidence file, post-fixed
     */
    public static final String ALL_MLN_EVIDENCE = sample_dumps + ExperimentAutomation.PREDICATE + "/AllEvidence.db";

    /**
     * gold standard evidence file, post-fixed
     */
    public static final String ALL_MLN_EVIDENCE_T1 = sample_dumps + ExperimentAutomation.PREDICATE
        + "/AllEvidence_T1.db";

    public static final String BASIC_REASON_OUT_FILE = sample_dumps + ExperimentAutomation.PREDICATE + "/out.db";

    /**
     * gold standard evidence file, post-fixed
     */
    public static final String REASONER_OUTPUT_MLN_EVIDENCE = sample_dumps + ExperimentAutomation.PREDICATE
        + "/outAll_1.db";

    /**
     * flag to denote if the type information of DBPedia instances should be reloaded in Database or old data should be
     * used.
     */
    public static boolean RELOAD_DBPEDIA_TYPES = ExperimentAutomation.RELOAD_TYPE;

    /**
     * insert DBPedia types SQL
     */
    public static String INSERT_DBP_TYPES =
        "INSERT IGNORE INTO DBPEDIA_TYPES (DBPEDIA_INSTANCE, INSTANCE_TYPE) VALUES ( ?, ? )";

    public static final String GET_DBPTYPE = "select INSTANCE_TYPE from DBPEDIA_TYPES where DBPEDIA_INSTANCE=?";

    public static final double SCALE_WEIGHT = ExperimentAutomation.SCALE_WEIGHT;

    public static final int TOP_K_NUMERIC_PROPERTIES = ExperimentAutomation.TOP_K_NUMERIC_PROPERTIES;

    public static boolean ENGAGE_INTER_STEP = ExperimentAutomation.ENGAGE_INTER_STEP;

    public static String OUTLIER_DETECTION_TECHNIQUE = "KDE";

}
