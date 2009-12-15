package au.edu.anu.delibdem.qsort.gui;

import java.util.Collection;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import moten.david.util.event.Event;
import moten.david.util.event.EventManager;
import moten.david.util.event.EventManagerListener;
import moten.david.util.math.Matrix;
import moten.david.util.math.MatrixProvider;
import au.edu.anu.delibdem.qsort.Data;
import au.edu.anu.delibdem.qsort.DataCombination;
import au.edu.anu.delibdem.qsort.QSort;
import au.edu.anu.delibdem.qsort.Data.DataComponents;

public class DataTree extends JTree {

	private static final long serialVersionUID = 2392669756231377460L;

	private DefaultMutableTreeNode referenceNode;

	private final Data data;

	public DataTree(Data data) {
		super(getRoot(data));
		this.data = data;
		setRootVisible(false);
		setShowsRootHandles(true);
		DefaultTreeCellRenderer renderer = new MyRenderer();
		setCellRenderer(renderer);
		EventManager.getInstance().addListener(Events.REFERENCE_SET,
				new EventManagerListener() {
					@Override
					public void notify(Event event) {
						DefaultTreeModel treeModel = (DefaultTreeModel) getModel();
						if (referenceNode != null)
							treeModel.nodeChanged(referenceNode);
						DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) getLastSelectedPathComponent();
						if (selectedNode.getUserObject() instanceof MatrixProvider) {
							if (selectedNode.getUserObject().equals(
									Model.getInstance().getReference())) {
								treeModel.nodeChanged(selectedNode);
								referenceNode = selectedNode;
							}
						}
					}
				});
	}

	private static void addDataCombinationNode(final Data data,
			DefaultMutableTreeNode parent, final DataCombination combination) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(combination);
		parent.add(node);
		DefaultMutableTreeNode matrixNode = new DefaultMutableTreeNode(
				new MatrixProvider() {
					@Override
					public Matrix getMatrix() {
						List<QSort> list = data.restrictList(combination
								.getParticipantType(), combination.getStage(),
								null);
						DataComponents d = data.buildMatrix(list, null);
						return d.correlations;
					}

					@Override
					public String toString() {
						return "Intersubjective Correlation";
					}
				});
		node.add(matrixNode);
	}

	private static TreeNode getRoot(Data data) {
		Collection<String> stageTypes = data.getStageTypes();
		Collection<String> participantTypes = data.getParticipantTypes();
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		for (String participantType : participantTypes) {
			for (String stage : stageTypes) {
				DataCombination combination = new DataCombination(
						participantType, stage);
				addDataCombinationNode(data, root, combination);
			}
			if (stageTypes.size() > 1) {
				DataCombination combination = new DataCombination(
						participantType, "all");
				addDataCombinationNode(data, root, combination);
			}
		}
		if (participantTypes.size() > 1) {
			for (String stage : data.getStageTypes()) {
				DataCombination combination = new DataCombination("all", stage);
				addDataCombinationNode(data, root, combination);
			}
		}
		if (participantTypes.size() > 1 && stageTypes.size() > 1) {
			DataCombination combination = new DataCombination("all", "all");
			addDataCombinationNode(data, root, combination);
		}
		return root;
	}

}