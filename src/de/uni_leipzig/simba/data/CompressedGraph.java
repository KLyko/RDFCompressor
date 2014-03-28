package de.uni_leipzig.simba.data;



import java.util.List;


/**
 * implements Serializable
 * List<Rule> rules
 * Graph addGraph //JENA Graph
 * 
 * @author Klaus Lyko
 *
 */
public interface CompressedGraph<IRule> {

//	/**
//	 * FIXME do we need this?
//	 * @return
//	 */
//	public Graph getAddGraph();
//	
//	/**
//	 * Setter for the addGraph, which is an RDF model holding all additional triples not compressed via Rules.
//	 * @param model
//	 */
//	public void setAddModel(Model model);
//	
//	/**
//	 * Adds a Rule. If an equal Rule already exists this one is updated.
//	 * @param r
//	 */
	public void addRule(IRule r) throws Exception;
	
	
	/**
	 * Method to compute all supersets of each rule, add pointers to them and remove redundant uris of the
	 * subsets in the supersets.
	 * @TODO is there an more time-wise efficient way of computing while generating the proiles?
	 */
	public void computeSuperRules();
	
	/**
	 * Method to remove redundant parent Rules. These are those parents which are also parents of the parent
	 * Example: r1 => r2, r0 and r2 => r0.  We can remove r0 from the parents of r1.
	 */
	public void removeRedundantParentRules();
	
	/**
	 * Computes the size, which is the sum of all URIs in all rules. 
	 * @return
	 */
	public int size();
	
    
    public List<IRule> getRules();
    
    public String serialize();
}
