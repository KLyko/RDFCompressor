package de.uni_leipzig.simba.decompress;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.compressors.CompressorException;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * 
 * Input bzipped  CompressedGraph
 * Output .nt file, JENA Graph, ...
 * @author Klaus Lyko
 *
 */
public interface DeCompressor {

	public Model decompress(File file) throws FileNotFoundException, IOException, CompressorException, ArchiveException;
}
