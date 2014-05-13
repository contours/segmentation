package segmentation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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
            
            List<List<String>> texts = loadTexts(options.valuesOf(FILES));

            // possibly use withValuesConvertedBy to validate that these are
            // actually existing textfiles?

            // have option for segmentCounts if there is only one file?
            // otherwise load a file with filenames and counts?
            // segmentCounts = 
            List<Integer> segmentCounts = Arrays.asList(options.valueOf(NUM_SEGMENTS));
                    
            Segmenter segmenter = new BayesSegWrapper(options);
            List<int[]> segmentations = segmenter.segmentTexts(
                texts, segmentCounts);
            
            System.out.println(Arrays.deepToString(segmentations.toArray()));

            // if just one input file or stdin, print to stdout

            // otherwise write to outfiles; default to infile names + ".json"
        }
    }
    
    public static List<String> loadText(File file) throws IOException {
        return Files.lines(file.toPath()).collect(Collectors.toList());
    }

    private static List<List<String>> loadTexts(List<File> files) {
        return files.stream().map(file -> {
            try {
                return loadText(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }
}
