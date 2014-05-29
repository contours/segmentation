package in.aesh.segment;

import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * Interface for stemming implementations.
 * @author Ryan Shaw <ryanshaw@unc.edu>
 */
public interface Stemmer {

    /**
     * Stem a single word.
     * @param word to be stemmed
     * @return the stemmed word
     */
    String stemWord(String word);

    /**
     * Stem a list of words.
     * @param words to be stemmed
     * @return an {@link com.google.common.collect.ImmutableList} of stemmed words
     */
    ImmutableList<String> stemWords(List<String> words);
    
}
