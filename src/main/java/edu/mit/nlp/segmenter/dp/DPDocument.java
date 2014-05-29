package edu.mit.nlp.segmenter.dp;

import segmentation.Segment;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.math3.special.Gamma;
import segmentation.Utils;

public class DPDocument {

    final ImmutableList<String> words;
    final ImmutableList<ImmutableMultiset<String>> cumulativeWordCounts;
    final ImmutableList<Integer> cumulativeTotalWords;
    final int sentenceCount;
    final int vocabularySize;
    private final FastDCM fastdcm;
    private final Function<Double,Double> digamma;

    /**
     * Constructs a representation of a document suitable for dynamic 
     * programming, by creating a list of cumulative word counts per sentence.
     * Takes a list of sentences (lists of tokens), which are assumed to have
     * already been processed in whatever ways are desired (e.g. cleaned,
     * stemmed, stop-words removed, etc).
     * 
     * @param sentences a list of lists of tokens
     */
    private DPDocument(Builder builder) {
        
        this.words = builder.words;
        this.cumulativeWordCounts = builder.cumulativeWordCounts;
        this.cumulativeTotalWords = builder.cumulativeTotalWords;
        this.sentenceCount = builder.sentenceCount;
        this.vocabularySize = builder.words.size();
        this.fastdcm = new FastDCM(1, this.vocabularySize, builder.logGamma);
        this.digamma = Utils.memoize(Gamma::digamma);
    }
    
    public static class Builder {
        
        private final ImmutableMultiset.Builder<String> wordsB;
        private final ImmutableList.Builder<ImmutableMultiset<String>> cumulativeWordCountsB;
        private final ImmutableList.Builder<Integer> cumulativeTotalWordsB;
        private int wordCount;
        private int sentenceCount;
        private ImmutableList<String> words;
        private ImmutableList<ImmutableMultiset<String>> cumulativeWordCounts;
        private ImmutableList<Integer> cumulativeTotalWords;
        private Function<Double,Double> logGamma;
        
        public Builder() {
            this.wordsB = new ImmutableMultiset.Builder<>();
            this.cumulativeWordCountsB = new ImmutableList.Builder<>();
            this.cumulativeTotalWordsB = new ImmutableList.Builder<>();
        }
        
        public Builder addAll(List<List<String>> sentences) {
            sentences.stream().forEach(sentence -> {
                this.add(sentence);
            });
            return this;
        }
        
        public Builder add(List<String> sentence) {
            this.wordsB.addAll(sentence);
            ImmutableMultiset<String> wordsSoFar = wordsB.build();
            this.cumulativeWordCountsB.add(wordsSoFar);
            this.cumulativeTotalWordsB.add(wordsSoFar.size());
            this.wordCount = wordsSoFar.size();
            this.sentenceCount++;
            return this;
        }
        
        public int getWordCount() {
            return this.wordCount;
        }
        
        public DPDocument build(Function<Double,Double> logGamma) {
            ImmutableMultiset<String> wordCounts = wordsB.build();
            this.words = ImmutableList.copyOf(wordCounts.elementSet());
            this.cumulativeWordCounts = cumulativeWordCountsB.build();
            this.cumulativeTotalWords = cumulativeTotalWordsB.build();
            this.logGamma = logGamma;
            return new DPDocument(this);
        }
        
    }
    
    int countWordInSegment(String word, Segment segment) {
        int count = this.cumulativeWordCounts.get(
                segment.start + segment.length - 1).count(word); 
        if (segment.start > 0) {
            count -= this.cumulativeWordCounts.get(segment.start - 1).count(word);
        }
        return count;
    }
    
    int countWordsInSegment(Segment segment) {
        int count = this.cumulativeTotalWords.get(segment.start + segment.length - 1);
        if (segment.start > 0) {
            count -= this.cumulativeTotalWords.get(segment.start - 1);
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
        int[] counts = this.words.stream()
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
        
        double sumOfDigammasOfWordCounts = this.words.stream()
                .map(word -> this.countWordInSegment(word, segment) + prior)
                .map(digamma)
                .mapToDouble(Double::doubleValue)
                .sum();
        
        result += sumOfDigammasOfWordCounts;

        return prior * result;
    }
}
