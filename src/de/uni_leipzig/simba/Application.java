package de.uni_leipzig.simba;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.compressors.CompressorException;

import de.uni_leipzig.simba.compress.ModelCompressor;
import de.uni_leipzig.simba.decompress.DefaultDecompressor;

public class Application{

	
	public static Options getCLIOptions() {
		Options options = new Options();
		options.addOption("c", true, "Path to resource which should be compressed");
		options.addOption("d", true, "Path to compressed file *.cp.tar.bzip2 which should be decompressed");
		options.addOption("f", true, "Log file name extension");
		options.addOption("del", true, "using delete graph with this size");
//		options.addOption("comb", false, "");
		options.addOption("hdt",false,"If set value model is further processed with HDT.");
		return options;
	}
	
	
	
    public static void main(String[] args) throws ArchiveException{
  	    	CommandLineParser parser = new BasicParser();
  	    	try {
  				CommandLine cmd = parser.parse(getCLIOptions(), args);
  				if(cmd.hasOption("c")) {
	  				
	  	
	  				File path = new File(cmd.getOptionValue("c"));
	  				if(!path.exists()) {
	  					System.err.println("File/Directory to compress not found.");
	  					System.exit(0);
	  				}
	  				ModelCompressor compressor = new ModelCompressor(path);
//	  				if(cmd.hasOption("comb"))
//	  					compressor = new ModelCompressor(path);
	  				if(cmd.hasOption("f"))
	  					compressor.setLogFileSuffix(cmd.getOptionValue("f"));
	  				int delete = 1;
	  				if(cmd.hasOption("del")) {
	  					delete = Integer.parseInt(cmd.getOptionValue("del"));
	  				}
	  				if(cmd.hasOption("hdt")) {
	  					compressor.setHDT(true);
	  				}
	  				compressor.setFile(path);
	  				compressor.setDelete(delete);
	  				
	  				compressor.compress();
  				} else if(cmd.hasOption("d")) {
  					File path = new File(cmd.getOptionValue("d"));
  					if(!path.exists()) {
	  					System.err.println("File/Directory to compress not found.");
	  					System.exit(0);
	  				}
  					DefaultDecompressor dcmp = new DefaultDecompressor(path);
  					long start = System.currentTimeMillis();
  					try {
  						System.out.println("Start decompression:");
						dcmp.decompress(path);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (CompressorException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
  					long duration = System.currentTimeMillis()-start;
  					System.out.println("Succesfully decompressed "+path.getName() +" in "+duration+"ms = "+duration/1000+"s = "+duration/60000+"min.");
  				}
  			} catch (ParseException e) {
  				// TODO Auto-generated catch block
  				e.printStackTrace();
  			}
    }

}
