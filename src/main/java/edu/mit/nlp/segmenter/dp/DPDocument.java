package edu.mit.nlp.segmenter.dp;

import edu.mit.util.stats.FastDCM;
import edu.mit.util.stats.FastDigamma;
import edu.mit.util.stats.FastGamma;

public class DPDocument {

    private final double[][] sentences;
    private final int vocabularySize;
    private final double[][] cumSums;
    private final double[] cumSum;
    private final FastDCM fastdcm;
    private final FastDigamma digamma;

    /**
     * @param sentences an matrix representation of the document. there are
     * <code>sents.length</code> sentences, and each row of <code>sents</code>
     * is an array of size W (the size of the vocabulary).
     *
     */
    public DPDocument(double[][] sentences) {
        this.sentences = sentences;

        this.vocabularySize = this.sentences[0].length;
        cumSum = new double[getSentenceCount() + 1];
        cumSums = new double[getSentenceCount() + 1][this.vocabularySize];
        //fill 'em up
        for (int w = 0; w < this.vocabularySize; w++) {
            cumSums[0][w] = 0;
        }
        cumSum[0] = 0;

        makeCumulCounts();

        digamma = new FastDigamma();
        fastdcm = new FastDCM(1, this.vocabularySize, true);
    }
    
    /**
     * compute the gradient of the log-likelihood for a segment, under the DCM
     * model
     *
     * @param start the index of the first sentence in the segment
     * @param end the index of the last sentence in the segment
     * @param logprior the log of the symmetric dirichlet prior to use
     * @return
     *
     */
    public double segLLGradientExp(int start, int end, double logprior) {
        double prior = Math.exp(logprior);
        return prior * segDCMGradient(start, end, prior);
    }

    /**
     * Builds up the cumulative counts, a representation that facilitates fast
     * computation later.
     *
     */
    private void makeCumulCounts() {
        cumSum[0] = 0;
        for (int t = 0; t < getSentenceCount(); t++) {
            cumSum[t + 1] = 0;
            for (int w = 0; w < this.vocabularySize; w++) {
                cumSums[t + 1][w] = cumSums[t][w] + this.sentences[t][w];
                cumSum[t + 1] += cumSums[t + 1][w];
            }
        }
    }

    /**
     * compute the gradient of the log-likelihood for a segment, under the DCM
     * model
     *
     * @param start the index of the first sentence in the segment
     * @param end the index of the last sentence in the segment
     * @param prior the log of the symmetric dirichlet prior to use
     *
     */
    private double segDCMGradient(int start, int end, double prior) {
        if (prior == 0) {
            return Double.MAX_VALUE;
        }
        double out = this.vocabularySize
                * (digamma.digamma(this.vocabularySize * prior)
                - digamma.digamma(cumSum[end] - cumSum[start - 1] + this.vocabularySize * prior)
                - digamma.digamma(prior));
        for (int i = 0; i < this.vocabularySize; i++) {
            out += digamma.digamma(cumSums[end][i] - cumSums[start - 1][i] + prior);
        }
        return out;
    }

    final int getSentenceCount() {
        return this.sentences.length;
    }

    /**
     * If you have multiple documents, you might want to share the cache for the
     * gamma function across all documents. This lets you tell it to use a
     * specific FastGamma cache.
     *
     * @param fastGamma the caching fastGamma object
     *
     */
    void setGamma(FastGamma fastGamma) {
        fastdcm.setGamma(fastGamma);
    }

    /**
     * @param prior the value of the symmetric Dirichlet prior
     *
     */
    void setPrior(double prior) {
        fastdcm.setPrior(prior);
    }

    /**
     * compute the log likelihood of a segment under the DCM model
     *
     * @param start the index of the first sentence in the segment
     * @param end the index of the last sentence in the segment
     * @param prior the symmetric Dirichlet prior to use
     *
     *
     */
    double segLogLikelihood(int start, int end, double prior) {
        if (prior == 0) {
            return -Double.MAX_VALUE;
        }
        int[] counts = new int[this.vocabularySize];
        for (int i = 0; i < this.vocabularySize; i++) {
            counts[i] = (int) (cumSums[end][i] - cumSums[start - 1][i]);
        }
        return fastdcm.logDCM(counts);
    }

    double[] getCumulativeSums() {
        return cumSum;
    }
}
