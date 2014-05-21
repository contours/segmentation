package segmentation;

import com.google.common.base.Splitter;
import java.util.List;
import java.util.stream.Collectors;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import org.junit.Test;


public class StemmerTest {
    
    @Test
    public void testStemmer() {
        Stemmer stemmer = new Stemmer();
        String sentence = "gimme the ax lived in a house where everything is the same as it always was";
        List<String> stems = StreamUtils.stream(Splitter.on(' ').split(sentence))
                .map(word -> {
                    stemmer.add(word);
                    stemmer.stem();
                    return stemmer.toString();
                })
                .collect(Collectors.toList());
        assertThat(stems, contains("gimm","the","ax","live","in","a","hous",
                "where","everyth","is","the","same","as","it","alwai","wa"));
    }

}
