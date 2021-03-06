package de.uni_leipzig.simba.compress;

public class CompressorFactory {
	
	public enum Type {
		INDEX
	}
	
	public static Compressor getCompressor() {
		return new IndexBasedCompressor();
	}
	
	public static Compressor getCompressor(Type type) {
		switch(type) {
			case INDEX: return new IndexBasedCompressor();
			default: return new IndexBasedCompressor();
		}
	}
}
