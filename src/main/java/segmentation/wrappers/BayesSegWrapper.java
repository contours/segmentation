package segmentation.wrappers;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import edu.mit.nlp.segmenter.dp.DPSeg;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import segmentation.Segmentation;
import segmentation.Segmenter;
import segmentation.Stemmer;
import segmentation.Utils;

public class BayesSegWrapper implements Segmenter {
    public static final OptionParser OPTIONS;

    private static final OptionSpec<Double> PRIOR;
    private static final OptionSpec<File> STOPWORDS;

    static {
        OPTIONS = new OptionParser();
        PRIOR = OPTIONS.accepts("prior")
                .withRequiredArg().ofType(Double.class).defaultsTo(0.2);
        STOPWORDS = OPTIONS.accepts("stop-words")
                .withRequiredArg().ofType(File.class);
    }

    private final double prior;
    private final ImmutableList<String> stopwords;
    private final Stemmer stemmer;

    public BayesSegWrapper(OptionSet options) {
        this.prior = options.valueOf(PRIOR);
        // TODO allow a configurable stemming function defaulting to a null stemmer
        this.stemmer = new Stemmer();
        if (options.has(STOPWORDS)) {
            try {
                if (this.stemmer == null) {
                    this.stopwords = Utils.loadWords(options.valueOf(BayesSegWrapper.STOPWORDS));
                } else {
                    this.stopwords = Utils.stemWords(Utils.loadWords(options.valueOf(BayesSegWrapper.STOPWORDS)), stemmer);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.stopwords = ImmutableList.of();
        }
    }

    @Override
    public Map<String,Segmentation> segmentTexts(
            Map<String,List<String>> texts, 
            Map<String,Integer> segmentCounts) {
        DPSeg dpseg = new DPSeg(this.prepareTexts(texts), segmentCounts);
        return dpseg.segment(this.prior);
    }
    
    public double estimatePrior(
            Map<String,List<String>> texts, 
            Map<String,Integer> segmentCounts) {
        DPSeg dpseg = new DPSeg(this.prepareTexts(texts), segmentCounts);
        return dpseg.estimatePrior(this.prior);
    }

    private Map<String,List<List<String>>> prepareTexts(Map<String,List<String>> texts) {
        return texts.keySet().stream().map(key -> {
            List<String> text = texts.get(key);
            List<List<String>> sentences = text.stream()
                    .map(Utils::clean)
                    .map(Splitter.on(' ')::splitToList)
                    .map(words -> Utils.stemWords(words, this.stemmer))
                    .map(words -> Utils.removeStopwords(words, this.stopwords))
                    .collect(Utils.toImmutableList());
            return Maps.immutableEntry(key, sentences);
        }).collect(Utils.toImmutableMap());
    }
    

}
