package segmentation;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import segmentation.wrappers.BayesSegWrapper;

public class Main {
    public static void main(String[] args) throws IOException {

        OptionParser parser = BayesSegWrapper.OPTIONS;
        
        OptionSpec<File> FILES = parser.nonOptions("sentence files to be segmented").ofType(File.class);
        OptionSpec<Integer> NUM_SEGMENTS = parser.accepts("num-segments")
                .withRequiredArg().ofType(Integer.class);
        OptionSpec<File> REFERENCE = parser.accepts("reference-segmentation")
                .withRequiredArg().ofType(File.class);
        OptionSpec<String> CODER = parser.accepts("coder")
                .withRequiredArg().ofType(String.class);
        OptionSpec<Void> ESTIMATE_PRIOR = parser.accepts("estimate-prior");
        OptionSpec<Void> HELP = parser.accepts("help").forHelp();

        OptionSet options = parser.parse(args);

        if (options.has(HELP)) {
            parser.printHelpOn(System.out);
        } else {
            List<File> files = options.valuesOf(FILES);

            Map<String,List<String>> texts;
            if (files.isEmpty()) {
                // todo: handle System.in if no file args
                System.exit(0);
                return;
            } else {
                texts = Utils.loadTexts(files, f -> { 
                    String filename = f.getName();
                    return "interviews:" + filename.substring(0, filename.length() - 4);
                });
            }

            Map<String,Integer> segmentCounts;
            if (options.has(NUM_SEGMENTS)) {
                if (texts.size() > 1) {
                    System.err.println("to specify segment counts for > 1 texts, provide a reference segmentation");
                    System.exit(1);
                    return;
                } else {
                    segmentCounts = texts.keySet().stream()
                            .map(key -> Maps.immutableEntry(key, options.valueOf(NUM_SEGMENTS)))
                            .collect(Utils.toImmutableMap());
                }
            } else {
                Gson gson = new Gson();
                Segmentations reference = gson.fromJson(
                        new FileReader(options.valueOf(REFERENCE)), Segmentations.class);
                segmentCounts = reference.getSegmentCounts(options.valueOf(CODER));
            }
                    
            Segmenter segmenter = new BayesSegWrapper(options);
            
            if (options.has(ESTIMATE_PRIOR)) {
                System.out.println(
                        ((BayesSegWrapper) segmenter).estimatePrior(texts, segmentCounts));
            } else {
                Map<String,Segmentation> segmentations = segmenter.segmentTexts(texts, segmentCounts);
                segmentations.keySet().forEach(id -> {
                    System.out.println(id);
                    System.out.println(segmentations.get(id));
                    System.out.println();
                });
            }

            // todo: write segmentation JSON to stdout
        }
    }
    

}
