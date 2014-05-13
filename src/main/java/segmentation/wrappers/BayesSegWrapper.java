package segmentation.wrappers;

import edu.mit.nlp.segmenter.dp.DPDocument;
import edu.mit.nlp.segmenter.dp.DPSeg;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import segmentation.Segmenter;

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

    private static DPDocument createDPDocument(TextWrapper textw) {
        double[][] wot = textw.createWordOccurrenceTable(); // D x T matrix
        double[][] sentences = new double[wot[0].length][wot.length];
        for (int i = 0; i < wot.length; i++) {
            for (int j = 0; j < wot[i].length; j++) {
                sentences[j][i] = wot[i][j];
            }
        }
        return new DPDocument(sentences);
    }

    private static int[] indexesToMasses(int[] indexes, int mass) {
        // DPSeg returns 0-based index of first sentence in each segment.
        // We want to convert these to mass-based segmentations.
        return IntStream.rangeClosed(1, indexes.length)
                .map(i -> {
                    return (i < indexes.length)
                            ? (indexes[i] - indexes[i - 1])
                            : (mass - indexes[i - 1]);
                })
                .toArray();
    }

    private static List<String> loadWords(File file) throws IOException {
        return Files.lines(file.toPath())
                .map(line -> line.trim().toLowerCase())
                .collect(Collectors.toList());
    }

    private final boolean useFixedBlocks;
    private final double prior;
    private final double dispersion;
    private final boolean useDuration;
    private final boolean debug;
    private final List<String> stopwords;

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
    public List<int[]> segmentTexts(List<List<String>> texts, List<Integer> segmentCounts) {
        DPSeg dpseg = new DPSeg(
                convertTextsToDPDocuments(texts), 
                segmentCounts.stream().mapToInt(i->i).toArray());
        dpseg.setDebug(this.debug);
        dpseg.setUseDuration(this.useDuration);
        int[][] segmentations = dpseg.segment(this.prior, this.dispersion);
        return convertDPSegmentationsToMasses(segmentations, texts);
    }

    private List<int[]> convertDPSegmentationsToMasses(int[][] responses, List<List<String>> texts) {
        if (this.useFixedBlocks) {
            throw new UnsupportedOperationException(
                    "TODO: handle using fixed blocks by converting window segmentations to sentence segmentations");
        }
        return IntStream.range(0, texts.size())
                .mapToObj(i -> indexesToMasses(responses[i], texts.get(i).size()))
                .collect(Collectors.toList());
    }

    private DPDocument[] convertTextsToDPDocuments(List<List<String>> texts) {
        boolean doStemming = true;
        return texts.stream()
                .map(text -> new TextWrapper(text, this.stopwords, doStemming))
                .map(textw -> createDPDocument(textw))
                .collect(Collectors.toList())
                .toArray(new DPDocument[texts.size()]);
    }

}
