package segmentation;

import java.util.List;
import java.util.Map;

public interface Segmenter {
    public Map<String,Segmentation> segmentTexts(
        Map<String,List<String>> texts, Map<String,Integer> numSegments);
}
