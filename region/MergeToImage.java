package mapwriter.region;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class MergeToImage {
	public static final int MAX_WIDTH = 8192;
	public static final int MAX_HEIGHT = 8192;
	
	public static BufferedImage mergeRegions(RegionManager regionManager, int x, int z, int w, int h, int dimension) {
		// create the image and graphics context
		// this is the most likely place to run out of memory
		BufferedImage mergedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		
		// copy region PNGs to the image
		for (int zi = 0; zi < h; zi += Region.SIZE) {
			for (int xi = 0; xi < w; xi += Region.SIZE) {
				//MwUtil.log("merging region (%d,%d)", rX << Mw.REGION_SHIFT, rZ << Mw.REGION_SHIFT);
				
				// get region pixels
				Region region = regionManager.getRegion(x + xi, z + zi, 0, dimension);
				int[] regionPixels = region.surfacePixels.getPixels();
				if (regionPixels != null) {
					mergedImage.setRGB(xi, zi, Region.SIZE, Region.SIZE, regionPixels, 0, Region.SIZE);
				}
			}
		}
		
		return mergedImage;
	}
	
	public static boolean writeImage(BufferedImage img, File f) {
		boolean error = true;
		try {
			ImageIO.write(img, "png", f);
			error = false;
		} catch (IOException e) {
		}
		return error;
	}
	
	public static int merge(RegionManager regionManager, int xCentre, int zCentre, int w, int h, int dimension, File dir, String basename) {
		
		// round up to nearest 512 block boundary
		w = ((w + Region.SIZE - 1) & Region.MASK);
		h = ((h + Region.SIZE - 1) & Region.MASK);
		
		// clamp to an integer between 512 and 8192 inclusive
		w = Math.max(Region.SIZE, w);
		h = Math.max(Region.SIZE, h);
		
		int xMin = xCentre - (w / 2);
		int zMin = zCentre - (h / 2);
		
		// round to nearest region boundary
		xMin = Math.round(((float) xMin) / ((float) Region.SIZE)) * Region.SIZE;
		zMin = Math.round(((float) zMin) / ((float) Region.SIZE)) * Region.SIZE;
		
		int xMax = xMin + w;
		int zMax = zMin + h;
		
		RegionManager.logInfo("merging area starting at (%d,%d), %dx%d blocks",
				xMin, zMin, w, h);
		
		
		int countZ = 0;
		int count = 0;
		for (int z = zMin; z < zMax; z += MAX_WIDTH) {
			int imgH = Math.min(zMax - z, MAX_HEIGHT);
			int countX = 0;
			for (int x = xMin; x < xMax; x += MAX_WIDTH) {
				int imgW = Math.min(xMax - x, MAX_WIDTH);
				
				String imgName = String.format("%s.%d.%d.png", basename, countX, countZ);
				File f = new File(dir, imgName);
				RegionManager.logInfo("merging regions to image %s", f);
				
				BufferedImage img = mergeRegions(regionManager, x, z, imgW, imgH, dimension);
				writeImage(img, f);
				
				countX++;
				count++;
			}
			countZ++;
		}
		
		return count;
	}
}
