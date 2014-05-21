package edu.mit.util.stats;

import org.apache.commons.math3.special.Gamma;
import java.util.HashMap;
import java.util.Map;

// todo: get rid of this class in favor of a generic memoizer
public class FastDigamma {
    private final Map<Double,Double> memo; 
    
    public FastDigamma(){ 
        memo = new HashMap<>();
    }
    
    public double digamma(double in){
        return memo.computeIfAbsent(in, Gamma::digamma);
    }
}
