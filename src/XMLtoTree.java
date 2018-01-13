import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;

import prefuse.data.Node;
import prefuse.data.Table;
import prefuse.data.Tree;
import prefuse.data.parser.DataParseException;
import prefuse.data.parser.DataParser;
import prefuse.data.parser.ParserFactory;


/**
 * Creates a tree from a XML file
 */

/* 
 * TO-DO: it could take a while to generate a tree from a large XML file, so perhaps a
 * progress bar or a waiting screen should be implemented
 */
public class XMLtoTree {

	// String tokens used in the XML.
	private static final String DBNAME = "dbName";
	private static final String RECORDS = "records";
	private static final String DOC = "document";
	private static final String AREA = "area";
	private static final String DOCDATE = "date";
	private static final String AUTHOR = "authname";
	private static final String TITLE = "title";
	private static final String TYPE = "type";
	private static final String PID = "pid";
	private static final String PARSED = "parsed";
	private static final String UNPARSED = "unparsed";
	private static final String FIRSTNAME = "firstName";
	private static final String LASTNAME = "lastName";
	private static final String SEX = "sex";
	private static final String RACE = "race";
	private static final String DOB = "dob";
	private static final String UNKNOWN = "_Not Specified";

	private ParserFactory m_pf = ParserFactory.getDefaultFactory();
	private Class parseType = String.class;
	private DataParser dp = m_pf.getParser(parseType);
	
	private Table m_nodes = null;
	private Tree m_tree = null;
	private Node m_activeNode = null;
	
	private String db = "";
	private String patDoc = "patDoc";
	private PatientDocument currDoc = null;
	
	private Hashtable<String, ArrayList<String> > dbTable = new Hashtable<String, ArrayList<String> >(64);
	private Hashtable<String, ArrayList<String> > authTable = new Hashtable<String, ArrayList<String> >(64);
	private Hashtable<String, ArrayList<String> > areaTable = new Hashtable<String, ArrayList<String> >(64);
	private Hashtable<String, ArrayList<String> > typeTable = new Hashtable<String, ArrayList<String> >(64);
	private Hashtable<String, PatientDocument> pidTable = new Hashtable<String, PatientDocument>(64);
	
