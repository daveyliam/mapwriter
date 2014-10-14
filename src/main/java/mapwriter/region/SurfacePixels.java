package mapwriter.region;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;

public class SurfacePixels {
	
	protected Region region;
	protected File filename;
	protected int[] pixels = null;
	protected boolean cannotLoad = false;
	protected int updateCount = 0;
	
	public SurfacePixels(Region region, File filename) {
		this.region = region;
		this.filename = filename;
	}
	
	public void clear() {
		if (this.pixels != null) {
			Arrays.fill(this.pixels, 0);
		}
	}
	
	public void close() {
		if (this.updateCount > 0) {
			this.save();
		}
		this.pixels = null;
	}
	
	private void save() {
		if (this.pixels != null) {
			saveImage(this.filename, this.pixels, Region.SIZE, Region.SIZE);
			this.cannotLoad = false;
		}
		this.updateCount = 0;
	}
	
	private void load() {
		if (!this.cannotLoad) {
			this.pixels = loadImage(this.filename, Region.SIZE, Region.SIZE);
			if (this.pixels != null) {
				// set opaque black pixels to transparent so that
				// background texture shows
				for (int i = 0; i < this.pixels.length; i++) {
					int colour = this.pixels[i];
					if (colour == 0xff000000) {
						this.pixels[i] = 0;
					}
				}
			} else {
				this.cannotLoad = true;
			}
			this.updateCount = 0;
		}
	}
	
	public int[] getPixels() {
		if (this.pixels == null) {
			this.load();
		}
		return this.pixels;
	}
	
	public int[] getOrAllocatePixels() {
		this.getPixels();
		if (this.pixels == null) {
			this.pixels = new int[Region.SIZE * Region.SIZE];
			this.clear();
		}
		return this.pixels;
	}
	
	public void updateChunk(MwChunk chunk) {
		int x = (chunk.x << 4);
		int z = (chunk.z << 4);
		int offset = this.region.getPixelOffset(x, z);
		int[] pixels = this.getOrAllocatePixels();
		// TODO: refactor so that blockColours can be accessed
		// more directly
		ChunkRender.renderSurface(
			this.region.regionManager.blockColours,
			chunk, pixels, offset, Region.SIZE,
			(chunk.dimension == -1) // use ceiling algorithm for nether
		);
		this.region.updateZoomLevels(x, z, MwChunk.SIZE, MwChunk.SIZE);
		this.updateCount++;
	}
	
	// get the averaged colour of a 2x2 pixel area in the given pixels
	public static int getAverageOfPixelQuad(int[] pixels, int offset, int scanSize) {
		int p00 = pixels[offset];
		int p01 = pixels[offset + 1];
		int p10 = pixels[offset + scanSize];
		int p11 = pixels[offset + scanSize + 1];
		
		// ignore alpha channel
		int r = ((p00 >> 16) & 0xff) + ((p01 >> 16) & 0xff) + ((p10 >> 16) & 0xff) + ((p11 >> 16) & 0xff);
		r >>= 2;
		int g = ((p00 >>  8) & 0xff) + ((p01 >>  8) & 0xff) + ((p10 >>  8) & 0xff) + ((p11 >>  8) & 0xff);
		g >>= 2;
		int b =  (p00        & 0xff) +  (p01        & 0xff) + (p10         & 0xff) +  (p11        & 0xff);
		b >>= 2;
		return 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
	}
	
	// update an area of pixels in this region from an area of pixels in srcPixels,
	// scaling the pixels by 50%.
	public void updateScaled(int[] srcPixels, int srcX, int srcZ, int dstX, int dstZ, int dstW, int dstH) {
		int[] dstPixels = this.getOrAllocatePixels();
		for (int j = 0; j < dstH; j++) {
			for (int i = 0; i < dstW; i++) {
				int srcOffset = ((srcZ + (j * 2)) << Region.SHIFT) + (srcX + (i * 2));
				int dstPixel = getAverageOfPixelQuad(srcPixels, srcOffset, Region.SIZE);
				dstPixels[((dstZ + j) << Region.SHIFT) + (dstX + i)] = dstPixel;
			}
		}
		this.updateCount++;
	}
	
	public static void saveImage(File filename, int[] pixels, int w, int h) {
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		img.setRGB(0, 0, w, h, pixels, 0, w);
		
		try {
			//MwUtil.log("writing region %s to %s", this, this.imageFile);
			ImageIO.write(img, "png", filename);
		} catch (IOException e) {
			RegionManager.logError("saveImage: error: could not write image to %s", filename);
		}
	}
	
	public static int[] loadImage(File filename, int w, int h) {
		BufferedImage img = null;
		try {
			img = ImageIO.read(filename);
		} catch (IOException e) {
			img = null;
		}
		int[] pixels = null;
		if (img != null) {
			if ((img.getWidth() == w) && (img.getHeight() == h)) {
				pixels = new int[w * h];
				img.getRGB(0, 0, w, h, pixels, 0, w);
			} else {
				RegionManager.logWarning(
						"loadImage: image '%s' does not match expected dimensions (got %dx%d expected %dx%d)",
						filename, img.getWidth(), img.getHeight(), w, h
				);
			}
		}
		return pixels;
	}
}
