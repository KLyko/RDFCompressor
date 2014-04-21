package de.uni_leipzig.simba.compress;

import java.io.File;

public interface Compressor {
	/**
	 * Input .nt file, JENA Graph, ...
	 * Output bzipped  CompressedGraph
	 */
    public void compress(File input);
    
    public void setLogFileSuffix(String prefix);
}
