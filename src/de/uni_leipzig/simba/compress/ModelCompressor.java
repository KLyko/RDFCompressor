package de.uni_leipzig.simba.compress;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Observable;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;

import de.uni_leipzig.simba.io.ModelLoader;

/**
 * Class to create compressed graph (rules, their super rules) while iterating the model.
 * This is done by:
 * 	1. querying the RDF model for all properties
 * 	2. get all objects which are RDF resources
 * 	3. for each of this p-o pair get all subjects
 * Threreby we can construct the complete rules at once and have only to inspect all rules already present
 * whether they are a sub or superrule. Thsi should limit the complexityof our algorithm. 
 * @author Klaus Lyko
 *
 */
public class ModelCompressor extends IndexBasedCompressor implements Compressor, Runnable {
	/**The file to compress*/
	File input = null;
	
	private Model readModel()  {
		Model model = ModelFactory.createDefaultModel();
 		try {
			model = ModelLoader.getModel(input.getAbsolutePath());
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
		}
 		return model;
	}
	
	public ModelCompressor(File input) {
		setFile(input);
	}
	
	@Override
	public void run() {
		compress();
	}

	@Override
	public void compress() {
		Model model = readModel();
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
			int indexP = this.addIndex(p.getURI(), SPO.PREDICATE);
			NodeIterator nodeIter = model.listObjectsOfProperty(p);
			while(nodeIter.hasNext()) { //nest
				RDFNode o_node=nodeIter.next();
				String out = "";
				if(o_node.isResource()) {
					out+= res_p +" - "+o_node+" {";
					int indexO = addIndex(o_node.asResource().getURI(), SPO.OBJECT); 
					// we have an p - o pair
					HashSet<Integer> subsOfPO = new HashSet<Integer>();
					ResIterator subIter = model.listResourcesWithProperty(p, o_node);
					while(subIter.hasNext()) {
						Resource s = subIter.next();
						subsOfPO.add(addIndex(s.getURI(), SPO.SUBJECT));
						out+=s+", ";
					}
					out = out.substring(0,out.length()-2);
					out+="}";
					System.out.println(out);
					
				} else { // o isn't a resource
					//TODO implement
				}
			}
//			out = out.substring(0, out.length()-2);
//			out += "}";
//			System.out.println(out);
		}
	}

	@Override
	public void setFile(File input) {
		this.input = input;
	}

	@Override
	public void setDelete(int delete) {
		
	}

	@Override
	public void setLogFileSuffix(String prefix) {
		// TODO Auto-generated method stub
		
	}

}
