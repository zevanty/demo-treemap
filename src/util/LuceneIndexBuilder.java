import java.io.*;
import java.sql.*;
import java.util.ArrayList;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;

/**
 * Builds the index from the database.
 * 
 * Run the main() function to build the index.  Specify the database name in the main().
 */

/*
 * TO-DO: Since querying for all the database takes quite a long time, perhaps a progress bar or
 * a waiting/"building index" screen should be implemented
 */
public class LuceneIndexBuilder {
	
	private IndexWriter writer;
	private String indexDir = "./data/LuceneIndex";
	private String dbIP = "127.0.0.1";
	private String dbPort = "80";
	private String dbRootPath = "reports";
	private String dbTable = "data";

	/**
	 * Constructor.  It tries to find an existing index in the indexDir directory.  
	 * If none can be found, it creates a new index in that directory.
	 */
	public LuceneIndexBuilder() {
		try {
			//find existing index
			writer = new IndexWriter(indexDir, new StandardAnalyzer(), false);
		} catch (IOException ioe) {
			//assume IOException error is caused by not finding an index
			try {
				//if no existing index, create a new one
				writer = new IndexWriter(indexDir, new StandardAnalyzer(), true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Builds/updates the index
	 */
	/*
	 * It first obtains all the pid#, since pid# are unique and is the primary key in database.  
	 * Then it goes through each pid# to get the rest of the fields.  This method is employed 
	 * because it uses less heap space to query the database twice (first time for all pid#s, 
	 * second time for each entry using the pid#), rather than just one step of a "SELECT * FROM db" 
	 */
	public void addData() {
		try {
			Document doc;
			int i;
			IndexReader ir = IndexReader.open(writer.getDirectory());
			
			//connect to database and return only the "pid" field because it uses less java heap space
			String query = "SELECT pid FROM " + dbTable;
			Class.forName("com.mysql.jdbc.Driver");
			String url = "jdbc:mysql://" + dbIP + ":" + dbPort + "/" + dbRootPath;
			Connection con = DriverManager.getConnection(url, "access", "access");
			Statement stmt = con.createStatement();	
			ResultSet rs = stmt.executeQuery(query);
			
			//store all the pids into an array
			ArrayList<String> pidArray = new ArrayList<String>();
			while(rs.next()) {
				pidArray.add(rs.getString(1));
			}
			
			int numRows = pidArray.size();

			//retrieve one of the data in order to calculate the number of columns
			query = "SELECT * FROM " + dbTable + " WHERE pid='" + pidArray.get(0) + "'";
			rs = stmt.executeQuery(query);
			ResultSetMetaData rsmd = rs.getMetaData();
			int numColumns = rsmd.getColumnCount();
			
			//remember the column number for the following unique fields
			int pidIndex = 0; //pid
			int dateIndex = 0; //date
			int unparsedIndex = 0; //unparsed
			int dobIndex = 0; //dob
			for (i = 1; i <= numColumns; i++) {
				String curCol = rsmd.getColumnName(i);
				if (curCol.equals("unparsed")) {
					unparsedIndex = i;
				}
				else if (curCol.equals("pid")) {
					pidIndex = i;
				}
				else if( curCol.equals("date")) {
					dateIndex = i;
				}
				else if (curCol.equals("dob")) {
					dobIndex = i;
				}
			}	
			
			String resultData = "";
			for(int currIndex = 0; currIndex < numRows; currIndex++) {
				//check to see if data already exist in the index.  If not, then add to index
				//this works under the assumption that all "pid" field keys are unique
				if(ir.docFreq(new Term("pid", pidArray.get(currIndex))) < 1) {
					//add data to index
					query = "SELECT * FROM " + dbTable + " WHERE pid='" + pidArray.get(currIndex) + "'";
					rs = stmt.executeQuery(query);
					rs.next();
	
					//add data to index 
					doc = new Document();
					//store the database name in which the data is from
					doc.add(new Field("database", dbTable, Field.Store.YES, Field.Index.UN_TOKENIZED));
					//store the rest of the data
					for (i = 1; i <= numColumns; i++) {
						resultData = rs.getString(i);
						if (resultData == null) {
							resultData = "";
						}
						//don't index the "unparsed" field
						if(i == unparsedIndex) {
							doc.add(new Field(rsmd.getColumnName(i), resultData, Field.Store.YES, Field.Index.NO));
						}
						//tokenize all other fields except: pid, date, dob
						else if(i != pidIndex && i != dateIndex && i != dobIndex) {
							doc.add(new Field(rsmd.getColumnName(i), resultData, Field.Store.YES, Field.Index.TOKENIZED));
						}
						else {
							doc.add(new Field(rsmd.getColumnName(i), resultData, Field.Store.YES, Field.Index.UN_TOKENIZED));
						}
					}
					writer.addDocument(doc);
				}
			}
			writer.optimize();
			ir.close();
			writer.close(); 
			rs.close();
			stmt.close();
			con.close();
		} catch (SQLException s) {
			System.err.println("Error accessing database");
			s.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		LuceneIndexBuilder ib = new LuceneIndexBuilder();
		ib.addData();
	}

}
