package mapwriter.region;

public class ChunkToPixels {
	
	// values that change how height shading algorithm works
	public static final double brightenExponent = 0.35;
	public static final double darkenExponent = 0.35;
	public static final double brightenAmplitude = 0.7;
	public static final double darkenAmplitude = 1.4;
	
	public static int getFirstOpaqueBlockY(BlockColours bc, MwChunk chunk, int x, int y, int z) {
		boolean found = false;
		// search in a column downwards for the first opaque block (alpha = 0xff)
		for (; (y >= 0) && !found; y--) {
			int blockAndMeta = chunk.getBlockAndMetadata(x, y, z);
			int colour = bc.getColour(blockAndMeta);
			if (((colour >> 24) & 0xff) == 0xff) {
				found = true;
			}
		}
		return y + 1;
	}
	
	public static int getFirstNonOpaqueBlockY(BlockColours bc, MwChunk chunk, int x, int y, int z) {
		boolean found = false;
		// search in a column downwards for the first non opaque block (alpha != 0xff)
		for (; (y >= 0) && !found; y--) {
			int blockAndMeta = chunk.getBlockAndMetadata(x, y, z);
			int colour = bc.getColour(blockAndMeta);
			if (((colour >> 24) & 0xff) != 0xff) {
				found = true;
			}
		}
		return y + 1;
	}
	
	public static double getPixelHeightShading(int[] pixels, int offset, int scanSize) {
		int samples = 0;
		int height = (pixels[offset] >> 24) & 0xff;
		int heightDiff = 0;
		
		// if (pixelX > 0)
		if ((offset & (scanSize - 1)) != 0) {
			int heightW = ((pixels[offset - 1] >> 24) & 0xff);
			if ((heightW > 0) && (heightW < 255)) {
				heightDiff += height - heightW;
				samples++;
			}
		}
		// if (pixelZ > 0)
		if (offset >= scanSize) {
			int heightN = ((pixels[offset - scanSize] >> 24) & 0xff);
			if ((heightN > 0) && (heightN < 255)) {
				heightDiff += height - heightN;
				samples++;
			}
		}
		
		double heightShading = 0.0;
		if (samples > 0) {
			// emphasize small differences in height, but as the difference in height increases,
			// don't increase so much
			double h = (double) heightDiff / ((double) samples * 255.0);
			heightShading = (h >= 0.0) ?
					Math.pow(h, brightenExponent) * brightenAmplitude :
					-Math.pow(-h, darkenExponent) * darkenAmplitude;
		}
		
		return heightShading;
	}
	
	public static void getMapPixels(BlockColours bc, MwChunk chunk, int[] pixels, int offset, int scanSize) {
		boolean caveMap = (chunk.dimension == -1);
		
		// opaque layer
		for (int z = 0; z < MwChunk.SIZE; z++) {
			for (int x = 0; x < MwChunk.SIZE; x++) {
				
				int yEnd = (caveMap) ? getFirstNonOpaqueBlockY(bc, chunk, x, chunk.maxHeight - 1, z) : chunk.maxHeight - 1;
				int yStart = getFirstOpaqueBlockY(bc, chunk, x, yEnd, z);
				
				int biome = chunk.getBiome(x, z);
				
				double r = 0.0;
				double g = 0.0;
				double b = 0.0;
				for (int y = yStart; y <= yEnd; y++) {
					int blockAndMeta = chunk.getBlockAndMetadata(x, y, z);
					
					int c1 = bc.getColour(blockAndMeta);
					int c2 = bc.getBiomeColour(blockAndMeta, biome);
					
					double c1A = (double) ((c1 >> 24) & 0xff) / 255.0;
					double c1R = (double) ((c1 >> 16) & 0xff) / 255.0;
					double c1G = (double) ((c1 >> 8)  & 0xff) / 255.0;
					double c1B = (double) ((c1 >> 0)  & 0xff) / 255.0;
					
					double c2R = (double) ((c2 >> 16) & 0xff) / 255.0;
					double c2G = (double) ((c2 >> 8)  & 0xff) / 255.0;
					double c2B = (double) ((c2 >> 0)  & 0xff) / 255.0;
					
					// alpha blend and multiply
					r = r * (1.0 - c1A) + ((c1R * c2R) * c1A);
					g = g * (1.0 - c1A) + ((c1G * c2G) * c1A);
					b = b * (1.0 - c1A) + ((c1B * c2B) * c1A);
				}
				
				// shade heights
				int pixel = ((yStart & 0xff) << 24);
				int pixelOffset = offset + (z * scanSize) + x;
				pixels[pixelOffset] = pixel;
				double shading = 1.0 + getPixelHeightShading(pixels, pixelOffset, scanSize);
				
				/*
				// darken blocks depending on how far away they are from this depth slice
				if (depth != 0) {
					int bottomOfSlice = maxHeight - ((depth + 1) * maxHeight / Mw.HEIGHT_LEVELS) - 1;
					if (yRange[0] < bottomOfSlice) {
						shading *= 1.0 - 2.0 * ((double) (bottomOfSlice - yRange[0]) / (double) maxHeight);
					}
				}*/
				
				r = Math.min(Math.max(0.0, r * shading), 1.0);
				g = Math.min(Math.max(0.0, g * shading), 1.0);
				b = Math.min(Math.max(0.0, b * shading), 1.0);
				
				// now we have our final RGB values as doubles, convert to a packed ARGB pixel.
				pixels[offset + (z * scanSize) + x] = (pixel & 0xff000000) |
						((((int) (r * 255.0)) & 0xff) << 16) |
						((((int) (g * 255.0)) & 0xff) << 8) |
						((((int) (b * 255.0)) & 0xff));
			}
		}
		//MwUtil.log("chunk (%d, %d): height %d, %d blocks processed", thisx, thisz, maxHeight, count);
	}
}
