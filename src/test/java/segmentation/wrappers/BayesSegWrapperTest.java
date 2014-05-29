package segmentation.wrappers;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import segmentation.Segmentation;
import segmentation.Segmenter;
import segmentation.Utils;

public class BayesSegWrapperTest {
    
    private static <K> Map<K,Integer> map(K key, Integer value) {
        return ImmutableMap.of(key, value);
    }
    
    @Test
    public void testSegmentTexts() throws IOException {
        OptionParser parser = BayesSegWrapper.OPTIONS;
        OptionSet options = parser.parse("-s", "src/test/data/STOPWORD.list");
        Segmenter segmenter = new BayesSegWrapper(options);
        
        File file = new File("src/test/data/050.ref");
        String path = file.getAbsolutePath();
        List<File> files = Arrays.asList(file);
        Map<String,List<String>> texts = Utils.loadTexts(files);

        Map<String,Segmentation> segmentations;
        segmentations = segmenter.segmentTexts(texts, map(path, 1));
        assertThat(segmentations.get(path).toList(), contains(212));

        segmentations = segmenter.segmentTexts(texts, map(path, 3));
        assertThat(segmentations.get(path).toList(), contains(77,50,85));

        segmentations = segmenter.segmentTexts(texts, map(path, 5));
        assertThat(segmentations.get(path).toList(), contains(41,36,25,49,61));

        segmentations = segmenter.segmentTexts(texts, map(path, 7));
        assertThat(segmentations.get(path).toList(), contains(41,11,25,25,25,25,60));
    }
    
    @Test
    public void testLearnPrior() throws IOException {
        OptionParser parser = BayesSegWrapper.OPTIONS;
        OptionSet options = parser.parse("-p", "0.2"); // initial prior
        BayesSegWrapper segmenter = new BayesSegWrapper(options);
        
        Map.Entry<String,List<String>> text = Utils.loadText(new File("src/test/data/050.ref"));
        Map<String,Integer> segmentCounts = ImmutableMap.of(text.getKey(), 4);
        Map<String,List<String>> texts = ImmutableMap.of(text.getKey(), text.getValue());

        double estimate = segmenter.estimatePrior(texts, segmentCounts);
        assertThat(estimate, closeTo(0.8, 0.0002));
    }
    
}

