package au.edu.anu.delibdem.qsort;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import moten.david.util.math.FactorAnalysisException;
import moten.david.util.math.FactorAnalysisResults;
import moten.david.util.math.FactorExtractionMethod;
import moten.david.util.math.Function;
import moten.david.util.math.Matrix;
import moten.david.util.math.RegressionIntervalFunction;
import moten.david.util.math.SimpleHeirarchicalFormatter;
import moten.david.util.math.Vector;
import moten.david.util.math.Varimax.RotationMethod;
import moten.david.util.math.gui.GraphPanel;

import org.apache.commons.math.stat.regression.SimpleRegression;

public class Data implements Serializable {

	private static Logger log = Logger.getLogger(Data.class.getName());

	public static final String participantPrefix = "P";
	private static final long serialVersionUID = -8216642174736641063L;
	private static final String TAB = "\t";

	private final Map<Integer, String> statements = new HashMap<Integer, String>();

	public static final String PREDICTION_INTERVAL_95 = "Prediction_Interval_95";

	private final Map<String, Participant> participants = new HashMap<String, Participant>();
	private final Set<String> excludeParticipants = new TreeSet<String>();

	public List<String> getParticipants() {
		TreeSet<String> set = new TreeSet<String>();
		for (QSort q : qSorts) {
			set.add(q.getParticipant().getId());
		}
		return new ArrayList<String>(set);
	}

	private List<QSort> qSorts;
	private String title = "Untitled";

	public Data(String name) throws IOException {
		System.out.println("loading " + name);
		InputStream is = new FileInputStream(name);
		load(is);
		is.close();
	}

	public Data(File file) throws IOException {
		InputStream is = new FileInputStream(file);
		load(is);
		is.close();
	}

	public Data(InputStream is) throws IOException {
		load(is);
	}

	public Data() throws IOException {
	}

	public Set<String> getStageTypes() {
		Set<String> set = new TreeSet<String>();
		for (QSort q : qSorts) {
			set.add(q.getStage());
		}
		return set;
	}

	public Set<String> getParticipantTypes() {
		Set<String> set = new TreeSet<String>();
		for (QSort q : qSorts) {
			set.addAll(participants.get(q.getParticipant().getId()).getTypes());
		}
		return set;
	}

	private static enum Marker {
		STARTED, TITLE_READ, NUM_PARTICIPANTS_READ, NUM_VARIABLES_READ, PARTICIPANT_DATA_READ, NUM_Q_STATEMENTS_READ, NUM_P_STATEMENTS_READ, DATA_READ, STATEMENTS_READ, STATEMENTS_DATA_READ;
	}

