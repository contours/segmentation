package in.aesh.segment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Segmentation  {
    
    private final List<Segment> segments;
    
    public Segmentation(int[] segmentLengths) {
        this.segments = new ArrayList<>(segmentLengths.length); 
        Arrays.stream(segmentLengths).forEach(length -> {
            int start = this.segments.stream().mapToInt(s -> s.length).sum();
            this.segments.add(new Segment(start, length));
        });
    }
    
    public Segmentation(List<Segment> segments) {
        this.segments = segments;
    }
    
    public Stream<Segment> stream() {
        return this.segments.stream();
    }

    public int[] toArray() {
        return this.segments.stream()
                .mapToInt(segment -> segment.length)
                .toArray();
    }
    
    public List<Integer> toList() {
        return this.segments.stream()
                .mapToInt(segment -> segment.length)
                .boxed()
                .collect(Collectors.toList());
    }
    
    public int size() {
        return this.segments.size();
    }
    
    @Override
    public String toString() {
        return "[" + this.segments.stream()
                .map(segment -> Integer.toString(segment.length))
                .collect(Collectors.joining(", ")) + "]";
    }

}
