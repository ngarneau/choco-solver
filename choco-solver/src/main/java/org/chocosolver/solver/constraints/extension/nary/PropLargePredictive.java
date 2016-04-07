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
package org.chocosolver.solver.constraints.extension.nary;

import com.sun.org.apache.xalan.internal.utils.FeatureManager;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.measure.IMeasures;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.PropagatorEventType;
import org.chocosolver.solver.variables.ranges.IntIterableBitSet;
import org.chocosolver.solver.variables.ranges.IntIterableSet;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.Featurizer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <br/>
 *
 * @author Nicolas Garneau
 * @since 04/04/16
 */
public class PropLargePredictive extends Propagator<IntVar> {

    private BufferedWriter bw;
    private boolean streamReady = false;
    private boolean generateData = false;
    protected String currentPropagator;
    protected HashMap<String, PredictivePropagator> propagators = new HashMap<>(3);
    private Featurizer featurizer;

    private PropLargePredictive(IntVar[] vars) {
        super(vars, PropagatorPriority.QUADRATIC, true);
        createLogFile();
        this.featurizer = new Featurizer(this.solver);
    }

    private void createLogFile() {
        File yourFile = new File("score.txt");
        if(!yourFile.exists()) {
            try {
                yourFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileWriter fw = new FileWriter(yourFile);
            bw = new BufferedWriter(fw);
            streamReady = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PropLargePredictive(IntVar[] vars, Tuples tuples, PropLargeFactory propagatorFactory) {
        this(vars);
        this.propagators.put("STR2+", propagatorFactory.getStr2(vars, tuples));
        this.propagators.put("GAC2001", propagatorFactory.getGAC2001(vars, tuples));
        this.propagators.put("GAC2001+", propagatorFactory.getGAC2001Positive(vars, tuples));
        this.currentPropagator = "STR2+";
    }

    public void setGenerateData(boolean flag) {
        this.generateData = flag;
    }

    public void setCurrentPropagator(String index) {
        this.currentPropagator = index;
    }

    public void setStr2Propagator(PredictivePropagator propagator) {
        this.propagators.put("STR2+", propagator);
    }

    public void setFCPropagator(PredictivePropagator propagator) {
        this.propagators.put("FC", propagator);
    }

    public void setGAC2001Propagator(PredictivePropagator propagator) {
        this.propagators.put("GAC2001", propagator);
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if(this.generateData) {
            this.generateData(evtmask);
        }
        else {
            this.propagators.get(this.currentPropagator).propagate(evtmask);
        }
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        if(this.generateData) {
            this.generateData(idxVarInProp, mask);
        }
        else {
            this.propagators.get(this.currentPropagator).propagate(idxVarInProp, mask);
        }
    }

    @Override
    public ESat isEntailed() {
        return this.propagators.get(this.currentPropagator).isEntailed();
    }

    @Override
    public String toString() {
        return this.propagators.get(this.currentPropagator).toString();
    }

    private void generateData(int evtmask) throws ContradictionException{
        String logString = this.initLogEntry();
        long startTime = System.nanoTime();
        this.propagators.get(this.currentPropagator).propagate(evtmask);
        long endTime = System.nanoTime();
        long elapsedTime = endTime - startTime;
        logString += "time:" + elapsedTime;

        try {
            bw.write(logString + "\n");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateData(int idxVarInProp, int mask) throws ContradictionException {
        String logString = this.initLogEntry();
        long startTime = System.nanoTime();
        this.propagators.get(this.currentPropagator).propagate(idxVarInProp, mask);
        long endTime = System.nanoTime();
        long elapsedTime = endTime - startTime;
        logString += "time:" + elapsedTime;

        try {
            bw.write(logString + "\n");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String initLogEntry() {
        String solverVariablesState = this.getSolverVariablesState();
        String logString = solverVariablesState;
        HashMap<String, Double> features = this.featurizer.getFeatures();
        for (Map.Entry<String, Double> entry : features.entrySet())
        {
            logString += entry.getKey() + ":" + entry.getValue() + ", ";
        }
        return logString;
    }

    private String getSolverVariablesState() {
        String logString = "";
        IntVar[] solverVars;
        solverVars = solver.retrieveIntVars();
        for(int i = 0; i < solverVars.length; i++) {
            IntVar currentVar = solverVars[i];
            if(currentVar.isInstantiated()) {
                logString += currentVar.getName() + ":" +currentVar.getValue() + ",";
            }
            else {
                logString += currentVar.getName() + ":None,";
            }
        }
        return logString;
    }


}
