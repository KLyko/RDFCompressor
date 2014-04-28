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
			if(args.length >= 4){
				if(args[2].equals("-f"))
					compressor.setLogFileSuffix(args[3]);
			}		   
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
//	    	path = new File("uba/lubm50/");
//	    	path = new File("resources/wordnet-membermeronym.rdf");
//	    	path = new File ("resources/archive_hub_dump.nt");
			if (path.exists()){
//			    CompressorFactory cf = new CompressorFactory();
			    IndexBasedCompressor compressor = new IndexBasedCompressor();
			    compressor.setLogFileSuffix("bloom");
			    compressor.compress(path);
			}
	    }
//	    DefaultDecompressor decompr = new DefaultDecompressor();
//	    try {
////			decompr.decompress(new File("resources/dummy_decompress.txt"));
//	    	decompr.decompress(new File("resources/dummy_decompress_SMap.txt"));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
    }
}
