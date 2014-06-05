package in.aesh.segment;

import edu.mit.nlp.segmenter.dp.DPSeg;
import java.util.List;
import java.util.Map;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class BayesSegmenter extends Segmenter {

    private OptionSpec<Double> CONCENTRATION;
    private OptionSpec<Void> ESTIMATE_CONCENTRATION;
            
    private double α;
    private boolean estimate;

    @Override
    public Map<String,Segmentation> segmentTexts(
            Map<String,List<List<String>>> texts,
            Map<String,Integer> segmentCounts) {
        DPSeg dpseg = new DPSeg(texts, segmentCounts);
        return dpseg.segment(this.α);
    }
    
    public double estimateConcentrationParameter(
            Map<String,List<List<String>>> texts,
            Map<String,Integer> segmentCounts) {
        DPSeg dpseg = new DPSeg(texts, segmentCounts);
        return dpseg.estimateConcentrationParameter(this.α);
    }

    @Override
    public String getName() {
        return "bayes";
    }

    @Override
    public void addOptions(OptionParser parser) {
        CONCENTRATION = parser.accepts("concentration")
                .withRequiredArg().ofType(Double.class).required();
        ESTIMATE_CONCENTRATION = parser.accepts("estimate-concentration");
    }

    @Override
    public void init(OptionSet options) {
        this.α = options.valueOf(CONCENTRATION);
        this.estimate = options.has(ESTIMATE_CONCENTRATION);
    }

    

}
