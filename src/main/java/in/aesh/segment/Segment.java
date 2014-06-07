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
    public final boolean equals(Object o) {
        if (! (o instanceof Segment)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        Segment s = (Segment) o;
        return ((this.start == s.start) && (this.length == s.length));
    }

    @Override
    public final int hashCode() {
        int hash = 5;
        hash = 67 * hash + this.start;
        hash = 67 * hash + this.length;
        return hash;
    }

    @Override
    public final String toString() {
        return String.format("(%s,%s)", this.start, this.length);
    }
}
