package au.edu.anu.delibdem.qsort.gui;

import java.awt.GridLayout;

import javax.swing.JPanel;

import moten.david.util.event.EventManager;
import au.edu.anu.delibdem.qsort.Data;

public class DataGraphExtendedPanel extends JPanel {

	private static final long serialVersionUID = -2494288904017423756L;
	private DataGraphPanel dataGraphPanel;

	public DataGraphExtendedPanel(Data data) {
//		SpringLayout layout = new SpringLayout();
//		setLayout(layout);
//		dataGraphPanel = new DataGraphPanel(data);
//		add(dataGraphPanel);
//		
//		layout.putConstraint(SpringLayout.WEST, dataGraphPanel, 30,
//				SpringLayout.WEST, this);
//		layout.putConstraint(SpringLayout.EAST, dataGraphPanel, 0,
//				SpringLayout.EAST, this);
//		layout.putConstraint(SpringLayout.NORTH, dataGraphPanel, 0,
//				SpringLayout.NORTH, this);
//		layout.putConstraint(SpringLayout.SOUTH, dataGraphPanel, 0,
//				SpringLayout.SOUTH, this);
	
		setLayout(new GridLayout(1,1));
		dataGraphPanel = new DataGraphPanel(data);
		add(dataGraphPanel);
	}

	public void addEventManager(EventManager em) {
		dataGraphPanel.addEventManager(em);
	}
	
	public DataGraphPanel getDataGraphPanel() {
		return dataGraphPanel;
	}

}