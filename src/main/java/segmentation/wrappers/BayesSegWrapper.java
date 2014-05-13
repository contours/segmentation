package segmentation.wrappers;

import edu.mit.nlp.segmenter.dp.DPDocument;
import edu.mit.nlp.segmenter.dp.DPSeg;
import edu.mit.nlp.util.StrIntMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private static final OptionSpec<Void> KNOWN_SEGMENT_COUNT;
    private static final OptionSpec<Void> USE_DURATION;
    private static final OptionSpec<Void> DEBUG;
    private static final OptionSpec<File> STOPWORDS;

    static {
        OPTIONS = new OptionParser();
        DEBUG = OPTIONS.accepts("debug");
        FIXED_BLOCKS = OPTIONS.accepts("fixed-blocks");
        KNOWN_SEGMENT_COUNT = OPTIONS.accepts("known-segment-count");
        USE_DURATION = OPTIONS.accepts("use-duration");
        PRIOR = OPTIONS.accepts("prior")
                .withRequiredArg().ofType(Double.class).defaultsTo(0.2);
        DISPERSION = OPTIONS.accepts("dispersion")
                .withRequiredArg().ofType(Double.class).defaultsTo(1.0);
        STOPWORDS = OPTIONS.accepts("stop-words")
                .withRequiredArg().ofType(File.class);
    }

    private static DPDocument[] createDPDocument(TextWrapper textw) {
        double[][] w = textw.createWordOccurrenceTable(); // D x T matrix
        double[][] m_words = new double[w[0].length][w.length];
        for (int i = 0; i < w.length; i++) {
            for (int j = 0; j < w[i].length; j++) {
                m_words[j][i] = w[i][j];
            }
        }
        DPDocument doc = new DPDocument(m_words, textw.getReferenceSeg().size() + 1, true);
        return new DPDocument[]{doc};
    }

    private static int[][] createDPTruths(List<Integer> numSegments) {
        return numSegments.stream()
                .map(i -> IntStream.rangeClosed(1, i).toArray())
                .collect(Collectors.toList())
                .toArray(new int[numSegments.size()][]);
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
    private final boolean numSegsIsKnown;
    private final boolean useDuration;
    private final boolean debug;
    private final List<String> stopwords;

    public BayesSegWrapper(OptionSet options) {
        this.useFixedBlocks = options.has(FIXED_BLOCKS);
        this.prior = options.valueOf(PRIOR);
        this.dispersion = options.valueOf(DISPERSION);
        this.numSegsIsKnown = options.has(KNOWN_SEGMENT_COUNT);
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
                createDPTruths(segmentCounts));
        dpseg.m_debug = this.debug;
        dpseg.m_num_segments_known = this.numSegsIsKnown;
        dpseg.use_duration = this.useDuration;
        dpseg.segment(new double[]{
            Math.log(this.prior),
            Math.log(this.dispersion)
        });
        return convertDPResponsesToMasses(dpseg.getResponses(), texts);
    }

    private List<int[]> convertDPResponsesToMasses(int[][] responses, List<List<String>> texts) {
        if (this.useFixedBlocks) {
            throw new UnsupportedOperationException(
                    "TODO: handle using fixed blocks by converting window segmentations to sentence segmentations");
        }
        return IntStream.range(0, texts.size())
                .mapToObj(i -> indexesToMasses(responses[i], texts.get(i).size()))
                .collect(Collectors.toList());
    }

    private DPDocument[][] convertTextsToDPDocuments(List<List<String>> texts) {
        boolean doStemming = true;
        return texts.stream()
                .map(text -> { 
                    TextWrapper textw = new TextWrapper(text, this.stopwords, doStemming);
                    textw.printSentences();
                    return textw;
                })
                .map(textw -> createDPDocument(textw))
                .collect(Collectors.toList())
                .toArray(new DPDocument[texts.size()][1]);
    }

}