	public void load(InputStream is) throws IOException {
		log.info("loading data..");
		InputStreamReader isr = new InputStreamReader(is);
		LineCountingReader br = new LineCountingReader(new BufferedReader(isr));

		String line;
		this.qSorts = new ArrayList<QSort>();

		Marker marker = Marker.STARTED;
		int numParticipants = 0;
		int numParticipantsRead = 0;
		int numQStatements = 0;
		int numPStatements = 0;
		int numVariables = 0;
		while ((line = nextLine(br)) != null) {
			log.info(line);

			if (marker.equals(Marker.STARTED) && line.startsWith(":Title")) {
				title = getValue(line, 1);
				marker = Marker.TITLE_READ;
			} else if (marker.equals(Marker.TITLE_READ)
					&& line.startsWith(":Participants")) {
				numParticipants = Integer.parseInt(getValue(line, 1));
				marker = Marker.NUM_PARTICIPANTS_READ;
			} else if (marker.equals(Marker.NUM_PARTICIPANTS_READ)
					&& line.startsWith(":Variables")) {
				numVariables = Integer.parseInt(getValue(line, 1));
				marker = Marker.NUM_VARIABLES_READ;
			} else if (marker.equals(Marker.NUM_VARIABLES_READ)
					&& !line.startsWith(":")) {
				// is participant data
				numParticipantsRead++;
				String[] items = line.split(TAB);
				Participant participant = new Participant(items[0]);
				participants.put(participant.getId(), participant);
				for (int i = 0; i < numVariables; i++) {
					String value = items[i + 1];
					participant.getTypes().add(value);
				}
			} else if (marker.equals(Marker.NUM_VARIABLES_READ)
					&& line.startsWith(":Q Statements")) {
				if (numParticipantsRead != numParticipants)
					throw new RuntimeException(
							"Number of participants doesn't match the declared value on the :Participants line (declared="
									+ numParticipants
									+ ",actual="
									+ numParticipantsRead);
				numQStatements = Integer.parseInt(getValue(line, 1));
				marker = Marker.NUM_Q_STATEMENTS_READ;
			} else if (marker.equals(Marker.NUM_Q_STATEMENTS_READ)
					&& line.startsWith(":P Statements")) {
				numPStatements = Integer.parseInt(getValue(line, 1));
				marker = Marker.NUM_P_STATEMENTS_READ;
			} else if (marker.equals(Marker.NUM_P_STATEMENTS_READ)
					&& !line.startsWith(":")) {
				// is qsort data
				String[] items = line.split(TAB);
				QSort q = new QSort();
				Participant participant = participants.get(items[0]);
				if (participant == null)
					throw new RuntimeException(
							"Participant not found from line "
									+ br.linesRead
									+ ". Have you declared it in the participants section?");
				q.setParticipant(participant);
				q.setStage(items[1]);
				for (int j = 2; j < 2 + numQStatements; j++)
					q.getQResults().add(getDouble(items[j]));
				for (int j = 2 + numQStatements; j < 2 + numQStatements
						+ numPStatements; j++)
					q.getRankings().add(getDouble(items[j]));
				qSorts.add(q);
			} else if (marker.equals(Marker.NUM_P_STATEMENTS_READ)
					&& line.startsWith(":Statements")) {
				marker = marker.STATEMENTS_READ;
			} else if (marker.equals(Marker.STATEMENTS_READ)
					&& !line.startsWith(":")) {
				// is statement data
				String[] items = line.split(TAB);
				statements.put(Integer.parseInt(items[0]), items[1]);
			}
		}
		br.close();
		isr.close();
		is.close();
		log.info("loaded");
	}

	/**
	 * Returns the ith value (O based) from the line based on a tab delimiter
	 * 
	 * @param line
	 * @param i
	 * @return
	 */
	private String getValue(String line, int i) {
		String[] items = line.split(TAB);
		if (i >= items.length)
			throw new RuntimeException("Could not read the " + (i + 1)
					+ "th value from the line=" + line
					+ ". Perhaps there is a value missing on this line?");
		return items[i];
	}

	private static class LineCountingReader {
		private final BufferedReader br;

		public int getLinesRead() {
			return linesRead;
		}

		private int linesRead = 0;

		public LineCountingReader(BufferedReader br) {
			this.br = br;
		}

		public String readLine() throws IOException {
			String line = br.readLine();
			if (line != null)
				linesRead++;
			return line;
		}

		public void close() throws IOException {
			br.close();
		}

	}

	private String nextLine(LineCountingReader br) throws IOException {
		String line = br.readLine();
		while (line != null
				&& (line.startsWith("#") || line.trim().length() == 0))
			line = br.readLine();
		return line;
	}

	private Double getDouble(String s) {
		if (s == null || s.trim().equals(""))
			return null;
		else
			return Double.parseDouble(s);
	}

	private Integer getInt(String s) {
		if (s == null || s.trim().equals(""))
			return null;
		else
			return Integer.parseInt(s);
	}

