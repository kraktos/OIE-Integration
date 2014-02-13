
package code.dws.markovLogic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import code.dws.core.DistantSupervised;
import code.dws.dao.Pair;
import code.dws.utils.Constants;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

public class PreferenceBuilder {

    private OWLClass THING = null;
    private OWLDataFactory factory;

    private PelletReasoner reasoner;
    private OWLOntology ontology;
    private OWLOntologyManager manager;

    private HashMap<String, Double> domainFrequencies = new HashMap<String, Double>();
    private HashMap<String, Double> rangeFrequencies = new HashMap<String, Double>();

    private final static String OPREFIX = "http://dbpedia.org/ontology/";
    private final static String SPREFIX = "DBP#ontology/";

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        PreferenceBuilder pb = new PreferenceBuilder(
                "/home/arnab/Work/data/DBPedia/ontology/dbpediaTBox.owl");

        System.out.println("Builiding preferences for " + args[0]);
        // initiate the dom range un supervised learning technique
        pb.computeDomRan(args);
    }

    /**
     * method to learn the domain and range from the data set from the OIE
     * 
     * @param args
     * @throws IOException
     */
    public void computeDomRan(final String[] args) throws IOException {

        BufferedWriter domRanEvidenceWriter = new BufferedWriter(new FileWriter(
                Constants.DOM_RAN_EVIDENCE));

        DistantSupervised distSup = new DistantSupervised(args);
        distSup.learnDomRanFromFullData();

        List<Pair<String, Double>> domPairs = distSup.getTopDomainClass(0);
        List<Pair<String, Double>> ranPairs = distSup.getTopRangeClass(0);

        // iterate and populate domain values
        for (Pair<String, Double> domValPair : domPairs) {
            if (domValPair.getSecond() > Constants.DOMRAN_CONFIDENCE_THRESHOLD) {
                if (domValPair.getFirst().equals(Constants.UNTYPED))
                    this.addDomainFrequency(domValPair.getFirst(),
                            domValPair.getSecond());
                else
                    this.addDomainFrequency(Constants.ONTOLOGY_NAMESPACE + domValPair.getFirst(),
                            domValPair.getSecond());
            }
        }
        this.printDomainPreferences(domRanEvidenceWriter);

        // iterate and populate range values
        for (Pair<String, Double> ranValPair : ranPairs) {
            if (ranValPair.getSecond() > Constants.DOMRAN_CONFIDENCE_THRESHOLD) {
                if (ranValPair.getFirst().equals(Constants.UNTYPED))
                    this.addRangeFrequency(ranValPair.getFirst(), ranValPair.getSecond());
                else
                    this.addRangeFrequency(Constants.ONTOLOGY_NAMESPACE + ranValPair.getFirst(),
                            ranValPair.getSecond());
            }
        }
        this.printRangePreferences(domRanEvidenceWriter);

        // close the stream
        domRanEvidenceWriter.close();
        System.out.println("DOne writing to " + Constants.DOM_RAN_EVIDENCE);
    }

    private void printDomainPreferences(BufferedWriter domRanEvidenceWriter) throws IOException {
        this.printPreferences(this.domainFrequencies, "Domain", domRanEvidenceWriter);
    }

    private void printRangePreferences(BufferedWriter domRanEvidenceWriter) throws IOException {
        this.printPreferences(this.rangeFrequencies, "Range", domRanEvidenceWriter);
    }

    private void printPreferences(HashMap<String, Double> frequencies, String predicateSubfix,
            BufferedWriter domRanEvidenceWriter) throws IOException {

        // get all the classes in the ontology
        Set<OWLClass> concepts = this.ontology.getClassesInSignature();

        // iterate them
        for (OWLClass dbPediaClass : concepts) {

            String classStr = dbPediaClass.getIRI().toURI().toString();

            // only if it is of "dbpedia.org/ontology" format
            if (classStr.indexOf(Constants.ONTOLOGY_NAMESPACE) != -1) {

                // get its siblings
                Set<OWLClass> siblingConcepts = getSiblings(dbPediaClass);

                // iterate the siblings
                for (OWLClass sibling : siblingConcepts) {

                    String siblingClassStr = sibling.getIRI().toURI().toString();

                    if (siblingClassStr.indexOf(Constants.ONTOLOGY_NAMESPACE) != -1) {

                        // Case I : both are in learned list of dom range values
                        if (frequencies.containsKey(classStr) &&
                                frequencies.containsKey(siblingClassStr)
                                && frequencies.get(classStr) > frequencies.get(siblingClassStr)) {
                            domRanEvidenceWriter.write(formatPrefLine(predicateSubfix,
                                    classStr, siblingClassStr) + "\n");
                        }

                        // Case II : only the class is in learned list of dom
                        // range values
                        if (frequencies.containsKey(classStr)
                                && !frequencies.containsKey(siblingClassStr)) {
                            domRanEvidenceWriter.write(formatPrefLine(predicateSubfix,
                                    classStr, siblingClassStr) + "\n");
                        }
                    }
                }

                // Case III : special case of Case II, where preference
                // over UNTYPED ones are stated
                if (frequencies.containsKey(classStr) && frequencies.containsKey(Constants.UNTYPED)) {

                    if (isDirectSubClsOfThing(dbPediaClass)) {
                        if (frequencies.get(Constants.UNTYPED) > frequencies.get(classStr)) {
                            domRanEvidenceWriter.write(formatPrefLine(predicateSubfix,
                                    Constants.UNTYPED,
                                    classStr)
                                    + "\n");
                        } else {
                            domRanEvidenceWriter.write(formatPrefLine(predicateSubfix,
                                    classStr,
                                    Constants.UNTYPED)
                                    + "\n");
                        }
                    }
                }
            }

        } // end of for loop
    }

    private boolean isDirectSubClsOfThing(OWLClass c) {
        Set<OWLClass> superConcepts = reasoner.getSuperClasses(c, true).getFlattened();
        if (superConcepts.size() == 1 && superConcepts.contains(THING))
            return true;
        return false;
    }

    private String formatPrefLine(String subfix, String c1, String c2) {       
        c1 = c1.replace(OPREFIX, SPREFIX);
        c2 = c2.replace(OPREFIX, SPREFIX);
        return "prefer" + subfix + "(\"" + c1 + "\", \"" + c2 + "\")";
    }

    private Set<OWLClass> getSiblings(OWLClass c) {

        Set<OWLClass> superConcepts = reasoner.getSuperClasses(c, true).getFlattened();
        Set<OWLClass> siblingConcepts = new HashSet<OWLClass>();

        // System.out.println("Computing siblings for " + c);

        for (OWLClass superConcept : superConcepts) {
            /*
             * if (superConcept.equals(THING)) {
             * siblingConcepts.addAll(getLonelyTopConcepts()); }
             */
            // else {
            Set<OWLClass> siblings = reasoner.getSubClasses(superConcept, true).getFlattened();
            siblingConcepts.addAll(siblings);
            // }
        }
        siblingConcepts.remove(c);
        return siblingConcepts;

    }

    private Set<OWLClass> getLonelyTopConcepts() {
        Set<OWLClass> upper = reasoner.getSubClasses(THING, true).getFlattened();
        return upper;
    }

    private void addDomainFrequency(String conceptId, double frequency) {
        this.addFrequency(conceptId, frequency, domainFrequencies);
    }

    private void addRangeFrequency(String conceptId, double frequency) {
        this.addFrequency(conceptId, frequency, rangeFrequencies);
    }

    private void addFrequency(String conceptId, double frequency,
            HashMap<String, Double> frequencies) {
        frequencies.put(conceptId, frequency);
    }

    public PreferenceBuilder(String path) {

        manager = OWLManager.createOWLOntologyManager();
        File ontologyFile = new File(path);

        try {
            ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);
            PelletReasonerFactory reasonerFactory = new PelletReasonerFactory();
            reasoner = reasonerFactory.createReasoner(ontology);
            factory = this.manager.getOWLDataFactory();
            System.out.println("Loaded " + ontology.getOntologyID());
            this.THING = factory.getOWLThing();
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

}
