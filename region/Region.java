package mapwriter.region;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;

/*
	MwRegion class
	Represents a 32x32 chunk area (512x512 blocks).
*/
public class Region {
	
	public RegionManager regionManager;
	
	public static final int SHIFT = 9;
	public static final int SIZE = 1 << SHIFT;
	public static final int MASK = -SIZE;
	public static int maxZoom = 5;
	public static int minZoom = -5;
	
	public final int x;
	public final int z;
	public final int size;
	public final int dimension;
	public final int zoomLevel;
	public final Long key;
	public final File imageFile;
	public final RegionFile regionFile;
	
	public Region nextZoomLevel;
	private boolean cannotLoad = false;
	int updateCount = 0;
	int lastAccessedTick = 0;
	private int refCount = 0;
	private int[] pixels = null;
	
	public Region(RegionManager regionManager, int x, int z, int zoomLevel, int dimension) {
		
		this.regionManager = regionManager;
		
		this.zoomLevel = Math.min(Math.max(0, zoomLevel), Region.maxZoom);
		this.dimension = dimension;
		this.size = Region.SIZE << zoomLevel;
		this.x = x & (-this.size);
		this.z = z & (-this.size);
		
		this.key = getKey(this.x, this.z, this.zoomLevel, this.dimension);
		this.imageFile = getImageFile(regionManager.imageDir, this.x, this.z, this.zoomLevel, this.dimension);
		File regionFileName = getRegionFile(regionManager.worldDir, this.x, this.z, this.dimension);
		this.regionFile = new RegionFile(regionFileName);
		
		if (this.zoomLevel < maxZoom) {
			this.nextZoomLevel = this.regionManager.getRegion(this.x, this.z, this.zoomLevel + 1, this.dimension);
			this.nextZoomLevel.refCount++;
		} else {
			this.nextZoomLevel = null;
		}
		
		this.setAccessed();
		//MwUtil.log("created region %s", this);
	}
	
	public void close() {
		//RegionManager.logInfo("closing region %s", this);
		if (this.needsSaving()) {
			this.saveToImage();
		}
		if (this.regionFile.isOpen()) {
			this.regionFile.close();
		}
		if (this.nextZoomLevel != null) {
			this.nextZoomLevel.refCount--;
			this.nextZoomLevel = null;
		}
		this.pixels = null;
		this.updateCount = 0;
	}
	
	public boolean isLoaded() {
		return (this.pixels != null);
	}
	
	public int getRefCount() {
		return this.refCount;
	}
	
	public int[] allocatePixels() {
		this.pixels = new int[Region.SIZE * Region.SIZE];
		Arrays.fill(pixels, 0xff000000);
		return this.pixels;
	}
	
	public int[] getPixels() {
		this.setAccessed();
		if (this.pixels == null) {
			this.load();
		}
		return this.pixels;
	}
	
	public int[] getOrAllocatePixels() {
		int[] pixels = this.getPixels();
		if (pixels == null) {
			pixels = this.allocatePixels();
		}
		return pixels;
	}
	
	public String toString() {
		return String.format("(%d,%d) z%d dim%d", this.x, this.z, this.zoomLevel, this.dimension);
	}
	
	private static File addDimensionDirToPath(File dir, int dimension) {
		if (dimension != 0) {
			dir = new File(dir, "DIM" + dimension);
		}
		return dir;
	}
	
	public static File getImageFile(File imageDir, int x, int z, int zoomLevel, int dimension) {
		File dimDir = addDimensionDirToPath(imageDir, dimension);
		File zoomDir = new File(dimDir, "z" + zoomLevel);
		
		String filename = String.format("%d.%d.png",
				x >> (Region.SHIFT + zoomLevel),
				z >> (Region.SHIFT + zoomLevel));
		
		return new File(zoomDir, filename);
	}
	
	public static File getRegionFile(File worldDir, int x, int z, int dimension) {
		File dimDir = addDimensionDirToPath(worldDir, dimension);
		File regionDir = new File(dimDir, "region");
		
		String filename = String.format("r.%d.%d.mca",
				x >> Region.SHIFT,
				z >> Region.SHIFT);
		
		return new File(regionDir, filename);
	}
	
