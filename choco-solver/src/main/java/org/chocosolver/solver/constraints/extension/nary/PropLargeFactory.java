package org.chocosolver.solver.constraints.extension.nary;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.variables.IntVar;

/**
 * Created by Nicolas on 2016-04-04.
 */
public class PropLargeFactory {

    PredictivePropagator getStr2(IntVar[] VARS, Tuples TUPLES) {
        return new PropTableStr2(VARS, TUPLES.toMatrix());
    }

    PredictivePropagator getFC(IntVar[] VARS, Tuples TUPLES) {
        return new PropLargeFC(VARS, TUPLES);
    }

    PredictivePropagator getGAC2001(IntVar[] VARS, Tuples TUPLES) {
        return new PropLargeGAC2001(VARS, TUPLES);
    }

}
