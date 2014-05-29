package in.aesh.segment;

import java.util.List;
import java.util.Map;

/**
 * Generic interface for a topic segmenter.
 * @author Ryan Shaw <ryanshaw@unc.edu>
 */
public interface Segmenter {

    /**
     * Segment a set of texts.
     *
     * @param texts a map of text IDs to lists of lists of words
     * @param desiredNumSegments a map of text IDs to desired segment counts
     * @return a map of text IDs to segmentations
     */
    public Map<String,Segmentation> segmentTexts(
        Map<String,List<List<String>>> texts, Map<String,Integer> desiredNumSegments);
}
