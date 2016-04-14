/**
 * Copyright (c) 2015, Ecole des Mines de Nantes
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *    This product includes software developed by the <organization>.
 * 4. Neither the name of the <organization> nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.chocosolver.samples.integer;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.chocosolver.samples.AbstractProblem;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.search.strategy.ISF;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VariableFactory;

import java.util.Random;

/**
 * Small illustration of a table constraint
 * @author Guillaume Perez, Jean-Guillaume Fages
 */
public class Table extends AbstractProblem {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	IntVar[] vars;
	int nbTuples = 1000;
	int n = 15;
	int upB = 10000;
	int lowB = -10000;
	SparkConf sparkConf = new SparkConf().setAppName("Test").setMaster("local[2]");
	JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public void buildModel() {
		vars = new IntVar[n];
		for (int i = 0; i < vars.length; i++) {
			vars[i] = VariableFactory.enumerated("Q_" + i, lowB, upB, solver);
		}
		Random rand = new Random(12);
		Tuples tuples = new Tuples(true);
		System.out.println("Allowed tuples");
		for(int i = 0; i < nbTuples ; i++){
			int[] tuple = new int[n];
			for(int j = 0; j < n; j++){
				tuple[j] = rand.nextInt(upB - lowB) + lowB;
				System.out.print(tuple[j] + " ");
			}
			tuples.add(tuple);
			System.out.println();
		}
		solver.post(ICF.predictiveTable(vars,tuples, sparkContext));
	}

	@Override
	public void createSolver(){
		solver = new Solver("Table sample");
	}

	@Override
	public void configureSearch() {
		solver.set(ISF.minDom_LB(vars));
	}

	@Override
	public void prettyOut() {}

	@Override
	public void solve() {
		solver.findAllSolutions();
	}

	//***********************************************************************************
	// MAIN
	//***********************************************************************************

	public static void main(String[] args){
	    new Table().execute(args);
	}
}