	/**
	 * Parses an XML file and builds a tree.  It assumes that the root will have a "dbName" attribute, as well as
	 * it makes the assumption that the XML will only have 3 levels: root, document, and the fields for document
	 * @param filePath is the location of the XML file
	 * @return the Tree version of the XML
	 */
	public Tree parse(String filePath) {
		//create document object for the xml file, then we can navigate the document object
		File f = new File(filePath);
		SAXReader reader = new SAXReader();
		Document xmlFile;
		try {
			xmlFile = reader.read(f);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		//root element
		Element root = xmlFile.getRootElement();
		db = root.attributeValue(DBNAME);
		
		//initialize tree
		m_tree = new Tree();
		m_nodes = m_tree.getNodeTable();
		m_nodes.addColumn(db, parseType);
		m_nodes.addColumn(patDoc, PatientDocument.class);
		dbTable.put(db, new ArrayList<String>());
		
		//get each document record
		Iterator docIter = root.elementIterator();
		while (docIter.hasNext()) {
			Element docTag = (Element)docIter.next();
			currDoc = new PatientDocument();
			
			//remember the fields in the document
			Object[] fields = docTag.elements().toArray();	
			
			//read each field and get its text
			for(int i = 0; i < fields.length; i++) {
				DefaultElement currField = (DefaultElement)fields[i];
				processText(currField.getName(), currField.getText());
			}
			
			saveCurrentRecord(); //save the current document record
		}
		
		treeBuilder(); //build tree
		
		return m_tree;
	}
	
	/**
	 * Stores the information read in the document tag, which will later be used to build a tree
	 */
	private void saveCurrentRecord() {
		//convert to all upper case because "same" entries might be available in various cases,
		//so to group these entries, use only one case
		String currAuth = currDoc.getAuthname().trim().toUpperCase();
		String currArea = currDoc.getArea().trim().toUpperCase();
		String currType = currDoc.getType().trim().toUpperCase();
		String currPID = currDoc.getPID().trim();

		//fields cannot be empty
		if (currAuth.equals("")) {
			currAuth = UNKNOWN;
		}
		if (currArea.equals("")) {
			currArea = UNKNOWN;
		}
		if (currType.equals("")) {
			currType = UNKNOWN;
		}
		if (currPID.equals("")) {
			currPID = UNKNOWN;
		}
		
		pidTable.put(currPID, currDoc);
		
		//retrieve the appropriate list
		ArrayList<String> authList = dbTable.get(db);
		ArrayList<String> areaList = authTable.get(db + "_" + currAuth);
		ArrayList<String> typeList = areaTable.get(db + "_" + currAuth + "_" + currArea);
		ArrayList<String> pidList = typeTable.get(db + "_" + currAuth + "_" + currArea + "_" + currType);
		
		//if new author
		if (authList.size() == 0 || !authList.contains(currAuth)) {
			areaList = new ArrayList<String>();
			typeList = new ArrayList<String>();
			pidList = new ArrayList<String>();
			
			authList.add(currAuth);
			areaList.add(currArea);
			typeList.add(currType);
			pidList.add(currPID);
			
			dbTable.put(db, authList);
			authTable.put(db + "_" + currAuth, areaList);
			areaTable.put(db + "_" + currAuth + "_" + currArea, typeList);
			typeTable.put(db + "_" + currAuth + "_" + currArea + "_" + currType, pidList);
		}
		
		//else if current author exists, but new area
		else if (!areaList.contains(currArea)) {
			typeList = new ArrayList<String>();
			pidList = new ArrayList<String>();

			areaList.add(currArea);
			typeList.add(currType);
			pidList.add(currPID);
			
			authTable.put(db + "_" + currAuth, areaList);
			areaTable.put(db + "_" + currAuth + "_" + currArea, typeList);
			typeTable.put(db + "_" + currAuth + "_" + currArea + "_" + currType, pidList);
		}
		
		//else if current author & area exist, but new report type
		else if (!typeList.contains(currType)) {
			pidList = new ArrayList<String>();
			
			typeList.add(currType);
			pidList.add(currPID);
			
			areaTable.put(db + "_" + currAuth + "_" + currArea, typeList);
			typeTable.put(db + "_" + currAuth + "_" + currArea + "_" + currType, pidList);
		}
		//else if current author, area, and report type exist, just add the pid
		else {
			pidList.add(currPID);
			
			typeTable.put(db + "_" + currAuth + "_" + currArea + "_" + currType, pidList);
		}
	}
		  
	/**
	 * Builds the tree
	 */
	private void treeBuilder() {
		String currAuth;
		String currArea;
		String currType;
		String currPID;
		
		//because no direct way of establishing a node's parents, adjust the node's
		//name to contain this information
		String authFull;
		String areaFull;
		String typeFull;
		String pidFull;

		ArrayList<String> authList = dbTable.get(db);
		ArrayList<String> areaList;
		ArrayList<String> typeList;
		ArrayList<String> pidList;
		
		m_activeNode = m_tree.addRoot();
		try {
			m_activeNode.set(db, dp.parse(db));
			
			Node n;
			
			//go through each author in the list
			for (String author : authList) {
				currAuth = db + "_" + author;
				authFull = " Database: " + db + " \n Author: " + author + " ";
				n = m_tree.addChild(m_activeNode);
				m_activeNode = n;
				m_activeNode.set(db, dp.parse(authFull)); //add node
				
				//go through each area in the current author
				areaList = authTable.get(currAuth);
				for (String area : areaList) {
					currArea = currAuth + "_" + area;
					areaFull = authFull + "\n Area: " + area + " ";
					n = m_tree.addChild(m_activeNode);
					m_activeNode = n;
					m_activeNode.set(db, dp.parse(areaFull)); //add node
					
					//go through each report type in the current area
					typeList = areaTable.get(currArea);
					for (String type : typeList) {
						currType = currArea + "_" + type;
						typeFull = areaFull + "\n Report Type: " + type + " ";
						n = m_tree.addChild(m_activeNode);
						m_activeNode = n;
						m_activeNode.set(db, dp.parse(typeFull)); //add node
						
						//go through each pid in the current report type
						pidList = typeTable.get(currType);
						for(String pid : pidList) {
							pidFull = typeFull + "\n PID: " + pid + " ";
							n = m_tree.addChild(m_activeNode);
							m_activeNode = n;
							m_activeNode.set(db, dp.parse(pidFull)); //add node
							m_activeNode.set(patDoc, pidTable.get(pid)); //add the PatientDocument
							
							m_activeNode = m_activeNode.getParent(); //get pid's parent (current report type)
						}
						m_activeNode = m_activeNode.getParent(); //get report type's parent (current area)	
					}
					m_activeNode = m_activeNode.getParent(); //get area's parent (current author)
				}
				m_activeNode = m_activeNode.getParent(); //get author's parent (the root node)
			}
		} catch (DataParseException d) {
			throw new RuntimeException(d);
		}
	}
	
	/**
	 * Sets the data in the PatientDocument
	 * @param field is the current field
	 * @param text is the text in the field
	 */
	private void processText(String field, String text) {
		if (field == null) {
			return;
		}
		if (field.equals(AREA)) {
			currDoc.setArea(text);
		}
		else if (field.equals(DOCDATE)) {
			currDoc.setDate(text);
		}
		else if (field.equals(AUTHOR)) {
			currDoc.setAuthname(text);
		}
		else if (field.equals(TITLE)) {
			currDoc.setTitle(text);
		}
		else if (field.equals(TYPE)) {
			currDoc.setType(text);
		}
		else if (field.equals(PID)) {
			currDoc.setPID(text);
		}
		else if (field.equals(PARSED)) {
			currDoc.setParsed(text);
		}
		else if (field.equals(UNPARSED)) {
			currDoc.setUnparsed(text);
		}
		else if (field.equals(FIRSTNAME)) {
			currDoc.setFirstName(text);
		}
		else if (field.equals(LASTNAME)) {
			currDoc.setLastName(text);
		}
		else if (field.equals(SEX)) {
			currDoc.setSex(text);
		}
		else if (field.equals(RACE)) {
			currDoc.setRace(text);
		}
		else if (field.equals(DOB)) {
			currDoc.setDOB(text);
		}
	} 
} 