	private int getTerminatingInteger(String s) {
		StringBuffer num = new StringBuffer();
		int i = s.length() - 1;
		while (s.charAt(i) >= '0' && s.charAt(i) <= '9') {
			num.insert(0, s.charAt(i));
			i--;
		}
		Integer result = Integer.parseInt(num.toString());
		return result;
	}

	public Map<Integer, Integer> getOrdered(String columns[], String[] items,
			int starti, String label) {
		Map<Integer, Integer> ordered = new LinkedHashMap<Integer, Integer>();
		int i = starti;
		while (columns[i].startsWith(label)) {
			int n = getTerminatingInteger(columns[i]);
			ordered.put(n, getInt(items[i++]));
		}
		return ordered;
	}

	public List<QSort> getQSorts() {
		List<QSort> list = new ArrayList<QSort>(qSorts);
		for (int i = list.size() - 1; i >= 0; i--)
			if (excludeParticipants.contains(list.get(i).getParticipant()
					.getId())) {
				list.remove(i);
			}
		return list;
	}

	public void graph(String participantType, String stage, String bands,
			OutputStream imageOutputStream, boolean labelPoints, int size,
			Set<Integer> exclusions, Set<Integer> filter) throws IOException {
		List<QSort> subList = restrictList(participantType, stage, exclusions);
		if (stage.equals("all"))
			graphConnected(subList, imageOutputStream, labelPoints, size,
					filter);
		else
			graph(subList, imageOutputStream, labelPoints, size, filter, bands);
	}

	public DataComponents getDataComponents(String participantType,
			String stage, Set<Integer> exclusions, Set<Integer> filter) {
		List<QSort> subList = restrictList(participantType, stage, exclusions);
		return buildMatrix(subList, filter);

	}

	private Set<String> getParticipantTypes(String participantId) {
		return participants.get(participantId).getTypes();
	}

	public List<QSort> restrictList(String participantType, String stage,
			Set<Integer> exclusions) {
		// read data
		List<QSort> list = getQSorts();

		List<QSort> subList = new ArrayList<QSort>();
		for (QSort q : list) {
			if ((stage.equalsIgnoreCase("all") || q.getStage().trim()
					.equalsIgnoreCase(stage))
					&& (participantType.equalsIgnoreCase("all") || getParticipantTypes(
							q.getParticipant().getId()).contains(
							participantType))
					&& (exclusions == null || !exclusions.contains(q
							.getParticipant().getId()))) {

				boolean alreadyGotIt = false;
				for (QSort q2 : subList) {
					if (q2.getParticipant().getId().equals(
							q.getParticipant().getId())
							&& q2.getStage().equals(q.getStage())) {
						alreadyGotIt = true;
						break;
					}
				}
				if (!alreadyGotIt)
					subList.add(q);
			}
		}
		return subList;
	}

	public static class DataComponents {
		public List<QSort> list;
		public Matrix qSorts;
		public Matrix rankings;
		public Matrix correlations;
		public List<String> participants1;
		public List<String> participants2;
	}

