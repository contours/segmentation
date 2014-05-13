package edu.mit.nlp.segmenter.dp;

import edu.mit.util.stats.FastDigamma;
import edu.mit.util.stats.FastDoubleGamma;
import edu.mit.util.stats.FastGamma;
import edu.mit.util.stats.Stats;
import edu.mit.util.weka.LBFGSWrapper;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

/**
 * This class implements the dynamic programming Bayesian segmentation, for both
 * DCM and MAP language models.  *
 * <P>
 * Now with EM estimation of priors. Note that we use log-priors everywhere. The
 * reason is that the log of the prior is in [-inf,inf], while the prior itself
 * is in [0,inf]. Since my LBFGS engine doesn't take constraints, it's better to
 * search in log space. This requires only a small modification to the gradient
 * computation.
*
 */
public class DPSeg {

    private static final double MAX_LOG_DISPERSION = 10;

    private boolean useDuration;
    private boolean debug;
    private final DPDocument[] documents;
    private final int[][] segmentations;
    private final int[] segmentCounts;
    private final FastDigamma digamma;
    private final FastGamma fastGamma;

    /**
     * @param documents The documents to segment
     * @param segmentCounts number of segments per document
     *
     */
    public DPSeg(DPDocument[] documents, int[] segmentCounts) {
        this.useDuration = true;
        this.documents = documents;
        this.segmentations = new int[this.documents.length][];
        this.segmentCounts = segmentCounts;
        for (int i = 0; i < this.documents.length; i++) {
            segmentations[i] = new int[this.segmentCounts[i]];
        }
        this.debug = true;
        this.digamma = new FastDigamma();

        int startSize = Stream.of(documents).mapToInt(doc ->
                (int) DoubleStream.of(doc.getCumulativeSums()).max().getAsDouble())
                .sum();
        fastGamma = new FastDoubleGamma(startSize * 2, .6f);
    }

    /**
     * segEM estimates the parameters using a form of hard EM it computes the
     * best segmentation given the current parameters, then does a
     * gradient-based search for new parameters, and iterates. As an argument it
     * takes the initial settings, in log terms. one idea of how to speed this
     * up is to only recompute the segmentation for a subset of files. or, just
     * call segem on a few files, then call the final segmentation on all of
     * them. we could add a class member variable indicating "active" files, and
     * then only apply segment(), computeGradient(), and computeLL() to those
     * files. by default all files would be active.
     *
     * @param initialPrior
     * @param initialDispersion 
     * @return  
     */
    public double[] segEM(double initialPrior, double initialDispersion) {
        
        segment(initialPrior, initialDispersion);

        PriorOptimizer optimizer = new PriorOptimizer();
        optimizer.setEPS(1e-5);
        optimizer.setNumCorrections(6); //can afford this because it's relatively easy optimization
        
        int iteration = 0;
        double newLogPrior;
        double newLogDispersion;
        double improvement;
        double logLikelihood = Double.MAX_VALUE;

        do {
            optimizer.setEstimate(
                    new double[]{
                        Math.log(initialPrior), 
                        Math.log(initialDispersion) });
            optimizer.setMaxIteration(200);
            optimizer.setDebug(debug);

            double[] argmin = optimizer.findArgmin();
            newLogPrior = argmin[0];
            newLogDispersion = argmin[1];
            segment(Math.exp(newLogPrior), Math.exp(newLogDispersion));

            improvement = logLikelihood - optimizer.getMinFunction();
            logLikelihood = optimizer.getMinFunction();

        } while (improvement > 0 && ++iteration < 20);
        
        return new double[]{newLogPrior, newLogDispersion};
    }

    /**
     * segment each document in the dataset.
     *
     * @param prior
     * @param dispersion
     * @return the results for each document. kind of a bad design, it ought to
     * just return the segmentation.
     *
     */
    public int[][] segment(double prior, double dispersion) {

        for (int docctr = 0; docctr < this.documents.length; docctr++) {
            DPDocument doc = this.documents[docctr];
            doc.setGamma(fastGamma);
            doc.setPrior(prior);

            int numSegments = segmentCounts[docctr];
            int numSentences = doc.getSentenceCount(); //number of sentences
            
            //this is the DP
            double C[][] = new double[numSegments + 1][numSentences + 1];
            int B[][] = new int[numSegments + 1][numSentences + 1];

            //the semantics of C are:
            //C[i][t] = the ll of the best segmentation of x_0 .. x_[t-1] into i segments

            double[][] seglls = new double[numSentences + 1][numSentences + 1];
            for (int t = 0; t <= numSentences; t++) {
                for (int t2 = 0; t2 < t; t2++) {
                    seglls[t][t2] += doc.segLogLikelihood(t2 + 1, t, prior);
                }
            }
            C[0][0] = 0;
            B[0][0] = 0;
            for (int t = 1; t <= numSentences; t++) {
                C[0][t] = -Double.MAX_VALUE;
                B[0][t] = 1;
            }
            for (int i = 1; i <= numSegments; i++) {
                for (int t = 0; t < i; t++) {
                    C[i][t] = -Double.MAX_VALUE;
                    B[i][t] = -1;
                }
                for (int t = i; t <= numSentences; t++) {
                    double best_val = -Double.MAX_VALUE;
                    int best_idx = -1;
                    for (int t2 = 0; t2 < t; t2++) {
                        double score = C[i - 1][t2] + seglls[t][t2];
                        if (useDuration) {
                            double[] pdur = computePDur(
                                    numSentences, 
                                    (double) numSentences / numSegments, 
                                    Math.log(dispersion)); 
                            score += pdur[t - t2];
                        }
                        if (score > best_val) {
                            best_val = score;
                            best_idx = t2;
                        }
                    }
                    C[i][t] = best_val;
                    B[i][t] = best_idx;
                }
            }

            segmentations[docctr][numSegments - 1] = B[numSegments][numSentences];
            for (int k = numSegments - 1; k > 0; k--) {
                segmentations[docctr][k - 1] = B[k][segmentations[docctr][k]];
            }
        }
        return segmentations;
    }

