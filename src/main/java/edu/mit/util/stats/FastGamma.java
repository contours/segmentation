package edu.mit.util.stats;

// todo: replace CERN gamma with Apache Commons Math one
import cern.jet.stat.Gamma;
import java.util.HashMap;
import java.util.Map;

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
