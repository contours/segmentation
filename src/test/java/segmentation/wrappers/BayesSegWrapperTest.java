package segmentation.wrappers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import segmentation.Main;
import segmentation.Segmenter;

public class BayesSegWrapperTest {
    @Test
    public void testSegmentTexts() throws IOException {
        OptionParser parser = BayesSegWrapper.OPTIONS;
        OptionSet options = parser.parse("-k", "-s", "src/test/data/STOPWORD.list");
        Segmenter segmenter = new BayesSegWrapper(options);
        List<List<String>> texts = new ArrayList<>();
        texts.add(Main.loadText(new File("src/test/data/050.ref")));
        List<Integer> segmentCounts = Arrays.asList(new Integer[]{ 7 });
        List<int[]> segmentations = segmenter.segmentTexts(texts, segmentCounts);
        assertThat(segmentations, contains(new int[]{41,11,25,25,25,25,60}));
    }
}

