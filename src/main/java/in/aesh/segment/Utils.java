package in.aesh.segment;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
        return loadText(file, f -> f.toPath().toAbsolutePath().toString());
    }

    public static Map.Entry<String,List<String>> loadText(File file, 
            Function<File,String> file2id) throws IOException {
        return Maps.immutableEntry(
                file2id.apply(file), 
                Files.lines(file.toPath()).collect(Collectors.toList()));
    }

    public static ImmutableMap<String,List<String>> loadTexts(List<File> files) {
        return files.stream()
                .map(file -> {
                    try { return loadText(file); }
                    catch (IOException e) { throw new RuntimeException(e); }
                })
                .collect(toImmutableMap());
    }
    
    public static ImmutableMap<String,List<String>> loadTexts(List<File> files, 
            Function<File,String> file2id) {
        return files.stream()
                .map(file -> {
                    try { return loadText(file, file2id); }
                    catch (IOException e) { throw new RuntimeException(e); }
                })
                .collect(toImmutableMap());
    }

    public static ImmutableList<String> removeStopwords(List<String> words, List<String> stopwords) {
        return words.stream()
                .filter((String word) -> !stopwords.contains(word))
                .collect(Utils.toImmutableList());
    }


    public static String clean(String s) {
        String lowercased = s.toLowerCase();
        String filtered = CharMatcher.JAVA_LOWER_CASE
                .or(CharMatcher.WHITESPACE)
                .or(CharMatcher.anyOf("$'"))
                .retainFrom(lowercased);
        String collapsed = CharMatcher.WHITESPACE.trimAndCollapseFrom(filtered, ' ');
        return collapsed.replaceAll("([a-z])('[a-z])", "$1 $2");
    }

    public static ImmutableList<String> loadWords(File file) throws IOException {
        return Files.lines(file.toPath())
                .map((String line) -> line.trim().toLowerCase())
                .collect(Utils.toImmutableList());
    }

    private Utils() {}

}
