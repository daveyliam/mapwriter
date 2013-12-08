package mapwriter.tasks;

import java.util.Arrays;

import mapwriter.Mw;
import mapwriter.map.MapTexture;
import mapwriter.region.ChunkRender;
import mapwriter.region.MwChunk;
import mapwriter.region.RegionManager;

public class UpdateUndergroundChunksTask extends Task {
	MwChunk[] chunkArray;
	RegionManager regionManager;
	MapTexture mapTexture;
	int px, py, pz;
	int chunkArrayX, chunkArrayZ;
	
	// 3 x 3 chunks are updated surrounding the player
	private byte[][] columnFlags = new byte[9][256];
	
	public UpdateUndergroundChunksTask(Mw mw, MwChunk[] chunkArray, int chunkArrayX, int chunkArrayZ) {
		this.mapTexture = mw.mapTexture;
		this.regionManager = mw.regionManager;
		this.px = mw.playerXInt;
		this.py = mw.playerYInt + 1;
		this.pz = mw.playerZInt;
		this.chunkArray = chunkArray;
		this.chunkArrayX = chunkArrayX;
		this.chunkArrayZ = chunkArrayZ;
	}
	
	@Override
	public void run() {
		this.clearColumnFlags();
		this.processBlock(
			this.px - (this.chunkArrayX << 4),
			this.py,
			this.pz - (this.chunkArrayZ << 4)
		);
		for (int i = 0; i < 9; i++) {
			MwChunk chunk = this.chunkArray[i];
			byte[] mask = this.columnFlags[i];
			if ((chunk != null) && (mask != null)) {
				this.regionManager.updateChunk(chunk, this.py, mask);
				// copy updated region pixels to maptexture
				this.mapTexture.updateArea(
					this.regionManager,
					chunk.x << 4, this.py, chunk.z << 4,
					MwChunk.SIZE, MwChunk.SIZE, chunk.dimension
				);
			}
		}
	}
	
	@Override
	public void onComplete() {
	}
	
	private void clearColumnFlags() {
		for (byte[] chunkFlags : this.columnFlags) {
			Arrays.fill(chunkFlags, ChunkRender.MASK_UNPROCESSED);
		}
	}
	
	private void processBlock(int x, int y, int z) {
		int cxi = x >> 4;
		int czi = z >> 4;
		if ((cxi >= 0) && (cxi < 3) && (czi >= 0) && (czi < 3)) {
			int chunkOffset = ((z >> 4) * 3) + (x >> 4);
			int xi = x & 0xf;
			int zi = z & 0xf;
			int columnOffset = (zi << 4) + xi;
			byte columnFlag = this.columnFlags[chunkOffset][columnOffset];
			if (columnFlag == ChunkRender.MASK_UNPROCESSED) {
				// if column not yet processed
				MwChunk chunk = this.chunkArray[chunkOffset];
				if (chunk != null) {
					int blockAndMeta = chunk.getBlockAndMetadata(xi, y, zi);
					int colour = this.regionManager.blockColours.getColour(blockAndMeta);
					if ((colour & 0xff000000) != 0xff000000) {
						// if block is not opaque
						this.columnFlags[chunkOffset][columnOffset] = (byte) ChunkRender.MASK_NON_OPAQUE;
						this.processBlock(x + 1, y, z);
						this.processBlock(x - 1, y, z);
						this.processBlock(x, y, z + 1);
						this.processBlock(x, y, z - 1);
					} else {
						// block is opaque
						this.columnFlags[chunkOffset][columnOffset] = (byte) ChunkRender.MASK_OPAQUE;
					}
				}
			}
		}
	}
}
