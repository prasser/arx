/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2015 Florian Kohlmayer, Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deidentifier.arx.criteria;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.analysis.function.Exp;
import org.apache.commons.math3.analysis.function.Log;
import org.apache.commons.math3.distribution.BinomialDistribution;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.DataGeneralizationScheme;
import org.deidentifier.arx.DataSubset;
import org.deidentifier.arx.framework.check.groupify.HashGroupifyEntry;
import org.deidentifier.arx.framework.data.DataManager;

/**
 * (e,d)-Differential Privacy implemented with (k,b)-SDGS as proposed in:
 * 
 * Ninghui Li, Wahbeh H. Qardaji, Dong Su:
 * On sampling, anonymization, and differential privacy or, k-anonymization meets differential privacy. 
 * Proceedings of ASIACCS 2012. pp. 32-33
 * 
 * @author Raffael Bild
 * @author Fabian Prasser
 * @author Florian Kohlmayer
 */
public class EDDifferentialPrivacy extends ImplicitPrivacyCriterion{
    
    /** SVUID */
    private static final long        serialVersionUID = 242579895476272606L;

    /** Parameter */
    private final double             epsilon;
    /** Parameter */
    private final double             delta;
    /** Parameter */
    private final int                k;
    /** Parameter */
    private final double             beta;
    /** Parameter */
    private DataSubset               subset;
    /** Parameter */
    private transient DataManager    manager;
    /** Parameter */
    private DataGeneralizationScheme generalization;

    /**
     * Creates a new instance
     * @param epsilon
     * @param delta
     * @param generalization
     */
    public EDDifferentialPrivacy(double epsilon, double delta, 
                                 DataGeneralizationScheme generalization) {
        super(false, false);
        this.epsilon = epsilon;
        this.delta = delta;
        this.generalization = generalization;
        this.beta = calculateBeta(epsilon);
        this.k = calculateK(delta, epsilon, this.beta);
    }
    

    /**
     * Calculates a_n
     * @param n
     * @param epsilon
     * @param beta
     * @return
     */
    private double calculateA(int n, double epsilon, double beta) {
        double gamma = calculateGamma(epsilon, beta);
        return calculateBinomialSum((int) Math.floor(n * gamma) + 1, n, beta);
    }

    /**
     * Calculates beta_max
     * @param epsilon
     * @return
     */
    private double calculateBeta(double epsilon) {
        return 1.0d - (new Exp()).value(-1.0d * epsilon);
    }

    /**
     * Adds summands of the binomial distribution with probability beta
     * @param from
     * @param to
     * @param beta
     * @return
     */
    private double calculateBinomialSum(int from, int to, double beta) {
        BinomialDistribution binomialDistribution = new BinomialDistribution(to, beta);
        double sum = 0.0d;

        for (int j = from; j <= to; ++j) {
            sum += binomialDistribution.probability(j);
        }

        return sum;
    }

    /**
     * Calculates c_n
     * @param n
     * @param epsilon
     * @param beta
     * @return
     */
    private double calculateC(int n, double epsilon, double beta) {
        double gamma = calculateGamma(epsilon, beta);
        return (new Exp()).value(-1.0d * n * (gamma * (new Log()).value(gamma / beta) - (gamma - beta)));
    }

    /**
     * Calculates delta
     * @param k
     * @param epsilon
     * @param beta
     * @return
     */
    private double calculateDelta(int k, double epsilon, double beta) {
        double gamma = calculateGamma(epsilon, beta);
        int n_m = (int) Math.ceil((double) k / gamma - 1.0d);

        double delta = Double.MIN_VALUE;
        double bound = Double.MAX_VALUE;

        for (int n = n_m; delta < bound; ++n) {
            delta = Math.max(delta, calculateA(n, epsilon, beta));
            bound = calculateC(n, epsilon, beta);
        }

        return delta;
    }

    /**
     * Calculates gamma
     * @param epsilon
     * @param beta
     * @return
     */
    private double calculateGamma(double epsilon, double beta) {
        double power = (new Exp()).value(epsilon);
        return (power - 1.0d + beta) / power;
    }

    /**
     * Calculates k
     * @param delta
     * @param epsilon
     * @param beta
     * @return
     */
    private int calculateK(double delta, double epsilon, double beta) {
        int k = 1;

        for (double delta_k = Double.MAX_VALUE; delta_k > delta; ++k) {
            delta_k = calculateDelta(k, epsilon, beta);
        }

        return k;
    }
    
    /**
     * Returns the epsilon parameter of (e,d)-DP
     * @return
     */
    public double getEpsilon() {
        return epsilon;
    }

    /**
     * Returns the delta parameter of (e,d)-DP
     * @return
     */
    public double getDelta() {
        return delta;
    }

    /**
     * Returns the k parameter of (k,b)-SDGS
     * @return
     */
    public int getK() {
        return k;
    }

    /**
     * Returns the k parameter of (k,b)-SDGS
     * @return
     */
    public double getBeta() {
        return beta;
    }

    @Override
    public int getRequirements(){
        // Requires two counters
        return ARXConfiguration.REQUIREMENT_COUNTER |
               ARXConfiguration.REQUIREMENT_SECONDARY_COUNTER;
    }
    
    /**
     * Returns the defined generalization scheme
     * @return
     */
    public DataGeneralizationScheme getGeneralizationScheme() {
        return this.generalization;
    }

    /**
     * Creates a random sample based on beta
     *
     * @param manager
     */
    public void initialize(DataManager manager){
        
        // Needed for consistent de-serialization. We need to call this
        // method in the constructor of the class DataManager. The following
        // condition should hold, when this constructor is called during 
        // de-serialization, when we must not change the subset.
        if (subset != null && this.manager == null) {
            this.manager = manager;
            return;
        }
        
        // Needed to prevent inconsistencies. We need to call this
        // method in the constructor of the class DataManager. It will be called again, when
        // ARXConfiguration is initialized(). During the second call we must not change the subset.
        if (subset != null && this.manager == manager) {
            return;
        }

        // Create a data subset via sampling based on beta
        Set<Integer> subsetIndices = new HashSet<Integer>();
        SecureRandom rand = new SecureRandom();
        int records = manager.getDataGeneralized().getDataLength();
        for (int i = 0; i < records; ++i) {
            if (rand.nextDouble() < beta) {
                subsetIndices.add(i);
            }
        }
        this.subset = DataSubset.create(records, subsetIndices);
        this.manager = manager;
    }
    
    /**
     * Returns the research subset.
     *
     * @return
     */
    public DataSubset getSubset() {
        return this.subset;
    }

    @Override
    public boolean isAnonymous(HashGroupifyEntry entry) {
        return entry.count >= k;
    }

    @Override
    public String toString() {
        return "("+epsilon+","+delta+")-DP";
    }
}