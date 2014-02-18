package de.uni_leipzig.simba.compress;

import java.io.File;
import java.io.StringWriter;

import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class DefaultCompressor implements Compressor {

    public DefaultCompressor(){
    }
    
    public void compress(File input){
	Model model = parseInputFile(input);

	StringWriter output = new StringWriter();
	model.write(output, "TURTLE");
	System.out.println(output);

	// build inverse list of p/o tuples

	// build addgraph

	// serialize inverse list

	// compress with bzip
    }

    private Model parseInputFile(File input){
	Model m = FileManager.get().loadModel( input.toString() );
	return m;
    }
}
