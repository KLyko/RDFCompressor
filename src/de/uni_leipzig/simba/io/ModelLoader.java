package de.uni_leipzig.simba.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
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
	
	/**
	 * A Method to create a subModel of size percentage.
	 * @param original Original Model to get sub model from.
	 * @param percentage Percentage of the subModel: 0 < percentage < 1.
	 * @return
	 */
	public static Model createSubModel(Model original, float percentage) {
		if(percentage<=0)
			return ModelFactory.createDefaultModel();
		if(percentage>=1)
			return original;
		Model subModel = ModelFactory.createDefaultModel();
		subModel.setNsPrefixes(	original.getNsPrefixMap());
		long stmtMax = Math.round(original.size() * percentage);
		long stmtCount = 0;
		System.out.println(percentage+"% of "+original.size()+" = "+stmtMax);
		StmtIterator it = original.listStatements();
		while(it.hasNext() && stmtCount<stmtMax) {
			subModel.add(it.next());
			stmtCount++;
		}
		return subModel;
	}
	
	
	public static void main(String args[]) {
		String file = "resources/archive_hub_dump.nt";
		String folder = "resources/ubl_part/";
		Model m;
		try {
			m = ModelLoader.getModel(folder);

			System.out.println(m.size());
			float[] floats  = new float[]{0.1f, 0.2f, 0.5f, 0.8f, 1f};
			for(float perc : floats) {
				System.out.println("####### "+perc+"% #########");
				Model subModel = createSubModel(m, perc);
				System.out.println("sumModel: "+subModel.size()+" - "+m.difference(subModel).size());
			    for (Entry<String, String>  entry : subModel.getNsPrefixMap().entrySet()) {
			    	System.out.println("->"+entry);
			    }
			}
			
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
