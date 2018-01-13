import java.awt.Dimension;
import java.awt.Font;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.animate.ColorAnimator;
import prefuse.action.assignment.ColorAction;
import prefuse.action.layout.Layout;
import prefuse.action.layout.graph.SquarifiedTreeMapLayout;
import prefuse.controls.ControlAdapter;
import prefuse.data.Schema;
import prefuse.data.Tree;
import prefuse.data.expression.Predicate;
import prefuse.data.expression.parser.ExpressionParser;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.DefaultRendererFactory;
import prefuse.util.ColorLib;
import prefuse.util.ColorMap;
import prefuse.util.FontLib;
import prefuse.util.PrefuseLib;
import prefuse.util.ui.JFastLabel;
import prefuse.visual.DecoratorItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.VisualTree;
import prefuse.visual.expression.InGroupPredicate;
import prefuse.visual.sort.TreeDepthItemSorter;

/**
 * Builds the treemap and other features
 * 
 * Treemap code is based on the TreeMap.java example found in the prefuse.demos package
 */

/* !!!!!!!!!!!!!!
 * Note: There is a bug where sometimes, when a search is performed, whether it's with textbox or 
 * checkbox, none of the nodes will be highlighted.  I haven't narrowed down what is causing the 
 * problem because a search will return the list of nodes, but it won't highlight it for some reason.
 * Re-running the program (from DBTreeMap.java) sometimes will fix this issue.
 */
public class DBTreeMapBuilder extends Display {
	
	// create data description of labels, setting colors, fonts ahead of time
	private static final Schema LABEL_SCHEMA = PrefuseLib.getVisualItemSchema();
	static {
		LABEL_SCHEMA.setDefault(VisualItem.INTERACTIVE, false);
		LABEL_SCHEMA.setDefault(VisualItem.TEXTCOLOR, ColorLib.gray(200));
		LABEL_SCHEMA.setDefault(VisualItem.FONT, FontLib.getFont("Tahoma",16));
	}
	
	private static final String tree = "tree";
	private static final String treeNodes = "tree.nodes";
	private static final String treeEdges = "tree.edges";
	private static final String labels = "labels";

	private static HashSet<String> result = null;
	private static String currDB;
	
	private JLabel numResultsText;
	private JTextField queryInput;
	private JButton querySubmit;
	
	private JCheckBox resultsCB;
	private JCheckBox surgeryCB;
	private JCheckBox emergencyCB;
	private JCheckBox noteCB;
	private StringBuffer checkboxQuery;
	
	private static final String typeResults = "type:RSLTS ";
	private static final String typeSurgery = "type:SURG ";
	private static final String typeEmergency = "type:EMER ";
	private static final String typeNote = "type:NOTE ";
	
	private DBTreeMapBuilder currBuilder;
	
