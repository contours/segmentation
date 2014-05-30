package edu.mit.nlp.segmenter.dp;

import java.util.Arrays;
import java.util.function.IntToDoubleFunction;
import org.apache.commons.math3.special.Gamma;

/**
 *
 * @author Ryan Shaw <ryanshaw@unc.edu>
 */
public interface DirichletMultinomial {

    /**
     * Calculate the log-likelihood of the given vector of category counts.
     *
     * @param α concentration parameter
     * @param counts vector of category counts
     * @return the log-likelihood
     */
    static double logLikelihood(double α, int[] counts) {
        int K = counts.length;
        double A = K * α;
        int N = Arrays.stream(counts).sum();
        return -(lnΓ(A + N) - lnΓ(A)) + sumOver(counts, x -> lnΓ(α + x) - lnΓ(α));
    }

    /**
     * Calculate the gradient (derivative) of the log-likelihood of the the
     * given vector of category counts.
     *
     * @param α concentration parameter
     * @param counts vector of category counts
     * @return the gradient of the log-likelihood
     */
    static double logLikelihoodGradient(double α, int[] counts) {
        int K = counts.length;
        double A = K * α;
        int N = Arrays.stream(counts).sum();
        return α * (K * (ψ(A) - ψ(N + A) - ψ(α)) + sumOver(counts, x -> ψ(x + α)));
    }

    static double sumOver(int[] xs, IntToDoubleFunction f) {
        return Arrays.stream(xs).mapToDouble(f).sum();
    }

    static double lnΓ(double x) {
        return Gamma.logGamma(x);
    }

    static double ψ(double x) {
        return Gamma.digamma(x);
    }
}
