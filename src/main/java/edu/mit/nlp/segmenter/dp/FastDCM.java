package edu.mit.nlp.segmenter.dp;

import java.util.function.Function;

public class FastDCM {

    private final int W;
    private final Function<Double,Double> logGamma;
    private double prior;
    private double log_gamma_W_prior;
    private double W_log_gamma_prior;

    public FastDCM(double prior, int W, Function<Double,Double> logGamma) {
        this.prior = prior;
        this.W = W;
        this.logGamma = logGamma;
        this.log_gamma_W_prior = logGamma.apply(W * prior);
        this.W_log_gamma_prior = W * logGamma.apply(prior);
    }

    public double logDCM(int[] counts) {
        double logDCM = this.log_gamma_W_prior - this.W_log_gamma_prior;
        assert (this.W == counts.length) : "W is " + this.W + " but counts.length is " + counts.length;
        double N = 0;
        for (int i = 0; i < counts.length; i++) {
            N += counts[i] + this.prior;
            logDCM += this.logGamma.apply(counts[i] + this.prior);
        }
        logDCM -= this.logGamma.apply(N);
        return logDCM;
    }

    public void setPrior(double prior) {
        this.prior = prior;
        this.log_gamma_W_prior = this.logGamma.apply(W * prior);
        this.W_log_gamma_prior = W * this.logGamma.apply(prior);
    }

    public double getPrior() {
        return this.prior;
    }
}