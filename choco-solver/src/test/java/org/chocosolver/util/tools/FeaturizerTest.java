package org.chocosolver.util.tools;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.measure.IMeasures;
import org.chocosolver.solver.variables.IntVar;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.TreeMap;

import static org.mockito.Mockito.*;

/**
 * Created by Nicolas on 2016-04-06.
 */
public class FeaturizerTest {

    private static int DUMB_DOMAIN_SIZE = 2;
    private static int DUMB_RANGE = 4;

    @Test
    public void test_get_features_call_solver_get_variables() {
        Solver solver = mockSolver();
        IntVar[] vars = mockIntVars(solver);
        Featurizer featurizer = new Featurizer(solver);

        featurizer.getFeatures();

        verify(solver).retrieveIntVars();
    }

    @Test
    public void test_get_features_call_variables_get_domain_size() {
        Solver solver = mockSolver();
        IntVar[] vars = mockIntVars(solver);
        Featurizer featurizer = new Featurizer(solver);

        featurizer.getFeatures();

        for(int i = 0; i < vars.length; i++) {
            verify(vars[i], atLeastOnce()).getDomainSize();
        }
    }

    @Test
    public void test_get_features_call_variables_get_range() {
        Solver solver = mockSolver();
        IntVar[] vars = mockIntVars(solver);
        Featurizer featurizer = new Featurizer(solver);

        featurizer.getFeatures();

        for(int i = 0; i < vars.length; i++) {
            verify(vars[i]).getRange();
        }
    }

    @Test
    public void test_get_features_return_eight_features() {
        Solver solver = mockSolver();
        IntVar[] vars = mockIntVars(solver);
        Featurizer featurizer = new Featurizer(solver);

        TreeMap<String, Double> features = featurizer.getFeatures();

        assertEquals(12, features.size());
    }



    private IntVar[] mockIntVars(Solver solver) {
        IntVar[] vars = new IntVar[1];
        for (int i = 0; i < vars.length; i++) {
            IntVar intVar = mock(IntVar.class);
            when(intVar.getSolver()).thenReturn(solver);
            when(intVar.getDomainSize()).thenReturn(DUMB_DOMAIN_SIZE);
            when(intVar.getRange()).thenReturn(DUMB_RANGE);
            vars[i] = intVar;
        }
        when(solver.retrieveIntVars()).thenReturn(vars);
        return vars;
    }

    private Solver mockSolver() {
        Solver solver = mock(Solver.class);
        IMeasures measures = mock(IMeasures.class);
        when(solver.getMeasures()).thenReturn(measures);
        when(solver.getCstrs()).thenReturn(new Constraint[]{});
        when(measures.getCurrentDepth()).thenReturn(1L);
        return solver;
    }

}