	public DataComponents buildMatrix(List<QSort> list, Set<Integer> filter) {
		if (list == null)
			return null;
		list = new ArrayList<QSort>(list);

		// remove from the list all QSort objects that are missing a qResult or
		// a ranking value
		Set<QSort> removeThese = new HashSet<QSort>();
		for (QSort q : list) {
			if (q.getQResults().size() == 0) {
				removeThese.add(q);
			} else {
				for (Double v : q.getQResults()) {
					if (v == null)
						removeThese.add(q);
				}
			}
			if (q.getRankings().size() == 0)
				removeThese.add(q);
			else {
				for (Double v : q.getRankings()) {
					if (v == null)
						removeThese.add(q);
				}
			}
		}

		list.removeAll(removeThese);

		if (list.size() == 0) {
			return null;
		}
		// make the matrix of the qResults
		Matrix qSorts = new Matrix(list.size(), list.get(0).getQResults()
				.size());
		for (int i = 0; i < list.size(); i++) {
			QSort q = list.get(i);
			qSorts.setRowLabel(i + 1, q.getParticipant().getId() + "");
			for (int j = 0; j < q.getQResults().size(); j++) {
				qSorts.setValue(i + 1, j + 1, q.getQResults().get(j));
				qSorts.setColumnLabel(j + 1, "Q" + (j + 1));
			}
		}

		// make the matrix of rankings
		Matrix rankings = new Matrix(list.size(), list.get(0).getRankings()
				.size());
		for (int i = 0; i < list.size(); i++) {
			QSort q = list.get(i);
			rankings.setRowLabel(i + 1, q.getParticipant().getId() + "");
			for (int j = 0; j < q.getRankings().size(); j++) {
				rankings.setValue(i + 1, j + 1, q.getRankings().get(j));
				rankings.setColumnLabel(j + 1, "R" + (j + 1));
			}
		}

		// perform correlations
		Matrix qSortsCorrelated = qSorts.transpose()
				.getPearsonCorrelationMatrix();
		Matrix rankingsCorrelated = rankings.transpose()
				.getPearsonCorrelationMatrix();

		// compare rankings and qSorts
		List<String> participants1 = new ArrayList<String>();
		List<String> participants2 = new ArrayList<String>();
		Matrix m = new Matrix(1, 2);
		for (int i = 1; i <= qSortsCorrelated.rowCount(); i++) {
			for (int j = i + 1; j <= qSortsCorrelated.columnCount(); j++) {
				boolean includeIt = filter == null
						|| filter.size() == 0
						|| filter.contains(Integer.parseInt(qSortsCorrelated
								.getRowLabel(i)))
						|| filter.contains(Integer.parseInt(qSortsCorrelated
								.getRowLabel(j)));
				if (includeIt) {
					if (i != 1 || j != 2)
						m = m.addRow();
					participants1.add(qSortsCorrelated.getRowLabel(i));
					participants2.add(qSortsCorrelated.getRowLabel(j));
					m
							.setValue(m.rowCount(), 1, qSortsCorrelated
									.getValue(i, j));
					m.setValue(m.rowCount(), 2, rankingsCorrelated.getValue(i,
							j));
					m.setRowLabel(m.rowCount(), qSortsCorrelated.getRowLabel(i)
							+ ":" + qSortsCorrelated.getRowLabel(j));
				}
			}
		}
		m.setColumnLabel(1, "qSort");
		m.setColumnLabel(2, "ranking");

		DataComponents dataComponents = new DataComponents();
		dataComponents.list = list;
		dataComponents.qSorts = qSorts;
		dataComponents.rankings = rankings;
		dataComponents.participants1 = participants1;
		dataComponents.participants2 = participants2;
		dataComponents.correlations = m;
		return dataComponents;
	}

	public void writeMatrix(Matrix m, OutputStream os) throws IOException {
		os.write(m.getDelimited(TAB, true).getBytes());
		os.flush();
	}

	public GraphPanel getGraphConnected(List<QSort>[] list,
			boolean labelPoints, int size, Set<Integer> filter) {
		List<Vector> vectors1 = new ArrayList<Vector>();
		List<Vector> vectors2 = new ArrayList<Vector>();
		for (int vi = 0; vi < list.length; vi++) {
			DataComponents d = buildMatrix(list[vi], filter);
			if (d == null)
				return null;
			Matrix m = d.correlations;
			if (m == null)
				return null;

			Vector v1 = m.getColumnVector(1);
			Vector v2 = m.getColumnVector(2);
			for (int i = 1; i <= v1.rowCount(); i++) {
				v1.setRowLabel(i, d.participants2.get(i - 1));
				v2.setRowLabel(i, d.participants1.get(i - 1));
			}
			vectors1.add(v1);
			vectors2.add(v2);
		}

		GraphPanel gp = new GraphPanel(vectors1.toArray(new Vector[0]),
				vectors2.toArray(new Vector[0]));
		gp.setDisplayArrowHeads(false);
		gp.setBackground(Color.white);
		gp.setLabelsVisible(labelPoints);
		gp.setSize(size, size);
		gp.setXLabel("Intersubjective Agreement (Pearson)");
		gp.setYLabel("Preferences Agreement (Pearson)");
		return gp;
	}

