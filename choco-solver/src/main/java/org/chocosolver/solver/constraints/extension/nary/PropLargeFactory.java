package org.chocosolver.solver.constraints.extension.nary;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.variables.IntVar;

/**
 * Created by Nicolas on 2016-04-04.
 */
public class PropLargeFactory {

    Propagator<IntVar> getStr2(IntVar[] VARS, Tuples TUPLES) {
        return new PropTableStr2(VARS, TUPLES.toMatrix());
    }

}
