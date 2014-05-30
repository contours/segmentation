package in.aesh.segment;

import edu.mit.nlp.segmenter.dp.DPSeg;
import java.util.List;
import java.util.Map;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

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

    BayesSegWrapper(OptionSet options) {
        this.initialPrior = options.valueOf(PRIOR);
        this.estimatePrior = options.has(ESTIMATE_PRIOR);
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
        return dpseg.estimateConcentrationParameter(this.initialPrior);
    }

    

}
