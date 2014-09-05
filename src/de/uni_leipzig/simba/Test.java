package de.uni_leipzig.simba;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.VCARD;
import com.hp.hpl.jena.query.*;

import de.uni_leipzig.simba.io.ModelLoader;

public class Test {
	public static void testModelReading(File input) throws UnsupportedEncodingException, FileNotFoundException {
		ModelFactory.createDefaultModel();
		 
 		Model model = ModelLoader.getModel(input.getAbsolutePath());
// 		SimpleSelector selector = new SimpleSelector(null, VCARD.FN, null);
 		

 		
 		/*List all statements <s,p,o> where o is resource*/
// 		SimpleSelector selector = new SimpleSelector(null, null, (RDFNode)null) {
// 		    public boolean selects(Statement s)
// 		        { return s.getObject().isResource();  }
// 		};
//		StmtIterator iter = model.listStatements(selector);
//		while(iter.hasNext()) {
//		   Statement stmt = iter.nextStatement();
//		   System.out.print(stmt.getSubject().toString()+" ");
//		   System.out.print(stmt.getPredicate().toString()+ " ");
//		   System.out.println(stmt.getObject().toString());
//		}
 		
 		
 		/*Query all properties*/
 		String query = "SELECT DISTINCT ?p "+
 					"WHERE {" +
 					"?s ?p ?o" +
 					"}";
 		QueryExecution qe = QueryExecutionFactory.create(query, model);
 		ResultSet rs = qe.execSelect();
 		
 		while(rs.hasNext()) {
			QuerySolution qs = rs.nextSolution();
			Resource res_p = qs.getResource("?p");
			Property p = model.getProperty(res_p.getURI());
//			System.out.println();
			
			NodeIterator nodeIter = model.listObjectsOfProperty(p);
			while(nodeIter.hasNext()) { //nest
				RDFNode o_node=nodeIter.next();
				String out = "";
				if(o_node.isResource()) {
					out+= res_p +" - "+o_node+" {";
					// we have an p - o pair
					ResIterator subIter = model.listResourcesWithProperty(p, o_node);
					while(subIter.hasNext()) {
						Resource s = subIter.next();
						out+=s+", ";
					}
					out = out.substring(0,out.length()-2);
					out+="}";
					System.out.println(out);
					
				}
			}
//			out = out.substring(0, out.length()-2);
//			out += "}";
//			System.out.println(out);
		}
 		
 	
 		
 		
 		ResultSetFormatter.out(System.out, rs);

	 	// Important – free up resources used running the query
	 	qe.close();

	}
	
	
	public static void main(String args[]) {
		File f = new File("resources/dummy_data3.nt");
		try {
			testModelReading(f);
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
