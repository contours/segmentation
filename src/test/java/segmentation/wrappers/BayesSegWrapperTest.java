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
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;
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

        Map<String,List<Integer>> segmentations;
        segmentations = segmenter.segmentTexts(texts, map(path, 1));
        assertThat(segmentations.get(path), contains(212));

        segmentations = segmenter.segmentTexts(texts, map(path, 3));
        assertThat(segmentations.get(path), contains(77,50,85));

        segmentations = segmenter.segmentTexts(texts, map(path, 5));
        assertThat(segmentations.get(path), contains(41,36,25,49,61));

        segmentations = segmenter.segmentTexts(texts, map(path, 7));
        assertThat(segmentations.get(path), contains(41,11,25,25,25,25,60));
    }
    
    @Test
    public void testClean() {
        assertThat(BayesSegWrapper.clean("Sam Smith"), equalTo("sam smith"));
        assertThat(BayesSegWrapper.clean("~`!@#$%^&*()+={}[]|\\:;\"'<>,.?/"), equalTo("`$'"));
        assertThat(BayesSegWrapper.clean(" foo    bar "), equalTo("foo bar"));
        assertThat(BayesSegWrapper.clean("foo\tbar"), equalTo("foo bar"));
        assertThat(BayesSegWrapper.clean("Sam's house"), equalTo("sam 's house"));
    }
}