	public GraphPanel getGraph(List<QSort> list, boolean labelPoints, int size,
			Set<Integer> filter, final String bands,
			boolean includeRegressionLines) {
		DataComponents d = buildMatrix(list, filter);
		if (d == null)
			return null;
		Matrix m = d.correlations;
		if (m == null)
			return null;
		// if (textOutputStream != null)
		// textOutputStream.write(m.getDelimited(TAB, true).getBytes());

		final Vector v1 = m.getColumnVector(1);
		final Vector v2 = m.getColumnVector(2);
		for (int i = 1; i <= v1.rowCount(); i++) {
			v1.setRowLabel(i, d.participants2.get(i - 1));
			v2.setRowLabel(i, d.participants1.get(i - 1));
		}

		GraphPanel gp = new GraphPanel(v1, v2);
		gp.setDisplayMeans(true);
		gp.setDisplayArrowHeads(false);
		gp.setBackground(Color.white);
		gp.setLabelsVisible(labelPoints);
		gp.setSize(size, size);
		gp.setXLabel("Intersubjective Agreement (Pearson)");
		gp.setYLabel("Preferences Agreement (Pearson)");
		final SimpleRegression sr = new SimpleRegression();
		double[][] vals = new double[v1.size()][2];
		for (int i = 0; i < vals.length; i++) {
			vals[i][0] = v1.getValue(i + 1);
			vals[i][1] = v2.getValue(i + 1);
		}
		sr.addData(vals);

		if (includeRegressionLines) {
			final Function interval = new RegressionIntervalFunction(v1,
					PREDICTION_INTERVAL_95.equals(bands));

			gp.addFunction(new Function() {

				public double f(double x) {

					return sr.predict(x) + interval.f(x);
				}
			}, Color.lightGray);
			gp.addFunction(new Function() {

				public double f(double x) {
					return sr.predict(x) - interval.f(x);
				}
			}, Color.lightGray);
			gp.addFunction(new Function() {

				public double f(double x) {
					return sr.predict(x);
				}
			}, Color.BLACK);
		}
		DecimalFormat df = new DecimalFormat("0.00");
		gp.addComment(new Vector(new double[] { -0.8, 0.8 }), "r2="
				+ df.format(Math.pow(sr.getR(), 2)));
		return gp;
	}

	public Set<String> getParticipantIds(String participantType) {
		Set<String> result = new HashSet<String>();
		for (QSort q : restrictList(participantType, "all", null)) {
			result.add(q.getParticipant().getId());
		}
		return result;
	}

	private void graph(List<QSort> list, OutputStream imageOutputStream,
			boolean labelPoints, int size, Set<Integer> filter, String bands)
			throws IOException {

		GraphPanel gp = getGraph(list, labelPoints, size, filter, bands, true);
		if (gp != null)
			writeImage(gp, size, imageOutputStream);
	}

	private void graphConnected(List<QSort> list,
			OutputStream imageOutputStream, boolean labelPoints, int size,
			Set<Integer> filter) throws IOException {
		// split the list into separate lists by stage
		Map<String, List<QSort>> map = new LinkedHashMap<String, List<QSort>>();
		for (QSort q : list) {
			if (map.get(q.getStage()) == null)
				map.put(q.getStage(), new ArrayList<QSort>());
			map.get(q.getStage()).add(q);
		}

		GraphPanel gp = getGraphConnected(map.values()
				.toArray(new ArrayList[1]), labelPoints, size, filter);

		if (gp != null) {
			gp.setDisplayMeans(true);
			gp.setDisplayRegression(true);
			if (size <= 1000)
				writeImageConnected(gp, size, imageOutputStream);
			else
				writeImage(gp, size, imageOutputStream);
		}
	}

