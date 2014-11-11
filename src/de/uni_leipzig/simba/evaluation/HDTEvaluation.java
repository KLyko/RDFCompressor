package de.uni_leipzig.simba.evaluation;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;

import com.hp.hpl.jena.rdf.model.Model;

import de.uni_leipzig.simba.compress.BasicCompressor;
import de.uni_leipzig.simba.io.ModelLoader;

public class HDTEvaluation {
	public static String tmpDir = "resources";
	public String name;
	File file;
	File logFile = new File(tmpDir+"/"+"HDTlog"+".txt");
	
	public HDTEvaluation(String name, File file) {
		this.file = file;
		this.name = name;
		logFile = new File(tmpDir+"/"+name+"_HDTLog.txt");
	}
	private void runWithModel(File file) throws UnsupportedEncodingException, FileNotFoundException {
		Model model = ModelLoader.getModel(file.getAbsolutePath());
		File out = new File(file.getAbsolutePath()+".nt");
			try {
				OutputStream fos = new FileOutputStream(out, false);
//		        BZip2CompressorOutputStream  outputStream = new BZip2CompressorOutputStream (fos);

				model.write(fos, "N-TRIPLE");
			
//				outputStream.close();
				fos.close();
				run(out);
			} catch(Exception e) {
				e.printStackTrace();
			}
	}
	
	private void run(File f) throws IOException, ParserException {
		writeLogFile(logFile, "Start HDT on file "+f.getAbsolutePath(), false);
		long start = System.currentTimeMillis();
		OutputStream os = new FileOutputStream(f.getAbsolutePath() + ".hdt.tar.bz2", false);
		OutputStream bzos = new BZip2CompressorOutputStream(os);
		TarArchiveOutputStream aos = new TarArchiveOutputStream(bzos);
		
	
			HDT hdt = HDTManager.generateHDT(
				f.getAbsolutePath(),
				"urn:rdfcomp",
				RDFNotation.parse("ntriples"),
				new HDTSpecification(),
				null
			);
			
			hdt.saveToHDT(tmpDir + "/"+name+"_data.hdt", null);
			long saveToHDT = System.currentTimeMillis()-start;
			File filePrefix = new File(tmpDir + "/"+name+"_data.hdt");
			TarArchiveEntry entry = new TarArchiveEntry(filePrefix, "mappings.hdt");
			entry.setSize(filePrefix.length());
			aos.putArchiveEntry(entry);
			IOUtils.copy(new FileInputStream(filePrefix), aos);
			aos.closeArchiveEntry();
			aos.finish();
			aos.close();
			bzos.close();
			os.close();
			long saveToTarBzip2 = System.currentTimeMillis() - saveToHDT;
			long overall = System.currentTimeMillis() -start;
			String log = "Original size: "+f.length()+ "B = "+
					f.length()/1024+" KB"+" ="+
					f.length()/(1024*1024)+" MB"; 
			writeLogFile(logFile, log, true);
			
			
			Model model = ModelLoader.getModel(file.getAbsolutePath());
			long ntBzip2 = computeOrginalNTriple(model, file);
			
			log = "NT+BZIP size: "+ntBzip2+" B= "+ntBzip2/1024+" KB= "+ntBzip2/(1024*1024)+" MB";
			writeLogFile(logFile,log, true);
			log= "HDT size: "+filePrefix.length()+" B= "+filePrefix.length()/1024+" KB= "+filePrefix.length()/(1024*1024)+" MB";
			writeLogFile(logFile,log,true);
			long hdtBzip2Size = new File(f.getAbsolutePath() + ".hdt.tar.bz2").length();
			log= "HDT+BZIP2 size: "+hdtBzip2Size+" B= "+hdtBzip2Size/1024+" KB= "+hdtBzip2Size/(1024*1024)+" MB";
			writeLogFile(logFile,log,true);
			
			double ratio =  new Double(filePrefix.length()) / new Double(ntBzip2);
			log= "HDT / NTBZip2 ratio= "+ratio;
			writeLogFile(logFile,log,true);
			ratio = new Double(hdtBzip2Size) / new Double(ntBzip2);
			log= "HDT+BZIP2 / NTBZip2 ratio= "+ratio+" \n\n";
			writeLogFile(logFile,log,true);
			
			
			log ="Time HDT: "+saveToHDT+"ms = "+ saveToHDT /1000+" s ";writeLogFile(logFile,log,true);
			log +="Time HDT+BZIP: "+saveToTarBzip2+"ms ="+ saveToTarBzip2 /1000 +"s";writeLogFile(logFile,log,true);
			log +="Time overall: "+overall+"ms = "+ overall /1000 +" s ";writeLogFile(logFile,log,true);
			

	}
	
	
	public void run() throws IOException, ParserException {
		if(file.getAbsolutePath().endsWith(".nt") || file.getAbsolutePath().endsWith(".ttl")) {
			run(file);
		} else  {
			runWithModel(file);
		}
	}
	
	
	public long computeOrginalNTriple(Model model, File file) {
		File out = new File(file.getAbsolutePath()+".nt.bz2");

			try {
				OutputStream fos = new BufferedOutputStream(new FileOutputStream(out, false));
		        BZip2CompressorOutputStream  outputStream = new BZip2CompressorOutputStream (fos);
			    
				model.write(outputStream, "N-TRIPLE");
			
				outputStream.close();
				fos.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		if(out.exists())
			return out.length();
		else {
			System.err.println("Wasn't able to compute original ntriple size +bzip2!");
			return file.length();
			
		}
			
	}
	
	
	public static void main(String args[]) {
		File f = new File("resources/archive_hub_dump.nt");
		String name = f.getName();
		
		CommandLineParser parser = new BasicParser();
	    CommandLine cmd;
		try {
			cmd = parser.parse(getCLIOptions(), args);
			if(cmd.hasOption("file")) {
				f = new File(cmd.getOptionValue("file"));
				if(!f.exists()) {
					System.err.println("File/Directory to compress not found.");
					System.exit(0);
				}
			}
			if(cmd.hasOption("name")) {
				name = cmd.getOptionValue("name");
			}
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	
		HDTEvaluation hdtEval = new HDTEvaluation(name, f);
		
		
		try {
			hdtEval.run();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserException e) {
			e.printStackTrace();
		}
	}
	
	
	 
	protected void writeLogFile(File source, String log, boolean append) {
		logFile = new File(tmpDir+"/"+name+"_HDTlog"+".txt");
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

	public File getLogFile() {
		return logFile;
	}
	
	

	
	public static Options getCLIOptions() {
		Options options = new Options();
		options.addOption("file", true, "path to resource which should be compressed with HDT");
		options.addOption("name", true, "name of the dataset");
		return options;
	}
}
