package segmentation;

import java.util.List;

public interface Segmenter {
    public List<int[]> segmentTexts(
        List<List<String>> texts, List<Integer> numSegments);
}
