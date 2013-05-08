package org.cytoscape.data.writer.graphml;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.event.MenuEvent;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import cytoscape.util.CyFileFilter;
import cytoscape.util.CytoscapeAction;
import cytoscape.util.FileUtil;
import cytoscape.view.CyNetworkView;

public class ExportAsGraphMLAction extends CytoscapeAction {

	private static final long serialVersionUID = 3291201728797501188L;

	private static final String GRAPHML_EXT = "graphml";

	/**
	 * Creates a new ExportAsGMLAction object.
	 */
	public ExportAsGraphMLAction() {
		super("Network as GraphML file...");
		setPreferredMenu("File.Export");
	}

	public void actionPerformed(ActionEvent e) {
		String name;

		// Get Current Network and View
		final CyNetworkView networkView = Cytoscape.getCurrentNetworkView();
		final CyNetwork network = networkView != null ? networkView.getNetwork() : Cytoscape.getCurrentNetwork();

		try {

			CyFileFilter cyFileFilter = new CyFileFilter(GRAPHML_EXT);

			String suggestedFileName = network.getIdentifier();
			if (!suggestedFileName.endsWith("." + GRAPHML_EXT)) {
				suggestedFileName = suggestedFileName + "." + GRAPHML_EXT;
			}

			File suggestedFile = new File(suggestedFileName);

			name = FileUtil.getFile("Export Network as GraphML", FileUtil.SAVE,
					cyFileFilter, suggestedFile).toString();
		} catch (Exception exp) {
			// this is because the selection was canceled
			return;
		}

		if (!name.endsWith("." + GRAPHML_EXT))
			name = name + "." + GRAPHML_EXT;

		// Create Task
		ExportAsGraphMLTask task = new ExportAsGraphMLTask(name, network, networkView);

		// Configure JTask Dialog Pop-Up Box
		JTaskConfig jTaskConfig = new JTaskConfig();
		jTaskConfig.setOwner(Cytoscape.getDesktop());
		jTaskConfig.displayCloseButton(true);
		jTaskConfig.displayStatus(true);
		jTaskConfig.setAutoDispose(false);

		// Execute Task in New Thread; pop open JTask Dialog Box.
		TaskManager.executeTask(task, jTaskConfig);
	}

	public void menuSelected(MenuEvent e) {
		enableForNetwork();
	}

}

/**
 * Task to Save Graph Data to GML Format.
 */
class ExportAsGraphMLTask implements Task {
	private String fileName;
	private CyNetwork network;
	private CyNetworkView networkView;

	private TaskMonitor taskMonitor;

	/**
	 * Constructor.
	 * 
	 * @param network
	 *            Network Object.
	 * @param view
	 *            Network View Object.
	 */
	ExportAsGraphMLTask(String fileName, CyNetwork network, CyNetworkView networkView) {
		this.fileName = fileName;
		this.network = network;
		this.networkView = networkView;
	}

	/**
	 * Executes Task
	 */
	public void run() {
		taskMonitor.setStatus("Saving Network...");
		taskMonitor.setPercentCompleted(-1);

		try {
			int numNodes = network.getNodeCount();

			if (numNodes == 0) {
				throw new IllegalArgumentException("Network is empty.");
			}

			saveGraph();
			taskMonitor.setPercentCompleted(100);
			taskMonitor
					.setStatus("Network successfully saved to:  " + fileName);
		} catch (IllegalArgumentException e) {
			taskMonitor.setException(e, "Network is Empty.  Cannot be saved.");
		} catch (IOException e) {
			taskMonitor.setException(e, "Unable to save network.");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Halts the Task: Not Currently Implemented.
	 */
	public void halt() {
		// Task can not currently be halted.
	}

	/**
	 * Sets the Task Monitor.
	 * 
	 * @param taskMonitor
	 *            TaskMonitor Object.
	 */
	public void setTaskMonitor(TaskMonitor taskMonitor)
			throws IllegalThreadStateException {
		this.taskMonitor = taskMonitor;
	}

	/**
	 * Gets the Task Title.
	 * 
	 * @return Task Title.
	 */
	public String getTitle() {
		return new String("Saving Network as GraphML");
	}

	/**
	 * Saves Graph to File.
	 * 
	 * @throws IOException
	 *             Error Writing to File.
	 */
	private void saveGraph() throws Exception {
		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter(fileName);
			final GraphMLWriter writer = new GraphMLWriter(network, networkView, fileWriter, taskMonitor);
			writer.write();
		} finally {
			if (fileWriter != null) {
				fileWriter.close();
				fileWriter = null;
			}
		}

		Object[] ret_val = new Object[3];
		ret_val[0] = network;
		ret_val[1] = new File(fileName).toURI();
		// Currently unused (?)
		ret_val[2] = new Integer(-1);
		Cytoscape.firePropertyChange(Cytoscape.NETWORK_SAVED, null, ret_val);
	}

}
