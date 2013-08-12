package mapwriter;

import java.util.ArrayList;
import java.util.HashSet;

import mapwriter.map.MapUpdateChunksTask;
import mapwriter.region.MwChunk;
import mapwriter.region.RegionManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class ChunkManager {
	public Mw mw;
	private boolean closed = false;
	private ArrayList<HashSet<Chunk>> chunkBuckets;
	
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
	
	public synchronized void addChunk(Chunk chunk) {
		if (!this.closed) {
			this.getChunkBucket(chunk).add(chunk);
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
	
	public void onTick() {
		if (!this.closed) {
			int chunkBucketIndex = this.mw.tickCounter & 63;
			HashSet<Chunk> chunkBucket = this.chunkBuckets.get(chunkBucketIndex);
			MwChunk[] chunkArray = new MwChunk[chunkBucket.size()];
			int i = 0;
			for (Chunk chunk : chunkBucket) {
				chunkArray[i++] = copyToMwChunk(chunk);
				if (i >= chunkArray.length) {
					break;
				}
			}
			this.mw.executor.addTask(new MapUpdateChunksTask(this.mw.mapTexture, this.mw.regionManager, chunkArray));
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
			this.regionManager.writeChunkArray(this.chunkArray);
		}

		@Override
		public void onComplete() {
		}
	}
	
	private void addSaveChunkTask(Chunk chunk) {
		MwChunk[] chunkArray = new MwChunk[1];
		chunkArray[0] = copyToMwChunk(chunk);
		this.mw.executor.addTask(new SaveChunkTask(this.mw.regionManager, chunkArray));
	}
	
	private void addSaveChunkTask(HashSet<Chunk> chunkSet) {
		MwChunk[] chunkArray = new MwChunk[chunkSet.size()];
		int i = 0;
		for (Chunk chunk : chunkSet) {
			if ((chunk != null) && (!chunk.isEmpty()) && (i < chunkArray.length)) {
				MwChunk mwChunk = copyToMwChunk(chunk);
				chunkArray[i++] = mwChunk;
			}
		}
		this.mw.executor.addTask(new SaveChunkTask(this.mw.regionManager, chunkArray));
	}
}
