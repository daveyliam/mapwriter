package mapwriter.map;

import java.awt.Point;
import java.util.Arrays;

import mapwriter.Mw;
import mapwriter.Texture;
import mapwriter.region.ChunkRender;
import mapwriter.region.IChunk;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.chunk.Chunk;

import org.lwjgl.opengl.GL11;

public class UndergroundTexture extends Texture {
	
	private Mw mw;
	private int px = 0;
	private int py = 0;
	private int pz = 0;
	private int dimension = 0;
	private int updateX;
	private int updateZ;
	private byte[][] updateFlags = new byte[9][256];
	private Point[] loadedChunkArray;
	private int textureSize;
	private int textureChunks;
	private int[] pixels;
	
	class RenderChunk implements IChunk {
		Chunk chunk;
		
		public RenderChunk(Chunk chunk) {
			this.chunk = chunk;
		}
		
		@Override
		public int getMaxY() {
			return this.chunk.getTopFilledSegment() + 15;
		}
		
		@Override
		public int getBlockAndMetadata(int x, int y, int z) {
			Block block = this.chunk.getBlock(x, y, z);
            int blockid = Block.blockRegistry.getIDForObject(block);
			int meta = this.chunk.getBlockMetadata(x, y, z);
			return ((blockid & 0xfff) << 4) | (meta & 0xf);
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
		super(textureSize, textureSize, 0x00000000, GL11.GL_NEAREST, GL11.GL_NEAREST, GL11.GL_REPEAT);
		this.setLinearScaling(false);
		this.textureSize = textureSize;
		this.textureChunks = textureSize >> 4;
		this.loadedChunkArray = new Point[this.textureChunks * this.textureChunks];
		this.pixels = new int[textureSize * textureSize];
		Arrays.fill(this.pixels, 0xff000000);
		this.mw = mw;
	}
	
	public void clear() {
		Arrays.fill(this.pixels, 0xff000000);
		this.updateTexture();
	}
	
	public void clearChunkPixels(int cx, int cz) {
		int tx = (cx << 4) & (this.textureSize - 1);
		int tz = (cz << 4) & (this.textureSize - 1);
		for (int j = 0; j < 16; j++) {
			int offset = ((tz + j) * this.textureSize) + tx;
			Arrays.fill(this.pixels, offset, offset + 16, 0xff000000);
		}
		this.updateTextureArea(tx, tz, 16, 16);
	}
	
	void renderToTexture(int y) {
		this.setPixelBufPosition(0);
		for (int i = 0; i < this.pixels.length; i++) {
			int colour = this.pixels[i];
			int height = (colour >> 24) & 0xff;
			int alpha = (y >= height) ? 255 - ((y - height) * 8) : 0;
			if (alpha < 0) {
				alpha = 0;
			}
			this.pixelBufPut(((alpha << 24) & 0xff000000) | (colour & 0xffffff));
		}
		this.updateTexture();
	}
	
	public int getLoadedChunkOffset(int cx, int cz) {
		int cxOffset = cx & (this.textureChunks - 1);
		int czOffset = cz & (this.textureChunks - 1);
		return (czOffset * this.textureChunks) + cxOffset;
	}
	
	public void requestView(MapView view) {
		int cxMin = ((int) view.getMinX()) >> 4;
		int czMin = ((int) view.getMinZ()) >> 4;
		int cxMax = ((int) view.getMaxX()) >> 4;
		int czMax = ((int) view.getMaxZ()) >> 4;
		for (int cz = czMin; cz <= czMax; cz++) {
			for (int cx = cxMin; cx <= cxMax; cx++) {
				Point requestedChunk = new Point(cx, cz);
				int offset = this.getLoadedChunkOffset(cx, cz);
				Point currentChunk = this.loadedChunkArray[offset];
				if ((currentChunk == null) || !currentChunk.equals(requestedChunk)) {
					this.clearChunkPixels(cx, cz);
					this.loadedChunkArray[offset] = requestedChunk;
				}
			}
		}
	} 
	
	public boolean isChunkInTexture(int cx, int cz) {
		Point requestedChunk = new Point(cx, cz);
		int offset = this.getLoadedChunkOffset(cx, cz);
		Point chunk = this.loadedChunkArray[offset];
		return (chunk != null) && chunk.equals(requestedChunk);
	}
	
	public void update() {
		this.clearFlags();
		
		if (this.dimension != this.mw.playerDimension) {
			this.clear();
			this.dimension = this.mw.playerDimension;
		}
		this.px = this.mw.playerXInt;
		this.py = this.mw.playerYInt;
		this.pz = this.mw.playerZInt;
		
		this.updateX = (this.px >> 4) - 1;
		this.updateZ = (this.pz >> 4) - 1;
		
		this.processBlock(
			this.px - (this.updateX << 4),
			this.py,
			this.pz - (this.updateZ << 4)
		);
		
		int cxMax = this.updateX + 2;
		int czMax = this.updateZ + 2;
		WorldClient world = this.mw.mc.theWorld;
		int flagOffset = 0;
		for (int cz = this.updateZ; cz <= czMax; cz++) {
			for (int cx = this.updateX; cx <= cxMax; cx++) {
				if (this.isChunkInTexture(cx, cz)) {
					Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
					int tx = (cx << 4) & (this.textureSize - 1);
					int tz = (cz << 4) & (this.textureSize - 1);
					int pixelOffset = (tz * this.textureSize) + tx;
					byte[] mask = this.updateFlags[flagOffset];
					ChunkRender.renderUnderground(
						this.mw.blockColours,
						new RenderChunk(chunk),
						this.pixels, pixelOffset, this.textureSize,
						this.py, mask
					);
				}
				flagOffset += 1;
			}
		}
		
		this.renderToTexture(this.py + 1);
	}
	
	private void clearFlags() {
		for (byte[] chunkFlags : this.updateFlags) {
			Arrays.fill(chunkFlags, ChunkRender.FLAG_UNPROCESSED);
		}
	}
	
	private void processBlock(int xi, int y, int zi) {
		int x = (this.updateX << 4) + xi;
		int z = (this.updateZ << 4) + zi;
		
		int xDist = this.px - x;
		int zDist = this.pz - z;
		
		if (((xDist * xDist) + (zDist * zDist)) <= 256) {
			if (this.isChunkInTexture(x >> 4, z >> 4)) {
				int chunkOffset = ((zi >> 4) * 3) + (xi >> 4);
				int columnXi = xi & 0xf;
				int columnZi = zi & 0xf;
				int columnOffset = (columnZi << 4) + columnXi;
				byte columnFlag = this.updateFlags[chunkOffset][columnOffset];
				
				if (columnFlag == ChunkRender.FLAG_UNPROCESSED) {
					// if column not yet processed
					WorldClient world = this.mw.mc.theWorld;
					Block block = world.getBlock(x, y, z);
					if ((block == null) || !block.isOpaqueCube()) {
						// if block is not opaque
						this.updateFlags[chunkOffset][columnOffset] = (byte) ChunkRender.FLAG_NON_OPAQUE;
						this.processBlock(xi + 1, y, zi);
						this.processBlock(xi - 1, y, zi);
						this.processBlock(xi, y, zi + 1);
						this.processBlock(xi, y, zi - 1);
					} else {
						// block is opaque
						this.updateFlags[chunkOffset][columnOffset] = (byte) ChunkRender.FLAG_OPAQUE;
					}
				}
			}
		}
	}

}
