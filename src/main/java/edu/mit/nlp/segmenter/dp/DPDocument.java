package edu.mit.nlp.segmenter.dp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import in.aesh.segment.Segment;
import java.util.List;

/**
 * DPDocument stores document statistics and provides methods for efficient
 * inference of the maximum-likelihood segmentation via dynamic programming.
 * 
 * @author Ryan Shaw <ryanshaw@unc.edu>
 */
class DPDocument {

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
     * The number of sentences in the document.
     */
    final int sentenceCount;

    /**
     * Constructs a representation of a document suitable for dynamic 
     * programming, by creating a list of cumulative word counts per sentence.
     * Takes a list of sentences (lists of tokens), which are assumed to have
     * already been processed in whatever ways are desired (e.g. cleaned,
     * stemmed, stopwords removed, etc).
     * 
     * @param sentences a list of lists of tokens
     */
    DPDocument(List<List<String>> sentences) {
        
        ImmutableMultiset.Builder<String> wordsB = new ImmutableMultiset.Builder<>();
        ImmutableList.Builder<ImmutableMultiset<String>> cumulativeWordUsageCountsB = new ImmutableList.Builder<>();

        sentences.stream().forEach(sentence -> {
            wordsB.addAll(sentence);
            ImmutableMultiset<String> wordsSoFar = wordsB.build();
            cumulativeWordUsageCountsB.add(wordsSoFar);
            this.sentenceCount++;
        });
        this.sentenceCount = sentences.size();

        ImmutableMultiset<String> wordCounts = wordsB.build();
        this.vocabulary = ImmutableList.copyOf(wordCounts.elementSet());
        this.cumulativeWordUsageCounts = cumulativeWordUsageCountsB.build();
    }
    
    /**
     * Given a specific word (type) and a {@link segmentation.Segment} of this
     * document, returns the number of times that word (type) is used in that
     * segment.
     *
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
    
    int[] countWordsInSegment(Segment segment) {
        return this.vocabulary.stream()
                .mapToInt(word -> this.countWordInSegment(word, segment))
                .toArray();
    }

}
