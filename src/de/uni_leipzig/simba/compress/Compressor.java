package de.uni_leipzig.simba.compress;

import java.io.File;

public interface Compressor {
	/**
	 * Input .nt file, JENA Graph, ...
	 * Output bzipped  CompressedGraph
	 */
    public void compress();
    public void setFile(File input);
    public void setDelete(int delete);
    public void setLogFileSuffix(String prefix);
}
