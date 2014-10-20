package de.uni_leipzig.simba.evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.hp.hpl.jena.rdf.model.Model;

import de.uni_leipzig.simba.compress.ModelCompressor;
import de.uni_leipzig.simba.io.ModelLoader;

public class ScalabilityEvaluation {
	public static Options getCLIOptions() {
		Options options = new Options();
		options.addOption("c", true, "path to resource which should be compressed");
		return options;
	}
	
	public static void main(String[] args) throws ParseException {
		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(getCLIOptions(), args);
		if(!cmd.hasOption("c")) {
			System.err.println("File to compress must be specified.");
			System.exit(0);
		}
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
	
	public static void runScalabilityEval(File path) throws UnsupportedEncodingException, FileNotFoundException {
		float[] percantiles = new float[]{0.1f,0.2f,0.3f,0.4f,0.5f,0.6f,0.7f,0.8f,0.9f,1.0f};
		
		Model completeModel = ModelLoader.getModel(path.getAbsolutePath());
		
		for(float perc : percantiles) {
			String logExt="_"+perc+"_";
			ModelCompressor compr = new ModelCompressor(path);
			Model model = ModelLoader.createSubModel(completeModel, perc);
			compr.compress(model, logExt);
		}
		
	}
	
}
