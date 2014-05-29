package segmentation;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.contains;
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
        Path dir = FileSystems.getDefault().getPath("src/test/data/txt");
        Iterable<Path> filepaths = Files.newDirectoryStream(dir, "U-*.txt");
        List<File> files = Utils.stream(filepaths)
                .map(path -> path.toFile())
                .collect(Utils.toImmutableList());
        Map<String, List<String>> texts = Utils.loadTexts(files, 
                f -> { 
                    String filename = f.getName();
                    return "interviews:" + filename.substring(0, filename.length() - 4);
                });
        assertThat(texts.keySet(), contains("interviews:U-0005","interviews:U-0007","interviews:U-0008","interviews:U-0011","interviews:U-0012","interviews:U-0014","interviews:U-0017","interviews:U-0019","interviews:U-0020","interviews:U-0023","interviews:U-0098","interviews:U-0178","interviews:U-0180","interviews:U-0181","interviews:U-0183","interviews:U-0184","interviews:U-0185","interviews:U-0186","interviews:U-0193"));
    }

    @Test
    public void testClean() {
        assertThat(Utils.clean("Sam Smith"), equalTo("sam smith"));
        assertThat(Utils.clean("~`!@#$%^&*()+={}[]|\\:;\"'<>,.?/"), equalTo("$"));
        assertThat(Utils.clean(" foo    bar "), equalTo("foo bar"));
        assertThat(Utils.clean("foo\tbar"), equalTo("foo bar"));
        assertThat(Utils.clean("Sam's house"), equalTo("sam s house"));
        assertThat(Utils.clean("ménièr"), equalTo("m ni r"));
    }
}
