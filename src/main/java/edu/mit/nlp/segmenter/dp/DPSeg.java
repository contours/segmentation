package edu.mit.nlp.segmenter.dp;

import cc.mallet.optimize.LimitedMemoryBFGS;
import cc.mallet.optimize.OptimizationException;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import segmentation.Segment;
import segmentation.Segmentation;

/**
 * This class implements the dynamic programming Bayesian segmentation using
 * the DCM language model.
 */
public class DPSeg {

    private final DPDocumentMap documents;
    private final Map<String,Integer> segmentCounts;
    private final Map<String,Segmentation> segmentations;

    /**
     * @param texts
     * @param segmentCounts
     *
     */
    public DPSeg(Map<String,List<List<String>>> texts, Map<String,Integer> segmentCounts) {
        checkArgument(segmentCounts.keySet().containsAll(texts.keySet()));

        this.segmentCounts = segmentCounts;
        this.segmentations = new HashMap<>(texts.size());
        this.documents = new DPDocumentMap(texts);
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
    public Map<String,Segmentation> segment(final double prior) {

        this.documents.keySet().forEach(key -> {
            final DPDocument doc = this.documents.get(key);
            final int numSegments = this.segmentCounts.get(key);
            
            doc.setPrior(prior);

            this.segmentations.put(key, bestSegmentationOf(doc, numSegments));
        });
        
        return ImmutableMap.copyOf(this.segmentations);
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

        Optimizable optimizable = new Optimizable(initialPrior); 
        LimitedMemoryBFGS optimizer = new LimitedMemoryBFGS(optimizable);
        
        int iteration = 0;
        double logLikelihood = -Double.MAX_VALUE;
        double improvement;
        
        do {
            try {
                optimizer.optimize();
            } catch (OptimizationException e) {
                // Line search could not step in the current direction. This is 
                // not necessarily cause for alarm. Sometimes this happens close 
                // to the maximum, where the function may be very flat. So we 
                // assume it means we have converged.
                break;
            }
            improvement = optimizable.logLikelihood - logLikelihood;
            logLikelihood = optimizable.logLikelihood;
            
            segment(optimizable.prior);
            optimizer.reset();

            iteration++;
            
        } while (improvement > 0 && iteration++ < 20);
        
        return Math.exp(optimizable.logPrior);
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
        return this.documents.entrySet().stream().mapToDouble(e ->
                this.computeForDocument(e, logPrior, 
                        e.getValue()::segmentLogLikelihood)
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
        return this.documents.entrySet().stream().mapToDouble(e ->
                this.computeForDocument(e, logPrior, 
                        e.getValue()::segmentLogLikelihoodGradient)
        ).sum();
    }
    
    private interface SegmentFunction {
        double apply(Segment segment);
    }
    
    private double computeForDocument(Map.Entry<String,DPDocument> e, 
            final double logPrior, SegmentFunction f) {
        DPDocument doc = e.getValue();
        doc.setPrior(Math.exp(logPrior));

        return this.segmentations.get(e.getKey()).stream()
                .mapToDouble(segment -> f.apply(segment))
                .sum();
    }

    private class Optimizable 
    implements cc.mallet.optimize.Optimizable.ByGradientValue {

        private double prior;
        private double logPrior;
        private double logLikelihood;
        
        private Optimizable(double prior) {
            this.prior = prior;
            this.logPrior = Math.log(prior);
        }
        
        @Override
        public double getValue() {
            this.logLikelihood = computeTotalLogLikelihood(this.logPrior);
            return this.logLikelihood;
        }

        @Override
        public void getValueGradient(double[] gradient) {
            gradient[0] = computeGradient(this.logPrior);
        }

        @Override
        public int getNumParameters() {
            return 1;
        }

        @Override
        public void getParameters(double[] parameters) {
            parameters[0] = this.logPrior;
        }

        @Override
        public double getParameter(int index) {
            checkArgument(index==0);
            return this.logPrior;
        }

        @Override
        public void setParameters(double[] parameters) {
            this.logPrior = parameters[0];
            this.prior = Math.exp(parameters[0]);
        }

        @Override
        public void setParameter(int index, double parameter) {
            checkArgument(index==0);
            this.logPrior = parameter;
            this.prior = Math.exp(parameter);
        }
        
    }
    


}
