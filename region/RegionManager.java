package mapwriter.region;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import mapwriter.BackgroundExecutor;
import mapwriter.Mw;
import mapwriter.MwUtil;
import mapwriter.Task;
import mapwriter.map.MapTexture;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.RegionFileCache;

public class RegionManager {
	public final BlockColours blockColours;
	public final MapTexture mapTexture;
	public final BackgroundExecutor executor;
	public final File imageDir;
	public final File worldDir;
	private final boolean enableChunkSaving;
	
	HashMap<Long, Region> regionMap = new HashMap<Long, Region>();
	
	private HashSet<Chunk> chunkSet;
	private boolean chunkUpdateReady = false;
	private int chunkUpdateXIndex = 0;
	private int chunkUpdateZIndex = 0;
	private int chunkUpdateX = 0;
	private int chunkUpdateZ = 0;
	
	public RegionManager(File worldDir, File imageDir, BackgroundExecutor executor, MapTexture mapTexture, BlockColours blockColours, boolean enableChunkSaving) {
		this.executor = executor;
		this.mapTexture = mapTexture;
		this.blockColours = blockColours;
		this.imageDir = imageDir;
		this.worldDir = worldDir;
		this.chunkSet = new HashSet<Chunk>();
		this.enableChunkSaving = enableChunkSaving;
	}
	
	public void close() {
		for (Region region : this.regionMap.values()) {
			if (region != null) {
				region.addCloseTask();
			}
		}
		this.saveChunks();
		this.chunkSet.clear();
	}
	
	// must not return null
	public Region getRegion(int x, int z, int zoomLevel, int dimension) {
		Region region = this.regionMap.get(Region.getKey(x, z, zoomLevel, dimension));
		if (region == null) {
			// add region
			Region nextZoomLevel = null;
			if (zoomLevel + 1 <= Mw.maxZoom) {
				nextZoomLevel = this.getRegion(x, z, zoomLevel + 1, dimension);
			}
			region = new Region(this, x, z, zoomLevel, dimension, nextZoomLevel);
			this.regionMap.put(region.key, region);
		}
		return region;
	}
	
	public void addChunk(Chunk chunk) {
		if (this.enableChunkSaving) {
			this.chunkSet.add(chunk);
		}
	}
	
	public void removeChunk(Chunk chunk) {
		if (this.enableChunkSaving) {
			if (this.chunkSet.remove(chunk)) {
				this.addSaveChunkTask(chunk);
			}
		}
	}
	
	public void saveChunks() {
		if (this.enableChunkSaving) {
			for (Chunk chunk : this.chunkSet) {
				this.addSaveChunkTask(chunk);
			}
			RegionFileCache.clearRegionFileReferences();
			this.chunkSet.clear();
		}
	}
	
	private Chunk getNextUpdateChunk(World world, int playerX, int playerZ) {
		Chunk chunk = world.getChunkFromChunkCoords(
				this.chunkUpdateX + this.chunkUpdateXIndex,
				this.chunkUpdateZ + this.chunkUpdateZIndex);
		
		this.chunkUpdateXIndex++;
		if (this.chunkUpdateXIndex >= 32) {
			this.chunkUpdateXIndex = 0;
			this.chunkUpdateZIndex++;
			if (this.chunkUpdateZIndex >= 32) {
				this.resetChunkUpdate(playerX, playerZ);
			}
		}
		return chunk;
	}
	
	private void resetChunkUpdate(int playerX, int playerZ) {
		int rS = Mw.REGION_SIZE;
		int halfRS = (rS >> 1);
		this.chunkUpdateX = (playerX >> 4) - 16;
		this.chunkUpdateZ = (playerZ >> 4) - 16;
		this.chunkUpdateXIndex = 0;
		this.chunkUpdateZIndex = 0;
	}
	
	public void onTick(Mw mw) {
		if (!this.chunkUpdateReady) {
			this.resetChunkUpdate(mw.playerXInt, mw.playerZInt);
			this.chunkUpdateReady = true;
		}
		
		int attempts = 20;
		int chunksToUpdate = mw.chunksPerTick;
		while ((attempts-- > 0) && (chunksToUpdate > 0)) {
			Chunk chunk = this.getNextUpdateChunk(mw.mc.theWorld, mw.playerXInt, mw.playerZInt);
			if (!this.addUpdateChunkTask(chunk, mw.playerXInt, mw.playerZInt)) {
				chunksToUpdate--;
			}
		}
		
		if ((mw.tickCounter & 0xff) == 0xff) {
			this.closeInactiveRegions();
			//MwUtil.log("%d regions in map", this.regionMap.size());
		}
	}
	
