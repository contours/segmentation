package segmentation;

import com.google.common.collect.ImmutableList;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class StreamUtils {

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

    private StreamUtils() {}

}
