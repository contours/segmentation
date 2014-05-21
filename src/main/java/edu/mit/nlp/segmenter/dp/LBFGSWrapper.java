package edu.mit.nlp.segmenter.dp;

/**
 * This wraps the RISO Limited Memory BFGS quasi-newton optimization package. It
 * attempts to follow the same API as the Weka optimization package, for
 * convenience.
*
 */
import riso.numerical.LBFGS;

public abstract class LBFGSWrapper {

    private int m_max_its;
    //not sure how this parameter comes into play
    private double m_eps; //smaller?
    private final double xtol; //estimate of machine precision.  get this right
    //number of corrections, between 3 and 7
    //a higher number means more computation and time, but more accuracy, i guess
    private int m_num_corrections;

    private double[] m_estimate;
    private double m_value;
    private final int m_num_parameters;
    private boolean m_debug;

    public LBFGSWrapper(int num_parameters) {
        this.m_num_corrections = 3;
        this.xtol = 1.0e-16;
        this.m_eps = 1.0e-3;
        this.m_max_its = 200;
        this.m_num_parameters = num_parameters;
        this.m_estimate = new double[m_num_parameters];
    }

    public abstract double objectiveFunction(double[] x);

    public abstract double[] evaluateGradient(double[] x);

    public double[] getVarbValues() {
        return m_estimate;
    }

    public double getMinFunction() {
        return m_value;
    }

    /**
     * setEstimate Use this to initialize the search
     *
     * @param estimate
     */
    public void setEstimate(double[] estimate) {
        m_estimate = new double[estimate.length];
        System.arraycopy(estimate, 0, m_estimate, 0, estimate.length);
    }

    public void setDebug(boolean debug) {
        m_debug = debug;
    }

    public void setMaxIteration(int max_its) {
        m_max_its = max_its;
    }
    
    public void setEPS(double m_eps) {
        this.m_eps = m_eps;
    }

    public void setNumCorrections(int m_num_corrections) {
        this.m_num_corrections = m_num_corrections;
    }

    public double[] findArgmin() {
        double[] diagco = new double[m_num_parameters];
        int[] iprint = new int[2];
        iprint[0] = m_debug ? 1 : -1;  //output at every iteration (0 for 1st and last, -1 for never)
        iprint[1] = 3; //output the minimum level of info
        int[] iflag = new int[1];
        iflag[0] = 0;
        double[] gradient;
        int iteration = 0;
        do {
            m_value = objectiveFunction(m_estimate);
            gradient = evaluateGradient(m_estimate);
            try {
                LBFGS.lbfgs(m_num_parameters,
                        m_num_corrections,
                        m_estimate,
                        m_value,
                        gradient,
                        false, //true if we're providing the diag of cov matrix Hk0 (?)
                        diagco, //the cov matrix
                        iprint, //type of output generated
                        m_eps,
                        xtol, //estimate of machine precision
                        iflag //i don't get what this is about
                );
            } catch (LBFGS.ExceptionWithIflag e) {
                throw new RuntimeException(e);
            }
            iteration++;
        } while (iflag[0] != 0 && iteration <= m_max_its);
        return m_estimate;
    }


}
