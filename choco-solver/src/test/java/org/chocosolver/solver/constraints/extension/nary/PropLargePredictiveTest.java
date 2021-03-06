package org.chocosolver.solver.constraints.extension.nary;

import org.apache.spark.api.java.JavaSparkContext;
import org.chocosolver.memory.IEnvironment;
import org.chocosolver.memory.IStateBitSet;
import org.chocosolver.solver.Settings;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.measure.IMeasures;
import org.chocosolver.solver.variables.IntVar;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

/**
 * Created by Nicolas on 2016-04-04.
 */
@RunWith(MockitoJUnitRunner.class)
public class PropLargePredictiveTest {

    private PropLargePredictive propPredictive;
    private static int DUMB_INT = 1;
    private static long DUMB_DEPTH = 10;
    private Solver solver;
    private IMeasures measures;

    @Before
    public void init() {
        IStateBitSet stateBitSet = mock(IStateBitSet.class);
        Settings settings = mockSettings();
        IEnvironment env = mockEnvironment(stateBitSet);
        measures = mockMeasures();
        solver = mockSolver(settings, env, measures);
        PropLargeFactory factory = mockFactory();
        IntVar[] vars = mockIntVars(solver);
        Tuples tuples = mock(Tuples.class);
        this.propPredictive = new PropLargePredictive(vars, tuples, factory, mock(JavaSparkContext.class));
    }

    @Test
    public void test_propagate_call_current_propagator_evtmask() {
        PredictivePropagator propagator = mock(PredictivePropagator.class);
        this.propPredictive.setStr2Propagator(propagator);
        this.propPredictive.setCurrentPropagator("STR2+");
        try {
            this.propPredictive.propagate(DUMB_INT);
            verify(propagator).propagate(anyInt());
        } catch (ContradictionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_propagate_call_current_propagator_idxVarInProp() {
        PredictivePropagator propagator = mock(PredictivePropagator.class);
        this.propPredictive.setStr2Propagator(propagator);
        this.propPredictive.setCurrentPropagator("STR2+");
        try {
            this.propPredictive.propagate(DUMB_INT, DUMB_INT);
            verify(propagator).propagate(anyInt(), anyInt());
        } catch (ContradictionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_is_entailed_call_current_propagator() {
        PredictivePropagator propagator = mock(PredictivePropagator.class);
        this.propPredictive.setStr2Propagator(propagator);
        this.propPredictive.setCurrentPropagator("STR2+");
        this.propPredictive.isEntailed();
        verify(propagator).isEntailed();
    }

    @Test
    public void when_generate_data_flag_is_off_call_one_propagators() {
        PredictivePropagator propagator = mock(PredictivePropagator.class);
        this.propPredictive.setGenerateData(false);
        this.propPredictive.setStr2Propagator(propagator);
        this.propPredictive.setCurrentPropagator("STR2+");
        try {
            this.propPredictive.propagate(DUMB_INT, DUMB_INT);
            verify(propagator, times(1)).propagate(anyInt(), anyInt());
        } catch (ContradictionException e) {
            e.printStackTrace();
        }
    }


    private IntVar[] mockIntVars(Solver solver) {
        IntVar[] vars = new IntVar[1];
        for (int i = 0; i < vars.length; i++) {
            IntVar intVar = mock(IntVar.class);
            when(intVar.getSolver()).thenReturn(solver);
            vars[i] = intVar;
        }
        when(solver.retrieveIntVars()).thenReturn(vars);
        return vars;
    }

    private PropLargeFactory mockFactory() {
        PropLargeFactory factory = mock(PropLargeFactory.class);
        when(factory.getStr2(any(), any())).thenReturn(mock(PredictivePropagator.class));
        return factory;
    }

    private Settings mockSettings() {
        Settings settings = mock(Settings.class);
        when(settings.cloneVariableArrayInPropagator()).thenReturn(false);
        return settings;
    }

    private IEnvironment mockEnvironment(IStateBitSet stateBitSet) {
        IEnvironment environment = mock(IEnvironment.class);
        when(environment.makeBitSet(anyInt())).thenReturn(stateBitSet);
        return environment;
    }

    private IMeasures mockMeasures() {
        IMeasures measures = mock(IMeasures.class);
        when(measures.getCurrentDepth()).thenReturn(DUMB_DEPTH);
        return measures;
    }

    private Solver mockSolver(Settings settings, IEnvironment environment, IMeasures measures) {
        Solver solver = mock(Solver.class);
        when(solver.getSettings()).thenReturn(settings);
        when(solver.getEnvironment()).thenReturn(environment);
        when(solver.getMeasures()).thenReturn(measures);
        return solver;
    }

}