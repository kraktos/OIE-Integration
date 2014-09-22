package code.dws.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import code.dws.relationMap.workflow2.ClusteringWithDbpedia;
import code.dws.wordnet.SimilatityWebService;

public class Worker implements Runnable {

	// define Logger
	public static Logger logger = Logger.getLogger(Worker.class.getName());
	private boolean check;

	private List<String> arg1;
	private List<String> arg2;
	private BufferedWriter writer;

	public Worker(List<String> arg1, List<String> arg2, BufferedWriter writer,
			boolean check) {
		this.arg1 = arg1;
		this.arg2 = arg2;
		this.writer = writer;
		this.check = check;
	}

	@Override
	public void run() {
		try {
			getPairwiseSimScore();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

	}

	public void getPairwiseSimScore() throws IOException {

		logger.info("Size of Arg1 = " + arg1.size());
		logger.info("Size of Arg2 = " + arg2.size());

		long cnt = 0;
		long val = (check) ? (arg1.size() * (arg2.size() - 1) / 2) : (arg1
				.size() * arg2.size());

		for (int outer = 0; outer < arg1.size(); outer++) {

			for (int inner = 0; inner < arg2.size(); inner++) {

				if (check) {
					if (outer < inner) {
						cnt++;
						try {
							// based on Wordnet scores
							SimilatityWebService.getWordNetSimilarityScores(
									arg1.get(outer), arg2.get(inner), writer);
						} catch (Exception e) {
							ClusteringWithDbpedia.logger.error(e.getMessage());
						}
					}
				} else {
					cnt++;
					try {
						// based on Wordnet scores
						SimilatityWebService.getWordNetSimilarityScores(
								arg1.get(outer), arg2.get(inner), writer);
					} catch (Exception e) {
						ClusteringWithDbpedia.logger.error(e.getMessage());
					}
				}
			}

			logger.info("Completed " + (double) 100 * cnt / val + " %");

			writer.flush();
		}

		try {
			writer.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

	}

}
