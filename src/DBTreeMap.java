import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import prefuse.data.Tree;
import prefuse.util.ui.UILib;
import prefuse.visual.VisualItem;


/**
 * Demonstration showcasing a TreeMap layout of a hierarchical data
 * set and the use of  query binding for text search. Animation
 * is used to highlight changing search results.
 *
 * Code is based on the TreeMap.java example found in the prefuse.demos package
 * 
 * Run the main() function to generate treemap.  Any changes for testing should
 * be made in the main() function or the DBTreeMap.TREE_CHI variable.
 * 
 * Be sure to run DBtoXML.java and LuceneIndexBuilder.java before running this
 * program in order to generate the necessary files to build a treemap.
 */

/* 
 * TO-DO: it could take a while to generate a tree from a large XML file, so perhaps a
 * progress bar or a waiting screen should be implemented (either in this class or in
 * XMLtoTree.java)
 */
public class DBTreeMap {
	
	public static final String TREE_CHI = "./util/data/data.xml";
	
	private DBTreeMapBuilder treemap;
	private JFrame treemapWindow;
	private JPanel treemapSection;
	private JPanel dataPanel;
	private JTextArea reportName;
	private JButton moreInfo;
	private JButton exitPopup;
	private int treemapWidth;
	private int treemapHeight;
	private boolean hasSearchBar;
	private Popup pp;
	private PopupFactory factory = PopupFactory.getSharedInstance();
	private JFrame patWindow;
	private JPanel patPanel;
	private JTextPane patDetails;
	private VisualItem prevNode = null;
	private static final Color lightYellow = new Color(255,255,208);

	/**
	 * Constructor.  It sets up the whole treemap window
	 * @param datafile is the path/name of the XML file to generate the treemap from
	 * @param label is the name of database in which the treemap is based from
	 */
	public DBTreeMap(String datafile, final String label) {
		
		//get the tree
		Tree t = null;
		try {
			t = new XMLtoTree().parse(datafile);
		} catch ( Exception e ) {
			e.printStackTrace();
			System.exit(1);
		}
		
		//build the treemap
		treemap = new DBTreeMapBuilder(t, label);
		
		//displays current node name
		//!!! feature has been removed in current build
		//JFastLabel title =  treemap.displayNodeName(label);   
		
		//get hierarchy panel
		JPanel hierarchyPanel = treemap.createHierarchyDropDown();
		
		//create the search bar
		JPanel searchPanel = treemap.createSearchButton();
		hasSearchBar = true;
		
		//place hierarchy and search bar into same panel
		Box box = UILib.getBox(new Component[]{hierarchyPanel,searchPanel}, true, 10, 3, 0);
		
		//checkbox panel
		JPanel checkboxPanel = treemap.createTypeCheckbox();
		
		JPanel southernPanel = new JPanel();
		southernPanel.setLayout(new BoxLayout(southernPanel, BoxLayout.Y_AXIS));
		southernPanel.add(box);
		southernPanel.add(checkboxPanel);

		treemapSection = new JPanel(new BorderLayout());
		treemapSection.add(treemap, BorderLayout.CENTER);
		treemapSection.add(southernPanel, BorderLayout.SOUTH);
		UILib.setColor(treemapSection, Color.BLACK, Color.GRAY);
			  
		//popup
		popupSetup(label);
		
		//more info window
		moreInfoSetup();
		
		treemapWindow = new JFrame("t r e e m a p");
		treemapWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		treemapWindow.add(treemapSection);
		treemapWindow.pack();
		treemapWindow.setVisible(true);
		
	}
	
	/**
	 * Changes the title of the main window
	 * @param title is the new title
	 */
	public void setTitle(String title) {
		treemapWindow.setTitle(title);
	}
	
