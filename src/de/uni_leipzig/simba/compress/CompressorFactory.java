package de.uni_leipzig.simba.compress;

public class CompressorFactory {
	public static Compressor getCompressor() {
		return new DefaultCompressor();
	}
}
