package mapwriter.region;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraftforge.fml.common.FMLLog;

import org.apache.logging.log4j.Level;

public class MwChunk implements IChunk {
	public static final int SIZE = 16;
	
	public final int x;
	public final int z;
	public final int dimension;
	
	char[][] dataArray = new char[16][]; 
	public final byte[][] lightingArray;
	public final Map<BlockPos, TileEntity> tileentityMap;
	
	public final byte[] biomeArray;
	
	public final int maxY;
	
	public MwChunk(int x, int z, int dimension, char[][] data, byte[][] lightingArray, byte[] biomeArray, Map<BlockPos, TileEntity> TileEntityMap) {
		this.x = x;
		this.z = z;
		this.dimension = dimension;
		this.biomeArray = biomeArray;
		this.lightingArray = lightingArray;
		this.tileentityMap = TileEntityMap;
		this.dataArray = data;
		int maxY = 0;
		for (int y = 0; y < 16; y++) {
			if (data[y] != null) {
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
		byte[][] lsbArray = new byte[16][];
		char[][] data = new char[16][];
		byte[][] lightingArray = new byte[16][];
		Map<BlockPos, TileEntity> TileEntityMap = new HashMap<BlockPos, TileEntity>();
		
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
				NBTTagCompound nbttagcompound = CompressedStreamTools.read(dis);
				NBTTagCompound level = nbttagcompound.getCompoundTag("Level");
				
				int xNbt = level.getInteger("xPos");
				int zNbt = level.getInteger("zPos");
				
				if ((xNbt != x) || (zNbt != z)) {
					RegionManager.logWarning("chunk (%d, %d) has NBT coords (%d, %d)", x, z, xNbt, zNbt);
				}
				
				NBTTagList sections = level.getTagList("Sections", 10);
				
		        for (int k = 0; k < sections.tagCount(); ++k)
		        {
		        	NBTTagCompound section = sections.getCompoundTagAt(k);
							int y = section.getByte("Y");
							lsbArray[y & 0xf] = section.getByteArray("Blocks");
				            NibbleArray nibblearray = new NibbleArray(section.getByteArray("Data"));
				            NibbleArray nibblearray1 = section.hasKey("Add", 7) ? new NibbleArray(section.getByteArray("Add")) : null;
						
				            data[y & 0xf]  = new char[lsbArray[y].length];

				            for (int l = 0; l < data[y & 0xf].length; ++l)
				            {
				                int i1 = l & 15;
				                int j1 = l >> 8 & 15;
				                int k1 = l >> 4 & 15;
				                int l1 = nibblearray1 != null ? nibblearray1.get(i1, j1, k1) : 0;
				                data[y & 0xf][l] = (char)(l1 << 12 | (lsbArray[y][l] & 255) << 4 | nibblearray.get(i1, j1, k1));
				            }
		        }

				biomeArray = level.getByteArray("Biomes");

		        	NBTTagList nbttaglist2 = level.getTagList("TileEntities", 10);

		            if (nbttaglist2 != null)
		            {
		                for (int i1 = 0; i1 < nbttaglist2.tagCount(); ++i1)
		                {
		                    NBTTagCompound nbttagcompound4 = nbttaglist2.getCompoundTagAt(i1);
		                    TileEntity tileentity = TileEntity.createAndLoadEntity(nbttagcompound4);
		                    if (tileentity != null)
		                    {
		                    	TileEntityMap.put(tileentity.getPos(), tileentity);
		                    }
		                }
		            }
		        	
				
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
		
		return new MwChunk(x, z, dimension,data , lightingArray, biomeArray,TileEntityMap);
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
		
		int lsb = 0;
		int msb = 0;
		int meta = 0;
		
		BlockPos pos = new BlockPos(x,y,z);
		
		char data = ((this.dataArray  != null) && (this.dataArray[yi]  != null) && (this.dataArray[yi].length != 0)) ? this.dataArray[yi][offset] : 0;
		
		//check if the block has a tileentity if so use the blockdata in the tileentity 
		//(forgemultipart and carpenterblocks both save the block to be rendered in the tileentity map)
		if (this.tileentityMap.containsKey(pos))
		{
			
			 TileEntity value = (TileEntity)this.tileentityMap.get(pos);
			 NBTTagCompound tag = new NBTTagCompound();
			 value.writeToNBT(tag);
			 int id = 0;
			 
			 if (tag.getString("id") == "savedMultipart")
			 {
				String material = tag.getTagList("parts", 10).getCompoundTagAt(0).getString("material");
				int end = material.indexOf("_");
				
				//block with metadata
				if (end != -1)
				{
				id = Block.getIdFromBlock(Block.getBlockFromName(material.substring(5, end)));
				
				lsb = (id & 255);
								
				if (id > 255){msb = (id & 3840) >> 8;}
		        else    {msb = 0;}
				
				meta = Integer.parseInt(material.substring(end+1));
				}
				//block without metadata
				else
				{
					id = Block.getIdFromBlock(Block.getBlockFromName(material.substring(5)));
					
					lsb = (id & 255);
					if (id > 255){msb = (id & 3840) >> 8;}
			        else    {msb = 0;}
				}
				data = (char)(((msb & 0x0f) << 12) | ((lsb & 0xff) << 4) | (meta & 0x0f));
			 }
			 else if (tag.getString("id") =="TileEntityCarpentersBlock")
			 {
				NBTTagList TagList = tag.getTagList("cbAttrList", 10);
				String sid = TagList.getCompoundTagAt(0).getString("id");
				String smeta = TagList.getCompoundTagAt(0).getString("Damage"); 
				if (sid != "")
				{
					id = Integer.parseInt(sid.substring(0, sid.length()-1));
					
					lsb = (id & 255);
					if (id > 255){msb = (id & 3840) >> 8;}
			        else    {msb = 0;}
					
					if (smeta != "")
					{
						meta = Integer.parseInt(smeta.substring(0, smeta.length()-1));
					}
					
					data = (char)(((msb & 0x0f) << 12) | ((lsb & 0xff) << 4) | (meta & 0x0f));
				}
			 }
		 }

		//return ((offset & 1) == 1) ?
		//		((msb & 0xf0) << 8)  | ((lsb & 0xff) << 4) | ((meta & 0xf0) >> 4) :
		return (int)data;
	}
	
	//changed to use the NBTTagCompound that minecraft uses. this makes the local way of saving anvill data the same as Minecraft world data
    private NBTTagCompound writeChunkToNBT()
    {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        NBTTagCompound nbttagcompound1 = new NBTTagCompound();
        nbttagcompound.setTag("Level", nbttagcompound1);
        
        nbttagcompound1.setInteger("xPos", this.x);
        nbttagcompound1.setInteger("zPos", this.z);
        
        NBTTagList nbttaglist = new NBTTagList();
        
        int i = 16;
        NBTTagCompound nbttagcompound2;

        for (int y = 0; y < this.dataArray.length; y++)
        {    
        	if (this.dataArray[y] != null)
        	{
        	byte[] abyte = new byte[this.dataArray[y].length];
            NibbleArray nibblearray = new NibbleArray();
            NibbleArray nibblearray1 = null;

            for (int k = 0; k < this.dataArray[y].length; ++k)
            {
                char c0 = this.dataArray[y][k];
                int l = k & 15;
                int i1 = k >> 8 & 15;
                int j1 = k >> 4 & 15;

                if (c0 >> 12 != 0)
                {
                    if (nibblearray1 == null)
                    {
                        nibblearray1 = new NibbleArray();
                    }

                    nibblearray1.set(l, i1, j1, c0 >> 12);
                }

                abyte[k] = (byte)(c0 >> 4 & 255);
                nibblearray.set(l, i1, j1, c0 & 15);
            }
            
            
            	nbttagcompound2 = new NBTTagCompound();
            	nbttagcompound2.setByte("Y", (byte)y);
            	nbttagcompound2.setByteArray("Blocks", abyte);

                if (nibblearray1 != null)
                {
                	nbttagcompound2.setByteArray("Add", nibblearray1.getData());
                }

                nbttagcompound2.setByteArray("Data", nibblearray.getData());
                nbttaglist.appendTag(nbttagcompound2);
            }

        nbttagcompound1.setTag("Sections", nbttaglist);
        }
        nbttagcompound1.setByteArray("Biomes", this.biomeArray);

        NBTTagList nbttaglist2 = new NBTTagList();
        Iterator iterator1;

        NBTTagList nbttaglist3 = new NBTTagList();
        
        iterator1 = this.tileentityMap.values().iterator();

        while (iterator1.hasNext())
        {
            TileEntity tileentity = (TileEntity)iterator1.next();
            nbttagcompound2 = new NBTTagCompound();
            try {
            tileentity.writeToNBT(nbttagcompound2);
            nbttaglist3.appendTag(nbttagcompound2);
            }
            catch (Exception e)
            {
                FMLLog.log(Level.ERROR, e,
                        "A TileEntity type %s has throw an exception trying to write state. It will not persist. Report this to the mod author",
                        tileentity.getClass().getName());
            }
        }
        nbttagcompound1.setTag("TileEntities", nbttaglist3);
        
        return nbttagcompound;
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
				//Nbt chunkNbt = this.getNbt();
				try {
					//RegionManager.logInfo("writing chunk (%d, %d) to region file", this.x, this.z);
					//chunkNbt.writeElement(dos);
					//use minecraft build in save tool for saving the Anvil Data
					CompressedStreamTools.write(writeChunkToNBT(), dos);
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
