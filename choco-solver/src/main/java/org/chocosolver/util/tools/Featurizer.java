package org.chocosolver.util.tools;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.measure.IMeasures;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by Nicolas on 2016-04-06.
 */
public class Featurizer {

    private Solver solver;

    public Featurizer(Solver solver) {
        this.solver = solver;
    }

    public HashMap<String, Double> getFeatures() {
        HashMap<String, Double> features = new HashMap<>();
        IntVar[] vars = solver.retrieveIntVars();
        double[] domainSizes = this.getDomainSizes(vars);
        double[] domainHoles = this.getDomainHoles(vars);
        features.put("mean_domain", StatUtils.mean(domainSizes));
        features.put("f_quart_domain", StatUtils.percentile(domainSizes, 25));
        features.put("m_quart_domain", StatUtils.percentile(domainSizes, 50));
        features.put("l_quart_domain", StatUtils.percentile(domainSizes, 75));
        features.put("mean_holes", StatUtils.mean(domainHoles));
        features.put("f_quart_holes", StatUtils.percentile(domainHoles, 25));
        features.put("m_quart_holes", StatUtils.percentile(domainHoles, 50));
        features.put("l_quart_holes", StatUtils.percentile(domainHoles, 75));
        features.put("current_depth", this.getCurrentDepth());
        return features;
    }

    public double[] getFeaturesArray(int mask) {
        HashMap<String, Double> featuresHashmap = this.getFeatures();
        featuresHashmap.put("mask", (double)mask);
        Collection<Double> featuresCollection = featuresHashmap.values();
        return featuresCollection.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private double[] getDomainSizes(IntVar[] vars) {
        double[] count = new double[vars.length];
        double totalCount = 0.0;
        for(int i = 0; i < vars.length; i++) {
            double domainSize = (double)vars[i].getDomainSize();
            count[i] = domainSize;
            totalCount += domainSize;
        }
        if(totalCount > 0) {
            for (int i = 0; i < vars.length; i++) {
                count[i] = count[i] / totalCount;
            }
        }
        return count;
    }

    private double[] getDomainHoles(IntVar[] vars) {
        double[] count = new double[vars.length];
        double totalCount = 0.0;
        for(int i = 0; i < vars.length; i++) {
            double holes = (double)(vars[i].getRange() - vars[i].getDomainSize());
            count[i] = holes;
            totalCount += holes;
        }
        if(totalCount > 0) {
            for (int i = 0; i < vars.length; i++) {
                count[i] = count[i] / totalCount;
            }
        }
        return count;
    }

    private double getCurrentDepth() {
        IMeasures measures = this.solver.getMeasures();
        return (double) measures.getCurrentDepth();
    }
}
