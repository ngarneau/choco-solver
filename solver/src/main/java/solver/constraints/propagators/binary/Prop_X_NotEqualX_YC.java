/**
 *  Copyright (c) 1999-2011, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package solver.constraints.propagators.binary;

import choco.kernel.ESat;
import solver.Solver;
import solver.constraints.Constraint;
import solver.constraints.propagators.Propagator;
import solver.constraints.propagators.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.requests.IRequest;
import solver.requests.PropRequest;
import solver.variables.EventType;
import solver.variables.IntVar;

/**
 * A specific <code>Propagator</code> extension defining filtering algorithm for:
 * <br/>
 * <b>X =/= Y + C</b>
 * <br>where <i>X</i> and <i>Y</i> are <code>Variable</code> objects and <i>C</i> a constant.
 * <br>
 * Only react on X modifications.
 * <br>
 *
 * @author Charles Prud'homme
 * @since 25 nov. 2010
 */
public class Prop_X_NotEqualX_YC extends Propagator<IntVar> {

    IntVar x;
    IntVar y;
    int cste;

    public Prop_X_NotEqualX_YC(IntVar x, IntVar y, int c, Solver solver, Constraint<IntVar, Propagator<IntVar>> constraint) {
        super(new IntVar[]{x, y}, solver, constraint, PropagatorPriority.BINARY, false);
        this.x = x;
        this.y = y;
        this.cste = c;
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        if (vars[vIdx].hasEnumeratedDomain()) {
            return EventType.INSTANTIATE.mask;
        }
        return EventType.INSTANTIATE.mask + EventType.BOUND.mask;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void linkToVariables() {
        requests = new IRequest[1];
        vars[0].addPropagator(this, 0);
        requests[0] = new PropRequest<IntVar, Propagator<IntVar>>(this, vars[0], 0);
        vars[0].addRequest(requests[0]);
    }

    @Override
    public void propagate() throws ContradictionException {
        if (x.instantiated()) {
            removeValOnY();
        }
    }

    @Override
    public void propagateOnRequest(IRequest<IntVar> xRequest, int varIdx, int mask) throws ContradictionException {
        if (EventType.isInstantiate(mask)) {
            removeValOnY();
        } else if (EventType.isBound(mask)){
            propagate();
        }
    }

    @SuppressWarnings({"unchecked"})
    private void removeValOnY() throws ContradictionException {
        if(y.removeValue(x.getValue() - this.cste, this)){
            this.constraint.updateActivity(this);
        }
    }

    @Override
    public ESat isEntailed() {
        if ((x.getUB() < y.getLB() + this.cste) ||
                (y.getUB() < x.getLB() - this.cste))
            return ESat.TRUE;
        else if (x.instantiated()
                && y.instantiated()
                && x.getValue() == y.getValue() + this.cste)
            return ESat.FALSE;
        else
            return ESat.UNDEFINED;
    }

    @SuppressWarnings({"unchecked"})
    public void setPassive() {
        isActive.set(false);
        // then notify the linked variables
        for (int i = 0; i < requests.length; i++) {
            requests[i].getVariable().updateEntailment(requests[i]);
        }
    }
}
