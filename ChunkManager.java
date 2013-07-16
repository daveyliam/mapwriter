package mapwriter;

import java.util.HashSet;

import mapwriter.map.MapUpdateChunksTask;
import mapwriter.region.MwChunk;
import mapwriter.region.RegionManager;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class ChunkManager {
	public Mw mw;
	
	public static final int chunkArrayShift = 4;
	public static final int chunkArraySize = 1 << chunkArrayShift;
	
	private HashSet<Chunk> chunkSet;
	private MwChunk[] chunkArray;
	private boolean chunkUpdateReady = false;
	private boolean closed = false;
	private int chunkUpdateXIndex = 0;
	private int chunkUpdateZIndex = 0;
	private int chunkUpdateX = 0;
	private int chunkUpdateZ = 0;
	
	public ChunkManager(Mw mw) {
		this.mw = mw;
		this.chunkSet = new HashSet<Chunk>();
		this.chunkArray = new MwChunk[chunkArraySize * chunkArraySize];
	}
	
	public void close() {
		this.closed = true;
		this.saveChunks();
		this.chunkSet.clear();
		this.chunkArray = null;
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
	
	public void addChunk(Chunk chunk) {
		if (!this.closed) {
			this.chunkSet.add(chunk);
		}
	}
	
	public void removeChunk(Chunk chunk) {
		if (!this.closed) {
			this.chunkSet.remove(chunk);
			this.addSaveChunkTask(chunk);
		}
	}
	
	public void saveChunks() {
		this.addSaveChunkTask(this.chunkSet);
		this.chunkSet.clear();
	}
	
	private boolean addNextChunkToArray(World world, int playerX, int playerZ) {
		int chunkX = this.chunkUpdateX + this.chunkUpdateXIndex;
		int chunkZ = this.chunkUpdateZ + this.chunkUpdateZIndex;
		Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);

		int chunkArrayIndex = ((chunkZ & (chunkArraySize - 1)) * chunkArraySize) + (chunkX & (chunkArraySize - 1));
		this.chunkArray[chunkArrayIndex] = copyToMwChunk(chunk);
		
		this.chunkUpdateXIndex++;
		boolean arrayReady = false;
		if (this.chunkUpdateXIndex >= chunkArraySize) {
			this.chunkUpdateXIndex = 0;
			this.chunkUpdateZIndex++;
			if (this.chunkUpdateZIndex >= chunkArraySize) {
				this.resetChunkUpdate(playerX, playerZ);
				arrayReady = true;
			}
		}
		return arrayReady;
	}
	
	private void resetChunkUpdate(int playerX, int playerZ) {
		this.chunkUpdateX = (playerX >> 4) - 8;
		this.chunkUpdateZ = (playerZ >> 4) - 8;
		this.chunkUpdateXIndex = 0;
		this.chunkUpdateZIndex = 0;
	}
	
	public void onTick() {
		if (!this.closed) {
			if (!this.chunkUpdateReady) {
				this.resetChunkUpdate(this.mw.playerXInt, this.mw.playerZInt);
				this.chunkUpdateReady = true;
			}
			
			for (int i = 0; i < this.mw.chunksPerTick; i++) {
				boolean arrayReady = this.addNextChunkToArray(this.mw.mc.theWorld, this.mw.playerXInt, this.mw.playerZInt);
				if (arrayReady) {
					this.mw.executor.addTask(new MapUpdateChunksTask(this.mw.mapTexture, this.mw.regionManager, this.chunkArray));
				}
			}
		}
	}
	
	/*private boolean chunkSurroundedAndNotEmpty(Chunk chunk) {
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
	
	private boolean addUpdateChunkTask(Chunk chunk, int playerX, int playerZ) {
		boolean taskAdded = false;
		if (this.chunkSurroundedAndNotEmpty(chunk)) {
			
			MwChunk mwchunk = copyToMwChunk(chunk);
			
			int dx = ((mwchunk.x << 4) + 8) - playerX;
			int dz = ((mwchunk.z << 4) + 8) - playerZ;
			int distSquared = (dx * dx) + (dz * dz);
			
			Region region = this.getRegion(mwchunk.x << 4, mwchunk.z << 4, 0, mwchunk.dimension);
			Region textureRegion = this.getRegionAtTextureZoomLevel(region);
			region.updateChunk(mwchunk, textureRegion, this.mw.mapTexture, (distSquared <= 160));
			taskAdded = true;
		}
		return !taskAdded;
	}*/
	
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
