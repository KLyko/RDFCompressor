package de.uni_leipzig.simba.compress;

public class CompressorFactory {
	
	enum Type {
		INDEX
	}
	
	public static Compressor getCompressor() {
		return new DefaultCompressor();
	}
	
	public static Compressor getCompressor(Type type) {
		switch(type) {
			case INDEX: return new IndexBasedCompressor();
			default: return new DefaultCompressor();
		}
	}
}
