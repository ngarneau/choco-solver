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

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.tree.model.RandomForestModel;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.Featurizer;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.HashMap;

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
    private boolean canReadCPUTime;
    protected String currentPropagator;
    protected HashMap<String, PredictivePropagator> propagators = new HashMap<>(3);
    private Featurizer featurizer;
    private JavaSparkContext sc;
    private Tuples tuples;

    private static RandomForestModel model;
    private HashMap<Double, String> modelPropagators = new HashMap<>(2);

    private PropLargePredictive(IntVar[] vars, Tuples tuples, JavaSparkContext sparkContext) {
        super(vars, PropagatorPriority.QUADRATIC, true);
        createLogFile();
        this.tuples = tuples;
        this.featurizer = new Featurizer(this.solver);
        this.sc = sparkContext;

        if(this.model == null) {
            this.model = RandomForestModel.load(this.sc.sc(), "model");
        }
        this.modelPropagators.put(0.0, "GAC2001+");
        this.modelPropagators.put(1.0, "STR2+");
        this.modelPropagators.put(2.0, "FC");

        this.canReadCPUTime = ManagementFactory.getThreadMXBean().isThreadCpuTimeSupported();
        if(canReadCPUTime){
        	ManagementFactory.getThreadMXBean().setThreadCpuTimeEnabled(true);
        }
    }

    private void createLogFile() {
        File yourFile = new File("score" + System.currentTimeMillis() + ".txt");
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

    public PropLargePredictive(IntVar[] vars, Tuples tuples, PropLargeFactory propagatorFactory, JavaSparkContext sparkContext) {
        this(vars, tuples, sparkContext);
        this.propagators.put("STR2+", propagatorFactory.getStr2(vars, tuples));
        this.propagators.put("GAC2001", propagatorFactory.getGAC2001(vars, tuples));
        this.propagators.put("GAC2001+", propagatorFactory.getGAC2001Positive(vars, tuples));
        this.propagators.put("FC", propagatorFactory.getFC(vars, tuples));
        this.propagators.put("GAC3rm+", propagatorFactory.getGAC3rmPositive(vars, tuples));
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
            // For now with predict always the same thing
            double pred = 0.0;
            this.propagators.get(this.modelPropagators.get(pred)).propagate(evtmask);
        }
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        if(this.generateData) {
            this.generateData(idxVarInProp, mask);
        }
        else {
            // For now with predict always the same thing
            double pred = 0.0;
            this.propagators.get(this.modelPropagators.get(pred)).propagate(idxVarInProp, mask);
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
    
    public Tuples getPropagatorTuple(){
    	return tuples;
    }

    private void generateData(int evtmask) throws ContradictionException{
        String logString = this.initLogEntry(evtmask);

        long totalTime = 0L;
        int n = 1000;
        for(int i = 0; i < n; i++) {
            long starttime = System.nanoTime();
            propagators.get(currentPropagator).propagate(evtmask);
            long endtime = System.nanoTime();
            totalTime += endtime - starttime;
        }
        long avgTime = totalTime / n;

        logString += "\t" + avgTime;
        try {
            bw.write(logString + "\n");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateData(int idxVarInProp, int mask) throws ContradictionException {
        String logString = this.initLogEntry(mask);

        long totalTime = 0L;
        int n = 10000;
        for(int i = 0; i < n; i++) {
            long starttime = System.nanoTime();
            propagators.get(currentPropagator).propagate(idxVarInProp, mask);
            long endtime = System.nanoTime();
            totalTime += endtime - starttime;
        }
        long avgTime = totalTime / n;
        logString += "\t" + avgTime;

        try {
            bw.write(logString + "\n");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String initLogEntry(int mask) {
        String logString = "";
        double[] features = this.featurizer.getFeaturesArray(mask);
        logString += "[";
        for (int i = 0; i < features.length; i++)
        {
            if (i == features.length - 1){
                logString +=  features[i];
            }
            else {
                logString +=  features[i] + ", ";
            }
        }
        logString += "]";
        return logString;
    }


}
