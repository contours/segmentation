package segmentation;

import com.google.common.collect.ImmutableList;
import java.util.stream.Collector;

public final class StreamUtils {

    public static <T> Collector<T, ImmutableList.Builder<T>, ImmutableList<T>>
            toImmutableList() {
        return Collector.of(
                ImmutableList.Builder::new,
                ImmutableList.Builder::add,
                (l, r) -> l.addAll(r.build()),
                ImmutableList.Builder<T>::build);
    }

    private StreamUtils() {}

}
