package de.uni_leipzig.simba.compress;

import java.io.File;

import com.hp.hpl.jena.rdf.model.Model;

public interface Compressor {
	/**
	 * Input .nt file, JENA Graph, ...
	 * Output bzipped  CompressedGraph
	 */
    public void compress();
    public void compress(Model m, String logExt);
    public void setFile(File input);
    public void setDelete(int delete);
    public void setLogFileSuffix(String prefix);
}
