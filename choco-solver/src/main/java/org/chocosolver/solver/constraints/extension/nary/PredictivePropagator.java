package org.chocosolver.solver.constraints.extension.nary;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.variables.IntVar;

/**
 * Created by Nicolas on 2016-04-04.
 */
public abstract class PredictivePropagator extends Propagator<IntVar> {

    public PredictivePropagator(IntVar[] vars_, PropagatorPriority priority, boolean b) {
        super(vars_, priority, false);
    }

}
