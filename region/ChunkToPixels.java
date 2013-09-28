package mapwriter.region;

public class ChunkToPixels {
	
	// values that change how height shading algorithm works
	public static final double brightenExponent = 0.35;
	public static final double darkenExponent = 0.35;
	public static final double brightenAmplitude = 0.7;
	public static final double darkenAmplitude = 1.4;
	
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
	
	// get the height shading of a pixel.
	// requires the pixel to the west and the pixel to the north to have their
	// heights stored in the alpha channel to work.
	// the "height" of a pixel is the y value of the first opaque block in
	// the block column that created the pixel.
	public static double getPixelHeightShading(int[] pixels, int offset, int scanSize, int height) {
		int samples = 0;
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
		// if the dimension has a roof caveMap should be enabled
		boolean caveMap = (chunk.dimension == -1);
		
		for (int z = 0; z < MwChunk.SIZE; z++) {
			for (int x = 0; x < MwChunk.SIZE; x++) {
				
				// calculate the colour of a pixel by alpha blending the colour of each block
				// in a column until an opaque block is reached.
				// y is topmost block height to start rendering at.
				// for maps without a ceiling y is simply the height of the highest block in that chunk.
				// for maps with a ceiling y is the height of the first non opaque block starting from
				// the ceiling.
				int y = (caveMap) ? getFirstNonOpaqueBlockY(bc, chunk, x, chunk.maxHeight - 1, z) : chunk.maxHeight - 1;
				
				int biome = chunk.getBiome(x, z);
				
				// for every block in the column starting from the highest:
				//  - get the block colour
				//  - get the biome shading
				//  - extract colour components as doubles in the range [0.0, 1.0]
				//  - the shaded block colour is simply the block colour multiplied
				//    by the biome shading for each component
				//  - this shaded block colour is alpha blended with the running
				//    colour for this column
				//
				// so the final map colour is an alpha blended stack of all the
				// individual shaded block colours in the sequence [yStart .. yEnd]
				//
				// note that the "front to back" alpha blending algorithm is used
				// rather than the more common "back to front".
				//
				
				double a = 1.0;
				double r = 0.0;
				double g = 0.0;
				double b = 0.0;
				for (; y > 0; y--) {
					int blockAndMeta = chunk.getBlockAndMetadata(x, y, z);
					
					int c1 = bc.getColour(blockAndMeta);
					int alpha = (c1 >> 24) & 0xff;
					// no need to process block if it is transparent
					if (alpha > 0) {
						int c2 = bc.getBiomeColour(blockAndMeta, biome);
						
						// extract colour components as normalized doubles
						double c1A = (double) (alpha) / 255.0;
						double c1R = (double) ((c1 >> 16) & 0xff) / 255.0;
						double c1G = (double) ((c1 >> 8)  & 0xff) / 255.0;
						double c1B = (double) ((c1 >> 0)  & 0xff) / 255.0;
						
						// c2A is implicitly 1.0 (opaque)
						double c2R = (double) ((c2 >> 16) & 0xff) / 255.0;
						double c2G = (double) ((c2 >> 8)  & 0xff) / 255.0;
						double c2B = (double) ((c2 >> 0)  & 0xff) / 255.0;
						
						// alpha blend and multiply
						r = r + (a * c1A * c1R * c2R);
						g = g + (a * c1A * c1G * c2G);
						b = b + (a * c1A * c1B * c2B);
						a = a * (1.0 - c1A);
					}
					// break when an opaque block is encountered
					if (alpha == 0xff) {
						break;
					}
				}
				
				// get height shading based on neighboring pixel heights.
				// need to first add a dummy colour value with the block
				// height in the alpha channel.
				int pixelOffset = offset + (z * scanSize) + x;
				double shading = 1.0 + getPixelHeightShading(pixels, pixelOffset, scanSize, y);
				
				/*
				// darken blocks depending on how far away they are from this depth slice
				if (depth != 0) {
					int bottomOfSlice = maxHeight - ((depth + 1) * maxHeight / Mw.HEIGHT_LEVELS) - 1;
					if (yRange[0] < bottomOfSlice) {
						shading *= 1.0 - 2.0 * ((double) (bottomOfSlice - yRange[0]) / (double) maxHeight);
					}
				}*/
				
				// apply the height shading
				r = Math.min(Math.max(0.0, r * shading), 1.0);
				g = Math.min(Math.max(0.0, g * shading), 1.0);
				b = Math.min(Math.max(0.0, b * shading), 1.0);
				
				// now we have our final RGB values as doubles, convert to a packed ARGB pixel.
				pixels[pixelOffset] = ((y & 0xff) << 24) |
						((((int) (r * 255.0)) & 0xff) << 16) |
						((((int) (g * 255.0)) & 0xff) << 8) |
						((((int) (b * 255.0)) & 0xff));
			}
		}
		//MwUtil.log("chunk (%d, %d): height %d, %d blocks processed", thisx, thisz, maxHeight, count);
	}
}
