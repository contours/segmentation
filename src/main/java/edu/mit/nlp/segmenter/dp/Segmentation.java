package edu.mit.nlp.segmenter.dp;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Segmentation  {
    
    private final List<Segment> segments;
    
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
    
    @Override
    public String toString() {
        return "[" + this.segments.stream()
                .map(segment -> Integer.toString(segment.length))
                .collect(Collectors.joining(", ")) + "]";
    }

}
