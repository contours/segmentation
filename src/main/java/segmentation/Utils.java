package segmentation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Utils {

    // from: https://gist.github.com/JakeWharton/9734167
    public static <T> Collector<
                T,                             // input element type
                ImmutableList.Builder<T>,      // accumulation type
                ImmutableList<T>>              // result type
        toImmutableList() {
        return Collector.of(
                ImmutableList.Builder::new,    // supplier
                ImmutableList.Builder::add,    // accumulator
                (l,r) -> l.addAll(r.build()),  // combiner
                ImmutableList.Builder::build); // finisher
    }
            
    public static <K,V> Collector<
                    Map.Entry<K,V>,            // input element type
                    ImmutableMap.Builder<K,V>, // accumulation type
                    ImmutableMap<K,V>>         // result type
        toImmutableMap() {
        return Collector.of(
                ImmutableMap.Builder::new,     // supplier
                ImmutableMap.Builder::put,     // accumulator
                (l,r) -> l.putAll(r.build()),  // combiner
                ImmutableMap.Builder::build);  // finisher
    }
            
    public static <T> Stream<T> stream(Iterable<T> it) {
        return StreamSupport.stream(it.spliterator(), false);
    }
    
    /**
     * Memoizer takes a function as input, and returns a memoized version of 
     * the same function. The returned function is not threadsafe.
     * from: http://stackoverflow.com/a/3935792
     * 
     * @param <F>
     *          the input type of the function
     * @param <T>
     *          the output type of the function
     * @param inputFunction
     *          the input function to be memoized
     * @return the new, non-threadsafe memoized function
     */
    public static <F,T> Function<F,T> memoize(final Function<F,T> inputFunction) {
        return memoize(inputFunction, 16, 0.75f);
    }
    
    public static <F,T> Function<F,T> memoize(final Function<F,T> inputFunction, 
            int init_size, float load_factor) {
        return new Function<F,T>() {
            // Holds previous results
            Map<F, T> memoization = new HashMap<>(init_size, load_factor);
            
            @Override
            public T apply(final F input) {
                return memoization.computeIfAbsent(input, inputFunction);
            }
        };
    }

    public static Map.Entry<String,List<String>> loadText(File file) throws IOException {
        Path path = file.toPath();
        String abspath = path.toAbsolutePath().toString();
        return Maps.immutableEntry(abspath, Files.lines(path).collect(Collectors.toList()));
    }

    public static Map<String,List<String>> loadTexts(List<File> files) {
        return files.stream()
                .map(file -> {
                    try { return loadText(file); }
                    catch (IOException e) { throw new RuntimeException(e); }
                })
                .collect(toImmutableMap());
    }

    private Utils() {}

}
