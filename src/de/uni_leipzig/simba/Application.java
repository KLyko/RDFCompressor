package de.uni_leipzig.simba;

import java.io.File;
import java.io.IOException;

import de.uni_leipzig.simba.compress.Compressor;
import de.uni_leipzig.simba.compress.CompressorFactory;
import de.uni_leipzig.simba.compress.CompressorFactory.Type;
import de.uni_leipzig.simba.compress.IndexBasedCompressor;
import de.uni_leipzig.simba.decompress.DefaultDecompressor;

public class Application{

    public static void main(String[] args){
	// parse command line
	if (args.length > 0){
	    if (args[0].equals("-c")){
		File path = new File(args[1]);
		if (path.exists()){
		    CompressorFactory cf = new CompressorFactory();
		    Compressor compressor = cf.getCompressor(Type.INDEX);
		    compressor.compress(path);
		}
	    }
	    else if (args[0].equals("-d")){
		//Decompress
	    }
	    else{
		System.out.println("Invalid program call");
	    }
	}
	else{
	    System.out.println("Usage: java <programname> <inputfile>");
	    if(System.getProperty("user.name").equalsIgnoreCase("lyko")) {
	    	File path = new File("resources/dummy_data2.nt");
//	    	path = new File("resources/wordnet-membermeronym.rdf");
//	    	path = new File ("resources/archive_hub_dump.nt");
			if (path.exists()){
//			    CompressorFactory cf = new CompressorFactory();
			    IndexBasedCompressor compressor = new IndexBasedCompressor();
			    compressor.compress(path);
			}
	    }
	}
    }
}
