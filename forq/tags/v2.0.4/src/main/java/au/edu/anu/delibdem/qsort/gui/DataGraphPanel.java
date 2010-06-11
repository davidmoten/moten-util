package au.edu.anu.delibdem.qsort.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SpringLayout;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import moten.david.util.event.Event;
import moten.david.util.event.EventManager;
import moten.david.util.math.EigenvalueThreshold;
import moten.david.util.math.FactorAnalysisException;
import moten.david.util.math.FactorAnalysisResults;
import moten.david.util.math.FactorExtractionMethod;
import moten.david.util.math.Matrix;
import moten.david.util.math.Varimax.RotationMethod;
import moten.david.util.math.gui.GraphPanel;
import au.edu.anu.delibdem.qsort.Data;
import au.edu.anu.delibdem.qsort.DataSelection;
import au.edu.anu.delibdem.qsort.QSort;
import au.edu.anu.delibdem.qsort.gui.injection.ApplicationInjector;

public class DataGraphPanel extends JPanel {

	private static final long serialVersionUID = 998590047676207490L;
	private Data data;
	private DataSelection combination;
	private final List<EventManager> eventManagers = new ArrayList<EventManager>();

	private GraphPanel gp;

	public GraphPanel getGraphPanel() {
		return gp;
	}

	public DataSelection getCombination() {
		return combination;
	}

	public void setCombination(DataSelection combination) {
		this.combination = combination;
	}

	public DataGraphPanel(Data data) {
		this.data = data;
	}

	public DataGraphPanel(Data data, DataSelection combination) {
		super();
		this.data = data;
		this.combination = combination;
		setBackground(Color.white);
		setOpaque(true);
		update();
	}

	public void update() {
		removeAll();
		setLayout(new GridLayout(1, 1));
		JPanel panel;
		if (!combination.getStage().equalsIgnoreCase("all")) {
			panel = getGraphPanelStatic();
		} else {
			panel = getGraphPanelAll();
		}
		add(panel);
		invalidate();
	}

	private void doit(final AnalysisConfiguration... analyses) {
		Thread t = new Thread(new Runnable() {
			public void run() {
				List<FactorAnalysisResults> results = new ArrayList<FactorAnalysisResults>();

				for (AnalysisConfiguration analysis : analyses) {
					Status
							.setStatus("Performing "
									+ analysis.getExtractionMethod().toString()
									+ "...");
					FactorAnalysisResults r = getFactorAnalysisResults(data,
							combination, analysis.isIntersubjective(), analysis
									.getExtractionMethod(), analysis
									.getEigenvalueThreshold());

					if (r != null) {
						r.setTitle(analysis.getTitle());
						results.add(r);
					}
					Status.finish();
				}
				for (EventManager eventManager : eventManagers) {
					eventManager.notify(new Event(results
							.toArray(new FactorAnalysisResults[0]),
							Events.ANALYZED));
				}
			}
		});
		t.start();
	}

	private Matrix getMatrix(Data data, DataSelection combination,
			boolean isIntersubjective) {
		Matrix m = data.getRawData(combination, null, (isIntersubjective ? 1
				: 2));
		return m;
	}

	private FactorAnalysisResults getFactorAnalysisResults(Data data,
			DataSelection dataSelection, boolean isIntersubjective,
			FactorExtractionMethod method,
			EigenvalueThreshold eigenvalueThreshold) {
		Set<RotationMethod> rotationMethods = new HashSet<RotationMethod>();
		rotationMethods.add(RotationMethod.VARIMAX);
		try {
			Matrix m = getMatrix(data, dataSelection, isIntersubjective);
			if (m == null)
				return null;

			return m.analyzeFactors(method, eigenvalueThreshold,
					rotationMethods);
		} catch (FactorAnalysisException e) {
			throw new Error(e);
		}
	}

