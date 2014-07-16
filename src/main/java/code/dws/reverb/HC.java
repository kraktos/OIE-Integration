package code.dws.reverb;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import ch.usi.inf.sape.hac.HierarchicalAgglomerativeClusterer;
import ch.usi.inf.sape.hac.agglomeration.AgglomerationMethod;
import ch.usi.inf.sape.hac.agglomeration.CompleteLinkage;
import ch.usi.inf.sape.hac.dendrogram.Dendrogram;
import ch.usi.inf.sape.hac.dendrogram.DendrogramBuilder;
import ch.usi.inf.sape.hac.experiment.DefaultDissimilarityMeasure;
import ch.usi.inf.sape.hac.experiment.DefaultExperiment;
import ch.usi.inf.sape.hac.experiment.DissimilarityMeasure;
import ch.usi.inf.sape.hac.experiment.Experiment;

public class HC {
	static List<String> props;
	static Map<Pair<String, String>, Double> map;

	public static void main(String[] args) {

		init();

		Experiment experiment = pickExperiment(props, map);
		DissimilarityMeasure dissimilarityMeasure = pickDissimilarityMeasure();
		AgglomerationMethod agglomerationMethod = pickAgglomerationMethod();

		DendrogramBuilder dendrogramBuilder = new DendrogramBuilder(
				experiment.getNumberOfObservations());

		HierarchicalAgglomerativeClusterer clusterer = new HierarchicalAgglomerativeClusterer(
				experiment, dissimilarityMeasure, agglomerationMethod);

		clusterer.cluster(dendrogramBuilder);

		Dendrogram dendogram = dendrogramBuilder.getDendrogram();
		dendogram.dump();
	}

	private static void init() {

		KClustersAlgo.init();
		props = KClustersAlgo.getReverbProperties();
		map = KClustersAlgo.getScoreMap();

	}

	private static AgglomerationMethod pickAgglomerationMethod() {

		return new CompleteLinkage();
	}

	private static DissimilarityMeasure pickDissimilarityMeasure() {

		return new DefaultDissimilarityMeasure();
	}

	private static Experiment pickExperiment(List<String> props,
			Map<Pair<String, String>, Double> map2) {

		DefaultExperiment ex = new DefaultExperiment();
		ex.setObservations(props);
		ex.setValues(map);

		return ex;
	}
}
