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
package solver.constraints.nary.nogood;

import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.explanations.Deduction;
import solver.explanations.Explanation;
import solver.explanations.ValueRemoval;
import solver.explanations.VariableState;
import solver.variables.EventType;
import solver.variables.IntVar;
import util.ESat;

import java.util.ArrayList;
import java.util.List;

/**
 * A propagator for the specific Nogood store designed to store ONLY positive decisions.
 * <p/>
 * Related to "Nogood Recording from Restarts", C. Lecoutre et al.
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 20/06/13
 */
public class PropNogoodStore extends Propagator<IntVar> {

	List<INogood> units;
	List<INogood> nounits;

	public PropNogoodStore(IntVar[] vars) {
		super(vars, PropagatorPriority.VERY_SLOW, false);
		nounits = new ArrayList<INogood>();
		units = new ArrayList<INogood>();
	}

	@Override
	public int getPropagationConditions(int vIdx) {
		return EventType.INSTANTIATE.mask;
	}

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		unitPropagation();
		boolean again = false;
		for(INogood ng:nounits){
			again |= ng.propagate(this);
		}
		if(again){
			propagate(0);
		}
	}

	public void unitPropagation() throws ContradictionException {
		boolean again = false;
		for (INogood ng : units) {
			again |= (ng.propagate(this) && !ng.getVar(0).hasEnumeratedDomain());
		}
		if(again) {
			unitPropagation();
		}
	}

	@Override
	public ESat isEntailed() {
		for (INogood ng : units) {
			ESat sat = ng.isEntailed();
			if (!sat.equals(ESat.TRUE)) {
				return sat;
			}
		}
		for (INogood ng : nounits) {
			ESat sat = ng.isEntailed();
			if (!sat.equals(ESat.TRUE)) {
				return sat;
			}
		}
		return ESat.TRUE;
	}

    @Override
    public void explain(Deduction d, Explanation e) {
        e.add(solver.getExplainer().getPropagatorActivation(this));
        e.add(this);
        if (d != null && d.getmType() == Deduction.Type.ValRem) {
            ValueRemoval vr = (ValueRemoval) d;
            IntVar var = (IntVar) vr.getVar();
            int val = vr.getVal();
			for(INogood ng:nounits){
				boolean concerned = false;
				for(int i=0;i<ng.size() && !concerned;i++) {
					if (var == ng.getVar(i) && val == ng.getVal(i)) {
						concerned = true;
					}
				}
				if(concerned) {
					for (int i = 0; i < ng.size(); i++) {
						if (var != ng.getVar(i)) {
							ng.getVar(i).explain(VariableState.DOM, e);
						}
					}
				}
			}
		} else {
			super.explain(d, e);
		}
	}

	///*****************************************************************************************************************
	///  DEDICATED TO NOGOOD RECORDING *********************************************************************************
	///*****************************************************************************************************************

	public void addNogood(INogood ng) throws ContradictionException {
		if (ng.isUnit()) {
			units.add(ng);
		}else{
			nounits.add(ng);
		}
	}
}
