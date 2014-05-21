package edu.mit.util.stats;

import org.apache.commons.math3.special.Gamma;
import java.util.HashMap;
import java.util.Map;

// todo: get rid of this class in favor of a generic memoizer
public class FastGamma {
    
    private final Map<Double,Double> memo;

    public FastGamma(){ 
        this.memo = new HashMap<>();
    }
    
    public FastGamma(int init_size, float load_factor){ 
        this.memo = new HashMap<>(init_size, load_factor);
    }
    
    public double logGamma(final double in){
        return memo.computeIfAbsent(in, Gamma::logGamma);
    }
}
