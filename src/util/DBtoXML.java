package util;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 * Generates an XML file from a database.
 * 
 * It first obtains all the pid#, since pid# are unique and is the primary key in database.  
 * Then it goes through each pid# to get the rest of the fields.  This method is employed 
 * because it uses less heap space to query the database twice (first time for all pid#s, 
 * second time for each entry using the pid#), rather than just one step of a "SELECT * FROM dbTable" 
 */

/*
 * TO-DO: Since querying for all the database takes quite a long time, perhaps a progress bar or
 * a waiting/"building XML" screen should be implemented
 */
public class DBtoXML {

	private Vector<String> fields = null;
	private Vector<Vector<String>> results = new Vector<Vector<String>>();

	private Element root;
	private int pidIndex = -1;
	private String dbIP = "127.0.0.1";
	private String dbPort = "80";
	private String dbRootPath = "reports";
	private String dbTable = "data";

	/**
	 * Generates XML file from a database
	 * @throws SQLException
	 * @throws IOException
	 * @throws Exception
	 */
	public void generateXML() throws SQLException, IOException, Exception {
		connectToDB();
			
		//create the file
		OutputFormat format = OutputFormat.createPrettyPrint();
		XMLWriter xmlFile = new XMLWriter(new FileWriter("./data/" + dbTable + ".xml"), format);
		
		Document xmlData = DocumentHelper.createDocument();
		root = xmlData.addElement("records");
		root.addAttribute("dbName", dbTable);
		
		findPIDIndex();
		
		for(Vector<String> record : results) {
			addRecord(record);
		}
		
		xmlFile.write(xmlData);
		xmlFile.close();
	}
	
	/**
	 * Connects to the database to get the relevant information
	 * @throws SQLException
	 * @throws Exception
	 */
	private void connectToDB() throws SQLException, Exception {
		int numColumns = -1;
		
		String query = "SELECT pid FROM " + dbTable;
		
		//connect to database
		Class.forName("com.mysql.jdbc.Driver");
		String url ="jdbc:mysql://" + dbIP + ":" + dbPort + "/" + dbRootPath;
		Connection con = DriverManager.getConnection(url, "access", "access");
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		
		//retrieve the pid#s
		ArrayList<String> pidArray = new ArrayList<String>();
		while(rs.next()) {
			pidArray.add(rs.getString(1));
		}
		int numRows = pidArray.size();
		
		//get each entry in database based on pid#
		for (int currIndex = 1; currIndex < numRows; currIndex++) {
			query = "SELECT * FROM " + dbTable + " WHERE pid='" + pidArray.get(currIndex) + "'";
			rs = stmt.executeQuery(query);
			while(rs.next()) {
				//remember the fields found in this database
				if (numColumns < 0) {
					ResultSetMetaData rsmd = rs.getMetaData();
					numColumns = rsmd.getColumnCount();
					fields = new Vector<String>();
					for (int currCol = 1; currCol <= numColumns; currCol++) {
						fields.add(rsmd.getColumnName(currCol));
					}
				}
				
				//store the resulting row data
				Vector<String> rowData = new Vector<String>();
				for (int currCol = 1; currCol <= numColumns; currCol++) {
					rowData.add(rs.getString(currCol));
				}
				results.add(rowData);
			}
		}
		rs.close();
		stmt.close();
		con.close();
	}
	
	/**
	 * Get the column number in the database for the field "pid"
	 */
	private void findPIDIndex() {
		int i = 0;
		for (String s : fields) {
			if (s.equals("pid")) {
				pidIndex = i;
				return;
			}
			i++;
		}
	}
	
	/**
	 * Adds doc tags to Document object 
	 * @param docID is the document's pid value
	 */
	private void addRecord(Vector<String> record) {
		
		Element doc = root.addElement("document");
		if (pidIndex > 0) {
			doc.addAttribute("docID", record.get(pidIndex));
		}
		
		int i = 0;
		String currData = "";
		for(String elem : fields) {
			currData = record.get(i);
			if(currData == null) {
				currData = "";
			}
			doc.addElement(elem)
				.addText(currData);
			i++;
		}
		
	}
	
	public static void main(String[] args) {
		try {
			new DBtoXML().generateXML();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
	
}