	/**
	 * Creates a popup when user hovers over a node
	 * @param label is the current database
	 */
	private void popupSetup(final String label) {
		
		//the popup panel
		dataPanel = new JPanel();
		dataPanel.setLayout(new BoxLayout(dataPanel, BoxLayout.Y_AXIS));
		
		reportName = new JTextArea("");
		reportName.setEditable(false);
		reportName.setBackground(lightYellow);
		
		JButtonHandler jbh = new JButtonHandler();
		//button to obtain more info
		moreInfo = new JButton("More");
		moreInfo.setName("moreInfo");
		moreInfo.setBackground(lightYellow);
		moreInfo.addActionListener(jbh);
		//button to close the popup
		exitPopup = new JButton("Done");
		exitPopup.setName("exitPopup");
		exitPopup.setBackground(lightYellow);
		exitPopup.addActionListener(jbh);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.setBackground(lightYellow);
		buttonPanel.add(moreInfo);
		buttonPanel.add(Box.createRigidArea(new Dimension(10,0)));
		buttonPanel.add(exitPopup);
		
		dataPanel.add(reportName);
		dataPanel.add(buttonPanel);
		dataPanel.add(Box.createRigidArea(new Dimension(0,7)));
		dataPanel.setFocusable(true);
		dataPanel.setBackground(lightYellow);
			   
		//listener for when cursor enters a node
		//the change is fired from DBTreeMapBuilder
		treemap.addPropertyChangeListener("nodeEntered", new PropertyChangeListener(){
			public void propertyChange(PropertyChangeEvent evt) {
				VisualItem currNode = (VisualItem)evt.getOldValue();
				MouseEvent currMEvent = (MouseEvent)evt.getNewValue();
				
				//draw popup only if necessary
				if (prevNode == null || !prevNode.equals(currNode)) {
					prevNode = currNode; //prevents redrawing of popup if re-entering same node
					currNode.getVisualization().repaint();
					String currNodeName = currNode.getString(label);
			
					reportName.setText(currNodeName);
					
					//close previous popup
					if(pp != null) {
						pp.hide();
					}
					//The coordinates from currMEvent are based on the component's relative x-y coordinates.
					//However, the x-y coordinates for a popup is based on the screen's x-y coordinates,
					//so need to adjust currMEvent's x-y coordinates to match the screen's x-y coordinates.
					/* TO-DO: adjust popup's x-coordinate so that if popup will partially or completely fall 
					 * outside of viewing screen, popup will be repositioned to fall within viewing screen
					 * 
					 * TO-DO: disable "more info" button for non-pid# nodes, or add some information to the
					 * non-pid# nodes
					 */
					int xpos = adjustXPos(currMEvent.getX());
					int ypos = adjustYPos(currMEvent.getY());
					pp = factory.getPopup(treemapSection, dataPanel, xpos, ypos);
					patWindow.setLocation(xpos, ypos);
					//treemapWindow.repaint();
					pp.show();
				}
			}
		});

		//listener for when cursor leaves a node
		treemap.addPropertyChangeListener("nodeExited", new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				VisualItem currNode = (VisualItem)evt.getOldValue();
				MouseEvent currMEvent = (MouseEvent)evt.getNewValue();
				
				patWindow.setVisible(false);
				treemapWidth = treemap.getWidth();
				treemapHeight = treemap.getHeight();

				//remove the popup if cursor leaves the treemap
				/* TO-DO: guarentee popup will always disappear if outside of treemap.  Currently,
				 * popup will sometimes remain on screen if user clicks on one of the other panels,
				 * such as the dropdown or the search bar
				 */
				if (currMEvent.getX() >= treemapWidth-1 || currMEvent.getX() <= 0 ||
						currMEvent.getY() >= treemapHeight-1 || currMEvent.getY() <= 0) {
					//currNode.setStrokeColor(currNode.getEndStrokeColor());
					//currNode.getVisualization().repaint();
					//treemap.repaint();
					treemapWindow.repaint();
					prevNode = null;
					pp.hide();
				}
			}
		});
	}
	
	/**
	 * Sets up the "more info" window when user clicks for more info in the popup
	 */
	private void moreInfoSetup() {
		patPanel = new JPanel();
		patDetails = new JTextPane();
		patDetails.setEditable(false);
		patDetails.setSize(300, 300);
		JScrollPane jsp = new JScrollPane(patDetails);
		jsp.setPreferredSize(new Dimension(300,300));
		jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		patPanel.add(jsp);
		
		patWindow = new JFrame();
		patWindow.setSize(300, 330);
		patWindow.add(patPanel);
	}
	
	/**
	 * Adjusts x-pos for if the window has been moved to a new location.
	 * Necessary to display popup in the correct location.
	 * @param x is the current x-pos
	 * @return the new x-pos
	 */
	private int adjustXPos(int x) {
		x = x + treemapWindow.getX();
		return x;
	}
	
	/**
	 * Adjusts the y-pos for if the window has been moved and the size of the window bar.
	 * Necessary to display popup in the correct location.
	 * @param y is the current y-pos
	 * @return the new y-pos
	 */
	//TO-DO: need to find a more dynamic way of calculating bottom section's height
	private int adjustYPos(int y) {
		y = y + (treemapWindow.getHeight() - treemap.getHeight()) + treemapWindow.getY();
		
		//The bottom sections in the treemap has been manually calculated to be 50 pixels high.
		//Need to subtract this value since treemapWindow.getHeight() includes the bottom section's height,
		//while treemap.getHeight() only contains the height of the treemap itself
		//TO-DO: need to find a more dynamic way of calculating bottom section's height
		if (hasSearchBar) {
			y = y - 50;
		}
		return y;
	}
	
	/**
	 * Handler for the buttons in the popup
	 */
	private class JButtonHandler implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			JButton currButton = (JButton)e.getSource();
			
			//close the popup
			if (currButton.getName() == "exitPopup") {
				treemapWindow.repaint();
				prevNode = null;
				pp.hide();
			}
			
			//get more information about the node (only applies to nodes at the "pid" level)
			else if (currButton.getName() == "moreInfo") {
				PatientDocument pd = (PatientDocument)prevNode.get("patDoc");
				if (pd != null) {
					patDetails.setText(pd.getParsed());
					patWindow.setVisible(true);
				}
			}
		}
	}	
	
	
	public static void main(String argv[]) {
		UILib.setPlatformLookAndFeel();
			  
		//file path + name
		String infile = TREE_CHI;
		
		//!!! update "label" according to database table name!!!
		String label = "data";
		if ( argv.length > 1 ) {
			infile = argv[0];
			label = argv[1];
		}
		DBTreeMap dbtm = new DBTreeMap(infile, label);
		
	}
} 