	private void writeImage(GraphPanel gp, int imageSize, OutputStream imageOs)
			throws IOException {
		gp.setSize(imageSize, imageSize);
		ImageIO.write(gp.getImage(), "jpeg", imageOs);
	}

	private void writeImageConnected(GraphPanel gp, int imageSize,
			OutputStream imageOs) throws IOException {
		gp.setSize(imageSize, imageSize);
		gp.writeAnimatedImage(imageOs);
	}

	public void writeMatrix(boolean forced, String participantType,
			String stage, Set<Integer> exclusions, Set<Integer> filter,
			OutputStream os) throws IOException {
		List<QSort> list = restrictList(participantType, stage, exclusions);
		Matrix matrix = buildMatrix(list, filter).correlations;
		if (matrix != null)
			writeMatrix(matrix, os);
	}

	public Matrix getRawData(DataCombination dataCombination,
			Set<Integer> exclusions, Set<Integer> filter, int dataSet) {
		return getRawData(dataCombination.getParticipantType(), dataCombination
				.getStage(), exclusions, filter, dataSet);
	}

	public Matrix getRawData(String participantType, String stage,
			Set<Integer> exclusions, Set<Integer> filter, int dataSet) {
		List<QSort> subList = restrictList(participantType, stage, exclusions);

		if (subList.size() == 0) {
			return null;
		}

		int numRows = subList.get(0).getQResults().size();
		if (dataSet == 2)
			numRows = subList.get(0).getRankings().size();
		if (numRows == 0)
			return null;
		Matrix m = new Matrix(numRows, subList.size());
		int col = 1;
		for (QSort q : subList) {
			int row = 1;
			List<Double> items = q.getQResults();
			if (dataSet == 2)
				items = q.getRankings();
			for (double value : items) {
				m.setValue(row, col, value);
				row++;
			}
			m.setColumnLabel(col, participantPrefix
					+ q.getParticipant().getId());
			col++;
		}
		if (dataSet == 1)
			m.setRowLabelPattern("Stmt<index>");
		else
			m.setRowLabelPattern("Pref<index>");

		// m.writeToFile(new File("/matrix.txt"), false);
		m = m.removeColumnsWithNoStandardDeviation();
		return m;
	}

	public void analyze(boolean forced, String participantType, String stage,
			Set<Integer> exclusions, Set<Integer> filter, int dataSet,
			double threshold, boolean doPca, boolean doCentroid,
			Set<RotationMethod> rotationMethods, SimpleHeirarchicalFormatter f) {

		if (!forced) {
			f.header("Error", true);
			f.blockStart();
			f.item("Factor analysis of unforced not implemented");
			f.blockFinish();
			return;
		}

		Matrix m = getRawData(participantType, stage, exclusions, filter,
				dataSet);
		if (m == null) {
			f.header("Error", true);
			f.blockStart();
			f.item("No data to analyze");
			f.blockFinish();
			return;
		}

		FactorAnalysisResults results;
		try {
			if (doPca) {
				results = m.analyzeFactors(
						FactorExtractionMethod.PRINCIPAL_COMPONENTS_ANALYSIS,
						threshold, rotationMethods);
				results.process(results.getInitial(), f);
			}
			if (doCentroid) {
				results = m.analyzeFactors(
						FactorExtractionMethod.CENTROID_METHOD, threshold,
						rotationMethods);
				results.process(results.getInitial(), f);
			}
		} catch (FactorAnalysisException e) {
			f.header("Error", true);
			f.blockStart();
			f.item(e.getMessage());
			f.blockFinish();
		}
	}

	public Set<String> getExcludeParticipants() {
		return excludeParticipants;
	}

	public Map<Integer, String> getStatements() {
		return statements;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

}