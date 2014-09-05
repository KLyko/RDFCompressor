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

import de.uni_leipzig.simba.data.IndexProfile;
import de.uni_leipzig.simba.data.IndexRule;
import de.uni_leipzig.simba.data.InstantCompressedGraph;
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
		valueModel = ModelFactory.createDefaultModel();
		InstantCompressedGraph ruleGraph = new InstantCompressedGraph(model.size(), true, delete);
		
		/*Query all properties*/
 		String query = "SELECT DISTINCT ?p "+
 					"WHERE {" +
 					"?s ?p ?o" +
 					"}";
 		QueryExecution qe = QueryExecutionFactory.create(query, model);
 		ResultSet rs = qe.execSelect();
 		int valueStmtCount = 0;//count statements where oi isn't a resource
 		while(rs.hasNext()) {
			QuerySolution qs = rs.nextSolution();
			Resource res_p = qs.getResource("?p");
			Property p = model.getProperty(model.shortForm(res_p.getURI()));
			int indexP = this.addIndex(p.getURI(), SPO.PREDICATE);
			NodeIterator nodeIter = model.listObjectsOfProperty(p);
			while(nodeIter.hasNext()) { //nest
				RDFNode o_node=nodeIter.next();
				if(o_node.isResource()) {
					// we have an p - o pair
					int indexO = addIndex(model.shortForm(o_node.asResource().getURI()), SPO.OBJECT); 
					IndexProfile profile = new IndexProfile(indexP, indexO);
					ResIterator subIter = model.listResourcesWithProperty(p, o_node);
					while(subIter.hasNext()) {
						Resource s = subIter.next();
						profile.addSubject(addIndex(model.shortForm(s.getURI()), SPO.SUBJECT));
					}					
					//creating and adding rule
					IndexRule rule = new IndexRule(profile);
					ruleGraph.addRule(rule, profile.getSubjects());
					System.out.println("Added rule "+rule);					
				} else { // o isn't a resource
					ResIterator subIter = model.listResourcesWithProperty(p, o_node);
					while(subIter.hasNext()) {
						Resource sIR = valueModel.createResource(""+addIndex(model.shortForm(subIter.next().getURI()), SPO.SUBJECT));
						Property pIR = valueModel.createProperty(""+indexP);
						valueModel.add(sIR, pIR, o_node);
						valueStmtCount++;
						System.out.println("new Value statement <"+sIR+"><"+pIR+">"+o_node);		
					}
					
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
	
	
	public static void main(String args[]) {
		File file = new File("resources/dummy_data3.nt");
		ModelCompressor compr = new ModelCompressor(file);
		compr.compress();
	}

}
