package segmentation.wrappers;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import edu.mit.nlp.ling.Stemmer;
import edu.mit.nlp.segmenter.dp.DPSeg;
import edu.mit.nlp.util.Utils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import segmentation.Segmenter;
import segmentation.StreamUtils;

public class BayesSegWrapper implements Segmenter {
    public static final OptionParser OPTIONS;

    private static final OptionSpec<Void> FIXED_BLOCKS;
    private static final OptionSpec<Double> PRIOR;
    private static final OptionSpec<Double> DISPERSION;
    private static final OptionSpec<Void> USE_DURATION;
    private static final OptionSpec<Void> DEBUG;
    private static final OptionSpec<File> STOPWORDS;

    static {
        OPTIONS = new OptionParser();
        DEBUG = OPTIONS.accepts("debug");
        FIXED_BLOCKS = OPTIONS.accepts("fixed-blocks");
        USE_DURATION = OPTIONS.accepts("use-duration");
        PRIOR = OPTIONS.accepts("prior")
                .withRequiredArg().ofType(Double.class).defaultsTo(0.2);
        DISPERSION = OPTIONS.accepts("dispersion")
                .withRequiredArg().ofType(Double.class).defaultsTo(1.0);
        STOPWORDS = OPTIONS.accepts("stop-words")
                .withRequiredArg().ofType(File.class);
    }

    private static ImmutableList<String> loadWords(File file) throws IOException {
        return ImmutableList.copyOf(
                Files.lines(file.toPath())
                        .map(line -> line.trim().toLowerCase())
                        .collect(Collectors.toList()));
    }

    private final boolean useFixedBlocks;
    private final double prior;
    private final double dispersion;
    private final boolean useDuration;
    private final boolean debug;
    private final ImmutableList<String> stopwords;

    public BayesSegWrapper(OptionSet options) {
        this.useFixedBlocks = options.has(FIXED_BLOCKS);
        this.prior = options.valueOf(PRIOR);
        this.dispersion = options.valueOf(DISPERSION);
        this.useDuration = options.has(USE_DURATION);
        this.debug = options.has(DEBUG);
        try {
            this.stopwords = loadWords(options.valueOf(STOPWORDS));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<List<Integer>> segmentTexts(List<List<String>> texts, List<Integer> segmentCounts) {
        DPSeg dpseg = new DPSeg(this.prepareTexts(texts), segmentCounts);
        dpseg.setDebug(this.debug);
        return dpseg.segment(this.prior);
    }

    private List<List<List<String>>> prepareTexts(List<List<String>> texts) {
        return texts.stream().map(text ->
                text.stream()
                        .map(BayesSegWrapper::clean)
                        .map(Splitter.on(' ')::split)
                        .map(this::removeStopwords)
                        .map(this::stemWords)
                        .collect(StreamUtils.toImmutableList())
        ).collect(StreamUtils.toImmutableList());
    }
    
    static String clean(String s) {
        String lowercased = s.toLowerCase();
        String filtered = CharMatcher.JAVA_LOWER_CASE
                .or(CharMatcher.JAVA_DIGIT)
                .or(CharMatcher.WHITESPACE)
                .or(CharMatcher.anyOf("'`$"))
                .retainFrom(lowercased);
        String spaced = CharMatcher.WHITESPACE.trimAndCollapseFrom(filtered, ' ');
        return spaced.replaceAll("([a-z])(')([a-z])", "$1 '$3");
    }

    private List<String> removeStopwords(Iterable<String> words) {
        return StreamSupport.stream(words.spliterator(), false)
                .filter(word -> ! this.stopwords.contains(word))
                .collect(StreamUtils.toImmutableList());
    }
    
    private List<String> stemWords(List<String> words) {
        // TODO allow a configurable stemming function defaulting to a null stemmer
        return words.stream()
                .map(BayesSegWrapper::stemWord)
                .collect(StreamUtils.toImmutableList());
    }
    
    private static String stemWord(String word) {
        Stemmer stemmer = new Stemmer();
        stemmer.add(word.toCharArray(), word.length());
        stemmer.stem();
        return stemmer.toString();
    }
}
