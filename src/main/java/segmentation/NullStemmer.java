package segmentation;

import com.google.common.collect.ImmutableList;
import java.util.List;

public class NullStemmer implements Stemmer {

    @Override
    public String stemWord(String word) {
        return word;
    }

    @Override
    public ImmutableList<String> stemWords(List<String> words) {
        return ImmutableList.copyOf(words);
    }

}