	public JPanel getGraphPanelStatic() {
		List<QSort> list = data.restrictList(combination.getStage(),
				combination.getFilter());
		boolean labelPoints = false;
		JPanel panel = new JPanel();
		SpringLayout layout = new SpringLayout();
		panel.setLayout(layout);

		JCheckBox showLabels = new JCheckBox("show labels");
		panel.add(showLabels);
		final LinkButton analyze = new LinkButton("Analyze");
		panel.add(analyze);
		boolean showRegressionLines = true;
		gp = data.getGraph(list, labelPoints, 200, null, null,
				showRegressionLines);
		if (gp != null) {
			gp.setOpaque(true);
			panel.add(gp);
		}
		showLabels.setVisible(gp != null);
		layout.putConstraint(SpringLayout.WEST, showLabels, 20,
				SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.NORTH, showLabels, 10,
				SpringLayout.NORTH, panel);
		layout.putConstraint(SpringLayout.NORTH, analyze, 0,
				SpringLayout.NORTH, showLabels);
		layout.putConstraint(SpringLayout.SOUTH, analyze, 0,
				SpringLayout.SOUTH, showLabels);
		layout.putConstraint(SpringLayout.WEST, analyze, 20, SpringLayout.EAST,
				showLabels);
		if (gp != null) {
			layout.putConstraint(SpringLayout.WEST, gp, 0, SpringLayout.WEST,
					panel);
			layout.putConstraint(SpringLayout.EAST, gp, 0, SpringLayout.EAST,
					panel);
			layout.putConstraint(SpringLayout.NORTH, gp, 0, SpringLayout.SOUTH,
					showLabels);
			layout.putConstraint(SpringLayout.SOUTH, gp, 0, SpringLayout.SOUTH,
					panel);
		}
		showLabels.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				gp.setLabelsVisible(!gp.getLabelsVisible());
				gp.repaint();
			}
		});
		analyze.addActionListener(createAnalyzeActionListener());
		return panel;
	}

	private ActionListener createAnalyzeActionListener() {

		return new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Configuration configuration = ApplicationInjector.getInjector()
						.getInstance(Configuration.class);
				AnalyzeOptionsPanel options = new AnalyzeOptionsPanel();
				options.setPreferredSize(new Dimension(300, 80));
				int choice = JOptionPane.showOptionDialog(null, options,
						"Analysis Options", JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.PLAIN_MESSAGE, null, null, null);
				if (choice == JOptionPane.OK_OPTION) {
					doit(new AnalysisConfiguration(configuration
							.getQFactorsTitle(), options
							.getFactorExtractionMethod(), options
							.getEigenvalueThreshold(), true),
							new AnalysisConfiguration(configuration
									.getPreferencesTitle(), options
									.getFactorExtractionMethod(), options
									.getEigenvalueThreshold(), false));
				}
			}
		};
	}

	@SuppressWarnings("unchecked")
	public JPanel getGraphPanelAll() {
		List<QSort> list = data.restrictList("all", combination.getFilter());
		// split the list into separate lists by stage
		final Map<String, List<QSort>> map = new LinkedHashMap<String, List<QSort>>();
		for (QSort q : list) {
			if (map.get(q.getStage()) == null)
				map.put(q.getStage(), new ArrayList<QSort>());
			map.get(q.getStage()).add(q);
		}
		int size = 100;
		JPanel panel = new JPanel();
		SpringLayout layout = new SpringLayout();
		panel.setLayout(layout);

		gp = data.getGraphConnected(map.values().toArray(new ArrayList[1]),
				false, size, null);
		if (gp != null) {
			panel.add(gp);
			// gp.setBackground(getBackground());
			gp.setOpaque(true);
			gp.setProportionDrawn(1.0);
			gp.setDisplayMeans(true);
			gp.setDisplayRegression(true);
		}
		final JSlider slider = new JSlider(0, 100);
		slider.setValue(slider.getMaximum());

		// slider.setPreferredSize(new Dimension(400, 20));
		if (gp != null)
			panel.add(slider);

		final JButton animate = new JButton("Animate");
		if (gp != null)
			panel.add(animate);

		JCheckBox showLabels = new JCheckBox("show labels");

		if (gp != null)
			panel.add(showLabels);

		final LinkButton analyze = new LinkButton("Analyze");
		panel.add(analyze);

		layout.putConstraint(SpringLayout.WEST, showLabels, 20,
				SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, showLabels, 0,
				SpringLayout.VERTICAL_CENTER, slider);
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, analyze, 0,
				SpringLayout.VERTICAL_CENTER, slider);
		layout.putConstraint(SpringLayout.WEST, analyze, 20, SpringLayout.EAST,
				showLabels);
		layout.putConstraint(SpringLayout.WEST, slider, 20, SpringLayout.EAST,
				analyze);
		layout.putConstraint(SpringLayout.EAST, slider, -20, SpringLayout.WEST,
				animate);
		layout.putConstraint(SpringLayout.EAST, animate, -10,
				SpringLayout.EAST, panel);
		if (gp != null) {
			layout.putConstraint(SpringLayout.WEST, gp, 0, SpringLayout.WEST,
					panel);
			layout.putConstraint(SpringLayout.EAST, gp, 0, SpringLayout.EAST,
					panel);
			layout.putConstraint(SpringLayout.NORTH, gp, 10,
					SpringLayout.SOUTH, slider);
			layout.putConstraint(SpringLayout.SOUTH, gp, 0, SpringLayout.SOUTH,
					panel);
		}
		layout.putConstraint(SpringLayout.NORTH, slider, 5, SpringLayout.NORTH,
				panel);
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, animate, 0,
				SpringLayout.VERTICAL_CENTER, slider);

		slider.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				gp.setProportionDrawn(slider.getValue() * 1.0
						/ slider.getMaximum());
				gp.repaint();
			}
		});
		showLabels.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				gp.setLabelsVisible(!gp.getLabelsVisible());
				gp.repaint();
			}
		});

		final Timer timer = new Timer(50, null);
		timer.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (slider.getValue() == slider.getMaximum()) {
					timer.stop();
				} else
					slider.setValue(slider.getValue() + 1);
			}
		});
		animate.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (timer.isRunning())
					timer.stop();
				else {
					if (slider.getValue() == slider.getMaximum())
						slider.setValue(slider.getMinimum());
					timer.start();
				}
			}
		});
		// animate.doClick();
		analyze.addActionListener(createAnalyzeActionListener());
		return panel;
	}

	public Data getData() {
		return data;
	}

	public void setData(Data data) {
		this.data = data;
	}

	public void addEventManager(EventManager eventManager) {
		eventManagers.add(eventManager);
	}

	public static class AnalysisConfiguration {
		private final String title;
		private final EigenvalueThreshold eigenvalueThreshold;

		public EigenvalueThreshold getEigenvalueThreshold() {
			return eigenvalueThreshold;
		}

		public String getTitle() {
			return title;
		}

		public FactorExtractionMethod getExtractionMethod() {
			return extractionMethod;
		}

		public boolean isIntersubjective() {
			return isIntersubjective;
		}

		public AnalysisConfiguration(String title,
				FactorExtractionMethod extractionMethod,
				EigenvalueThreshold eigenvalueThreshold,
				boolean isIntersubjective) {
			super();
			this.title = title;
			this.extractionMethod = extractionMethod;
			this.eigenvalueThreshold = eigenvalueThreshold;
			this.isIntersubjective = isIntersubjective;
		}

		private final FactorExtractionMethod extractionMethod;
		private final boolean isIntersubjective;
	}

}