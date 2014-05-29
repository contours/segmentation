package segmentation;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class SegmentationsTest {

    @Test
    public void testLoadSegmentations() throws IOException {
        File file = new File("src/test/data/segmentations.json");
        Gson gson = new Gson();
        Segmentations segmentations = gson.fromJson(new FileReader(file), Segmentations.class);
        assertThat(segmentations.getId(), equalTo("datasets:u-series"));
        assertThat(segmentations.getSegmentationType(), equalTo("linear"));
        assertThat(segmentations.getItems().size(), equalTo(19));
        
        Map<String,Integer> segmentCounts = segmentations.getSegmentCounts(
                "annotators:docsouth");
        assertThat(segmentCounts.get("interviews:U-0005"), equalTo(11));
        assertThat(segmentCounts.get("interviews:U-0007"), equalTo(12));
        assertThat(segmentCounts.get("interviews:U-0008"), equalTo(21));
        
        Segmentation segmentation = segmentations.getItems()
                .get("interviews:U-0005").get("annotators:docsouth");
        assertThat(segmentation.size(), equalTo(11));
    }
}
