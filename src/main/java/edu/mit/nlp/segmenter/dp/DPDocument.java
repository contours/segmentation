package edu.mit.nlp.segmenter.dp;

import segmentation.Segment;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.math3.special.Gamma;
import segmentation.Utils;

/**
 * DPDocument stores document statistics and provides methods for efficient
 * inference of the maximum-likelihood segmentation via dynamic programming.
 * 
 * @author Jacob Eisenstein <jacobe@gatech.edu>
 * @author Ryan Shaw <ryanshaw@unc.edu>
 */
public class DPDocument {

    /**
     * A list of all the unique vocabulary (types) in this document.
     */
    final ImmutableList<String> vocabulary;

    /**
     * A list of cumulative word counts, one for each sentence in the document.
     * Each item in the list is a {@link com.google.common.collect.Multiset}
     * of word usage counts through that sentence. So, the first item is word 
     * usage counts for only the first sentence, and the last item is word 
     * usage counts for the entire document.
     */
    final ImmutableList<ImmutableMultiset<String>> cumulativeWordUsageCounts;

    /**
     * A list of total word (token) counts, one for each sentence in the document.
     * Each item in the list is the number of words (tokens) in the document
     * through that sentence. So, the first item is the number of words (tokens)
     * in the first sentence, and the last item is the number of words (tokens)
     * in the whole document.
     */
    final ImmutableList<Integer> cumulativeTokenCounts;

    /**
     * The number of sentences in the document.
     */
    final int sentenceCount;

    /**
     * The number of unique words (types) used in the document.
     */
    final int vocabularySize;
    
    private final FastDCM fastdcm;
    private final Function<Double,Double> digamma;

    /**
     * Constructs a representation of a document suitable for dynamic 
     * programming, by creating a list of cumulative word counts per sentence.
     * Takes a list of sentences (lists of tokens), which are assumed to have
     * already been processed in whatever ways are desired (e.g. cleaned,
     * stemmed, stopwords removed, etc).
     * 
     * @param sentences a list of lists of tokens
     */
    private DPDocument(Builder builder) {
        
        this.vocabulary = builder.words;
        this.cumulativeWordUsageCounts = builder.cumulativeWordUsageCounts;
        this.cumulativeTokenCounts = builder.cumulativeTokenCounts;
        this.sentenceCount = builder.sentenceCount;
        this.vocabularySize = builder.words.size();
        this.fastdcm = new FastDCM(1, this.vocabularySize, builder.logGamma);
        this.digamma = Utils.memoize(Gamma::digamma);
    }
    
    /**
     * Helper class for constructing DPDocuments.
     */
    public static class Builder {
        
        private final ImmutableMultiset.Builder<String> wordsB;
        private final ImmutableList.Builder<ImmutableMultiset<String>> cumulativeWordUsageCountsB;
        private final ImmutableList.Builder<Integer> cumulativeTokenCountsB;
        private int tokenCount;
        private int sentenceCount;
        private ImmutableList<String> words;
        private ImmutableList<ImmutableMultiset<String>> cumulativeWordUsageCounts;
        private ImmutableList<Integer> cumulativeTokenCounts;
        private Function<Double,Double> logGamma;
        
        /**
         * Create a new DPDocument.Builder instance.
         */
        public Builder() {
            this.wordsB = new ImmutableMultiset.Builder<>();
            this.cumulativeWordUsageCountsB = new ImmutableList.Builder<>();
            this.cumulativeTokenCountsB = new ImmutableList.Builder<>();
        }
        
        /**
         * Add a list of sentences to the DPDocument to be built..
         * @param sentences
         * @return the DPDocument.Builder instance
         */
        public Builder addAll(List<List<String>> sentences) {
            sentences.stream().forEach(sentence -> {
                this.add(sentence);
            });
            return this;
        }
        
