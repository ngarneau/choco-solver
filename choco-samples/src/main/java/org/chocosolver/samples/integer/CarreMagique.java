package org.chocosolver.samples.integer;

import java.util.ArrayList;
import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.constraints.IntConstraintFactory;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.search.limits.FailCounter;
import org.chocosolver.solver.search.loop.monitors.SearchMonitorFactory;
import org.chocosolver.solver.search.strategy.IntStrategyFactory;
import org.chocosolver.solver.trace.Chatterbox;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VariableFactory;

public class CarreMagique {
	public static final int HEURISTIQUE_DEFAUT = 0;
	public static final int HEURISTIQUE_DOMOVERWDEG = 1;
	public static final int HEURISTIQUE_IMPACT_BASED_SEARCH = 2;

	public static final int RESTART_AUCUN = 0;
	public static final int RESTART_LUBY = 1;
	public static final int RESTART_GEOMETRIQUE = 2;

	public static final int COHERENCE_DE_BORNES = 0;
	public static final int COHERENCE_DE_DOMAINE = 1;

	int n;
	int magicSum;

	private SparkConf sparkConf;
	private JavaSparkContext sparkContext;

	public CarreMagique(int size) {
		n = size;
		magicSum = size * (size * size + 1) / 2;
		sparkConf = new SparkConf().setAppName("CarreMagique").setMaster("local[2]");
		sparkContext = new JavaSparkContext(sparkConf);
	}

	public void solve() {

		final int coherence = COHERENCE_DE_DOMAINE;
		final int heuristique = HEURISTIQUE_DEFAUT;
		final int restart = RESTART_AUCUN;
		final boolean bris_symetries = false;

		// Creation du solveur
		Solver solver = new Solver();

		// Creation d'une matrice de dimensions n x n de variables dont les
		// domaines sont les entiers de 1 a n^2.
		IntVar[][] lignes;
		if (coherence == COHERENCE_DE_BORNES)
			lignes = VariableFactory.boundedMatrix("x", n, n, 1, n * n, solver);
		else
			lignes = VariableFactory.enumeratedMatrix("x", n, n, 1, n * n,
					solver);

		// Vecteur contenant toutes les variables de la matrice dans un seul
		// vecteur
		IntVar[] toutesLesVariables = new IntVar[n * n];
		for (int i = 0; i < n * n; i++) {
			toutesLesVariables[i] = lignes[i / n][i % n];
		}
		// Ajout d'une contrainte forcant toutes les variables a prendre des
		// variables differentes
		if (coherence == COHERENCE_DE_BORNES)
			solver.post(IntConstraintFactory.alldifferent(toutesLesVariables,
					"BC"));
		else
			solver.post(IntConstraintFactory.alldifferent(toutesLesVariables,
					"AC"));

		for (Constraint constraint : createTableConstraint(lignes)) {
			solver.post(constraint);
		}

		if (bris_symetries) {
			for (int i = 1; i < n / 2; i++)
				solver.post(IntConstraintFactory.arithm(lignes[i - 1][i - 1],
						"<", lignes[i][i]));

			solver.post(IntConstraintFactory.arithm(lignes[0][0], "<",
					lignes[n - 1][0]));
			solver.post(IntConstraintFactory.arithm(lignes[0][0], "<",
					lignes[0][n - 1]));
			solver.post(IntConstraintFactory.arithm(lignes[0][0], "<",
					lignes[n - 1][n - 1]));
			// solver.post(IntConstraintFactory.arithm(lignes[n - 1][0], "<",
			// lignes[0][n - 1]));
		}

		switch (heuristique) {
		case HEURISTIQUE_DOMOVERWDEG:
			solver.set(IntStrategyFactory.domOverWDeg(toutesLesVariables, 42));
			break;
		case HEURISTIQUE_IMPACT_BASED_SEARCH:
			solver.set(IntStrategyFactory.impact(toutesLesVariables, 42));
			break;
		}

		switch (restart) {
		case RESTART_LUBY:
			SearchMonitorFactory.luby(solver, 2, 2, new FailCounter(solver, 2),
					25000);
			break;
		case RESTART_GEOMETRIQUE:
			SearchMonitorFactory.geometrical(solver, 2, 2.1, new FailCounter(
					solver, 2), 25000);
			break;
		}

		solver.findSolution();
		do {
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					if (lignes[i][j].getValue() < 10)
						System.out.print(" ");
					if (lignes[i][j].getValue() < 100)
						System.out.print(" ");
					System.out.print(lignes[i][j].getValue());
					System.out.print("  ");
				}
				System.out.println("");
			}
			System.out.println();
		} while (solver.nextSolution());
		Chatterbox.printStatistics(solver);
	}

	private List<Constraint> createTableConstraint(IntVar[][] magicSquareVars) {
		Solver linEqSolver = new Solver();
		IntVar[] equationVar = VariableFactory.enumeratedArray(
				"linearEquationVar", n, getAllPossibleValues(), linEqSolver);
		IntVar magicSumVar = VariableFactory.fixed(magicSum, linEqSolver);
		linEqSolver.post(IntConstraintFactory.alldifferent(equationVar));
		linEqSolver.post(IntConstraintFactory.sum(equationVar, magicSumVar));
		Tuples tableEntries = new Tuples();
		if (linEqSolver.findSolution()) {
			do {
				tableEntries.add(getSolutionFromVars(equationVar));
			} while (linEqSolver.nextSolution());
		}
		return generateAllConstraintsFromTuples(tableEntries, magicSquareVars);
	}

	private int[] getAllPossibleValues() {
		int[] possibleValues = new int[n * n];
		for (int i = 0; i < n * n; i++) {
			possibleValues[i] = i + 1;
		}
		return possibleValues;
	}

	private int[] getSolutionFromVars(IntVar[] vars) {
		int[] solution = new int[vars.length];
		for (int i = 0; i < vars.length; i++) {
			solution[i] = vars[i].getValue();
		}
		return solution;
	}

	private List<Constraint> generateAllConstraintsFromTuples(Tuples tuples,
        IntVar[][] magicSquareVars) {
		List<Constraint> constraints = new ArrayList<Constraint>();
		for (int i = 0; i < n; i++) {
			constraints.add(ICF.predictiveTable(magicSquareVars[i], tuples, sparkContext));
			constraints.add(ICF.predictiveTable(getColumn(i, magicSquareVars), tuples, sparkContext));
		}
		constraints.add(ICF.predictiveTable(getDiag1(magicSquareVars), tuples, sparkContext));
		constraints.add(ICF.predictiveTable(getDiag2(magicSquareVars), tuples, sparkContext));
		return constraints;
	}

	private IntVar[] getColumn(int index, IntVar[][] vars) {
		IntVar[] column = new IntVar[n];
		for (int i = 0; i < n; i++) {
			column[i] = vars[i][index];
		}
		return column;
	}

	private IntVar[] getDiag1(IntVar[][] vars) {
		IntVar[] diag1 = new IntVar[n];
		for (int i = 0; i < n; i++) {
			diag1[i] = vars[i][i];
		}
		return diag1;
	}

	private IntVar[] getDiag2(IntVar[][] vars) {
		IntVar[] diag2 = new IntVar[n];
		for (int i = 0; i < n; i++) {
			diag2[i] = vars[i][n - i - 1];
		}
		return diag2;
	}

	public static void main(String[] args) {
		CarreMagique test = new CarreMagique(4);
		test.solve();
	}
}
