package de.uni_leipzig.simba.data;



import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * implements Serializable
 * List<Rule> rules
 * Graph addGraph //JENA Graph
 * 
 * @author Klaus Lyko
 *
 */
public interface CompressedGraph {

	/**
	 * FIXME do we need this?
	 * @return
	 */
	public Graph getAddGraph();
	
	/**
	 * Setter for the addGraph, which is an RDF model holding all additional triples not compressed via Rules.
	 * @param model
	 */
	public void setAddModel(Model model);
	
	/**
	 * Adds a Rule. If an equal Rule already exists this one is updated.
	 * @param r
	 */
	public void addRule(Rule r);
	
	
	/**
	 * Method to compute all supersets of each rule, add pointers to them and remove redundant uris of the
	 * subsets in the supersets.
	 * @TODO is there an more time-wise efficient way of computing while generating the proiles?
	 */
	public void computeRedundantRules();
}
