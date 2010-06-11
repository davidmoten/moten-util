package moten.david.util.math.gp;

import java.awt.Point;
import java.util.Map;

import moten.david.util.math.Matrix;
import moten.david.util.math.Matrix.MatrixFunction;

public class ReferenceQuartimaxCriterion implements MatrixFunction {

	private Matrix reference;
	private Matrix matrix;

	public ReferenceQuartimaxCriterion(Matrix matrix, Matrix reference) {
		this.reference = reference;
		this.matrix = matrix;
	}

	
	public double function(Matrix m) {
		Map<Point, Double> map = matrix.times(m).getMatchedCorrelations(
				reference);
		//TODO write the quartimax criterion
		throw new Error("not implemented yet");
	}
}
