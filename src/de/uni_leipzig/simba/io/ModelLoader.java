package de.uni_leipzig.simba.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

public class ModelLoader {
	private static Model loadLUBMModel(String pathToFolder) throws UnsupportedEncodingException, FileNotFoundException {		
		File folder = new File(pathToFolder);
		FilenameFilter nameFilter = new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
					return name.endsWith(".owl") || name.endsWith(".rdf");
			}			
		};
		String[] files = folder.list(nameFilter);
		
		Model globalModel = null;
		Logger logger = Logger.getLogger(ModelLoader.class);
		if(!pathToFolder.endsWith("/") && !pathToFolder.endsWith("\\"))
			pathToFolder = pathToFolder+"/";
		
		for(String fileName : files) {
			logger.info("Reading file "+fileName);
			
			Model model = loadModelFile(pathToFolder+fileName);
			if(globalModel == null) {
				globalModel = model;
			}else {
				long size0 = globalModel.size();
				globalModel = globalModel.union(model);
				logger.info("GlobalModel from "+size0+" now "+globalModel.size());
			}
		}
		return globalModel;		
	}
	
	private static Model loadModelFile(String pathToFile) throws UnsupportedEncodingException, FileNotFoundException {
		FileManager.get().addLocatorClassLoader(ModelLoader.class.getClassLoader());
//		InputStream stream = new FileInputStream(pathToFile);
//		InputStreamReader in = new InputStreamReader(stream, "utf-8");
		return  FileManager.get().loadModel(pathToFile);
	}
	
	/**
	 * 
	 * @param path
	 * @return
	 * @throws FileNotFoundException 
	 * @throws UnsupportedEncodingException 
	 */
	public static Model getModel(String path) throws UnsupportedEncodingException, FileNotFoundException {
		File file = new File(path);
		if(file.isDirectory()) {
			return loadLUBMModel(path);
		}
		else {
			return loadModelFile(path);
		}
	}
	
	public static void main(String args[]) {
		String file = "resources/jamendo.rdf";
		String folder = "resources/LUBM/lubm_50/";
		Model m;
		try {
			m = ModelLoader.getModel(file);

			System.out.println(m.size());
			
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
