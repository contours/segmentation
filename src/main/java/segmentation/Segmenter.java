package segmentation;

import java.util.List;

public interface Segmenter {
    public List<List<Integer>> segmentTexts(
        List<List<String>> texts, List<Integer> numSegments);
}
