package org.chocosolver.solver.constraints.extension.nary;

import org.chocosolver.memory.IEnvironment;
import org.chocosolver.memory.IStateBitSet;
import org.chocosolver.solver.Settings;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.testng.annotations.*;

import static org.mockito.Mockito.*;

/**
 * Created by Nicolas on 2016-04-04.
 */
public class PropLargePredictiveTest {

    private PropLargePredictive propPredictive;
    private static int DUMB_INT = 1;

    @BeforeClass
    public void setUp() {
        IStateBitSet stateBitSet = mock(IStateBitSet.class);
        Settings settings = mockSettings();
        IEnvironment env = mockEnvironment(stateBitSet);
        Solver solver = mockSolver(settings, env);
        PropLargeFactory factory = mockFactory();
        IntVar[] vars = mockIntVars(solver);
        Tuples tuples = mock(Tuples.class);
        this.propPredictive = new PropLargePredictive(vars, tuples, factory);
    }

    @Test
    public void test_propagate_call_current_propagator_evtmask() {
        Propagator<IntVar> propagator = mock(Propagator.class);
        this.propPredictive.setCurrentPropagator(propagator);
        try {
            this.propPredictive.propagate(DUMB_INT);
            verify(propagator).propagate(anyInt());
        } catch (ContradictionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_propagate_call_current_propagator_idxVarInProp() {
        Propagator<IntVar> propagator = mock(Propagator.class);
        this.propPredictive.setCurrentPropagator(propagator);
        try {
            this.propPredictive.propagate(DUMB_INT, DUMB_INT);
            verify(propagator).propagate(anyInt(), anyInt());
        } catch (ContradictionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_is_entailed_call_current_propagator() {
        Propagator<IntVar> propagator = mock(Propagator.class);
        this.propPredictive.setCurrentPropagator(propagator);
        this.propPredictive.isEntailed();
        verify(propagator).isEntailed();
    }

    private IntVar[] mockIntVars(Solver solver) {
        IntVar[] vars = new IntVar[1];
        for (int i = 0; i < vars.length; i++) {
            IntVar intVar = mock(IntVar.class);
            when(intVar.getSolver()).thenReturn(solver);
            vars[i] = intVar;
        }
        return vars;
    }

    private PropLargeFactory mockFactory() {
        PropLargeFactory factory = mock(PropLargeFactory.class);
        when(factory.getStr2(any(), any())).thenReturn(mock(PropTableStr2.class));
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

    private Solver mockSolver(Settings settings, IEnvironment environment) {
        Solver solver = mock(Solver.class);
        when(solver.getSettings()).thenReturn(settings);
        when(solver.getEnvironment()).thenReturn(environment);
        return solver;
    }

}