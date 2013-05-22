package mapwriter.region;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;

import mapwriter.Mw;
import mapwriter.MwUtil;
import mapwriter.Task;

// this task generates a merged PNG map image directly from saved chunk data.

public class MergeTask extends Task {
	public static final int MAX_WIDTH = 8192;
	public static final int MAX_HEIGHT = 8192;
	private int dimension;
	private int rXMin;
	private int rZMin;
	private int rXMax;
	private int rZMax;
	private String errorMsg = null;
	private RegionManager regionManager;
	private File outputFile;
	
	public MergeTask(RegionManager regionManager, File outputFile, int dimension, int xCentre, int zCentre, int w, int h) {
		this.regionManager = regionManager;
		this.outputFile = outputFile;
		this.dimension = dimension;
		
		w = Math.min(Math.max(0, w), MAX_WIDTH - Mw.REGION_SIZE);
		h = Math.min(Math.max(0, h), MAX_HEIGHT - Mw.REGION_SIZE);
		
		this.rXMin = (xCentre - (w / 2)) >> Mw.REGION_SHIFT;
		this.rZMin = (zCentre - (h / 2)) >> Mw.REGION_SHIFT;
		this.rXMax = (xCentre + (w / 2)) >> Mw.REGION_SHIFT;
		this.rZMax = (zCentre + (h / 2)) >> Mw.REGION_SHIFT;
	}
	
	public boolean writeImage(BufferedImage img, File imageFile) {
		boolean error = true;
		try {
			ImageIO.write(img, "png", imageFile);
			error = false;
		} catch (IOException e) {
			MwUtil.log("error: could not write image to %s", imageFile.getName());
		}
		return error;
	}
	
	@Override
	public void run() {
		
		boolean error = false;
		
		if ((this.rXMax < this.rXMin) || (this.rZMax < this.rZMin)) {
			this.errorMsg = "error: xMax is less than xMin or zMax is less than zMin";
			error = true;
		}
		
		int w = (this.rXMax - this.rXMin + 1) << Mw.REGION_SHIFT;
		int h = (this.rZMax - this.rZMin + 1) << Mw.REGION_SHIFT;
		
		if ((w > MAX_WIDTH) || (h > MAX_HEIGHT)) {
			this.errorMsg = String.format("error: area specified is too large (%dx%d pixels)", w, h);
			error = true;
		}
		
		if (!error) {
			
			MwUtil.log("merging area (%d,%d) -> (%d,%d)",
					rXMin << Mw.REGION_SHIFT, rZMin << Mw.REGION_SHIFT,
					rXMax << Mw.REGION_SHIFT, rZMax << Mw.REGION_SHIFT);
			
			int pixels[] = new int[Mw.REGION_SIZE * Mw.REGION_SIZE];
			
			// create the image and graphics context
			// this is the most likely place to run out of memory
			BufferedImage mergedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			
			// copy region PNGs to the image
			for (int rZ = this.rZMin; rZ <= this.rZMax; rZ++) {
				for (int rX = this.rXMin; rX <= this.rXMax; rX++) {
					//MwUtil.log("merging region (%d,%d)", rX << Mw.REGION_SHIFT, rZ << Mw.REGION_SHIFT);
					Arrays.fill(pixels, 0);
					int czStart = rZ << 5;
					int cxStart = rX << 5;
					if (MwChunk.regionFileExists(cxStart, czStart, this.dimension, this.regionManager.worldDir)) {	
						for (int cz = 0; cz < 32; cz++) {
							for (int cx = 0; cx < 32; cx++) {
								MwChunk chunk = MwChunk.read(cxStart + cx, czStart + cz, this.dimension, this.regionManager.worldDir);
								if (!chunk.isEmpty()) {
									int offset = ((cz << 4) * Mw.REGION_SIZE) + (cx << 4);
									ChunkToPixels.getMapPixels(this.regionManager.blockColours, chunk, pixels, offset, Mw.REGION_SIZE);
								}
							}
						}
					}
					
					for (int i = 0; i < pixels.length; i++) {
						pixels[i] |= 0xff000000;
					}
					
					int dstX = (rX - this.rXMin) << Mw.REGION_SHIFT;
					int dstZ = (rZ - this.rZMin) << Mw.REGION_SHIFT;
					mergedImage.setRGB(dstX, dstZ, Mw.REGION_SIZE, Mw.REGION_SIZE, pixels, 0, Mw.REGION_SIZE);
				}
			}
			
			MwUtil.log("writing merged image to '%s'", this.outputFile.getAbsolutePath());
			
			// write the image to disk
			if (!this.writeImage(mergedImage, this.outputFile)) {
				MwUtil.log("done merging");
				this.errorMsg = "merge successful";
			} else {
				this.errorMsg = "error: failed to write merged image";
			}
		}
	}

	@Override
	public void onComplete() {
		if (this.errorMsg != null) {
			MwUtil.printBoth(this.errorMsg);
		}
	}
}