	/**
	 * Constructor. It builds the treemap.
	 * @param t is the Tree to build the treemap with
	 * @param label is the database in which the data is located
	 */
	public DBTreeMapBuilder(final Tree t, String label) {
		super(new Visualization());
		
		currDB = label;
		currBuilder = this;
		
		//add the tree to the visualization
		VisualTree vt = m_vis.addTree(tree, t);
		m_vis.setVisible(treeEdges, null, false);
		
		//set the hover level to be the topmost level of tree
		setHoverDepth(1);

		//add labels to the visualization
		//feature available in the original prefuse.demos.Treemap.java, but taken out in this build
		//first create a filter to show labels only at top-level nodes
		//final Predicate labelP = (Predicate)ExpressionParser.parse("treedepth()=1");
		//now create the labels as decorators of the nodes
		//m_vis.addDecorators(labels, treeNodes, labelP, LABEL_SCHEMA);
		
		// set up the renderers - one for nodes and one for labels
		NodeRenderer nr = new NodeRenderer();
		DefaultRendererFactory rf = new DefaultRendererFactory();
		rf.add(new InGroupPredicate(treeNodes), nr);
		//rf.add(new InGroupPredicate(labels), new LabelRenderer(currDB));
		m_vis.setRendererFactory(rf);
		
		// border colors
		final ColorAction borderColor = new BorderColorAction(treeNodes);
		final ColorAction fillColor = new FillColorAction(treeNodes);
		
		// color settings
		ActionList colors = new ActionList();
		colors.add(fillColor);
		colors.add(borderColor);
		m_vis.putAction("colors", colors);
		
		// animate paint change
		ActionList animatePaint = new ActionList(400);
		animatePaint.add(new ColorAnimator(treeNodes));
		animatePaint.add(new RepaintAction());
		m_vis.putAction("animatePaint", animatePaint);
		
		// create the single filtering and layout action list
		ActionList layout = new ActionList();
		layout.add(new SquarifiedTreeMapLayout(tree));
		//layout.add(new LabelLayout(labels));
		layout.add(colors);
		layout.add(new RepaintAction());
		m_vis.putAction("layout", layout);
		
		// initialize our display
		setSize(700, 600);
		setItemSorter(new TreeDepthItemSorter());
		
		//fires listeners when cursor enters or leaves a node
		addControlListener(new ControlAdapter() {
			public void itemEntered(VisualItem item, MouseEvent e) {
				item.setStrokeColor(borderColor.getColor(item));
				//item.getVisualization().repaint();
				//use parameters oldVal and newVal as placeholders for VisualItem and MouseEvent
				currBuilder.firePropertyChange("nodeEntered", item, e);
			}
			public void itemExited(VisualItem item, MouseEvent e) {
				item.setStrokeColor(item.getEndStrokeColor());
				//item.getVisualization().repaint();
				//use parameters oldVal and newVal as placeholders for VisualItem and MouseEvent
				currBuilder.firePropertyChange("nodeExited", item, e);
			}
		});
		
		//redraws treemap when window resizes
		addComponentListener( new ComponentListener() {
			public void componentResized(ComponentEvent e) {
				//currWindow.setRect(getX(), getY(), getWidth(), getHeight());
				setSize(getWidth(), getHeight());
				m_vis.run("layout");
			}
			public void componentMoved(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {}  	
			public void componentShown(ComponentEvent e) {}
		});
		
		// perform layout
		m_vis.run("layout");
	}
	
	/**
	 * Displays the name of the current node
	 * @param label is the name of the database
	 * @return a JFastLabel with the name of the current node
	 */
	public JFastLabel displayNodeName(final String label) {
		final JFastLabel title = new JFastLabel("                 ");
		title.setPreferredSize(new Dimension(350, 20));
		title.setVerticalAlignment(SwingConstants.BOTTOM);
		title.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
		title.setFont(FontLib.getFont("Tahoma", Font.PLAIN, 16));
		
		//listener to display current node when the cursor enters or leaves a node
		addControlListener(new ControlAdapter() {
			public void itemEntered(VisualItem item, MouseEvent e) {
				title.setText(item.getString(label));
			}
			public void itemExited(VisualItem item, MouseEvent e) {
				if(e.getX() >= getWidth()-1 || e.getX() <= 0 ||
						e.getY() >= getHeight() - 1 || e.getY() <= 0) {
					title.setText(null);
				}
			}
		});	
		return title;
	}
	
	/**
	 * Creates a Lucene text-based search panel
	 * @return a JPanel that contains this search panel
	 */
	public JPanel createSearchButton() {
		JPanel searchPanel = new JPanel();
		searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));
		
		//number of results found
		numResultsText = new JLabel();
		
		//text input field
		queryInput = new JTextField(15);
		queryInput.setVisible(true);
		queryInput.setMaximumSize(new Dimension(150,20));
		queryInput.addKeyListener(new TextFieldHandler());
		
		JButtonHandler jbh = new JButtonHandler();	
		
		//submit button
		querySubmit = new JButton("Submit");
		querySubmit.setName("querySubmit");
		querySubmit.addActionListener(jbh);
		
		//reset button
		JButton resetMap = new JButton("Reset");
		resetMap.setName("resetMap");
		resetMap.addActionListener(jbh);
		
		searchPanel.add(numResultsText);
		searchPanel.add(Box.createRigidArea(new Dimension(10,0)));
		searchPanel.add(queryInput);
		searchPanel.add(Box.createRigidArea(new Dimension(10,0)));
		searchPanel.add(querySubmit);
		searchPanel.add(Box.createRigidArea(new Dimension(10,0)));
		searchPanel.add(resetMap);
		
