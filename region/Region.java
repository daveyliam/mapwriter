package mapwriter.region;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Arrays;

import javax.imageio.ImageIO;

import mapwriter.Mw;
import mapwriter.MwUtil;
import mapwriter.Render;
import mapwriter.Task;
import mapwriter.map.MapTexture;

/*
	MwRegion class
	Represents a 32x32 chunk area (512x512 blocks).
*/
public class Region {
	public final RegionManager regionManager;
	
	public final int x;
	public final int z;
	public final int size;
	public final int dimension;
	public final int zoomLevel;
	public final int index;
	public final Long key;
	public final File imageFile;
	public final Region nextZoomLevel;
	
	public boolean cannotLoad = false;
	public int updateCount = 0;
	public int lastUpdated = 0;
	public int[] chunkSums = new int[32 * 32];
	public SoftReference<int[]> pixelsSoftRef = new SoftReference<int[]>(null);
	public int[] pixelsHardRef = null;
	
	public Region(RegionManager regionManager, int x, int z, int zoomLevel, int dimension, Region nextZoomLevel) {
		this.regionManager = regionManager;
		this.zoomLevel = Math.min(Math.max(0, zoomLevel), Mw.maxZoom);
		this.dimension = dimension;
		this.size = Mw.REGION_SIZE << zoomLevel;
		this.x = x & (~(this.size - 1));
		this.z = z & (~(this.size - 1));
		
		this.index = MapTexture.getRegionIndex(this.x, this.z, this.zoomLevel);
		this.key = this.getKey(this.x, this.z, this.zoomLevel, this.dimension);
		this.imageFile = this.getImageFile(regionManager.imageDir);
		this.nextZoomLevel = nextZoomLevel;
		
		//MwUtil.log("created region %s", this);
	}
	
	public void close() {
		//MwUtil.log("region %s closed", this);
		this.pixelsSoftRef = null;
		this.pixelsHardRef = null;
		this.updateCount = 0;
		this.lastUpdated = 0;
		this.chunkSums = null;
	}
	
	public int[] allocatePixels() {
		int[] pixels = new int[Mw.REGION_SIZE * Mw.REGION_SIZE];
		Arrays.fill(pixels, 0xff000000);
		this.pixelsSoftRef = new SoftReference<int[]>(pixels);
		return pixels;
	}
	
