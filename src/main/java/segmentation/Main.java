package segmentation;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import segmentation.wrappers.BayesSegWrapper;

public class Main {
    
    private static final OptionParser PARSER = new OptionParser();

    private static final OptionSpec<File> FILES = PARSER.nonOptions(
            "sentence files to be segmented").ofType(File.class);
    private static final OptionSpec<String> DOCNAME_PREFIX = PARSER.accepts(
            "docname-prefix").withRequiredArg().ofType(String.class);
    private static final OptionSpec<Integer> NUM_SEGMENTS = PARSER.accepts(
            "num-segments").withRequiredArg().ofType(Integer.class);
    private static final OptionSpec<File> REFERENCE = PARSER.accepts(
            "reference-segmentation").withRequiredArg().ofType(File.class);
    private static final OptionSpec<String> CODER = PARSER.accepts(
            "coder").withRequiredArg().ofType(String.class);
    private static final OptionSpec<Void> STEM = PARSER.accepts(
            "stem");
    private static final OptionSpec<File> STOPWORDS = PARSER.accepts(
            "stopwords").withRequiredArg().ofType(File.class);
    private static final OptionSpec<Void> HELP = PARSER.accepts(
            "help").forHelp();

    private final OptionSet options;
    private final Stemmer stemmer;
    private final List<String> stopwords;
    
    public static void main(String[] args) throws IOException {
        new Main(args).run();
    }

    public Main(String[] args) throws IOException {

        options = PARSER.parse(args);
        if (options.has(HELP)) {
            PARSER.printHelpOn(System.out);
            System.exit(0);
        }

        stemmer = options.has(STEM) ? new PorterStemmer() : new NullStemmer();

        stopwords = options.has(STOPWORDS)
                ? stemmer.stemWords(Utils.loadWords(options.valueOf(STOPWORDS)))
                : ImmutableList.of();
    }

    public Map<String,List<List<String>>> loadAndPrepareTexts() {
        return this.prepareTexts(this.loadTexts());
    }

    private void run() throws FileNotFoundException {
        Map<String,List<List<String>>> texts = loadAndPrepareTexts();

        Segmenter segmenter = new BayesSegWrapper(options);
        Map<String,Segmentation> segmentations = segmenter.segmentTexts(
                texts, getDesiredSegmentCounts(texts.keySet()));

        segmentations.keySet().forEach(id -> {
            // todo: write segmentation JSON to stdout
            System.out.println(id);
            System.out.println(segmentations.get(id));
            System.out.println();
        });
    }

    private Map<String, List<String>> loadTexts() {
        List<File> files = options.valuesOf(FILES);
        if (files.isEmpty()) {
            // todo: handle System.in if no file args
            System.exit(0);
        }
        Map<String,List<String>> texts;
        if (options.has(DOCNAME_PREFIX)) {
            texts = Utils.loadTexts(files, f -> {
                String filename = f.getName();
                return options.valueOf(DOCNAME_PREFIX)
                        + filename.substring(0, filename.length() - 4);
            });
        } else {
            texts = Utils.loadTexts(files);
        }
        return texts;
    }

    private Map<String,List<List<String>>> prepareTexts(
            Map<String,List<String>> texts) {
        return texts.keySet().stream().map(key -> {
            List<String> text = texts.get(key);
            List<List<String>> sentences = text.stream()
                    .map(Utils::clean)
                    .map(Splitter.on(' ')::splitToList)
                    .map(stemmer::stemWords)
                    .map(words -> Utils.removeStopwords(words, stopwords))
                    .collect(Utils.toImmutableList());
            return Maps.immutableEntry(key, sentences);
        }).collect(Utils.toImmutableMap());
    }


    private Map<String, Integer> getDesiredSegmentCounts(Set<String> textIDs)
            throws FileNotFoundException, JsonIOException, JsonSyntaxException {

        Map<String,Integer> segmentCounts;
        if (options.has(NUM_SEGMENTS)) {
            if (textIDs.size() > 1) {
                System.err.println("To specify segment counts for > 1 texts, "
                        + "provide a reference segmentation");
                System.exit(1);
            }
            segmentCounts = textIDs.stream()
                    .map(key -> Maps.immutableEntry(key, options.valueOf(NUM_SEGMENTS)))
                    .collect(Utils.toImmutableMap());
        } else {
            Gson gson = new Gson();
            Segmentations reference = gson.fromJson(
                    new FileReader(options.valueOf(REFERENCE)), Segmentations.class);
            if (options.has(CODER)) {
                segmentCounts = reference.getSegmentCounts(options.valueOf(CODER));
            } else {
                segmentCounts = reference.getMeanSegmentCounts();
            }
        }
        assert segmentCounts.keySet().equals(textIDs);
        return segmentCounts;
    }
    
}