	private boolean chunkSurroundedAndNotEmpty(Chunk chunk) {
		boolean surrounded = false;
		if ((chunk != null) && !chunk.isEmpty()) {
			int cx = chunk.xPosition;
			int cz = chunk.zPosition;
			surrounded = !chunk.worldObj.getChunkFromChunkCoords(cx - 1, cz).isEmpty() &&
					!chunk.worldObj.getChunkFromChunkCoords(cx + 1, cz).isEmpty() &&
					!chunk.worldObj.getChunkFromChunkCoords(cx, cz - 1).isEmpty() &&
					!chunk.worldObj.getChunkFromChunkCoords(cx, cz + 1).isEmpty();
		}
		return surrounded;
	}
	
	private Region getRegionAtTextureZoomLevel(Region region) {
		Region textureRegion = null;
		while ((region != null) && (textureRegion == null)) {
			if (this.mapTexture.isRegionInTexture(region)) {
				textureRegion = region;
			}
			region = region.nextZoomLevel;
		}
		return textureRegion;
	}
	
	private boolean addUpdateChunkTask(Chunk chunk, int playerX, int playerZ) {
		boolean taskAdded = false;
		if (this.chunkSurroundedAndNotEmpty(chunk)) {
			
			MwChunk mwchunk = MwChunk.copyFromChunk(chunk);
			
			int dx = ((mwchunk.x << 4) + 8) - playerX;
			int dz = ((mwchunk.z << 4) + 8) - playerZ;
			int distSquared = (dx * dx) + (dz * dz);
			
			Region region = this.getRegion(mwchunk.x << 4, mwchunk.z << 4, 0, mwchunk.dimension);
			Region textureRegion = this.getRegionAtTextureZoomLevel(region);
			region.addUpdateChunkTask(mwchunk, textureRegion, this.mapTexture, (distSquared <= 160));
			taskAdded = true;
		}
		return !taskAdded;
	}
	
	private void closeInactiveRegions() {
		Iterator it = this.regionMap.values().iterator();
	    while (it.hasNext()) {
	        Region region = (Region) it.next();
	        if ((region != null) && (region.needsSaving())) {
				//MwUtil.log("closing region %s", region);
				region.addSaveTask();
			}
	    }
	}
	
	public void reloadRegions(int xStart, int zStart, int w, int h, int dimension) {
		// read all zoom level 0 regions
		// then find all regions with a backing image at zoom level 0
		
		xStart &= Mw.REGION_MASK;
		zStart &= Mw.REGION_MASK;
		w = (w + Mw.REGION_SIZE) & Mw.REGION_MASK;
		h = (h + Mw.REGION_SIZE) & Mw.REGION_MASK;
		
		MwUtil.log("recreating zoom levels for regions from (%d, %d) to (%d, %d)", xStart, zStart, xStart + w, zStart + h);
		
		for (int rX = xStart; rX < (xStart + w); rX += Mw.REGION_SIZE) {
			for (int rZ = zStart; rZ < (zStart + h); rZ += Mw.REGION_SIZE) {
				if (MwChunk.regionFileExists(rX >> 5, rZ >> 5, dimension, this.worldDir)) {
					Region region = this.getRegion(rX, rZ, 0, dimension);
					if (this.mapTexture.isRegionInTexture(region)) {
						region.addLoadAndUpdateZoomLevelsTask(this.mapTexture);
					} else {
						region.addLoadAndUpdateZoomLevelsTask(null);
					}
				}
			}
		}
	}
	
	private class SaveChunkTask extends Task {
		private final File worldDir;
		private final MwChunk chunk;
		
		public SaveChunkTask(MwChunk chunk, File worldDir) {
			this.worldDir = worldDir;
			this.chunk = chunk;
		}

		@Override
		public void run() {
			this.chunk.write(this.worldDir);
		}

		@Override
		public void onComplete() {
		}
	}
	
	private void addSaveChunkTask(Chunk chunk) {
		MwChunk mwChunk = MwChunk.copyFromChunk(chunk);
		this.executor.addTask(new SaveChunkTask(mwChunk, this.worldDir));
	}
}