	public int[] getPixels() {
		return this.pixelsSoftRef.get();
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
	
	private File getImageFile(File imageDir) {
		String filename = String.format("%d.%d.png",
				this.x >> (Mw.REGION_SHIFT + this.zoomLevel),
				this.z >> (Mw.REGION_SHIFT + this.zoomLevel));
		
		File file = imageDir;
		if (this.dimension != 0) {
			file = new File(file, "DIM" + this.dimension);
		}
		file = new File(file, "z" + this.zoomLevel);
		
		return new File(file, filename);
	}
	
	public boolean equals(int x, int z, int zoomLevel, int dimension) {
		int mask = (Mw.REGION_SIZE << zoomLevel) - 1;
		x &= ~mask;
		z &= ~mask;
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
		x = (x >> (Mw.REGION_SHIFT + zoomLevel)) & 0xffff;
		z = (z >> (Mw.REGION_SHIFT + zoomLevel)) & 0xffff;
		zoomLevel = zoomLevel & 0xff;
		dimension = dimension & 0xff;
		return Long.valueOf(
				(((long) dimension) << 40) |
				(((long) zoomLevel) << 32) |
				(((long) z) << 16) |
				((long) x));
	}
	
	// returns the parent region at the specified zoom level.
	// can return null.
	public Region getParentRegionAtZoomLevel(int zoomLevel) {
		Region region = this;
		while ((region != null) && (region.zoomLevel != zoomLevel)) {
			region = region.nextZoomLevel;
		}
		return region;
	}
	
	public int getPixelOffset(int x, int z) {
		return (((z >> this.zoomLevel) & (Mw.REGION_SIZE - 1)) << Mw.REGION_SHIFT) +
				((x >> this.zoomLevel) & (Mw.REGION_SIZE - 1));
	}
	
	public boolean isModified() {
		return (this.updateCount > 0);
	}
	
	public void setUpdated() {
		int[] pixels = this.getPixels();
		if (pixels != null) {
			// upgrade to hard reference on update so that the updates
			// aren't garbage collected.
			this.pixelsHardRef = pixels;
		}
		this.updateCount++;
		this.lastUpdated = 0;
	}
	
	public void setSaved() {
		this.updateCount = 0;
		this.pixelsHardRef = null;
		this.cannotLoad = false;
	}
	
	public boolean needsSaving() {
		this.lastUpdated++;
		return (this.updateCount > 0) && (this.lastUpdated > 5);
	}
	
	// returns true if chunk not updated
	public boolean updateChunk(MwChunk chunk, boolean force) {
		boolean updated = false;
		
		if ((this.zoomLevel == 0) && !chunk.isEmpty()) {
			//MwUtil.log("updating chunk (%d, %d) region (%d, %d, %d)", chunk.x, chunk.z, this.x, this.z, this.zoomLevel);
			this.load();
			int chunkSum = chunk.getCheckSum();
			int chunkSumIndex = ((chunk.z & 0x1f) << 5) + (chunk.x & 0x1f);
			
			if (force || (chunkSum != this.chunkSums[chunkSumIndex])) {
				//MwUtil.log("updating chunk (%d, %d) chunkSum = %d, chunkSum[%d, %d] = %d",
				//	chunk.x, chunk.z, chunkSum, chunk.z & 31, chunk.x & 31, this.chunkSums[chunkSumIndex]);
				
				int[] pixels = this.getOrAllocatePixels();
				int x = (chunk.x << 4);
				int z = (chunk.z << 4);
				int offset = this.getPixelOffset(x, z);
				ChunkToPixels.getMapPixels(this.regionManager.blockColours, chunk, pixels, offset, Mw.REGION_SIZE);
				this.updateZoomLevels(x, z, Mw.CHUNK_SIZE, Mw.CHUNK_SIZE);
				
				this.chunkSums[chunkSumIndex] = chunkSum;
				this.setUpdated();
				
				updated = true;
			}
		}
		return !updated;
	}
	
	private void updateZoomLevels(int x, int z, int w, int h) {
		Region region = this;
		while (region.nextZoomLevel != null) {
			region.load();
			region.updateNextZoomLevel(x, z, w, h);
			region = region.nextZoomLevel;
		}
	}
	
	// x, z, w, h, in world block coordinates
	public boolean updateNextZoomLevel(int x, int z, int w, int h) {
		boolean error = true;
		
		Region dstRegion = this.nextZoomLevel;
		int[] srcPixels = this.getPixels();
		
		if ((dstRegion != null) && (srcPixels != null) && ((this.zoomLevel + 1) == dstRegion.zoomLevel)) {
			
			dstRegion.load();
			int[] dstPixels = dstRegion.getOrAllocatePixels();
			
			int dstW = Math.max(1, (w >> dstRegion.zoomLevel));
			int dstH = Math.max(1, (h >> dstRegion.zoomLevel));
			
			// AND srcX and srcZ by -2 (0xfffffffe) to make sure that
			// they are always even. This prevents out of bounds exceptions
			// at higher zoom levels.
			int srcX = (x >> this.zoomLevel) & (Mw.REGION_SIZE - 1) & (-2);
			int srcZ = (z >> this.zoomLevel) & (Mw.REGION_SIZE - 1) & (-2);
			int dstX = (x >> dstRegion.zoomLevel) & (Mw.REGION_SIZE - 1);
			int dstZ = (z >> dstRegion.zoomLevel) & (Mw.REGION_SIZE - 1);
			
			for (int j = 0; j < dstH; j++) {
				for (int i = 0; i < dstW; i++) {
					int srcOffset = ((srcZ + (j * 2)) << Mw.REGION_SHIFT) + (srcX + (i * 2));
					int dstPixel = Render.getAverageOfPixelQuad(srcPixels, srcOffset, Mw.REGION_SIZE);
					dstPixels[((dstZ + j) << Mw.REGION_SHIFT) + (dstX + i)] = dstPixel;
				}
			}
			
			dstRegion.setUpdated();
			error = false;
		}
		
		return error;
	}
	
	private void loadFromRegionFile() {
		int cxStart = (this.x >> 4);
		int czStart = (this.z >> 4);
		
		boolean regionExists = MwChunk.regionFileExists(cxStart, czStart, this.dimension, this.regionManager.worldDir);
		if (regionExists) {
			int[] pixels = this.allocatePixels();
			for (int cz = 0; cz < 32; cz++) {
				for (int cx = 0; cx < 32; cx++) {
					// load chunk from anvil file
					MwChunk chunk = MwChunk.read(cxStart + cx, czStart + cz, this.dimension, this.regionManager.worldDir);
					if (!chunk.isEmpty()) {
						int offset = ((cz << 4) << Mw.REGION_SHIFT) + (cx << 4);
						ChunkToPixels.getMapPixels(this.regionManager.blockColours, chunk, pixels, offset, Mw.REGION_SIZE);
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
			if ((img.getWidth() == Mw.REGION_SIZE) && (img.getHeight() == Mw.REGION_SIZE)) {
				pixels = this.allocatePixels();
				img.getRGB(0, 0, Mw.REGION_SIZE, Mw.REGION_SIZE,
						pixels, 0, Mw.REGION_SIZE);
			} else {
				MwUtil.log("error: MwRegion.load: image '%s' has invalid dimensions (%dx%d)", this.imageFile, img.getWidth(), img.getHeight());
			}
		}
	}
	
	public void load() {
		int[] pixels = this.getPixels();
		if (!this.cannotLoad && (pixels == null)) {
			this.updateCount = 0;
			this.lastUpdated = 0;
			this.chunkSums = new int[32 * 32];
			
			//MwUtil.log("loading region %s", this);
			if (this.zoomLevel == 0) {
				this.loadFromRegionFile();
			} else {
				this.loadFromImageFile();
			}
			//this.updateZoomLevels(this.x, this.z, this.size, this.size);
			// this should be the only place that empty can be set to true.
			// it will be set to false on any update.
			this.cannotLoad = (this.getPixels() == null);
		}
	}
	
	public void save() {
		int[] pixels = this.getPixels();
		if ((pixels != null) && (this.zoomLevel > 0)) {
			BufferedImage img = new BufferedImage(Mw.REGION_SIZE, Mw.REGION_SIZE, BufferedImage.TYPE_INT_ARGB);
			img.setRGB(0, 0, Mw.REGION_SIZE, Mw.REGION_SIZE,
				pixels, 0, Mw.REGION_SIZE);
			
			// write the given image to the image file
			File dir = this.imageFile.getParentFile();
			if (!dir.exists()) {
				dir.mkdirs();
			}
			
			try {
				//MwUtil.log("writing region %s to %s", this, this.imageFile);
				ImageIO.write(img, "png", this.imageFile);
			} catch (IOException e) {
				MwUtil.log("MwRegion.writeImage: error: could not write image to %s", this.imageFile.getName());
			}
		}
		this.setSaved();
	}
	
	class LoadTask extends Task {
		private final Region region;
		private final MapTexture mapTexture;
		private final boolean updateZoomLevels;
		
		// read from image file when given img is null.
		// otherwise write the given image to 
		public LoadTask(Region region, MapTexture mapTexture, boolean updateZoomLevels) {
			this.region = region;
			this.mapTexture = mapTexture;
			this.updateZoomLevels = updateZoomLevels;
		}
		
		public void run() {
			//MwUtil.log("Region.LoadTask: region %s", region);
			this.region.load();
			if (this.updateZoomLevels) {
				this.region.updateZoomLevels(this.region.x, this.region.z, this.region.size, this.region.size);
			}
			if (this.mapTexture != null) {
				this.mapTexture.updateFromRegion(this.region, this.region.x, this.region.z, this.region.size, this.region.size);
			}
		}
		
		public void onComplete() {
		}
	}
	
	class SaveTask extends Task {
		private final Region region;
		private final boolean close;
		
		public SaveTask(Region region, boolean close) {
			this.region = region;
			this.close = close;
		}
		
		public void run() {
			if (this.region.isModified()) {
				this.region.save();
			}
			if (this.close) {
				this.region.close();
			}
		}
		
		public void onComplete() {
		}
	}
	
	class UpdateChunkTask extends Task {
		
		private final Region baseRegion;
		private final Region textureRegion;
		private final MapTexture mapTexture;
		private final MwChunk chunk;
		private final boolean forceUpdate;
		
		public UpdateChunkTask(MwChunk chunk, Region baseRegion, Region textureRegion, MapTexture mapTexture, boolean forceUpdate) {
			this.baseRegion = baseRegion;
			this.textureRegion = textureRegion;
			this.mapTexture = mapTexture;
			this.chunk = chunk;
			this.forceUpdate = forceUpdate;
		}
		
		@Override
		public void run() {
			this.baseRegion.load();
			this.baseRegion.updateChunk(this.chunk, this.forceUpdate);
			if ((this.textureRegion != null) && (this.mapTexture != null)) {
				this.mapTexture.updateFromRegion(
						this.textureRegion, this.chunk.x << 4, this.chunk.z << 4, Mw.CHUNK_SIZE, Mw.CHUNK_SIZE);
			}
		}
		
		@Override
		public void onComplete() {
		}
	}
	
	public void addLoadTask(MapTexture mapTexture) {
		this.regionManager.executor.addTask(new LoadTask(this, mapTexture, false));
	}
	
	public void addLoadAndUpdateZoomLevelsTask(MapTexture mapTexture) {
		this.regionManager.executor.addTask(new LoadTask(this, mapTexture, true));
	}
	
	public void addSaveTask() {
		this.regionManager.executor.addTask(new SaveTask(this, false));
	}
	
	public void addCloseTask() {
		this.regionManager.executor.addTask(new SaveTask(this, true));
	}
	
	public void addUpdateChunkTask(MwChunk mwChunk, Region textureRegion, MapTexture mapTexture, boolean forceUpdate) {
		this.regionManager.executor.addTask(new UpdateChunkTask(mwChunk, this, textureRegion, mapTexture, forceUpdate));
	}
}
