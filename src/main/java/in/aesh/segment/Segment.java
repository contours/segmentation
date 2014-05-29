package in.aesh.segment;

import static com.google.common.base.Preconditions.checkArgument;

public class Segment {
    
    public final int start;
    public final int length;
    
    public Segment(int start, int length) {
        checkArgument(start >= 0, "Segment start must be >= 0; was %s", start);
        checkArgument(length > 0, "Segment length must be > 0; was %s", length);
        this.start = start;
        this.length = length;
    }
    
    @Override
    public String toString() {
        return String.format("(%s,%s)", this.start, this.length);
    }
}
