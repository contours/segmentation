package in.aesh.segment;

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
import java.util.function.Function;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * Main entry point.
 * @author Ryan Shaw <ryanshaw@unc.edu>
 */
public class Main {

    private static final OptionParser parser;
    private static final OptionSpec<Integer> NUM_SEGMENTS;
    private static final OptionSpec<File> REFERENCE;
    private static final OptionSpec<String> CODER;
    private static final OptionSpec<Void> STEM;
    private static final OptionSpec<File> STOPWORDS;
    private static final OptionSpec<String> DOCNAME_PREFIX;
    private static final OptionSpec<File> FILES;

    
    static {
        parser = new OptionParser();
        NUM_SEGMENTS = parser.accepts("num-segments").withRequiredArg().ofType(Integer.class);
        REFERENCE = parser.accepts("reference-segmentation").withRequiredArg().ofType(File.class);
        CODER = parser.accepts("coder").withRequiredArg().ofType(String.class);
        STEM = parser.accepts("stem");
        STOPWORDS = parser.accepts("stopwords").withRequiredArg().ofType(File.class);
        DOCNAME_PREFIX = parser.accepts("docname-prefix").withRequiredArg().ofType(String.class);
        FILES = parser.nonOptions("sentence files to be segmented").ofType(File.class);
    }

    /**
     * Main entry point
     * @param args command-line arguments
     * @throws IOException if files could not be loaded
     */
    public static void main(String[] args) throws IOException {
        new Main(args).run();
    }

    private static List<Segmenter> loadSegmenters(String[] args) throws IOException {
        OptionParser initialParser = new OptionParser();
        initialParser.allowsUnrecognizedOptions();
        OptionSpec<Void> HELP = initialParser.accepts("help").forHelp();
        OptionSpec<String> ALGORITHM = initialParser.accepts("algorithm").withRequiredArg().ofType(String.class);
        OptionSet initialOptions = initialParser.parse(args);

        List<String> algorithms = initialOptions.has(ALGORITHM)
                ? initialOptions.valuesOf(ALGORITHM) : ImmutableList.of();
        List<Segmenter> segmenters = algorithms.stream()
                .map(Segmenter::load)
                .collect(Utils.toImmutableList());
        if (segmenters.isEmpty()) {
            segmenters = Segmenter.loadAll();
        }
        segmenters.forEach(segmenter -> segmenter.addOptions(parser));
        if (initialOptions.has(HELP)) {
            initialParser.printHelpOn(System.out);
            parser.printHelpOn(System.out);
            System.exit(0);
        }
        return segmenters;
    }

    private static Map<String,List<List<String>>> prepareTexts(Map<String,List<String>> texts, Stemmer stemmer, List<String> stopwords) {
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


    private final OptionSet options;
    private final List<File> files;
    private final Function<File,String> file2id;
    private final List<Segmenter> segmenters;
    private final Stemmer stemmer;
    private final List<String> stopwords;

    /**
     * Main entry point
     * @param args command-line arguments
     * @throws IOException if files could not be loaded
     */
    public Main(String[] args) throws IOException {

        this.segmenters = loadSegmenters(args);

        this.options = parser.parse(args);

        this.files = this.options.valuesOf(FILES);
        if (this.files.isEmpty()) {
            // todo: handle System.in if no file args
            System.exit(0);
        }

        if (this.options.has(DOCNAME_PREFIX)) {
            this.file2id = f -> {
                String filename = f.getName();
                // todo: properly strip suffix
                return this.options.valueOf(DOCNAME_PREFIX)
                        + filename.substring(0, filename.length() - 4);
            };
        } else {
            this.file2id = f -> f.toPath().toAbsolutePath().toString();
        }

        this.stemmer = this.options.has(STEM) ? new PorterStemmer() : new NullStemmer();
        this.stopwords = this.options.has(STOPWORDS)
                ? stemmer.stemWords(Utils.loadWords(this.options.valueOf(STOPWORDS)))
                : ImmutableList.of();
    }

    /**
     * Get the texts as they will be sent to the segmenters. Convenience method
     * for testing.
     * @return a map of text IDs to lists of lists of tokens
     */
    public final Map<String,List<List<String>>> loadAndPrepareTexts() {
        return prepareTexts(Utils.loadTexts(this.files, this.file2id),
                this.stemmer, this.stopwords);
    }

    private Map<String, Integer> getDesiredSegmentCounts(Set<String> textIDs)
            throws FileNotFoundException, JsonIOException, JsonSyntaxException {
        Map<String,Integer> segmentCounts;
        if (this.options.has(NUM_SEGMENTS)) {
            if (textIDs.size() > 1) {
                System.err.println("To specify segment counts for > 1 texts, "
                        + "provide a reference segmentation");
                System.exit(1);
            }
            segmentCounts = textIDs.stream()
                    .map(key -> Maps.immutableEntry(key, this.options.valueOf(NUM_SEGMENTS)))
                    .collect(Utils.toImmutableMap());
        } else {
            Gson gson = new Gson();
            Segmentations reference = gson.fromJson(
                    new FileReader(this.options.valueOf(REFERENCE)), Segmentations.class);
            if (this.options.has(CODER)) {
                segmentCounts = reference.getSegmentCounts(this.options.valueOf(CODER));
            } else {
                segmentCounts = reference.getMeanSegmentCounts();
            }
        }
        assert segmentCounts.keySet().equals(textIDs);
        return segmentCounts;
    }

    private void run() throws FileNotFoundException {
        // todo: write segmentation JSON to stdout

        Map<String,List<List<String>>> texts = this.loadAndPrepareTexts();
        Map<String, Integer> segmentCounts = this.getDesiredSegmentCounts(texts.keySet());

        this.segmenters.forEach(segmenter -> {
            segmenter.init(this.options);
            Map<String,Segmentation> segmentations = segmenter.segmentTexts(
                    texts, segmentCounts);
            segmentations.keySet().forEach(id -> {
                System.out.println(id);
                System.out.println(segmentations.get(id));
                System.out.println();
            });
        });
    }

    
}