    private double computeTotalLogLikelihood(double[] estimate) {
        double logLikelihood = 0;
        
        double logPrior = estimate[0];
        double logDispersion = estimate[1];
        
        for (int docctr = 0; docctr < this.documents.length; docctr++) {
            this.documents[docctr].setGamma(fastGamma);
            this.documents[docctr].setPrior(Math.exp(logPrior));

            int numSegments = segmentCounts[docctr];
            int numSentences = this.documents[docctr].getSentenceCount();
            
            double[] pdur = computePDur(numSentences, (double) numSentences / numSegments, logDispersion);

            for (int k = 0; k < numSegments - 1; k++) {
                if (this.useDuration) {
                    logLikelihood += pdur[this.segmentations[docctr][k + 1] - this.segmentations[docctr][k]];
                }
                logLikelihood += this.documents[docctr].segLogLikelihood(
                        this.segmentations[docctr][k] + 1, 
                        this.segmentations[docctr][k + 1], 
                        logPrior);
            }
            if (this.useDuration) {
                logLikelihood += pdur[numSentences - this.segmentations[docctr][numSegments - 1]];
            }
            logLikelihood += this.documents[docctr].segLogLikelihood(
                    this.segmentations[docctr][numSegments - 1] + 1, 
                    this.documents[docctr].getSentenceCount(), 
                    logPrior);
        }
        return logLikelihood;
    }

    private double[] computeGradient(double[] estimate){

        double logPrior = estimate[0];
        double logDispersion = estimate[1];
        
        for (int docctr = 0; docctr < this.documents.length; docctr++) {
            this.documents[docctr].setGamma(fastGamma);
            this.documents[docctr].setPrior(Math.exp(logPrior));

            int numSegments = segmentCounts[docctr];
            int numSentences = this.documents[docctr].getSentenceCount();

            for (int k = 0; k < numSegments - 1; k++){
                if (this.useDuration){
                    logDispersion += Stats.computeDispersionGradient(
                            this.segmentations[docctr][k+1] - this.segmentations[docctr][k],
                            (double) numSentences / numSegments,
                            logDispersion,
                            digamma);
                }
                logPrior += this.documents[docctr].segLLGradientExp(
                        this.segmentations[docctr][k]+1,
                        this.segmentations[docctr][k+1],
                        logPrior);
            }
            if (useDuration){
                logDispersion += Stats.computeDispersionGradient(
                        numSentences - this.segmentations[docctr][numSegments-1],
                        (double) numSentences / numSegments,
                        logDispersion,
                        digamma);
            }
            logPrior += this.documents[docctr].segLLGradientExp(
                    this.segmentations[docctr][numSegments - 1]+1,
                    this.documents[docctr].getSentenceCount(),
                    logPrior);
        }
        return new double[]{logPrior, logDispersion};
    }

    public void setDebug(boolean m_debug) {
        this.debug = m_debug;
    }

    public void setUseDuration(boolean use_duration) {
        this.useDuration = use_duration;
    }

    private double[] computePDur(int numSentences, double meanDuration, double logDispersion) {
        double pdur[] = new double[numSentences + 1];
        if (logDispersion > MAX_LOG_DISPERSION) {
            logDispersion = MAX_LOG_DISPERSION;
        }
        for (int i = 0; i <= numSentences; i++) {
            pdur[i] = Stats.myLogNBinPdf2(i, meanDuration, Math.exp(logDispersion));
        }
        return pdur;
    }

    /**
     * A class for LBFGS optimization of the priors
     *
     */
    private class PriorOptimizer extends LBFGSWrapper {

        public PriorOptimizer() {
            super(2); // 2 parameters: prior + dispersion
        }

        // we're doing the argmin, so invert it
        @Override
        public double objectiveFunction(double[] estimate) {
            return -computeTotalLogLikelihood(estimate);
        }

        @Override
        public double[] evaluateGradient(double[] estimate) {
            return DoubleStream.of(computeGradient(estimate)).map(d -> -d).toArray();
        }
    }
}
