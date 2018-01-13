import java.util.ArrayList;
import java.util.HashSet;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

/**
 * Searches the Lucene Index for documents that match search criteria.
 * 
 * It must have an index already present in order for it to work.
 */
public class SearchByLucene {
	
	private IndexReader ir;

	/**
	 * Constructor that sets up index reader.  Assumes the index is located in ./util/data/LuceneIndex directory
	 */
	public SearchByLucene() {
		try {
			//ir = IndexReader.open("./util/data/LuceneIndex");
			ir = IndexReader.open("C:\\eclipse\\workspace\\treemap\\src\\data\\LuceneIndex");
		} catch ( Exception e) {
			System.err.println("Error opening index");
			e.printStackTrace();
		}
	}
	
	/**
	 * Constructor that sets up index reader.
	 * @param indexPath is the location where the index is stored
	 */
	public SearchByLucene(String indexPath) {
		try {
			ir = IndexReader.open(indexPath);
		} catch ( Exception e) {
			System.err.println("Error opening index");
			e.printStackTrace();
		}
	}
	
	/**
	 * Searches the index to get the pid#s of results
	 * @param queryString is the query
	 * @return a HashSet of the list of pid# that matches search query
	 */
	public HashSet<String> FindPIDs(String queryString) {
		try {
			ArrayList<String> fields = new ArrayList<String>(ir.getFieldNames(IndexReader.FieldOption.ALL));
			
			//if no fields could be found in index, then index is improper, so return null
			if(fields.size() == 0) {
				return null;
			}

			HashSet<String> results = null;
			String pid;
			IndexSearcher searcher = new IndexSearcher(ir);
			QueryParser qp;
			Query query;
			Hits hits;
			int hitCount;
			
			//begin searching on all fields
			for (String fieldName : fields) {
				qp = new QueryParser(fieldName, new StandardAnalyzer());
				query = qp.parse(queryString);

				// Search for the query
				hits = searcher.search(query);
		
				// Examine the Hits object to see if there were any matches
				hitCount = hits.length();
				if (hitCount > 0) {
					results = new HashSet<String>();
					for (int i = 0; i < hitCount; i++) {
						pid = hits.doc(i).get("pid");
						results.add(pid);
					}
				}
			}
			return results;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
