package mapwriter.region;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public class MergeToImage {
	public static final int MAX_WIDTH = 8192;
	public static final int MAX_HEIGHT = 8192;
	
	public static BufferedImage merge(RegionManager regionManager, int xCentre, int zCentre, int w, int h, int dimension) {
		
		w = Math.min(Math.max(0, w), MAX_WIDTH - Region.SIZE);
		h = Math.min(Math.max(0, h), MAX_HEIGHT - Region.SIZE);
		
		int rXMin = (xCentre - (w / 2)) >> Region.SHIFT;
		int rZMin = (zCentre - (h / 2)) >> Region.SHIFT;
		int rXMax = (xCentre + (w / 2)) >> Region.SHIFT;
		int rZMax = (zCentre + (h / 2)) >> Region.SHIFT;
		
		RegionManager.logInfo("merging area (%d,%d) -> (%d,%d)",
				rXMin << Region.SHIFT, rZMin << Region.SHIFT,
				rXMax << Region.SHIFT, rZMax << Region.SHIFT);
		
		int pixels[] = new int[Region.SIZE * Region.SIZE];
		
		// create the image and graphics context
		// this is the most likely place to run out of memory
		BufferedImage mergedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		
		// copy region PNGs to the image
		for (int rZ = rZMin; rZ <= rZMax; rZ++) {
			for (int rX = rXMin; rX <= rXMax; rX++) {
				//MwUtil.log("merging region (%d,%d)", rX << Mw.REGION_SHIFT, rZ << Mw.REGION_SHIFT);
				Arrays.fill(pixels, 0);
				
				Region region = regionManager.getRegion(rX << Region.SHIFT, rZ << Region.SHIFT, 0, dimension);
				int[] regionPixels = region.getPixels();
				if (regionPixels != null) {
					for (int i = 0; (i < pixels.length) && (i < regionPixels.length); i++) {
						pixels[i] = regionPixels[i] | 0xff000000;
					}
				} else {
					Arrays.fill(pixels, 0xff000000);
				}
				
				int dstX = (rX - rXMin) << Region.SHIFT;
				int dstZ = (rZ - rZMin) << Region.SHIFT;
				mergedImage.setRGB(dstX, dstZ, Region.SIZE, Region.SIZE, pixels, 0, Region.SIZE);
			}
		}
		
		RegionManager.logInfo("image merge complete");
		
		return mergedImage;
	}
}
