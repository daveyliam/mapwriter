package mapwriter.region;

public class ChunkRender {
	
	public static final byte FLAG_UNPROCESSED = 0;
	public static final byte FLAG_NON_OPAQUE = 1;
	public static final byte FLAG_OPAQUE = 2;

	
	// values that change how height shading algorithm works
	public static final double brightenExponent = 0.35;
	public static final double darkenExponent = 0.35;
	public static final double brightenAmplitude = 0.7;
	public static final double darkenAmplitude = 1.4;
	
	// get the height shading of a pixel.
	// requires the pixel to the west and the pixel to the north to have their
	// heights stored in the alpha channel to work.
	// the "height" of a pixel is the y value of the first opaque block in
	// the block column that created the pixel.
	// height values of 0 and 255 are ignored as these are used as the clear
	// values for pixels.
	public static double getHeightShading(int height, int heightW, int heightN) {
		int samples = 0;
		int heightDiff = 0;
		
		if ((heightW > 0) && (heightW < 255)) {
			heightDiff += height - heightW;
			samples++;
		}

		if ((heightN > 0) && (heightN < 255)) {
			heightDiff += height - heightN;
			samples++;
		}
		
		double heightDiffFactor = 0.0;
		if (samples > 0) {
			heightDiffFactor = (double) heightDiff / ((double) samples * 255.0);
		}
		
		// emphasize small differences in height, but as the difference in height increases,
		// don't increase so much
		// TODO: probably more accurate to use atan here rather than a fractional
		// exponent.
		return (heightDiffFactor >= 0.0) ?
				Math.pow(heightDiffFactor, brightenExponent) * brightenAmplitude :
				-Math.pow(-heightDiffFactor, darkenExponent) * darkenAmplitude;
	}
	
	// calculate the colour of a pixel by alpha blending the colour of each block
	// in a column until an opaque block is reached.
	// y is topmost block height to start rendering at.
	// for maps without a ceiling y is simply the height of the highest block in that chunk.
	// for maps with a ceiling y is the height of the first non opaque block starting from
	// the ceiling.
	//
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
	public static int getColumnColour(BlockColours bc, IChunk chunk, int x, int y, int z, int heightW, int heightN) {
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
				int biome = chunk.getBiome(x, z);
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
		
		/*
		// darken blocks depending on how far away they are from this depth slice
		if (depth != 0) {
			int bottomOfSlice = maxHeight - ((depth + 1) * maxHeight / Mw.HEIGHT_LEVELS) - 1;
			if (yRange[0] < bottomOfSlice) {
				shading *= 1.0 - 2.0 * ((double) (bottomOfSlice - yRange[0]) / (double) maxHeight);
			}
		}*/
		
		double heightShading = getHeightShading(y, heightW, heightN);
		int lightValue = chunk.getLightValue(x, y + 1, z);
		double lightShading = (double) lightValue / 15.0;
		double shading = (heightShading + 1.0) * lightShading;
		
		// apply the shading
		r = Math.min(Math.max(0.0, r * shading), 1.0);
		g = Math.min(Math.max(0.0, g * shading), 1.0);
		b = Math.min(Math.max(0.0, b * shading), 1.0);
		
		// now we have our final RGB values as doubles, convert to a packed ARGB pixel.
		return ((y & 0xff) << 24) |
			((((int) (r * 255.0)) & 0xff) << 16) |
			((((int) (g * 255.0)) & 0xff) << 8) |
			((((int) (b * 255.0)) & 0xff));
	}
	
	static int getPixelHeightN(int[] pixels, int offset, int scanSize) {
		return (offset >= scanSize) ? ((pixels[offset - scanSize] >> 24) & 0xff) : -1;
	}
	
	static int getPixelHeightW(int[] pixels, int offset, int scanSize) {
		return ((offset & (scanSize - 1)) >= 1) ? ((pixels[offset - 1] >> 24) & 0xff) : -1;
	}
	
	public static void renderSurface(BlockColours bc, IChunk chunk, int[] pixels, int offset, int scanSize, boolean dimensionHasCeiling) {
		int chunkMaxY = chunk.getMaxY();
		for (int z = 0; z < MwChunk.SIZE; z++) {
			for (int x = 0; x < MwChunk.SIZE; x++) {
				// for the nether dimension search for the first non-opaque
				// block below the ceiling.
				// cannot use y = chunkMaxY as the nether sometimes spawns
				// mushrooms above the ceiling height. this fixes the
				// rectangular grey areas (ceiling bedrock) on the nether map.
				int y;
				if (dimensionHasCeiling) {
					for (y = 127; y >= 0; y--) {
						int blockAndMeta = chunk.getBlockAndMetadata(x, y, z);
						int alpha = (bc.getColour(blockAndMeta) >> 24) & 0xff;
						if (alpha != 0xff) {
							break;
						}
					}
				} else {
					y = chunkMaxY;
				}
				
				int pixelOffset = offset + (z * scanSize) + x;
				pixels[pixelOffset] = getColumnColour(
					bc, chunk, x, y, z, 
					getPixelHeightW(pixels, pixelOffset, scanSize),
					getPixelHeightN(pixels, pixelOffset, scanSize)
				);
			}
		}
	}
	
	public static void renderUnderground(BlockColours bc, IChunk chunk, int[] pixels, int offset, int scanSize, int startY, byte[] mask) {
		startY = Math.min(Math.max(0, startY), 255);
		for (int z = 0; z < MwChunk.SIZE; z++) {
			for (int x = 0; x < MwChunk.SIZE; x++) {
				
				// only process columns where the mask bit is set.
				// process all columns if mask is null.
				if ((mask != null) && ((mask[(z * 16) + x]) != FLAG_NON_OPAQUE)) {
					continue;
				}
				
				// get the last non transparent block before the first opaque block searching
				// towards the sky from startY
				int lastNonTransparentY = startY;
				for (int y = startY; y < chunk.getMaxY(); y++) {
					int blockAndMeta = chunk.getBlockAndMetadata(x, y, z);
					int alpha = (bc.getColour(blockAndMeta) >> 24) & 0xff;
					if (alpha == 0xff) {
						break;
					}
					if (alpha > 0) {
						lastNonTransparentY = y;
					}
				}
				
				int pixelOffset = offset + (z * scanSize) + x;
				pixels[pixelOffset] = getColumnColour(
					bc, chunk, x, lastNonTransparentY, z,
					getPixelHeightW(pixels, pixelOffset, scanSize),
					getPixelHeightN(pixels, pixelOffset, scanSize)
				);
			}
		}
	}
}
