package in.aesh.segment;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Generic interface for a topic segmenter.
 * @author Ryan Shaw <ryanshaw@unc.edu>
 */
public abstract class Segmenter {

    /**
     * Service loader for segmenter implementations.
     */
    private static final ServiceLoader<Segmenter> loader = ServiceLoader.load(Segmenter.class);

    /**
     * Load and return the segmenter with the specified name.
     * @param name
     * @return a Segmenter implementation
     * @throws IllegalArgumentException if no segmenter by that name
     */
    public static final Segmenter load(String name) {
        for (Segmenter s : loader) {
            if (s.getName().equals(name)) {
                return s;
            }
        }
        throw new IllegalArgumentException(String.format("no segmenter named '%s'", name));
    }

    /**
     * Load all known segmenters.
     * @return a list of Segmenter implementations
     */
    public static final ImmutableList<Segmenter> loadAll() {
        return Utils.stream(loader).collect(Utils.toImmutableList());
    }

    /**
     * The name to be specified via the CLI for using this segmenter.
     * @return name
     */
    public abstract String getName();

    /**
     * Configure a parser to accept segmenter-specific options.
     * @param parser
     */
    public abstract void addOptions(OptionParser parser);

    /**
     * Configure this segmenter with the specific options.
     * @param options
     */
    public abstract void init(OptionSet options);

    /**
     * Segment a set of texts.
     *
     * @param texts a map of text IDs to lists of lists of words
     * @param desiredNumSegments a map of text IDs to desired segment counts
     * @return a map of text IDs to segmentations
     */
    public abstract Map<String,Segmentation> segmentTexts(
        Map<String,List<List<String>>> texts, Map<String,Integer> desiredNumSegments);
}
