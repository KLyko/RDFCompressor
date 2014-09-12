package de.uni_leipzig.simba;

import java.io.File;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import de.uni_leipzig.simba.compress.Compressor;
import de.uni_leipzig.simba.compress.CompressorFactory;
import de.uni_leipzig.simba.compress.CompressorFactory.Type;
import de.uni_leipzig.simba.compress.IndexBasedCompressor;
import de.uni_leipzig.simba.compress.ModelCompressor;

public class Application{

	
	public static Options getCLIOptions() {
		Options options = new Options();
		options.addOption("c", true, "path to resource which should be compressed");
		options.addOption("f", true, "log file extension");
		options.addOption("del", true, "using delete graph with this size");
		options.addOption("comb", false, "");
		return options;
	}
	
	
	
    public static void main(String[] args){
    	  if(System.getProperty("user.name").equalsIgnoreCase("lyko")) {
  	    	File path = new File("resources/dummy_data3.nt");
//  	    	path = new File("uba/lubm50/");
//  	    	path = new File("resources/wordnet-membermeronym.rdf");
//  	    	path = new File ("resources/archive_hub_dump.nt");
//  	    		path = new File("resources/ubl_part/");
//  	    	path = new File("C:\\Users\\Lyko\\Downloads\\jamendo-rdf\\jamendo.rdf");
  			if (path.exists()){
//  			    CompressorFactory cf = new CompressorFactory();
  				ModelCompressor compressor = new ModelCompressor(path);
//  			    compressor.setLogFileSuffix("bloom");
  			    compressor.compress();
  			}
  	    } else {
  	    	CommandLineParser parser = new BasicParser();
  	    	try {
  				CommandLine cmd = parser.parse(getCLIOptions(), args);
  				if(!cmd.hasOption("c")) {
  					System.err.println("File to compress must be specified.");
  					System.exit(0);
  				}
  				File path = new File(cmd.getOptionValue("c"));
  				if(!path.exists()) {
  					System.err.println("File/Directory to compress not found.");
  					System.exit(0);
  				}
  				Compressor compressor = CompressorFactory.getCompressor(Type.INDEX);
  				if(cmd.hasOption("comb"))
  					compressor = new ModelCompressor(path);
  				if(cmd.hasOption("f"))
  					compressor.setLogFileSuffix(cmd.getOptionValue("f"));
  				int delete = -1;
  				if(cmd.hasOption("del")) {
  					delete = Integer.parseInt(cmd.getOptionValue("del"));
  				}
  				compressor.setFile(path);
  				compressor.setDelete(delete);
  				
  				compressor.compress();
  			} catch (ParseException e) {
  				// TODO Auto-generated catch block
  				e.printStackTrace();
  			}
  	    }
    }

}
