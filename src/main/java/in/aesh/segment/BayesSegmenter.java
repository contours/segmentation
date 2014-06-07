package in.aesh.segment;

import edu.mit.nlp.segmenter.dp.DPSeg;
import java.text.MessageFormat;
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
    public Segmentations segmentTexts(
            Map<String,List<List<String>>> texts,
            Map<String,Integer> segmentCounts,
            String preprocessingDescription) {
        DPSeg dpseg = new DPSeg(texts, segmentCounts);
        double final_α;
        if (this.estimate) {
            final_α = dpseg.estimateConcentrationParameter(this.α);
        } else {
            dpseg.segment(this.α);
            final_α = this.α;
        }
        String coder = MessageFormat.format("{0}{1}-α{2}",
                this.getName(), preprocessingDescription, final_α);
        return new Segmentations.Builder()
                .add(coder, dpseg.getSegmentations())
                .build(coder);
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
