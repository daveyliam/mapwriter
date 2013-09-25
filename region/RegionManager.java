package mapwriter.region;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Logger;

public class RegionManager {
	private final HashMap<Long, Region> regionMap;
	
	public final File worldDir;
	public final File imageDir;
	public BlockColours blockColours;
	public static Logger logger;
	public final static int maxLoadedRegions = 128;
	
	//private int regionArraySize;
	//private Region[] regionArray;
	private int currentPruneTick = 0;
	
	public static void logInfo(String s, Object...args) {
		if (logger != null) {
			logger.info(String.format(s, args));
		}
	}
	
	public static void logWarning(String s, Object...args) {
		if (logger != null) {
			logger.warning(String.format(s, args));
		}
	}
	
	public static void logError(String s, Object...args) {
		if (logger != null) {
			logger.severe(String.format(s, args));
		}
	}
	
	public RegionManager(File worldDir, File imageDir, BlockColours blockColours) {
		this.worldDir = worldDir;
		this.imageDir = imageDir;
		this.blockColours = blockColours;
		this.regionMap = new HashMap<Long, Region>();
	}
	
	public void close() {
		for (Region region : this.regionMap.values()) {
			if (region != null) {
				region.close();
			}
		}
		this.regionMap.clear();
	}
	
	public void saveUpdatedRegions() {
		for (Region region : this.regionMap.values()) {
	        if ((region != null) && region.needsSaving()) {
				region.saveToImage();
			}
	    }
	}
	
	public void unloadRegion(Region region) {
		this.regionMap.remove(region.key);
		region.close();
	}
	
	public Region getLeastAccessedRegion() {
		int minLastAccessedTick = this.currentPruneTick;
		Region leastAccessedRegion = null;
		for (Region region : this.regionMap.values()) {
	        if ((region != null) && (region.getRefCount() <= 0) && (region.lastAccessedTick < minLastAccessedTick)) {
				minLastAccessedTick = region.lastAccessedTick;
				leastAccessedRegion = region;
			}
	    }
		return leastAccessedRegion;
	}
	
	public int pruneRegions() {
		int count = 0;
		boolean error = false;
		while ((!error) && (this.regionMap.size() > maxLoadedRegions)) {
			Region region = this.getLeastAccessedRegion();
			if (region != null) {
				this.unloadRegion(region);
				count++;
			} else {
				error = true;
			}
		}
		//if (count > 0) {
		//	RegionManager.logInfo("%d unused regions closed", count);
		//}
		if (this.regionMap.size() > maxLoadedRegions) {
			RegionManager.logWarning("unable to close enough regions (%d regions loaded, limit is %d)", this.regionMap.size(), maxLoadedRegions);
		}
		this.currentPruneTick++;
		return count;
	}
	
	public int getCurrentTick() {
		return this.currentPruneTick;
	}
	
	public File getDimensionDir(int dimension) {
		File dimDir;
		if (dimension != 0) {
			dimDir = new File(this.worldDir, "DIM" + dimension);
		} else {
			dimDir = this.worldDir;
		}
		return dimDir;
	}
	
	// must not return null
	public Region getRegion(int x, int z, int zoomLevel, int dimension) {
		Region region = this.regionMap.get(Region.getKey(x, z, zoomLevel, dimension));
		if (region == null) {
			// add region
			region = new Region(this, x, z, zoomLevel, dimension);
			this.regionMap.put(region.key, region);
		}
		return region;
	}
	
	public void updateChunk(MwChunk chunk) {	
		Region region = this.getRegion(chunk.x << 4, chunk.z << 4, 0, chunk.dimension);
		region.updateChunk(chunk);
	}
	
	public void reloadRegions(int xStart, int zStart, int w, int h, int dimension) {
		// read all zoom level 0 regions
		// then find all regions with a backing image at zoom level 0
		
		xStart &= Region.MASK;
		zStart &= Region.MASK;
		w = (w + Region.SIZE) & Region.MASK;
		h = (h + Region.SIZE) & Region.MASK;
		
		logInfo("reloading regions from (%d, %d) to (%d, %d)", xStart, zStart, xStart + w, zStart + h);
		
		for (int rX = xStart; rX < (xStart + w); rX += Region.SIZE) {
			for (int rZ = zStart; rZ < (zStart + h); rZ += Region.SIZE) {
				Region region = this.getRegion(rX, rZ, 0, dimension);
				boolean regionAlreadyLoaded = region.isLoaded();
				region.reload();
				region.updateZoomLevels();
				if (!regionAlreadyLoaded) {
					this.unloadRegion(region);
				}
			}
		}
	}
	
	public void saveChunkArray(MwChunk[] chunkArray) {
		for (MwChunk chunk : chunkArray) {
			if (chunk != null) {
				Region region = this.getRegion(chunk.x << 4, chunk.z << 4, 0, chunk.dimension);
				chunk.write(region.regionFile);
			}
		}
	}
}
