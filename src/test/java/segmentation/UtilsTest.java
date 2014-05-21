package segmentation;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class UtilsTest {
    @Test
    public void testLoadText() throws IOException {
        File file = new File("src/test/data/txt/U-0005.txt");
        Map.Entry<String,List<String>> text = Utils.loadText(file);
        String key = text.getKey();
        assertThat(key, equalTo(file.getAbsolutePath()));
        List<String> sentences = text.getValue(); 
        assertThat(sentences.size(), equalTo(1157));
        assertThat(sentences.get(0), equalTo(
                ": Are we ready to go? Okay, this is tape number 11.19.03-JJ with James A. Jones in the Prospect Community in Robeson County."));
        assertThat(sentences.get(sentences.size()-1), equalTo(
                "He and Jimmy Goins-who's that tribal chief right now? Three musketeers they were always called in school."));
    }
    
    @Test
    public void testLoadTexts() throws IOException {
        
    }
}
