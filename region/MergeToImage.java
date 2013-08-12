package mapwriter.region;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public class MergeToImage {
	public static final int MAX_WIDTH = 8192;
	public static final int MAX_HEIGHT = 8192;
	
	public static BufferedImage merge(RegionManager regionManager, int xCentre, int zCentre, int w, int h, int dimension) {
		
		// round up to nearest 512 block boundary
		w = ((w + Region.SIZE - 1) & Region.MASK);
		h = ((h + Region.SIZE - 1) & Region.MASK);
		
		// clamp to an integer between 512 and 8192 inclusive
		w = Math.min(Math.max(Region.SIZE, w), MAX_WIDTH);
		h = Math.min(Math.max(Region.SIZE, h), MAX_HEIGHT);
		
		int xMin = xCentre - (w / 2);
		int zMin = zCentre - (h / 2);
		
		// round to nearest region boundary
		xMin = Math.round(((float) xMin) / ((float) Region.SIZE)) * Region.SIZE;
		zMin = Math.round(((float) zMin) / ((float) Region.SIZE)) * Region.SIZE;
		
		int xMax = xMin + w;
		int zMax = zMin + h;
		
		RegionManager.logInfo("merging area starting at (%d,%d), %dx%d blocks",
				xMin, zMin, w, h);
		
		int pixels[] = new int[Region.SIZE * Region.SIZE];
		
		// create the image and graphics context
		// this is the most likely place to run out of memory
		BufferedImage mergedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		
		// copy region PNGs to the image
		for (int z = zMin; z < zMax; z += Region.SIZE) {
			for (int x = xMin; x < xMax; x += Region.SIZE) {
				//MwUtil.log("merging region (%d,%d)", rX << Mw.REGION_SHIFT, rZ << Mw.REGION_SHIFT);
				Arrays.fill(pixels, 0);
				
				// get region pixels
				Region region = regionManager.getRegion(x, z, 0, dimension);
				int[] regionPixels = region.getPixels();
				if (regionPixels != null) {
					for (int i = 0; (i < pixels.length) && (i < regionPixels.length); i++) {
						pixels[i] = regionPixels[i] | 0xff000000;
					}
				} else {
					Arrays.fill(pixels, 0xff000000);
				}
				
				// copy region pixels to the output image
				mergedImage.setRGB(x - xMin, z - zMin, Region.SIZE, Region.SIZE, pixels, 0, Region.SIZE);
			}
		}
		
		RegionManager.logInfo("image merge complete");
		
		return mergedImage;
	}
}
