package mapwriter.region;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MwChunk implements IChunk {
	public static final int SIZE = 16;
	
	public final int x;
	public final int z;
	public final int dimension;
	
	public final byte[][] msbArray;
	public final byte[][] lsbArray;
	public final byte[][] metaArray;
	public final byte[][] lightingArray;
	
	public final byte[] biomeArray;
	
	public final int maxY;
	
	public MwChunk(int x, int z, int dimension, byte[][] msbArray, byte[][] lsbArray, byte[][] metaArray, byte[][] lightingArray, byte[] biomeArray) {
		this.x = x;
		this.z = z;
		this.dimension = dimension;
		this.msbArray = msbArray;
		this.lsbArray = lsbArray;
		this.metaArray = metaArray;
		this.biomeArray = biomeArray;
		this.lightingArray = lightingArray;
		int maxY = 0;
		for (int y = 0; y < 16; y++) {
			if (lsbArray[y] != null) {
				maxY = (y << 4) + 15;
			}
		}
		this.maxY = maxY;
	}
	
	public String toString() {
		return String.format("(%d, %d) dim%d", this.x, this.z, this.dimension);
	}
	
	// load from anvil file
	public static MwChunk read(int x, int z, int dimension, RegionFileCache regionFileCache) {
		
		byte[] biomeArray = null;
		byte[][] msbArray = new byte[16][];
		byte[][] lsbArray = new byte[16][];
		byte[][] metaArray = new byte[16][];
		byte[][] lightingArray = new byte[16][];
		
        DataInputStream dis = null;
        RegionFile regionFile = regionFileCache.getRegionFile(x << 4, z << 4, dimension);
        if (!regionFile.isOpen()) {
        	if (regionFile.exists()) {
        		regionFile.open();
        	}
        }
		
        if (regionFile.isOpen()) {
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
					RegionManager.logWarning("chunk (%d, %d) has NBT coords (%d, %d)", x, z, xNbt, zNbt);
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
				
			} catch (IOException e) {
				RegionManager.logError("%s: could not read chunk (%d, %d) from region file\n", e, x, z);
			} finally {
				try { dis.close(); }
				catch (IOException e) {
					RegionManager.logError("MwChunk.read: %s while closing input stream", e);
				}
			}
			//this.log("MwChunk.read: chunk (%d, %d) empty=%b", this.x, this.z, empty);
		} else {
			//this.log("MwChunk.read: chunk (%d, %d) input stream is null", this.x, this.z); 
		}
		
		return new MwChunk(x, z, dimension, msbArray, lsbArray, metaArray, lightingArray, biomeArray);
	}
	
	public boolean isEmpty() {
		return (this.maxY <= 0);
	}
	
	public int getBiome(int x, int z) {
		return (this.biomeArray != null) ? (int) (this.biomeArray[((z & 0xf) << 4) | (x & 0xf)]) & 0xff : 0;
	}
	
	public int getLightValue(int x, int y, int z) {
		//int yi = (y >> 4) & 0xf;
		//int offset = ((y & 0xf) << 8) | ((z & 0xf) << 4) | (x & 0xf);
		
		//int light = ((this.lightingArray  != null) && (this.lightingArray[yi]  != null)) ? this.lightingArray[yi][offset  >> 1] : 15;
		
		//return ((offset & 1) == 1) ? ((light >> 4) & 0xf) : (light & 0xf);
		return 15;
	}
	
	public int getMaxY() {
		return this.maxY;
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
	
	public synchronized boolean write(RegionFileCache regionFileCache) {
		boolean error = false;
		RegionFile regionFile = regionFileCache.getRegionFile(this.x << 4, this.z << 4, this.dimension);
		if (!regionFile.isOpen()) {
        	error = regionFile.open();
        }
		if (!error) {
			DataOutputStream dos = regionFile.getChunkDataOutputStream(this.x & 31, this.z & 31);
			if (dos != null) {
				Nbt chunkNbt = this.getNbt();
				try {
					//RegionManager.logInfo("writing chunk (%d, %d) to region file", this.x, this.z);
					chunkNbt.writeElement(dos);
				} catch (IOException e) {
					RegionManager.logError("%s: could not write chunk (%d, %d) to region file", e, this.x, this.z);
					error = true;
				} finally {
					try { dos.close(); }
					catch (IOException e) {
						RegionManager.logError("%s while closing chunk data output stream", e);
					}
				}
			} else {
				RegionManager.logError("error: could not get output stream for chunk (%d, %d)", this.x, this.z);
			}
		} else {
			RegionManager.logError("error: could not open region file for chunk (%d, %d)", this.x, this.z);
		}
		
		return error;
	}
}
