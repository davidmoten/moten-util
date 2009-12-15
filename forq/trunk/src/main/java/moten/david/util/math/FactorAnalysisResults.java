package moten.david.util.math;

import java.io.PrintWriter;
import java.text.DecimalFormat;

public class FactorAnalysisResults {

	public FactorExtractionMethod extractionMethod;

	private Matrix initial;

	private Matrix correlations;

	private Matrix eigenvalues;

	private Matrix eigenvectors;

	private Vector percentVariance;

	private double eigenvalueThreshold = 0;

	private Matrix principalEigenvalues;

	private Matrix principalEigenvectors;

	private Matrix loadings;

	private Matrix principalLoadings;

	private RotatedLoadings rotatedLoadings;

	private long extractionTimeMs;

	private long rotationTimeMs;

	private String title;
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public static SimpleHeirarchicalFormatter getTextFormatter(
			final PrintWriter out) {
		return new SimpleHeirarchicalFormatter() {

			public void blockFinish() {
				out.println();
			}

			public void blockStart() {
				out.println();
			}

			public void header(String s, boolean collapsed) {
				out.println(s);
			}

			public void item(Object object) {
				out.println(object);
			}

			public void link(String s, String id, Object object, String action) {
				out.println(s);
				out.println(object);
			}

			public void image(String s, String id, Object object, String action) {
				out.println("image: " + s);
				out.println(object);
			}
		};

	}

	public String toString() {
//		StringOutputStream sos = new StringOutputStream();
//		final PrintWriter out = new PrintWriter(sos);
//		SimpleHeirarchicalFormatter f = getTextFormatter(out);
//		process(initial, f);
//		return sos.toString();
		return title;
	}

	public void process(Matrix data, SimpleHeirarchicalFormatter f) {

		f.header(extractionMethod.toString(), false);
		f.blockStart();

		// f.header("Raw Data", true);
		// f.blockStart();
		// f.item(initial);
		// f.blockFinish();

		f.header("Correlations", true);
		f.blockStart();
		f.item(correlations);
		f.blockFinish();

		f.header("Eigenvalues", true);
		f.blockStart();
		f.item("Extraction time = "
				+ new DecimalFormat("0.000").format(extractionTimeMs / 1000.0)
				+ "s");
		f.item(eigenvalues.getDiagonal().setRowLabels(
				eigenvalues.getRowLabels()).setColumnLabel(1, "Eigenvalue"));
		f.blockFinish();

		f.header("Eigenvectors", true);
		f.blockStart();
		f.item(eigenvectors);
		f.blockFinish();

		f.header("Percent Variance", true);
		f.blockStart();
		f.item(percentVariance);
		f.blockFinish();

		f.header("Loadings", true);
		f.blockStart();
		f.item(loadings);
		f.blockFinish();

		f.header("Eigenvalue Threshold", true);
		f.blockStart();
		f.item("eigenvalue threshold to extract principal loadings = "
				+ eigenvalueThreshold);
		f.blockFinish();

		f.header("Principal Eigenvalues", true);
		f.blockStart();
		f.item(principalEigenvalues.getDiagonal().setRowLabels(
				principalEigenvalues.getRowLabels()).setColumnLabel(1,
				"Eigenvalue"));
		f.blockFinish();

		f.header("Principal Eigenvectors", true);
		f.blockStart();
		f.item(principalEigenvectors);
		f.blockFinish();

		f.header("Principal Loadings", true);
		f.blockStart();
		f.item(principalLoadings);
		f.blockFinish();

		f.header("Principal Rotated Loadings", false);
		f.blockStart();
		rotatedLoadings.format(data, f);
		f.blockFinish();

		f.blockFinish();
	}

	public FactorExtractionMethod getExtractionMethod() {
		return extractionMethod;
	}

	public Matrix getInitial() {
		return initial;
	}

	public Matrix getCorrelations() {
		return correlations;
	}

	public Matrix getEigenvalues() {
		return eigenvalues;
	}

	public Matrix getEigenvectors() {
		return eigenvectors;
	}

	public Vector getPercentVariance() {
		return percentVariance;
	}

	public double getEigenvalueThreshold() {
		return eigenvalueThreshold;
	}

	public Matrix getPrincipalEigenvalues() {
		return principalEigenvalues;
	}

	public Matrix getPrincipalEigenvectors() {
		return principalEigenvectors;
	}

	public Matrix getLoadings() {
		return loadings;
	}

	public Matrix getPrincipalLoadings() {
		return principalLoadings;
	}

	public RotatedLoadings getRotatedLoadings() {
		return rotatedLoadings;
	}

	public void setExtractionMethod(FactorExtractionMethod extractionMethod) {
		this.extractionMethod = extractionMethod;
	}

	public void setInitial(Matrix initial) {
		this.initial = initial;
	}

	public void setCorrelations(Matrix correlations) {
		this.correlations = correlations;
	}

	public void setEigenvalues(Matrix eigenvalues) {
		this.eigenvalues = eigenvalues;
	}

	public void setEigenvectors(Matrix eigenvectors) {
		this.eigenvectors = eigenvectors;
	}

	public void setPercentVariance(Vector percentVariance) {
		this.percentVariance = percentVariance;
	}

	public void setEigenvalueThreshold(double eigenvalueThreshold) {
		this.eigenvalueThreshold = eigenvalueThreshold;
	}

	public void setPrincipalEigenvalues(Matrix principalEigenvalues) {
		this.principalEigenvalues = principalEigenvalues;
	}

	public void setPrincipalEigenvectors(Matrix principalEigenvectors) {
		this.principalEigenvectors = principalEigenvectors;
	}

	public void setLoadings(Matrix loadings) {
		this.loadings = loadings;
	}

	public void setPrincipalLoadings(Matrix principalLoadings) {
		this.principalLoadings = principalLoadings;
	}

	public void setRotatedLoadings(RotatedLoadings rotatedLoadings) {
		this.rotatedLoadings = rotatedLoadings;
	}

	public long getExtractionTimeMs() {
		return extractionTimeMs;
	}

	public void setExtractionTimeMs(long extractionTimeMs) {
		this.extractionTimeMs = extractionTimeMs;
	}

	public long getRotationTimeMs() {
		return rotationTimeMs;
	}

	public void setRotationTimeMs(long rotationTimeMs) {
		this.rotationTimeMs = rotationTimeMs;
	}
}