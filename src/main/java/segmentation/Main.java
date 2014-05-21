package segmentation;

import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import segmentation.wrappers.BayesSegWrapper;

public class Main {
    public static void main(String[] args) throws IOException {

        OptionParser parser = BayesSegWrapper.OPTIONS;
        
        OptionSpec<File> FILES = parser.nonOptions().ofType(File.class);
        OptionSpec<Integer> NUM_SEGMENTS = parser.accepts("num-segments")
                .withRequiredArg().ofType(Integer.class);
        OptionSpec<Void> HELP = parser.accepts("help").forHelp();

        OptionSet options = parser.parse(args);

        if (options.has(HELP)) {
            parser.printHelpOn(System.out);
        } else {

            // todo: handle System.in if no file args
            
            List<File> files = options.valuesOf(FILES);
            Map<String,List<String>> texts = Utils.loadTexts(files);

            // possibly use withValuesConvertedBy to validate that these are
            // actually existing textfiles?

            // have option for segmentCounts if there is only one file?
            // otherwise load a file with filenames and counts?
            // segmentCounts = 
            if (texts.size() != 1) {
                throw new IllegalArgumentException("cannot handle more than 1 text yet");
            }
            Map<String,Integer> segmentCounts = texts.keySet().stream()
                    .map(key -> Maps.immutableEntry(key, options.valueOf(NUM_SEGMENTS)))
                    .collect(Utils.toImmutableMap());
                    
            // TODO: check that segment counts <= sentence counts
            
            Segmenter segmenter = new BayesSegWrapper(options);
            Map<String,List<Integer>> segmentations = segmenter.segmentTexts(texts, segmentCounts);
            
            segmentations.keySet().forEach(id -> {
                System.out.println(id);
                System.out.println(Arrays.toString(segmentations.get(id).toArray()));
                System.out.println();
            });


            // if just one input file or stdin, print to stdout

            // otherwise write to outfiles; default to infile names + ".json"
        }
    }
    

}
