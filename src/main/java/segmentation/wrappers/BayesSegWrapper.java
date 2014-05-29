package segmentation.wrappers;

import edu.mit.nlp.segmenter.dp.DPSeg;
import java.util.List;
import java.util.Map;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import segmentation.Segmentation;
import segmentation.Segmenter;

public class BayesSegWrapper implements Segmenter {
    public static final OptionParser OPTIONS;

    private static final OptionSpec<Double> PRIOR;
    private static final OptionSpec<Void> ESTIMATE_PRIOR;
            
    static {
        OPTIONS = new OptionParser();
        PRIOR = OPTIONS.accepts("prior")
                .withRequiredArg().ofType(Double.class);
        ESTIMATE_PRIOR = OPTIONS.accepts("estimate-prior");
    }

    private final double initialPrior;
    private final boolean estimatePrior;

    public BayesSegWrapper(OptionSet options) {
        this(options.valueOf(PRIOR), options.has(ESTIMATE_PRIOR));
    }

    BayesSegWrapper(double initialPrior) {
        this(initialPrior, false);
    }

    BayesSegWrapper(double initialPrior, boolean estimatePrior) {
        this.initialPrior = initialPrior;
        this.estimatePrior = estimatePrior;
    }

    @Override
    public Map<String,Segmentation> segmentTexts(
            Map<String,List<List<String>>> texts,
            Map<String,Integer> segmentCounts) {
        DPSeg dpseg = new DPSeg(texts, segmentCounts);
        return dpseg.segment(this.initialPrior);
    }
    
    public double estimatePrior(
            Map<String,List<List<String>>> texts,
            Map<String,Integer> segmentCounts) {
        DPSeg dpseg = new DPSeg(texts, segmentCounts);
        return dpseg.estimatePrior(this.initialPrior);
    }

    

}
