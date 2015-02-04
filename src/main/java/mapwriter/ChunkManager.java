package mapwriter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import mapwriter.region.MwChunk;
import mapwriter.tasks.SaveChunkTask;
import mapwriter.tasks.UpdateSurfaceChunksTask;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class ChunkManager {
	public Mw mw;
	private boolean closed = false;
	private CircularHashMap<Chunk, Integer> chunkMap = new CircularHashMap<Chunk, Integer>();
	
	private static final int VISIBLE_FLAG = 0x01;
	private static final int VIEWED_FLAG = 0x02;
	
	public ChunkManager(Mw mw) {
		this.mw = mw;
	}
	
	public synchronized void close() {
		this.closed = true;
		this.saveChunks();
		this.chunkMap.clear();
	}
	
	// create MwChunk from Minecraft chunk.
	// only MwChunk's should be used in the background thread.
	// make this a full copy of chunk data to prevent possible race conditions <-- done
	public static MwChunk copyToMwChunk(Chunk chunk) {
		
		byte[][] msbArray = new byte[16][];
		byte[][] lsbArray = new byte[16][];
		byte[][] metaArray = new byte[16][];
		byte[][] lightingArray = new byte[16][];
		Map TileEntityMap = new HashMap();
		TileEntityMap.putAll(chunk.chunkTileEntityMap);
		
		ExtendedBlockStorage[] storageArrays = chunk.getBlockStorageArray();
		if (storageArrays != null) {
			for (ExtendedBlockStorage storage : storageArrays) {
				if (storage != null) {
					int y = (storage.getYLocation() >> 4) & 0xf;
					lsbArray[y] = Arrays.copyOf(storage.getBlockLSBArray(), storage.getBlockLSBArray().length);
					msbArray[y] = (storage.getBlockMSBArray() != null) ? Arrays.copyOf(storage.getBlockMSBArray().data, storage.getBlockMSBArray().data.length)  : null;
					metaArray[y] = (storage.getMetadataArray() != null) ? Arrays.copyOf(storage.getMetadataArray().data, storage.getMetadataArray().data.length) : null;
					lightingArray[y] = (storage.getBlocklightArray() != null) ? Arrays.copyOf(storage.getBlocklightArray().data, storage.getBlocklightArray().data.length) : null;
				}
			}
		}
		
		return new MwChunk(chunk.xPosition, chunk.zPosition, chunk.worldObj.provider.dimensionId,
				msbArray, lsbArray, metaArray, lightingArray, Arrays.copyOf(chunk.getBiomeArray(),chunk.getBiomeArray().length),TileEntityMap);
	}
	
	public synchronized void addChunk(Chunk chunk) {
		if (!this.closed && (chunk != null)) {
			this.chunkMap.put(chunk, 0);
		}
	}
	
	public synchronized void removeChunk(Chunk chunk) {
		if (!this.closed && (chunk != null)) {
            if(!this.chunkMap.containsKey(chunk)) return; //FIXME: Is this failsafe enough for unloading?
			int flags = this.chunkMap.get(chunk);
			if ((flags & VIEWED_FLAG) != 0) {
				this.addSaveChunkTask(chunk);
			}
			this.chunkMap.remove(chunk);
		}
	}
	
	public synchronized void saveChunks() {
		for (Map.Entry<Chunk, Integer> entry : this.chunkMap.entrySet()) {
			int flags = entry.getValue();
			if ((flags & VIEWED_FLAG) != 0) {
				this.addSaveChunkTask(entry.getKey());
			}
		}
	}
	
	public void updateUndergroundChunks() {
		int chunkArrayX = (this.mw.playerXInt >> 4) - 1;
		int chunkArrayZ = (this.mw.playerZInt >> 4) - 1;
		MwChunk[] chunkArray = new MwChunk[9];
		for (int z = 0; z < 3; z++) {
			for (int x = 0; x < 3; x++) {
				Chunk chunk = this.mw.mc.theWorld.getChunkFromChunkCoords(
					chunkArrayX + x,
					chunkArrayZ + z
				);
				if (!chunk.isEmpty()) {
					chunkArray[(z * 3) + x] = copyToMwChunk(chunk);
				}
			}
		}
	}
	
	public void updateSurfaceChunks() {
		int chunksToUpdate = Math.min(this.chunkMap.size(), this.mw.chunksPerTick);
		MwChunk[] chunkArray = new MwChunk[chunksToUpdate];
		for (int i = 0; i < chunksToUpdate; i++) {
			Map.Entry<Chunk, Integer> entry = this.chunkMap.getNextEntry();
			if (entry != null) {
				// if this chunk is within a certain distance to the player then
				// add it to the viewed set
				Chunk chunk = entry.getKey();

				int flags = entry.getValue();
				if (MwUtil.distToChunkSq(this.mw.playerXInt, this.mw.playerZInt, chunk) <= this.mw.maxChunkSaveDistSq) {
					flags |= (VISIBLE_FLAG | VIEWED_FLAG);
				} else {
					flags &= ~VISIBLE_FLAG;
				}
				entry.setValue(flags);
				
				if ((flags & VISIBLE_FLAG) != 0) {
					chunkArray[i] = copyToMwChunk(chunk);
				} else {
					chunkArray[i] = null;
				}
			}
		}
		
		this.mw.executor.addTask(new UpdateSurfaceChunksTask(this.mw, chunkArray));
	}
	
	public void onTick() {
		if (!this.closed) {
			if ((this.mw.tickCounter & 0xf) == 0) {
				this.updateUndergroundChunks();
			} else {
				this.updateSurfaceChunks();
			}
		}
	}
	
	public void forceChunks(MwChunk[] chunkArray){
		this.mw.executor.addTask(new UpdateSurfaceChunksTask(this.mw, chunkArray));
	}
	
	private void addSaveChunkTask(Chunk chunk) {
		if ((this.mw.multiplayer && this.mw.regionFileOutputEnabledMP) || 
			(!this.mw.multiplayer && this.mw.regionFileOutputEnabledSP)) {
			if (!chunk.isEmpty()) {
				this.mw.executor.addTask(new SaveChunkTask(copyToMwChunk(chunk), this.mw.regionManager));
			}
		}
	}
}