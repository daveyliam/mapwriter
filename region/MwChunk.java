package mapwriter.region;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import mapwriter.Mw;
import mapwriter.MwUtil;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.world.chunk.storage.RegionFileCache;

public class MwChunk {
	public final int x;
	public final int z;
	public final int dimension;
	
	public final byte[][] msbArray;
	public final byte[][] lsbArray;
	public final byte[][] metaArray;
	
	public final byte[] biomeArray;
	public final int[] heightMap;
	
	public final int maxHeight;
	
	public MwChunk(int x, int z, int dimension, byte[][] msbArray, byte[][] lsbArray, byte[][] metaArray, byte[] biomeArray, int[] heightMap) {
		this.x = x;
		this.z = z;
		this.dimension = dimension;
		this.msbArray = msbArray;
		this.lsbArray = lsbArray;
		this.metaArray = metaArray;
		this.biomeArray = biomeArray;
		int maxY = 0;
		for (int y = 0; y < 16; y++) {
			if (lsbArray[y] != null) {
				maxY = y + 1;
			}
		}
		this.maxHeight = maxY << 4;
		this.heightMap = (heightMap != null) ? heightMap : this.genHeightMap();
	}
	
	public String toString() {
		return String.format("(%d, %d) dim%d", this.x, this.z, this.dimension);
	}
	
	public static File getRegionFileName(int x, int z, int dimension, File worldDir) {
		File dimensionDir = MwUtil.getDimensionDir(worldDir, dimension);
		File regionDir = new File(dimensionDir, "region");
        return new File(regionDir, "r." + (x >> 5) + "." + (z >> 5) + ".mca");
	}
	
	public static boolean regionFileExists(int x, int z, int dimension, File worldDir) {
		return getRegionFileName(x, z, dimension, worldDir).isFile();
	}
	
	// load from anvil file
	public static MwChunk read(int x, int z, int dimension, File worldDir) {
		
		boolean error = true;
		byte[] biomeArray = null;
		byte[][] msbArray = new byte[16][];
		byte[][] lsbArray = new byte[16][];
		byte[][] metaArray = new byte[16][];
		
		File dimensionDir = MwUtil.getDimensionDir(worldDir, dimension);
		//MwUtil.log("MwChunk.read: (%d, %d)", this.x, this.z);
		//File regionDir = new File(dimensionDir, "region");
        File regionFileName = getRegionFileName(x, z, dimension, worldDir);
        DataInputStream dis = null;
        if (regionFileName.isFile()) {
        	RegionFile regionFile = RegionFileCache.createOrLoadRegionFile(dimensionDir, x, z);
        	dis = regionFile.getChunkDataInputStream(x & 31, z & 31);
        }
		
		if (dis != null) {
			try {
				
				//chunk NBT structure:
				//
				//COMPOUND ""
				//COMPOUND "level"
				//  INT "xPos"
				//  INT "zPos"
				//  LONG "LastUpdate"
				//  BYTE "TerrainPopulated"
				//  BYTE_ARRAY "Biomes"
				//  INT_ARRAY "HeightMap"
				//  LIST(COMPOUND) "Sections"
				//	      BYTE "Y"
				//	      BYTE_ARRAY "Blocks"
				//	      BYTE_ARRAY "Add"
				//	      BYTE_ARRAY "Data"
				//	      BYTE_ARRAY "BlockLight"
				//	      BYTE_ARRAY "SkyLight"
				//			 END
				//  LIST(COMPOUND) "Entities"
				//  LIST(COMPOUND) "TileEntities"
				//  LIST(COMPOUND) "TileTicks"
				//END
				//END
				
				Nbt root = Nbt.readNextElement(dis);
				Nbt level = root.getChild("Level");
				
				int xNbt = level.getChild("xPos").getInt();
				int zNbt = level.getChild("zPos").getInt();
				if ((xNbt != x) || (zNbt != z)) {
					MwUtil.log("warning: chunk (%d, %d) has NBT coords (%d, %d)", x, z, xNbt, zNbt);
				}
				
				Nbt sections = level.getChild("Sections");
			
				// loop through each of the sections (16 x 16 x 16 block volumes) present
				for (int i = 0; i < sections.size(); i++) {
					Nbt section = sections.getChild(i);
					if (!section.isNull()) {
						int y = section.getChild("Y").getByte();
						lsbArray[y & 0xf] = section.getChild("Blocks").getByteArray();
						msbArray[y & 0xf] = section.getChild("Add").getByteArray();
						metaArray[y & 0xf] = section.getChild("Data").getByteArray();
					}
				}
				biomeArray = level.getChild("Biomes").getByteArray();
				
				error = false;
				
			} catch (IOException e) {
				MwUtil.log("%s: could not read chunk (%d, %d) from region file\n", e, x, z);
				error = true;
			} finally {
				try { dis.close(); }
				catch (IOException e) {
					MwUtil.log("MwChunk.read: %s while closing input stream", e);
				}
			}
			//MwUtil.log("MwChunk.read: chunk (%d, %d) empty=%b", this.x, this.z, empty);
		} else {
			//MwUtil.log("MwChunk.read: chunk (%d, %d) input stream is null", this.x, this.z); 
		}
		return new MwChunk(x, z, dimension, msbArray, lsbArray, metaArray, biomeArray, null);
	}
	
