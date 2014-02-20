package de.uni_leipzig.simba.compress;

public interface IndexBasedCompressorInterface {
	public void addAbbreviation(String sURI, String fullURI);
	public String getAbbreviation(String uri);
	public String getFullUri(String sUri);
	
	/**
	 * Adds/Gets an Index for this Subjet, Predicate or Object URI. Differentiated by the enum SPO.
	 * @param uri
	 * @param SPOrO
	 * @return newly created of existed index.
	 */
	public int addIndex(String uri, SPO SPOrO);
	
	
	public void writeIndexFiles();
}