	public boolean regionFileExists() {
		return this.regionFile.exists();
	}
	
	public boolean equals(int x, int z, int zoomLevel, int dimension) {
		x &= -this.size;
		z &= -this.size;
		return (this.x == x) && (this.z == z) && (this.zoomLevel == zoomLevel) && (this.dimension == dimension);
	}
	
	@Override
	public boolean equals(Object o) {
		boolean equal = false;
		if (o != null) {
			if (o instanceof Region) {
				Region region = (Region) o;
				equal = this.equals(region.x, region.z, region.zoomLevel, region.dimension);
			}
		}
		return equal;
	}
	
	public static Long getKey(int x, int z, int zoomLevel, int dimension) {
		x = (x >> (Region.SHIFT + zoomLevel)) & 0xffff;
		z = (z >> (Region.SHIFT + zoomLevel)) & 0xffff;
		zoomLevel = zoomLevel & 0xff;
		dimension = dimension & 0xff;
		return Long.valueOf(
				(((long) dimension) << 40) |
				(((long) zoomLevel) << 32) |
				(((long) z) << 16) |
				((long) x));
	}
	
	public int getPixelOffset(int x, int z) {
		return (((z >> this.zoomLevel) & (Region.SIZE - 1)) << Region.SHIFT) +
				((x >> this.zoomLevel) & (Region.SIZE - 1));
	}
	
	public boolean isChunkWithin(MwChunk chunk) {
		int x = (chunk.x << 4) & (-this.size);
		int z = (chunk.z << 4) & (-this.size);
		return (x == this.x) && (z == this.z) && (chunk.dimension == this.dimension);
	}
	
	public boolean isModified() {
		return (this.updateCount > 0);
	}
	
	public void setUpdated() {
		this.updateCount++;
		this.setAccessed();
	}
	
	private void setAccessed() {
		this.lastAccessedTick = this.regionManager.getCurrentTick();
	}
	
	public void setSaved() {
		this.updateCount = 0;
		this.cannotLoad = false;
	}
	
	public boolean needsSaving() {
		// regions at zoom level 0 do not need to be saved
		// (the chunks are saved separately)
		return (this.updateCount > 0) && (this.zoomLevel > 0);
	}
	
	// returns true if chunk not updated
	public boolean updateChunk(MwChunk chunk) {
		boolean updated = false;
		
		if ((this.zoomLevel == 0) && !chunk.isEmpty()) {
			//RegionManager.logInfo("updating chunk %s in region %s", chunk, this);
			int[] pixels = this.getOrAllocatePixels();
			int x = (chunk.x << 4);
			int z = (chunk.z << 4);
			int offset = this.getPixelOffset(x, z);
			ChunkToPixels.getMapPixels(this.regionManager.blockColours, chunk, pixels, offset, Region.SIZE);
			this.updateZoomLevels(x, z, MwChunk.SIZE, MwChunk.SIZE);
			
			this.setUpdated();
			
			updated = true;
		}
		return !updated;
	}
	
	public void updateZoomLevels(int x, int z, int w, int h) {
		Region region = this;
		while (region.nextZoomLevel != null) {
			region.updateNextZoomLevel(x, z, w, h);
			region = region.nextZoomLevel;
		}
	}
	
	public void updateZoomLevels() {
		this.updateZoomLevels(this.x, this.z, this.size, this.size);
	}
	
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
	
