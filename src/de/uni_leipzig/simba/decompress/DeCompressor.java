package de.uni_leipzig.simba.decompress;

import java.io.File;

/**
 * 
 * Input bzipped  CompressedGraph
 * Output .nt file, JENA Graph, ...
 * @author Klaus Lyko
 *
 */
public interface DeCompressor {

	public File decompress(File file);
}
