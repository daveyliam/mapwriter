package mapwriter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import mapwriter.region.MergeToImage;
import mapwriter.region.RegionManager;

public class MergeTask extends Task {
	
	final RegionManager regionManager;
	final File outputFile;
	final int x, z, w, h, dimension;
	String msg;
	
	public MergeTask(RegionManager regionManager, int x, int z, int w, int h, int dimension, File outputFile) {
		this.regionManager = regionManager;
		this.x = x;
		this.z = z;
		this.w = w;
		this.h = h;
		this.dimension = dimension;
		this.outputFile = outputFile;
	}
	
	@Override
	public void run() {
		BufferedImage mergedImage = MergeToImage.merge(this.regionManager, this.x, this.z, this.w, this.h, this.dimension);
		if (mergedImage != null) {
			try {
				//MwUtil.log("writing region %s to %s", this, this.imageFile);
				ImageIO.write(mergedImage, "png", this.outputFile);
				this.msg = String.format("successfully wrote merged image to %s", this.outputFile);
			} catch (IOException e) {
				this.msg = String.format("MergeTask: error: could not write image to %s", this.outputFile);
			}
		}
	}
	
	@Override
	public void onComplete() {
		MwUtil.printBoth(this.msg);
	}

}
