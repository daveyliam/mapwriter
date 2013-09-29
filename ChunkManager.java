package mapwriter;

import mapwriter.map.MapTexture;
import mapwriter.region.MwChunk;
import mapwriter.region.RegionManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class ChunkManager {
	public Mw mw;
	private boolean closed = false;
	private CircularList<Chunk> allChunkList = new CircularList<Chunk>();
	private CircularList<Chunk> viewedChunkList = new CircularList<Chunk>();
	
	public ChunkManager(Mw mw) {
		this.mw = mw;
	}
	
	public synchronized void close() {
		this.closed = true;
		this.saveChunks();
		this.viewedChunkList.clear();
		this.allChunkList.clear();
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
	
	public synchronized void addChunk(Chunk chunk) {
		if (!this.closed && (chunk != null)) {
			this.allChunkList.add(chunk);
		}
	}
	
	public synchronized void removeChunk(Chunk chunk) {
		if (!this.closed && (chunk != null)) {
			this.allChunkList.remove(chunk);
			if (this.viewedChunkList.contains(chunk)) {
				this.addSaveChunkTask(chunk);
				this.viewedChunkList.remove(chunk);
			}
		}
	}
	
	public synchronized void saveChunks() {
		for (Chunk chunk : this.viewedChunkList.getAll()) {
			this.addSaveChunkTask(chunk);
		}
	}

	public class MapUpdateChunksTask extends Task {
		MwChunk[] chunkArray;
		RegionManager regionManager;
		MapTexture mapTexture;
		
		public MapUpdateChunksTask(MapTexture mapTexture, RegionManager regionManager, MwChunk[] chunkArray) {
			this.mapTexture = mapTexture;
			this.regionManager = regionManager;
			this.chunkArray = chunkArray;
		}
		
		@Override
		public void run() {
			for (MwChunk chunk : this.chunkArray) {
				if (chunk != null) {
					// update the chunk in the region pixels
					this.regionManager.updateChunk(chunk);
					// copy updated region pixels to maptexture
					this.mapTexture.updateChunk(this.regionManager, chunk);
				}
			}
			// unload least accessed regions
			this.regionManager.pruneRegions();
		}
		
		@Override
		public void onComplete() {
		}
	}
	
	public void onTick() {
		if (!this.closed) {
			int chunksToUpdate = Math.min(this.allChunkList.size(), this.mw.chunksPerTick);
			
			for (int i = 0; i < chunksToUpdate; i++) {
				Chunk chunk = this.allChunkList.getNext();
				if (chunk != null) {
					// if this chunk is within a certain distance to the player then
					// add it to the viewed set
					if (MwUtil.distToChunkSq(this.mw.playerXInt, this.mw.playerZInt, chunk) <= this.mw.maxChunkSaveDistSq) {
						this.viewedChunkList.add(chunk);
					}
				}
			}
			
			chunksToUpdate = Math.min(this.viewedChunkList.size(), this.mw.chunksPerTick);
			MwChunk[] chunkArray = new MwChunk[chunksToUpdate];
			for (int i = 0; i < chunksToUpdate; i++) {
				Chunk chunk = this.viewedChunkList.getNext();
				if (chunk != null) {
					chunkArray[i] = copyToMwChunk(chunk);
				} else {
					chunkArray[i] = null;
				}
			}
			this.mw.executor.addTask(new MapUpdateChunksTask(this.mw.mapTexture, this.mw.regionManager, chunkArray));
		}
	}
	
	private class SaveChunkTask extends Task {
		private final RegionManager regionManager;
		private final MwChunk mwChunk;
		
		public SaveChunkTask(RegionManager regionManager, MwChunk mwChunk) {
			this.regionManager = regionManager;
			this.mwChunk = mwChunk;
		}

		@Override
		public void run() {
			this.regionManager.saveChunk(this.mwChunk);
		}

		@Override
		public void onComplete() {
		}
	}
	
	private void addSaveChunkTask(Chunk chunk) {
		if (!chunk.isEmpty()) {
			this.mw.executor.addTask(new SaveChunkTask(this.mw.regionManager, copyToMwChunk(chunk)));
		}
	}
}
