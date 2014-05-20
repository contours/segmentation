package edu.mit.nlp.segmenter.dp;

import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableList;
import edu.mit.util.weka.LBFGSWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class implements the dynamic programming Bayesian segmentation using
 * the DCM language model.
 */
public class DPSeg {

    private boolean debug;
    private final DPDocumentList documents;
    private final List<Integer> segmentCounts;
    private final Map<DPDocument,Segmentation> segmentations;

    /**
     * @param texts
     * @param segmentCounts
     *
     */
    public DPSeg(List<List<List<String>>> texts, List<Integer> segmentCounts) {
        checkArgument(segmentCounts.size() == texts.size(), 
                "%s documents but %s segment counts specified", 
                texts.size(), segmentCounts.size());

        this.segmentCounts = segmentCounts;
        this.segmentations = new HashMap<>(texts.size());
        this.documents = new DPDocumentList(texts);

        this.debug = true;
    }

    private static Segmentation bestSegmentationOf(DPDocument doc, int numSegments) {

        double[][] bestScores = new double[numSegments+1][doc.sentenceCount+1];
        Segment[][] bestSegments = new Segment[numSegments+1][doc.sentenceCount+1];
        
        double[][] segLLs = new double[doc.sentenceCount+1][doc.sentenceCount+1];
        for (int start = 0; start < doc.sentenceCount; start++) {
            for (int length = 1; start+length <= doc.sentenceCount; length++) {
                segLLs[start][length] = doc.segmentLogLikelihood(new Segment(start, length));
            }
        }
        
        for (int end = 1; end <= doc.sentenceCount; end++) {
            bestScores[0][end] = -Double.MAX_VALUE;
        }
        for (int i = 1; i <= numSegments; i++) {
            for (int end = 0; end < i; end++) {
                bestScores[i][end] = -Double.MAX_VALUE;
            }
            for (int end = i; end <= doc.sentenceCount; end++) {
                double bestScore = -Double.MAX_VALUE;
                int bestStart = -1;
                for (int start = 0; start < end; start++) {
                    double score = bestScores[i-1][start] + segLLs[start][end-start];
                    if (score > bestScore) {
                        bestScore = score;
                        bestStart = start;
                    }
                }
                bestScores[i][end] = bestScore;
                bestSegments[i][end] = new Segment(bestStart, end-bestStart);
            }
        }
        
        // Working backward, build a list of the segmentation masses.
        List<Segment> bestSegmentation = new ArrayList<>(numSegments);
        bestSegmentation.add(bestSegments[numSegments][doc.sentenceCount]);
        for (int k = numSegments-1; k > 0; k--) {
            int remainingMass = doc.sentenceCount 
                    - bestSegmentation.stream().mapToInt(s -> s.length).sum();
            bestSegmentation.add(0, bestSegments[k][remainingMass]);
        }
        
        return new Segmentation(ImmutableList.copyOf(bestSegmentation));
    }
    
    /**
     *
     * @param prior
     * @return
     */
    public List<List<Integer>> segment(final double prior) {

        IntStream.range(0, this.documents.size()).forEach(index -> {
            final DPDocument doc = this.documents.get(index);
            final int numSegments = this.segmentCounts.get(index);
            
            doc.setPrior(prior);

            this.segmentations.put(doc, bestSegmentationOf(doc, numSegments));
        });
        
        
        return ImmutableList.copyOf(
                this.segmentations.values().stream()
                        .map(Segmentation::toList)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Estimates the parameters using a form of hard EM it computes the
     * best segmentation given the current parameters, then does a
     * gradient-based search for new parameters, and iterates.
     *
     * @param initialPrior
     * @return the new estimate of the prior
     */
    public double estimatePrior(final double initialPrior) {

        segment(initialPrior);

        PriorOptimizer optimizer = new PriorOptimizer();
        optimizer.setEPS(1e-5);
        optimizer.setNumCorrections(6); //can afford this because it's relatively easy optimization
        optimizer.setDebug(debug);

        double prior = initialPrior;
        int iteration = 0;
        double improvement;
        double logLikelihood = Double.MAX_VALUE;

        do {
            prior = optimizer.reestimatePrior(prior);
            segment(prior);

            improvement = logLikelihood - optimizer.getMinFunction();
            logLikelihood = optimizer.getMinFunction();

        } while (improvement > 0 && ++iteration < 20);

        return prior;
    }

    public void setDebug(boolean m_debug) {
        this.debug = m_debug;
    }

    private static int sum(List<Integer> integers) {
        return integers.stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Given the log of the Dirichlet prior, calculate the log-likelihood across
     * all documents. This is the objective function being maximized by the 
     * PriorOptimizer.
     * 
     * @param logPrior log of the prior being re-estimated.
     * @return the log-likelihood across all documents given this (log) prior
     */
    private double computeTotalLogLikelihood(final double logPrior) {
        return this.documents.stream().mapToDouble(doc ->
                this.computeForDocument(doc, logPrior, 
                        doc::segmentLogLikelihood)
        ).sum();
    }

    /**
     * Given the log of the Dirichlet prior, calculate the gradient of the 
     * log-likelihood across all documents. This is the gradient of the objective
     * function being maximized by the PriorOptimizer.
     * 
     * @param logPrior log of the prior being re-estimated.
     * @return the gradient of he log-likelihood across all documents
     */
    private double computeGradient(final double logPrior) {
        return logPrior + this.documents.stream().mapToDouble(doc ->
                this.computeForDocument(doc, logPrior, 
                        doc::segmentLogLikelihoodGradient)
        ).sum();
    }
    
    private interface SegmentFunction {
        double apply(Segment segment);
    }
    
    private double computeForDocument(DPDocument doc, final double logPrior, SegmentFunction f) {
        doc.setPrior(Math.exp(logPrior));

        return this.segmentations.get(doc).stream()
                .mapToDouble(segment -> f.apply(segment))
                .sum();
    }

    /**
     * A class for LBFGS optimization of the priors.
     * See http://en.wikipedia.org/wiki/Limited-memory_BFGS
     */
    private class PriorOptimizer extends LBFGSWrapper {

        public PriorOptimizer() {
            super(1); // just one parameter, the prior
        }

        // we're doing the argmin, so invert it
        @Override
        public double objectiveFunction(double[] estimate) {
            return -computeTotalLogLikelihood(estimate[0]);
        }

        @Override
        public double[] evaluateGradient(double[] estimate) {
            return new double[]{-computeGradient(estimate[0])};
        }

        /**       
         * Note that the optimizer uses log-priors everywhere. The reason is 
         * that the log of the prior is in [-inf,inf], while the prior itself
         * is in [0,inf]. Since the LBFGS engine doesn't take constraints, it's 
         * better to search in log space.
         */
        private double reestimatePrior(double prior) {
            this.setEstimate(new double[]{Math.log(prior)});
            return Math.exp(this.findArgmin()[0]);
        }
    }

}
