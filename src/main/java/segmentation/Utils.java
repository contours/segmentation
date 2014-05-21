package segmentation;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Utils {

    public static <T> Collector<T, ImmutableList.Builder<T>, ImmutableList<T>>
            toImmutableList() {
        return Collector.of(
                ImmutableList.Builder::new,
                ImmutableList.Builder::add,
                (l, r) -> l.addAll(r.build()),
                ImmutableList.Builder<T>::build);
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

    private Utils() {}

}
