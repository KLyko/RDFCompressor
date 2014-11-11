package de.uni_leipzig.simba.evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.hp.hpl.jena.rdf.model.Model;

import de.uni_leipzig.simba.compress.ModelCompressor;
import de.uni_leipzig.simba.decompress.DefaultDecompressor;
import de.uni_leipzig.simba.io.ModelLoader;

public class ScalabilityEvaluation {
	public static final String SEP =";";
	public static Options getCLIOptions() {
		Options options = new Options();
		options.addOption("c", true, "path to resource which should be compressed");
		options.addOption("d", true, "path to decompress");
		return options;
	}
	
	public static void main(String[] args) throws ParseException {
		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(getCLIOptions(), args);
		if(cmd.hasOption("c")) {
			File path = new File(cmd.getOptionValue("c"));
			try {
				
				runScalabilityEval(path);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(cmd.hasOption("d")) {
			File path = new File(cmd.getOptionValue("d"));
			runDecompression(path);
		}
	}
	
	public static void runScalabilityEval(File path) throws UnsupportedEncodingException, FileNotFoundException {
		float[] percantiles = new float[]{0.1f,0.2f,0.3f,0.4f,0.5f,0.6f,0.7f,0.8f,0.9f,1.0f};
		
		Model completeModel = ModelLoader.getModel(path.getAbsolutePath());
		for(int run =1; run<=3; run++) {
			for(float perc : percantiles) {
				String logExt="_run"+run+"_"+perc+"_";
				ModelCompressor compr = new ModelCompressor(path);
				Model model = ModelLoader.createSubModel(completeModel, perc);
				compr.compress(model, logExt);
			}
		}
		
		
	}
	
	public static void runDecompression(File f) {
		String[] files;		File logFile;
		if(f.isDirectory()) {
			FilenameFilter nameFilter = new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
						return name.endsWith(".cp.tar.bz2");
				}			
			};
			files = f.list(nameFilter);
			logFile = new File(f.getAbsoluteFile()+"/decompression_scaler.csv");
		} else {
			files = new String[1];
			files[0] = f.getName();
			logFile = new File(f.getName()+"decompression_scaler.csv");
		}

		writeLogFile(logFile,"on File "+f.getAbsolutePath(), false);
		writeLogFile(logFile,"File"+SEP+"time in ms"+SEP+"time in s",true);
		
		for(String s : files) {
			
			File file = new File(f.getAbsoluteFile()+"/"+s);
			System.out.println(file.getName().substring(0, file.getName().indexOf(".cp")));
			DefaultDecompressor dcomp = new DefaultDecompressor(file);
			try {
				long start = System.currentTimeMillis();
				dcomp.decompress(file);
				long dur = System.currentTimeMillis()-start;
				String log = file.getName()+SEP+dur+SEP+dur/1000;
				writeLogFile(logFile,log,true);
			} catch(Exception e) {
				e.printStackTrace();
			}
			
		}
			
		
	}
	
	protected static void writeLogFile(File logFile, String log, boolean append) {
//		logFile = new File(source.getAbsolutePath()+"_log"+logFileSuffix+".txt");
		try {			
			FileWriter writer =  new FileWriter(logFile, append);
			writer.write(log);
			writer.write(System.getProperty("line.separator"));
			writer.flush();
			writer.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
}
