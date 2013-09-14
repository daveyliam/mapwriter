package mapwriter;

import java.util.ArrayList;
import java.util.HashSet;

import mapwriter.map.MapTexture;
import mapwriter.region.MwChunk;
import mapwriter.region.RegionManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class ChunkManager {
	public Mw mw;
	private boolean closed = false;
	private ArrayList<HashSet<Chunk>> chunkBuckets;
	private boolean[] chunkViewedBitfield = new boolean[64 * 64];
	
	public ChunkManager(Mw mw) {
		this.mw = mw;
		this.chunkBuckets = new ArrayList<HashSet<Chunk>>();
		for (int i = 0; i < 64; i++) {
			this.chunkBuckets.add(new HashSet<Chunk>());
		}
	}
	
	public synchronized void close() {
		this.closed = true;
		this.saveChunks();
		this.chunkBuckets.clear();
	}
	
	// create MwChunk from Minecraft chunk.
	// only MwChunk's should be used in the background thread.
	// TODO: make this a full copy of chunk data
	public static MwChunk copyToMwChunk(Chunk chunk) {
		byte[][] msbArray = new byte[16][];
		byte[][] lsbArray = new byte[16][];
		byte[][] metaArray = new byte[16][];
		
		ExtendedBlockStorage[] storageArrays = chunk.getBlockStorageArray();
		if (storageArrays != null) {
			for (ExtendedBlockStorage storage : storageArrays) {
				if (storage != null) {
					int y = (storage.getYLocation() >> 4) & 0xf;
					lsbArray[y] = storage.getBlockLSBArray();
					msbArray[y] = (storage.getBlockMSBArray() != null) ? storage.getBlockMSBArray().data : null;
					metaArray[y] = (storage.getMetadataArray() != null) ? storage.getMetadataArray().data : null;
				}
			}
		}
		
		return new MwChunk(chunk.xPosition, chunk.zPosition, chunk.worldObj.provider.dimensionId,
				msbArray, lsbArray, metaArray, chunk.getBiomeArray(), chunk.heightMap);
	}
	
	public HashSet<Chunk> getChunkBucket(Chunk chunk) {
		int index = ((chunk.zPosition & 7) << 3) | (chunk.xPosition & 7);
		return this.chunkBuckets.get(index);
	}
	
	private void setChunkViewed(Chunk chunk, boolean value) {
		int i = ((chunk.zPosition & 63) << 6) + (chunk.xPosition & 63);
		this.chunkViewedBitfield[i] = value;
	}
	
	private boolean getChunkViewed(Chunk chunk) {
		int i = ((chunk.zPosition & 63) << 6) + (chunk.xPosition & 63);
		return this.chunkViewedBitfield[i];
	}
	
	public synchronized void addChunk(Chunk chunk) {
		if (!this.closed) {
			this.getChunkBucket(chunk).add(chunk);
			// clear viewed flag on chunk add
			this.setChunkViewed(chunk, false);
		}
	}
	
	public synchronized void removeChunk(Chunk chunk) {
		if (!this.closed) {
			this.getChunkBucket(chunk).remove(chunk);
			this.addSaveChunkTask(chunk);
		}
	}
	
	public synchronized void saveChunks() {
		for (HashSet<Chunk> chunkBucket : this.chunkBuckets) {
			this.addSaveChunkTask(chunkBucket);
		}
	}

	public class MapUpdateChunksTask extends Task {
		ArrayList<MwChunk> chunkList;
		RegionManager regionManager;
		MapTexture mapTexture;
		
		public MapUpdateChunksTask(MapTexture mapTexture, RegionManager regionManager, ArrayList<MwChunk> chunkList) {
			this.mapTexture = mapTexture;
			this.regionManager = regionManager;
			this.chunkList = chunkList;
		}
		
		@Override
		public void run() {
			for (MwChunk chunk : this.chunkList) {
				if (chunk != null) {
					this.regionManager.updateChunk(chunk);
					this.mapTexture.updateChunk(this.regionManager, chunk);
				}
			}
			// copy updated pixels to maptexture
			// unload least accessed regions
			this.regionManager.pruneRegions();
		}
		
		@Override
		public void onComplete() {
			// update GL texture of mapTexture if updated
			this.mapTexture.updateGLTexture();
		}
	}
	
	public int playerDistToChunkSq(Chunk chunk) {
		int dx = (chunk.xPosition << 4) + 8 - this.mw.playerXInt;
		int dz = (chunk.zPosition << 4) + 8 - this.mw.playerZInt;
		return (dx * dx) + (dz * dz);
	}
	
	public void onTick() {
		if (!this.closed) {
			int chunkBucketIndex = this.mw.tickCounter & 63;
			HashSet<Chunk> chunkBucket = this.chunkBuckets.get(chunkBucketIndex);
			ArrayList<MwChunk> chunkList = new ArrayList<MwChunk>();
			for (Chunk chunk : chunkBucket) {
				// if this chunk is within a certain distance to the player then
				// set the 'viewed' flag for this chunk
				if (this.playerDistToChunkSq(chunk) <= this.mw.maxChunkSaveDistSq) {
					this.setChunkViewed(chunk, true);
				}
				// only update chunk if it has been viewed
				if (this.getChunkViewed(chunk)) {
					chunkList.add(copyToMwChunk(chunk));
				}
			}
			this.mw.executor.addTask(new MapUpdateChunksTask(this.mw.mapTexture, this.mw.regionManager, chunkList));
		}
	}
	
	private class SaveChunkTask extends Task {
		private final RegionManager regionManager;
		private final MwChunk[] chunkArray;
		
		public SaveChunkTask(RegionManager regionManager, MwChunk[] chunkArray) {
			this.regionManager = regionManager;
			this.chunkArray = chunkArray;
		}

		@Override
		public void run() {
			this.regionManager.saveChunkArray(this.chunkArray);
		}

		@Override
		public void onComplete() {
		}
	}
	
	private void addSaveChunkTask(Chunk chunk) {
		if ((chunk != null) && (!chunk.isEmpty()) && (this.getChunkViewed(chunk))) {
			MwChunk[] chunkArray = new MwChunk[1];
			chunkArray[0] = copyToMwChunk(chunk);
			this.mw.executor.addTask(new SaveChunkTask(this.mw.regionManager, chunkArray));
		}
	}
	
	private void addSaveChunkTask(HashSet<Chunk> chunkSet) {
		MwChunk[] chunkArray = new MwChunk[chunkSet.size()];
		int i = 0;
		for (Chunk chunk : chunkSet) {
			// only save chunks that are not empty and have been viewed
			if ((chunk != null) && (!chunk.isEmpty()) && (i < chunkArray.length) && (this.getChunkViewed(chunk))) {
				MwChunk mwChunk = copyToMwChunk(chunk);
				chunkArray[i++] = mwChunk;
			}
		}
		this.mw.executor.addTask(new SaveChunkTask(this.mw.regionManager, chunkArray));
	}
}
