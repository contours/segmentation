package edu.mit.util.stats;

import java.util.HashMap;
import java.util.Map;

public class FastDigamma {
    private final Map<Double,Double> memo; 
    
    public FastDigamma(){ 
        memo = new HashMap<>();
    }
    
    public double digamma(double in){
        // todo: replace aliasi digamma with Apache Commons Math one
        return memo.computeIfAbsent(in, com.aliasi.util.Math::digamma);
    }
}