	// create from Minecraft chunk
	public static MwChunk copyFromChunk(Chunk chunk) {
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
	
	private int[] genHeightMap() {
		int[] heightMap = new int[256];
		for (int z = 0; z < 16; z++) {
			for (int x = 0; x < 16; x++) {
				int height = 0;
				for (int y = this.maxHeight - 1; (height == 0) && (y >= 0); y--) {
					if (this.getBlockAndMetadata(x, y, z) != 0) {
						height = y;
					}
				}
				heightMap[(z << 4) | x] = height;
			}
		}
		return heightMap;
	}
	
	public int getHeight(int x, int z) {
		return this.heightMap[((z & 0xf) << 4) | (x & 0xf)];
	}
	
	public boolean isEmpty() {
		return (this.maxHeight <= 0);
	}
	
	public int getBiome(int x, int z) {
		return (this.biomeArray != null) ? (int) (this.biomeArray[((z & 0xf) << 4) | (x & 0xf)]) & 0xff : 0;
	}
	
	public int getBlockAndMetadata(int x, int y, int z) {
		int yi = (y >> 4) & 0xf;
		int offset = ((y & 0xf) << 8) | ((z & 0xf) << 4) | (x & 0xf);
		
		int lsb  = ((this.lsbArray  != null) && (this.lsbArray[yi]  != null)) ? this.lsbArray[yi][offset]       : 0;
		int msb  = ((this.msbArray  != null) && (this.msbArray[yi]  != null)) ? this.msbArray[yi][offset  >> 1] : 0;
		int meta = ((this.metaArray != null) && (this.metaArray[yi] != null)) ? this.metaArray[yi][offset >> 1] : 0;
		
		return ((offset & 1) == 1) ?
				((msb & 0xf0) << 8)  | ((lsb & 0xff) << 4) | ((meta & 0xf0) >> 4) :
				((msb & 0x0f) << 12) | ((lsb & 0xff) << 4) | (meta & 0x0f);
	}
	
	public int getCheckSum() {
		// start checksum with x and z coordinates
		int sum = ((this.z & 0xffff) << 16) | (this.x & 0xffff);
		for (int z = 0; z < Mw.CHUNK_SIZE; z++) {
			for (int x = 0; x < Mw.CHUNK_SIZE; x++) {
				// get the uppermost non air block in the chunk column.
				// won't work well for dimensions with a ceiling.
				int y = this.getHeight(x, z);
				int blockAndMeta = this.getBlockAndMetadata(x, y, z);
				
				// rotate left 5
				sum = ((sum >> 27) & 0x1f) | (sum << 5);
				// xor in height and block data
				sum ^= ((y & 0xff) << 16) | (blockAndMeta & 0xffff);
			}
		}
		return sum;
	}
	
	public Nbt getNbt() {
		Nbt sections = new Nbt(Nbt.TAG_LIST, "Sections", null);
		
		for (int y = 0; y < 16; y++) {
			Nbt section = new Nbt(Nbt.TAG_COMPOUND, "", null);
			
			section.addChild(new Nbt(Nbt.TAG_BYTE, "Y", (byte) y));
			
			if ((this.lsbArray != null) && (this.lsbArray[y] != null)) {
				section.addChild(new Nbt(Nbt.TAG_BYTE_ARRAY, "Blocks", this.lsbArray[y]));
			}
			if ((this.msbArray != null) && (this.msbArray[y] != null)) {
				section.addChild(new Nbt(Nbt.TAG_BYTE_ARRAY, "Add", this.msbArray[y]));
			}
			if ((this.metaArray != null) && (this.metaArray[y] != null)) {
				section.addChild(new Nbt(Nbt.TAG_BYTE_ARRAY, "Data", this.metaArray[y]));
			}
			
			sections.addChild(section);
		}
		
		Nbt level = new Nbt(Nbt.TAG_COMPOUND, "Level", null);
		level.addChild(new Nbt(Nbt.TAG_INT, "xPos", this.x));
		level.addChild(new Nbt(Nbt.TAG_INT, "zPos", this.z));
		level.addChild(sections);
		
		if (this.biomeArray != null) {
			level.addChild(new Nbt(Nbt.TAG_BYTE_ARRAY, "Biomes", this.biomeArray));
		}
		
		Nbt root = new Nbt(Nbt.TAG_COMPOUND, "", null);
		root.addChild(level);
		
		return root;
	}
	
	public synchronized boolean write(File worldDir) {
		boolean error = false;
		File dimensionDir = MwUtil.getDimensionDir(worldDir, this.dimension);
		RegionFile regionFile = RegionFileCache.createOrLoadRegionFile(dimensionDir, this.x, this.z);
		DataOutputStream dos = regionFile.getChunkDataOutputStream(this.x & 31, this.z & 31);
		
		if (dos != null) {
			Nbt chunkNbt = this.getNbt();
			try {
				//MwUtil.log("writing chunk (%d, %d) to region file", this.x, this.z);
				chunkNbt.writeElement(dos);
			} catch (IOException e) {
				MwUtil.log("%s: could not write chunk (%d, %d) to region file", e, this.x, this.z);
				error = true;
			} finally {
				try { dos.close(); }
				catch (IOException e) {
					MwUtil.log("MwChunk.write: %s while closing output stream", e);
				}
			}
		} else {
			MwUtil.log("error: could not get output stream for chunk (%d, %d)", this.x, this.z);
		}
		
		return error;
	}
}
