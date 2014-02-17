package de.uni_leipzig.simba.compress;

import java.io.File;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class DefaultCompressor implements Compressor {

    public DefaultCompressor(){
	String path = "resources/dummy_data.nt";
	File input = new File(path);
	System.out.println(input);
	//Model model = parseInputFile(path);

	// build inverse list of p/o tuples

	// build addgraph

	// serialize inverse list

	// compress with bzip
    }
    
    private Model parseInputFile(String path){
	Model model = ModelFactory.createDefaultModel();
	//model.read(new StringReader(input), null);
	return model;
    }

    public static void main(String[] args){
	// parse command line
	if (args.length > 0){
	    File path = new File(args[0]);
	    //DefaultCompressor dc = new DefaultCompressor(path);
	}
	else{
	    System.out.println("Usage: java <programname> <inputfile>");
	}
	DefaultCompressor dc = new DefaultCompressor();
    }
}
