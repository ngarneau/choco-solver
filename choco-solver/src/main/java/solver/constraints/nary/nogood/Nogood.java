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

import solver.exception.ContradictionException;
import solver.variables.IntVar;
import util.ESat;

/**
 * A class to define a Nogood of size > 1.
 * <p/>
 * Made of a list of variables, a list of values and an int.
 * {vars, values} matches positive decisions.
 * A positive decision d_i is vars_i=values_i.
 * <p/>
 * Related to "Nogood Recording from Restarts", C. Lecoutre et al.
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 20/06/13
 */
public class Nogood implements INogood {

	private final static int NOT_FOUND = -1;
	private final static int ENTAILED = -2;

	final IntVar[] vars;
	final int[] values;
	int idx1, idx2;

	public Nogood(IntVar[] vars, int[] values) {
		this.values = values;
		this.vars = vars;
		assert values.length==vars.length;
	}

	public boolean propagate(PropNogoodStore pngs) throws ContradictionException {
		if(idx1==ENTAILED || idx2==ENTAILED){
			return false;
		}
		if(idx1 == NOT_FOUND || vars[idx1].isInstantiated()){
			idx1 = computeNot(idx2);
		}
		if(idx2 == NOT_FOUND || vars[idx2].isInstantiated()){
			idx2 = computeNot(idx1);
		}
		if(idx1==ENTAILED || idx2==ENTAILED){
			return false;
		}
		if(idx1==NOT_FOUND || idx2==NOT_FOUND){
			if(idx1==idx2){
				pngs.contradiction(vars[0],"");
			}else{
				int tmp = Math.max(idx1,idx2);
				return vars[tmp].removeValue(values[tmp],pngs);
			}
		}
		return false;
	}

	private int computeNot(int not){
		for (int i = 0; i < vars.length; i++) {
			if (vars[i].contains(values[i])) {
				if (i!=not && !vars[i].isInstantiated()) {
					return i;
				}
			} else {
				return ENTAILED;
			}
		}
		return NOT_FOUND;
	}

	@Override
	public boolean isUnit() {
		return false;
	}

	public ESat isEntailed() {
		int c = 0;
		for (int i = 0; i < vars.length; i++) {
			if (vars[i].contains(values[i])) {
				if (vars[i].isInstantiated()) {
					c++;
				}
			} else {
				return ESat.TRUE;
			}
		}
		return c == vars.length ? ESat.FALSE : ESat.UNDEFINED;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < vars.length; i++) {
			sb.append(vars[i].getName()).append("==").append(values[i]).append(',');
		}
		return sb.toString();
	}

	@Override
	public int size() {
		return vars.length;
	}

	@Override
	public IntVar getVar(int i) {
		return vars[i];
	}

	@Override
	public int getVal(int i) {
		return values[i];
	}
}
