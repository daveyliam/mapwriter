package mapwriter.map;

import java.awt.Point;
import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.chunk.Chunk;

import org.lwjgl.opengl.GL11;

import mapwriter.Mw;
import mapwriter.Texture;
import mapwriter.region.ChunkRender;
import mapwriter.region.IChunk;

public class UndergroundTexture extends Texture {
	
	private static int UPDATE_SIZE = 20;
	
	private Mw mw;
	private int updateX;
	private int updateZ;
	private boolean[] updateFlags = new boolean[UPDATE_SIZE * UPDATE_SIZE];
	private Point[] loadedChunkArray;
	private int textureSize;
	private int textureChunks;
	private byte[] heights;
	
	class RenderChunk implements IChunk {
		Chunk chunk;
		
		public RenderChunk(Chunk chunk) {
			this.chunk = chunk;
		}
		
		@Override
		public int getBlockAndMetadata(int x, int y, int z) {
			int blockID = this.chunk.getBlockID(x, y, z);
			int meta = this.chunk.getBlockMetadata(x, y, z);
			return ((blockID & 0xfff) << 4) | (meta & 0xf);
		}

		@Override
		public int getBiome(int x, int z) {
			return (int) this.chunk.getBiomeArray()[(z * 16) + x];
		}

		@Override
		public int getLightValue(int x, int y, int z) {
			return this.chunk.getBlockLightValue(x, y, z, 0);
		}
	}
	
	public UndergroundTexture(Mw mw, int textureSize, boolean linearScaling) {	
		super(textureSize, textureSize, 0xff000000, GL11.GL_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT);
		this.setLinearScaling(linearScaling);
		this.textureSize = textureSize;
		this.textureChunks = textureSize >> 4;
		this.loadedChunkArray = new Point[this.textureChunks * this.textureChunks];
		this.heights = new byte[textureSize * textureSize];
		this.fillHeightRect(0, 0, this.textureSize, textureSize, (byte) 64);
		this.mw = mw;
	}
	
	public void fillHeightRect(int x, int z, int w, int h, byte height) {
		for (int j = 0; j < h; j++) {
			for (int i = 0; i < h; i++) {
				int offset = ((z + j) * this.textureSize) + x + i;
				this.heights[offset] = height;
			}
		}
	}
	
	void updateTextureChunk(int cx, int cz) {
		int tx = (cx << 4) & (this.textureSize - 1);
		int tz = (cz << 4) & (this.textureSize - 1);
		this.updateTextureArea(tx, tz, 16, 16);
	}
	
	public void requestView(int xMin, int zMin, int xMax, int zMax) {
		int cxMin = xMin >> 4;
		int czMin = zMin >> 4;
		int cxMax = xMax >> 4;
		int czMax = zMax >> 4;
		for (int cz = czMin; cz < czMax; cz++) {
			for (int cx = cxMin; cx < cxMax; cx++) {
				Point requestedChunk = new Point(cx, cz);
				int cxOffset = (requestedChunk.x) & (this.textureChunks - 1);
				int czOffset = (requestedChunk.y) & (this.textureChunks - 1);
				int offset = (czOffset * this.textureChunks) + cxOffset;
				Point currentChunk = this.loadedChunkArray[offset];
				if ((currentChunk == null) || !currentChunk.equals(requestedChunk)) {
					int tx = cxOffset << 4;
					int tz = czOffset << 4;
					this.fillRect(tx, tz, 16, 16, 0xff000000);
					this.fillHeightRect(tx, tz, 16, 16, (byte) 64);
					this.updateTextureArea(tx, tz, 16, 16);
					this.loadedChunkArray[offset] = requestedChunk;
				}
			}
		}
	} 
	
	public boolean isChunkInTexture(int x, int z) {
		Point requestedChunk = new Point(x >> 4, z >> 4);
		int cxOffset = (requestedChunk.x) & (this.textureChunks - 1);
		int czOffset = (requestedChunk.y) & (this.textureChunks - 1);
		int offset = (czOffset * this.textureChunks) + cxOffset;
		Point chunk = this.loadedChunkArray[offset];
		return (chunk != null) && chunk.equals(requestedChunk);
	}
	
	public void update() {
		this.clearFlags();
		int radius = (UPDATE_SIZE / 2);
		this.updateX = this.mw.playerXInt - radius;
		this.updateZ = this.mw.playerZInt - radius;
		this.processBlock(
			radius,
			this.mw.playerYInt,
			radius
		);
		int cxMin = this.updateX >> 4;
		int czMin = this.updateZ >> 4;
		int cxMax = (this.updateX + UPDATE_SIZE) >> 4;
		int czMax = (this.updateZ + UPDATE_SIZE) >> 4;
		for (int cz = czMin; cz <= czMax; cz++) {
			for (int cx = cxMin; cx <= cxMax; cx++) {
				this.updateTextureChunk(cx, cz);
			}
		}
	}
	
	private void clearFlags() {
		Arrays.fill(this.updateFlags, false);
	}
	
	public int getColumnColour(WorldClient world, int x, int y, int z, int heightW, int heightN) {
		Chunk chunk = world.getChunkFromBlockCoords(x, z);
		RenderChunk rChunk = new RenderChunk(chunk);
		return ChunkRender.getColumnColour(
			this.mw.blockColours,
			rChunk, x & 15, y, z & 15, heightW, heightN
		);
	}
	
	private void processBlock(int xi, int y, int zi) {
		if ((xi >= 0) && (xi < UPDATE_SIZE) && (zi >= 0) && (zi < UPDATE_SIZE)) {
			int offset = (zi * UPDATE_SIZE) + xi;
			int x = this.updateX + xi;
			int z = this.updateZ + zi;
			// only process columns that are loaded
			if (this.isChunkInTexture(x, z)) {
				WorldClient world = this.mw.mc.theWorld;
				boolean flag = this.updateFlags[offset];
				this.updateFlags[offset] = true;
				if (!flag) {
					// if column not yet processed
					int blockID = world.getBlockId(x, y, z);
					Block block = Block.blocksList[blockID];
					if ((block == null) || !block.isOpaqueCube()) {
						// if block is air
						int tx = x & (this.textureSize - 1);
						int tz = z & (this.textureSize - 1);
						int hOffset = (tz * this.textureSize) + tx;
						int heightN = -1;
						int heightW = -1;
						if (tz > 0) {
							heightN = this.heights[hOffset - this.textureSize];
						}
						if (tx > 0) {
							heightW = this.heights[hOffset - 1];
						}
						int colour = this.getColumnColour(world, x, y, z, heightW, heightN);
						this.setRGB(tx, tz, colour | 0xff000000);
						this.heights[hOffset] = (byte) ((colour >> 24) & 0xff);
						
						this.processBlock(xi + 1, y, zi);
						this.processBlock(xi - 1, y, zi);
						this.processBlock(xi, y, zi + 1);
						this.processBlock(xi, y, zi - 1);
					}
				}
			}
		}
	}
}
