package edu.mit.util.stats;

public class FastDCM {

    private final int W;
    private final FastGamma gamma;
    private double prior;
    private double log_gamma_W_prior;
    private double W_log_gamma_prior;

    public FastDCM(double prior, int W, FastGamma gamma) {
        this.prior = prior;
        this.W = W;
        this.gamma = gamma;
        this.log_gamma_W_prior = gamma.logGamma(W * prior);
        this.W_log_gamma_prior = W * gamma.logGamma(prior);
    }

    public double logDCM(int[] counts) {
        double logDCM = this.log_gamma_W_prior - this.W_log_gamma_prior;
        assert (this.W == counts.length) : "W is " + this.W + " but counts.length is " + counts.length;
        if (Math.abs(W_log_gamma_prior - this.gamma.logGamma(prior) * this.W) > .0001) {
            System.out.println(String.format("believed: %.4e ; true %.4e", this.W_log_gamma_prior, this.gamma.logGamma(prior) * W));
            System.out.println(String.format("W = %d/%d", W, counts.length));
        }
        double N = 0;
        for (int i = 0; i < counts.length; i++) {
            N += counts[i] + prior;
            logDCM += this.gamma.logGamma(counts[i] + this.prior);
        }
        logDCM -= this.gamma.logGamma(N);
        return logDCM;
    }

    public void setPrior(double prior) {
        this.prior = prior;
        this.log_gamma_W_prior = this.gamma.logGamma(W * prior);
        this.W_log_gamma_prior = W * this.gamma.logGamma(prior);
    }

    public double getPrior() {
        return this.prior;
    }
}