        /**
         * Add a single sentence to the DPDocument to be built.
         * @param sentence
         * @return the DPDocument.Builder instance
         */
        public Builder add(List<String> sentence) {
            this.wordsB.addAll(sentence);
            ImmutableMultiset<String> wordsSoFar = wordsB.build();
            this.cumulativeWordUsageCountsB.add(wordsSoFar);
            this.cumulativeTokenCountsB.add(wordsSoFar.size());
            this.tokenCount = wordsSoFar.size();
            this.sentenceCount++;
            return this;
        }
        
        /**
         * The number of words (tokens) added to this DPDocument.Builder so far.
         * @return the number of words (tokens)
         */
        public int getTokenCount() {
            return this.tokenCount;
        }
        
        /**
         * Create and return a new DPDocument using the provided logGamma function.
         * @param logGamma a memoized logGamma function
         * @return the new DPDocument
         */
        public DPDocument build(Function<Double,Double> logGamma) {
            ImmutableMultiset<String> wordCounts = wordsB.build();
            this.words = ImmutableList.copyOf(wordCounts.elementSet());
            this.cumulativeWordUsageCounts = cumulativeWordUsageCountsB.build();
            this.cumulativeTokenCounts = cumulativeTokenCountsB.build();
            this.logGamma = logGamma;
            return new DPDocument(this);
        }
        
    }
    
    /**
     * Given a specific word (type) and a {@link segmentation.Segment} of this
     * document, returns the number of times that word (type) is used in that
     * segment.
     * @param word
     * @param segment
     * @return the number of times the word appears in the segment
     */
    int countWordInSegment(String word, Segment segment) {
        int count = this.cumulativeWordUsageCounts.get(
                segment.start + segment.length - 1).count(word); 
        if (segment.start > 0) {
            count -= this.cumulativeWordUsageCounts.get(segment.start - 1).count(word);
        }
        return count;
    }
    
    /**
     * Given a {@link segmentation.Segment} of this document, returns the total
     * number of words (tokens) used in the segment.
     * @param segment
     * @return
     */
    int countWordsInSegment(Segment segment) {
        int count = this.cumulativeTokenCounts.get(segment.start + segment.length - 1);
        if (segment.start > 0) {
            count -= this.cumulativeTokenCounts.get(segment.start - 1);
        }
        return count;
    }
    
    /**
     * Set the Dirichlet prior to use when computing log-likelihood and gradient.
     * 
     * @param prior the value of the symmetric Dirichlet prior
     */
    void setPrior(double prior) {
        fastdcm.setPrior(prior);
    }

    /**
     * Compute the log-likelihood of a segment.
     *
     * @param segment the segment
     * @return the log-likelihood
     */
    double segmentLogLikelihood(Segment segment) {
        if (fastdcm.getPrior() == 0) {
            return -Double.MAX_VALUE;
        }
        int[] counts = this.vocabulary.stream()
                .mapToInt(word -> this.countWordInSegment(word, segment))
                .toArray();
        return fastdcm.logDCM(counts);
    }

    /**
     * Compute the gradient of the log-likelihood for a segment.
     *
     * @param segment the segment
     * @return the gradient
     *
     */
    double segmentLogLikelihoodGradient(Segment segment) {
        final double prior = fastdcm.getPrior();
        if (prior == 0) {
            return Double.MAX_VALUE;
        }
        double d1 = digamma.apply(this.vocabularySize * prior);
        double d2 = digamma.apply(this.countWordsInSegment(segment) + this.vocabularySize * prior);
        double d3 = digamma.apply(prior);
        double result = this.vocabularySize * (d1 - d2 - d3);
        
        double sumOfDigammasOfWordCounts = this.vocabulary.stream()
                .map(word -> this.countWordInSegment(word, segment) + prior)
                .map(digamma)
                .mapToDouble(Double::doubleValue)
                .sum();
        
        result += sumOfDigammasOfWordCounts;

        return prior * result;
    }
}