		return searchPanel;
	}
	
	/**
	 * Creates a dropdown list that specifies the level of the tree the user wishes to hover
	 * @return a JPanel that contains this dropdown list
	 */
	public JPanel createHierarchyDropDown() {
		JPanel dropdownPanel = new JPanel();
		dropdownPanel.setLayout(new BoxLayout(dropdownPanel, BoxLayout.X_AXIS));
		
		JLabel title = new JLabel("Set Hover Type: ");
		String[] hierarchy = { "Author", "Area", "Report Type", "PID" };
		//Note: hierarchy could be changed to whatever is suitable
		
		JComboBox dropdown = new JComboBox(hierarchy);
		dropdown.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox hierachyList = (JComboBox)e.getSource();
				int depth = hierachyList.getSelectedIndex() + 1;
				setHoverDepth(depth);	//changes the hover type
			}
		});

		dropdownPanel.add(title);
		dropdownPanel.add(dropdown);
		return dropdownPanel;
	}
	
	/**
	 * Creates checkboxes that filters the Lucene search results
	 * @return
	 */
	public JPanel createTypeCheckbox() {
		JPanel checkboxPanel = new JPanel();
		checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.X_AXIS));
		
		JLabel filter = new JLabel("Filter by Report Type: ");
		
		checkboxQuery = new StringBuffer("");
		resultsCB = new JCheckBox("Results");
		surgeryCB = new JCheckBox("Surgery");
		emergencyCB = new JCheckBox("Emergency");
		noteCB = new JCheckBox("Note");
		
		CheckBoxListener cbl = new CheckBoxListener();
		resultsCB.addItemListener(cbl);
		surgeryCB.addItemListener(cbl);
		emergencyCB.addItemListener(cbl);
		noteCB.addItemListener(cbl);
		
		checkboxPanel.add(filter);
		checkboxPanel.add(resultsCB);
		checkboxPanel.add(surgeryCB);
		checkboxPanel.add(emergencyCB);
		checkboxPanel.add(noteCB);
		
		return checkboxPanel;
	}
	
	/**
	 * Sets the level in the tree in which the user will retrieve the information
	 * @param depth is the depth level of the tree
	 */
	private void setHoverDepth(int depth) {
		m_vis.setInteractive(treeNodes, null, true);
		Predicate noLeaf = (Predicate)ExpressionParser.parse("treedepth()!=" + depth);
		m_vis.setInteractive(treeNodes, noLeaf, false);
		m_vis.repaint();
	}
	
	/**
	 * Formats the search query
	 */
	private void setupSearchQuery() {
		String input = queryInput.getText().toString().trim();
		String query = "";
		
		//if empty text field, search query is based on the checkboxes
		if (input.equals("")) {
			query = checkboxQuery.toString();
		}
		
		//else if no checkboxes, search query is based on the text field
		else if (checkboxQuery.toString().trim().length() == 0) {
			query = input;
		}
		
		//else, search query is the combination of the checkboxes and text field
		else {
			query = "(" + checkboxQuery.toString() + ") AND " + input;
		}
		highlightResults(query); //perform search
		
	}
	
	/**
	 * Highlight the nodes that matches the search criteria
	 * @param query
	 */
	private void highlightResults(String query) {
		//clear the search results
		if (query.equals("")) {
			if(result != null) {
				result.clear();
			}
			numResultsText.setText("");
			queryInput.setText("");
		}
		//find the nodes
		else {
			result = new SearchByLucene().FindPIDs(query);
			int numResults = 0;
			if (result != null) {
				numResults = result.size();
			}
			if (numResults == 1) {
				numResultsText.setText(numResults + " result found");
			}
			else {
				numResultsText.setText(numResults + " results found");
			}
		}

		//performs the visualization
		//m_vis.cancel("animatePaint");
		m_vis.run("colors");
		m_vis.run("animatePaint");
	}
	
	/**
	 * Removes the query generated when a user selects a checkbox
	 * @param type is the type of checkbox
	 */
	private void removeCBQuery(String type) {
		int indexStart = -1;
		indexStart = checkboxQuery.indexOf(type);
		if (indexStart > -1) {
			checkboxQuery.delete(indexStart, indexStart+type.length());
		}
	}
	
	// ------------------------------------------------------------------------
	// helper classes
	
	/**
	 * Set the stroke color for drawing treemap node outlines. A graded
	 * grayscale ramp is used, with higer nodes in the tree drawn in
	 * lighter shades of gray.
	 */
	public static class BorderColorAction extends ColorAction {
		
		public BorderColorAction(String group) {
			super(group, VisualItem.STROKECOLOR);
		}
		
		public int getColor(VisualItem item) {
			NodeItem nitem = (NodeItem)item;
			if ( nitem.isHover() )
				return ColorLib.rgb(99,130,191);
			
			int depth = nitem.getDepth();
			if ( depth < 2 ) {
				return ColorLib.gray(100);
			} else if ( depth < 4 ) {
				return ColorLib.gray(75);
			} else {
				return ColorLib.gray(50);
			}
		}
	}
	
	/**
	 * Set fill colors for treemap nodes. Search items are colored
	 * in pink, while normal nodes are shaded according to their
	 * depth in the tree.
	 */
	public static class FillColorAction extends ColorAction {
		private ColorMap cmap = new ColorMap(
			ColorLib.getInterpolatedPalette(10,
				ColorLib.rgb(85,85,85), ColorLib.rgb(0,0,0)), 0, 9);

		public FillColorAction(String group) {
			super(group, VisualItem.FILLCOLOR);
		}
		
		public int getColor(VisualItem item) {
			if ( item instanceof NodeItem ) {
				NodeItem nitem = (NodeItem)item;
				if ( nitem.getChildCount() > 0 ) {
					return 0; // no fill for parent nodes
				} 
				else {
					String pid = item.getSourceTuple().get(currDB).toString();
					String key = " PID: ";
					int pidIndex = pid.indexOf(key);
					if (pidIndex > -1) {
						pid = pid.substring(pidIndex+key.length()).trim();
					}
					//highlight node if found
					if(result != null && result.size() > 0 && result.contains(pid)) {
						return ColorLib.rgb(191, 99, 130);
					}
					else
						return cmap.getColor(nitem.getDepth());
				}
			} else {
				return cmap.getColor(0);
			}
		}
		
	}
	
	/**
	 * Set label positions. Labels are assumed to be DecoratorItem instances,
	 * decorating their respective nodes. The layout simply gets the bounds
	 * of the decorated node and assigns the label coordinates to the center
	 * of those bounds.
	 */
	public static class LabelLayout extends Layout {
		public LabelLayout(String group) {
			super(group);
		}
		public void run(double frac) {
			Iterator iter = m_vis.items(m_group);
			while ( iter.hasNext() ) {
				DecoratorItem item = (DecoratorItem)iter.next();
				VisualItem node = item.getDecoratedItem();
				Rectangle2D bounds = node.getBounds();
				setX(item, null, bounds.getCenterX());
				setY(item, null, bounds.getCenterY());
			}
		}
	} 
	
	/**
	 * A renderer for treemap nodes. Draws simple rectangles, but defers
	 * the bounds management to the layout.
	 */
	public static class NodeRenderer extends AbstractShapeRenderer {
		private Rectangle2D m_bounds = new Rectangle2D.Double();
		
		public NodeRenderer() {
			m_manageBounds = false;
		}
		protected Shape getRawShape(VisualItem item) {
			m_bounds.setRect(item.getBounds());
			return m_bounds;
		}
	} 
	
	/**
	 * A handler for the search field.  It performs the search when the "Enter" key is pressed
	 */
	private class TextFieldHandler extends KeyAdapter {
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == e.VK_ENTER) {
				setupSearchQuery();
			}
		}
	}
	
	/**
	 * A handler for when the user either presses the Search button or the Reset button.
	 * It will perform the search, or removes all visualization on the treemap
	 */
	private class JButtonHandler implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			JButton currButton = (JButton)e.getSource();
			
			//search button
			if(currButton.getName() == "querySubmit") {
				setupSearchQuery();
			}
			
			//reset button
			else if(currButton.getName() == "resetMap") {
				resultsCB.setSelected(false);
				surgeryCB.setSelected(false);
				emergencyCB.setSelected(false);
				noteCB.setSelected(false);
			}
			
			highlightResults(""); //perform the visualization
		}
	}
	
	/**
	 * A handler for when the user selects or deselects a checkbox.  It will adjust 
	 * the search query accordingly
	 */
	private class CheckBoxListener implements ItemListener {
		public void itemStateChanged(ItemEvent e) {
			Object source = e.getItemSelectable();
			
			//report type: results
			if (source == resultsCB) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					checkboxQuery.append(typeResults);
				}
				else {
					removeCBQuery(typeResults);
				}
			}
			
			//report type: surgery
			else if (source == surgeryCB) {
				if(e.getStateChange() == ItemEvent.SELECTED) {
					checkboxQuery.append(typeSurgery);
				}
				else {
					removeCBQuery(typeSurgery);
				}
			}
			
			//report type: Emergency
			else if (source == emergencyCB) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					checkboxQuery.append(typeEmergency);
				}
				else {
					removeCBQuery(typeEmergency);
				}
			}
			
			//report type: note
			else if (source == noteCB) {
				if(e.getStateChange() == ItemEvent.SELECTED) {
					checkboxQuery.append(typeNote);
				}
				else {
					removeCBQuery(typeNote);
				}
			}
			
			//perform the visualization
			setupSearchQuery();
		}
	}
}
