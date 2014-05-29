package segmentation.wrappers;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import segmentation.Main;
import segmentation.Segmentation;
import segmentation.Segmenter;

public class BayesSegWrapperTest {
    
    private static <K> Map<K,Integer> map(K key, Integer value) {
        return ImmutableMap.of(key, value);
    }
    
    @Test
    public void testSegmentTexts() throws IOException {
        Map<String,List<List<String>>> texts = new Main(new String[]{
            "-stop", "src/test/data/STOPWORD.list",
            "-stem",
            "src/test/data/050.ref" }).loadAndPrepareTexts();
        String textID = texts.keySet().toArray(new String[]{})[0];

        Segmenter segmenter = new BayesSegWrapper(0.2);
        
        Map<String,Segmentation> segmentations = segmenter.segmentTexts(texts, map(textID, 1));
        assertThat(segmentations.get(textID).toList(), contains(212));

        segmentations = segmenter.segmentTexts(texts, map(textID, 3));
        assertThat(segmentations.get(textID).toList(), contains(77,50,85));

        segmentations = segmenter.segmentTexts(texts, map(textID, 5));
        assertThat(segmentations.get(textID).toList(), contains(41,36,25,49,61));

        segmentations = segmenter.segmentTexts(texts, map(textID, 7));
        assertThat(segmentations.get(textID).toList(), contains(41,11,25,25,25,25,60));
    }
    
    @Test
    public void testLearnPrior() throws IOException {
        Map<String,List<List<String>>> texts = new Main(new String[]{
            "-stop", "src/test/data/STOPWORD.list",
            "-stem",
            "src/test/data/050.ref" }).loadAndPrepareTexts();
        String textID = texts.keySet().toArray(new String[]{})[0];

        BayesSegWrapper segmenter = new BayesSegWrapper(0.2);
        
        double estimate = segmenter.estimatePrior(texts, map(textID, 4));
        assertThat(estimate, closeTo(0.5857, 0.0002));
    }
    
}

