package org.chocosolver.util.tools;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.constraints.extension.nary.PropLargePredictive;
import org.chocosolver.solver.search.measure.IMeasures;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.iterators.DisposableValueIterator;

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
        //----------------- HIGH COST FEATURES ----------------------------//
        features.put("phase_transition_indicator", this.getPhaseTransitionIndicator(vars));
        features.put("tup_Per_Vpp_Norm", getTupPerVvpNorm(vars));
        features.put("mean_var_constraints", getMeanNumberOfContraints(vars));
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
    
    private double getPhaseTransitionIndicator(IntVar[] vars){
    	double phaseTransitionIndicator = 0d;
    	for(int cstrIndex = 0; cstrIndex < solver.getNbCstrs(); cstrIndex++){
    		Constraint cstr = solver.getCstrs()[cstrIndex];
    		if(cstr.getPropagator(0) instanceof PropLargePredictive){
	    		PropLargePredictive prop = (PropLargePredictive) cstr.getPropagator(0);
	    		Tuples tuples = prop.getPropagatorTuple();
	    		int maxValue = 0;
	    		for(int intVarIndex =0; intVarIndex < prop.getVars().length; intVarIndex++){
	    			DisposableValueIterator iter = prop.getVar(intVarIndex).getValueIterator(true);
	    			while(iter.hasNext()){
	    				int value = iter.next();
	    				int count = countNumberOfTupleContainingIntVarValue(intVarIndex, value, tuples);
	    				if(maxValue < count){
	    					maxValue = count;
	    				}
	    			}
	    		}
	    		phaseTransitionIndicator += Math.log(1 - maxValue)/Math.log(2);
    		}
    	}
    	double totalDomainSize = 0.0d;
    	for(int allVarIndex = 0; allVarIndex < vars.length; allVarIndex++){
    		IntVar var = vars[allVarIndex];
    		totalDomainSize += Math.log(var.getDomainSize());
    	}
    	return phaseTransitionIndicator / totalDomainSize;
    }
    
    private double getTupPerVvpNorm(IntVar[] vars){
    	double tupPerVvpNorm = 0d;
   		int count = 0;
    	for(int cstrIndex = 0; cstrIndex < solver.getNbCstrs(); cstrIndex++){
    		Constraint cstr = solver.getCstrs()[cstrIndex];
    		if(cstr.getPropagator(0) instanceof  PropLargePredictive){
	    		PropLargePredictive prop = (PropLargePredictive) cstr.getPropagator(0);
	    		Tuples tuples = prop.getPropagatorTuple();
	    		for(int intVarIndex =0; intVarIndex < prop.getVars().length; intVarIndex++){
	    			DisposableValueIterator iter = prop.getVar(intVarIndex).getValueIterator(true);
	    			while(iter.hasNext()){
	    				int value = iter.next();
	    				count += countNumberOfTupleContainingIntVarValue(intVarIndex, value, tuples);
	    			}
	    		}
	    		tupPerVvpNorm  += (double) count / (double) tuples.nbTuples();
    		}
    	}
    	return tupPerVvpNorm;
    }
    
    private int countNumberOfTupleContainingIntVarValue(int varIndex, int value, Tuples tuples){
    	int count = 0;
    	for(int tupleIndex = 0; tupleIndex < tuples.nbTuples(); tupleIndex++){
    		if(tuples.get(tupleIndex)[varIndex] == value){
    			count++;
    		}
    	}
    	return count;
    }
    
    private double getMeanNumberOfContraints(IntVar[] vars){
    	double totalConstraint = solver.getCstrs().length * vars.length;
    	double totalEffectiveConstraint = 0.d;
    	for(IntVar var : vars){
    		for(Constraint cstr : solver.getCstrs()){
    			for(int cstrVarIndex = 0; cstrVarIndex < cstr.getPropagator(0).getVars().length; cstrVarIndex++){
    				if(var.getId() == cstr.getPropagator(0).getVar(cstrVarIndex).getId()){
    					totalEffectiveConstraint++;
    					break;
    				}
    			}
    		}
    	}
    	return totalEffectiveConstraint / totalConstraint;
    }
}