	// x, z, w, h, in world block coordinates
	public boolean updateNextZoomLevel(int x, int z, int w, int h) {
		boolean error = true;
		Region dstRegion = this.nextZoomLevel;
		int[] srcPixels = this.getPixels();
		
		if ((dstRegion != null) && (srcPixels != null) && ((this.zoomLevel + 1) == dstRegion.zoomLevel)) {

			int[] dstPixels = dstRegion.getOrAllocatePixels();
			
			int dstW = Math.max(1, (w >> dstRegion.zoomLevel));
			int dstH = Math.max(1, (h >> dstRegion.zoomLevel));
			
			// AND srcX and srcZ by -2 (0xfffffffe) to make sure that
			// they are always even. This prevents out of bounds exceptions
			// at higher zoom levels.
			int srcX = (x >> this.zoomLevel) & (Region.SIZE - 1) & (-2);
			int srcZ = (z >> this.zoomLevel) & (Region.SIZE - 1) & (-2);
			int dstX = (x >> dstRegion.zoomLevel) & (Region.SIZE - 1);
			int dstZ = (z >> dstRegion.zoomLevel) & (Region.SIZE - 1);
			
			for (int j = 0; j < dstH; j++) {
				for (int i = 0; i < dstW; i++) {
					int srcOffset = ((srcZ + (j * 2)) << Region.SHIFT) + (srcX + (i * 2));
					int dstPixel = getAverageOfPixelQuad(srcPixels, srcOffset, Region.SIZE);
					dstPixels[((dstZ + j) << Region.SHIFT) + (dstX + i)] = dstPixel;
				}
			}
			
			dstRegion.setUpdated();
			error = false;
		}
		
		return error;
	}
	
	private void loadFromRegionFile() {
		if (this.regionFileExists()) {
			int[] pixels = this.allocatePixels();
			for (int cz = 0; cz < 32; cz++) {
				for (int cx = 0; cx < 32; cx++) {
					// load chunk from anvil file
					MwChunk chunk = MwChunk.read(cx, cz, this.dimension, this.regionFile);
					if (!chunk.isEmpty()) {
						int offset = ((cz << 4) << Region.SHIFT) + (cx << 4);
						// hopefully accessing the non final field blockColours from a separate
						// thread will be fine. All threads should be shut down before it is ever closed.
						ChunkToPixels.getMapPixels(this.regionManager.blockColours, chunk, pixels, offset, Region.SIZE);
					}
				}
			}
		}
	}
	
	private void loadFromImageFile() {
		BufferedImage img = null;
		try {
			img = ImageIO.read(this.imageFile);
		} catch (IOException e) {
			img = null;
		}
		int[] pixels = null;
		if (img != null) {
			if ((img.getWidth() == Region.SIZE) && (img.getHeight() == Region.SIZE)) {
				pixels = this.allocatePixels();
				img.getRGB(0, 0, Region.SIZE, Region.SIZE,
						pixels, 0, Region.SIZE);
			} else {
				RegionManager.logWarning("MwRegion.load: image '%s' has invalid dimensions (%dx%d)", this.imageFile, img.getWidth(), img.getHeight());
			}
		}
	}
	
	public void reload() {
		this.updateCount = 0;
		
		//RegionManager.logInfo("loading region %s", this);
		if (this.zoomLevel == 0) {
			this.loadFromRegionFile();
		} else {
			this.loadFromImageFile();
		}
		//this.updateZoomLevels(this.x, this.z, this.size, this.size);
	}
	
	private void load() {
		// all updates will be overwritten
		if (!this.cannotLoad) {
			this.reload();
		}
		
		// this should be the only place that cannotLoad can be set to true.
		// it will be set to false on any update.
		this.cannotLoad = (this.pixels == null);
	}
	
	public void saveToImage() {
		int[] pixels = this.getPixels();
		if (pixels != null) {
			BufferedImage img = new BufferedImage(Region.SIZE, Region.SIZE, BufferedImage.TYPE_INT_RGB);
			img.setRGB(0, 0, Region.SIZE, Region.SIZE,
				pixels, 0, Region.SIZE);
			
			// write the given image to the image file
			File dir = this.imageFile.getParentFile();
			if (!dir.exists()) {
				dir.mkdirs();
			}
			
			try {
				//MwUtil.log("writing region %s to %s", this, this.imageFile);
				ImageIO.write(img, "png", this.imageFile);
			} catch (IOException e) {
				RegionManager.logError("MwRegion.writeImage: error: could not write image to %s", this.imageFile.getName());
			}
		}
		this.setSaved();
	}
}
