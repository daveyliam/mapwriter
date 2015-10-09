package mapwriter.region;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkPosition;

import org.apache.logging.log4j.Level;

import cpw.mods.fml.common.FMLLog;

public class MwChunk implements IChunk {
	public static final int SIZE = 16;

	public final int x;
	public final int z;
	public final int dimension;

	public final byte[][] msbArray;
	public final byte[][] lsbArray;
	public final byte[][] metaArray;
	public final byte[][] lightingArray;
	public final Map tileentityMap;

	public final byte[] biomeArray;

	public final int maxY;

	public MwChunk(int x, int z, int dimension, byte[][] msbArray,
			byte[][] lsbArray, byte[][] metaArray, byte[][] lightingArray,
			byte[] biomeArray, Map TileEntityMap) {
		this.x = x;
		this.z = z;
		this.dimension = dimension;
		this.msbArray = msbArray;
		this.lsbArray = lsbArray;
		this.metaArray = metaArray;
		this.biomeArray = biomeArray;
		this.lightingArray = lightingArray;
		this.tileentityMap = TileEntityMap;
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
	public static MwChunk read(int x, int z, int dimension,
			RegionFileCache regionFileCache) {

		byte[] biomeArray = null;
		byte[][] msbArray = new byte[16][];
		byte[][] lsbArray = new byte[16][];
		byte[][] metaArray = new byte[16][];
		byte[][] lightingArray = new byte[16][];
		Map TileEntityMap = new HashMap();

		DataInputStream dis = null;
		RegionFile regionFile = regionFileCache.getRegionFile(x << 4, z << 4,
				dimension);
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

				// chunk NBT structure:
				//
				// COMPOUND ""
				// COMPOUND "level"
				// INT "xPos"
				// INT "zPos"
				// LONG "LastUpdate"
				// BYTE "TerrainPopulated"
				// BYTE_ARRAY "Biomes"
				// INT_ARRAY "HeightMap"
				// LIST(COMPOUND) "Sections"
				// BYTE "Y"
				// BYTE_ARRAY "Blocks"
				// BYTE_ARRAY "Add"
				// BYTE_ARRAY "Data"
				// BYTE_ARRAY "BlockLight"
				// BYTE_ARRAY "SkyLight"
				// END
				// LIST(COMPOUND) "Entities"
				// LIST(COMPOUND) "TileEntities"
				// LIST(COMPOUND) "TileTicks"
				// END
				// END
				NBTTagCompound nbttagcompound = CompressedStreamTools.read(dis);
				NBTTagCompound level = nbttagcompound.getCompoundTag("Level");

				int xNbt = level.getInteger("xPos");
				int zNbt = level.getInteger("zPos");

				if ((xNbt != x) || (zNbt != z)) {
					RegionManager.logWarning(
							"chunk (%d, %d) has NBT coords (%d, %d)", x, z,
							xNbt, zNbt);
				}

				NBTTagList sections = level.getTagList("Sections", 10);

				for (int k = 0; k < sections.tagCount(); ++k) {
					NBTTagCompound section = sections.getCompoundTagAt(k);
					int y = section.getByte("Y");
					lsbArray[y & 0xf] = section.getByteArray("Blocks");
					if (section.hasKey("Add", 7)) {
						msbArray[y & 0xf] = section.getByteArray("Add");
					}
					metaArray[y & 0xf] = section.getByteArray("Data");
				}

				biomeArray = level.getByteArray("Biomes");

				NBTTagList nbttaglist2 = level.getTagList("TileEntities", 10);

				if (nbttaglist2 != null) {
					for (int i1 = 0; i1 < nbttaglist2.tagCount(); ++i1) {
						NBTTagCompound nbttagcompound4 = nbttaglist2
								.getCompoundTagAt(i1);
						TileEntity tileentity = TileEntity
								.createAndLoadEntity(nbttagcompound4);
						if (tileentity != null) {
							ChunkPosition chunkposition = new ChunkPosition(
									tileentity.xCoord, tileentity.yCoord,
									tileentity.zCoord);

							TileEntityMap.put(chunkposition, tileentity);
						}
					}
				}

			} catch (IOException e) {
				RegionManager.logError(
						"%s: could not read chunk (%d, %d) from region file\n",
						e, x, z);
			} finally {
				try {
					dis.close();
				} catch (IOException e) {
					RegionManager.logError(
							"MwChunk.read: %s while closing input stream", e);
				}
			}
			// this.log("MwChunk.read: chunk (%d, %d) empty=%b", this.x, this.z,
			// empty);
		} else {
			// this.log("MwChunk.read: chunk (%d, %d) input stream is null",
			// this.x, this.z);
		}

		return new MwChunk(x, z, dimension, msbArray, lsbArray, metaArray,
				lightingArray, biomeArray, TileEntityMap);
	}

	public boolean isEmpty() {
		return (this.maxY <= 0);
	}

	public int getBiome(int x, int z) {
		return (this.biomeArray != null) ? (int) (this.biomeArray[((z & 0xf) << 4)
				| (x & 0xf)]) & 0xff
				: 0;
	}

	public int getLightValue(int x, int y, int z) {
		// int yi = (y >> 4) & 0xf;
		// int offset = ((y & 0xf) << 8) | ((z & 0xf) << 4) | (x & 0xf);

		// int light = ((this.lightingArray != null) && (this.lightingArray[yi]
		// != null)) ? this.lightingArray[yi][offset >> 1] : 15;

		// return ((offset & 1) == 1) ? ((light >> 4) & 0xf) : (light & 0xf);
		return 15;
	}

	public int getMaxY() {
		return this.maxY;
	}

	private static java.lang.reflect.Method CarpenterMethod = null;

	public static void carpenterdata() {
		try {
			Class<?> act = Class
					.forName("com.carpentersblocks.tileentity.TEBase");
			CarpenterMethod = act.getMethod("getAttribute", byte.class);
		} catch (SecurityException e) {
			// ...
		} catch (NoSuchMethodException e) {
			// ...
		} catch (ClassNotFoundException e) {
			//
		}
	}

	private static java.lang.reflect.Method FMPMethodParts = null;
	private static java.lang.reflect.Method FMPMethodMaterial = null;

	private static java.lang.reflect.Field FMPFieldBlock = null;
	private static java.lang.reflect.Field FMPFieldMeta = null;

	public static void FMPdata() {
		try {
			Class<?> act = Class.forName("codechicken.multipart.TileMultipart");
			FMPMethodParts = act.getMethod("jPartList");
			act = Class.forName("codechicken.microblock.Microblock");
			FMPMethodMaterial = act.getMethod("getIMaterial");

			act = Class.forName("codechicken.microblock.BlockMicroMaterial");
			FMPFieldBlock = act.getDeclaredField("block");
			FMPFieldBlock.setAccessible(true);

			FMPFieldMeta = act.getDeclaredField("meta");
			FMPFieldMeta.setAccessible(true);

		} catch (SecurityException e) {
			// ...
		} catch (NoSuchMethodException e) {
			// ...
		} catch (ClassNotFoundException e) {
			//
		} catch (NoSuchFieldException e) {
			//
		}
	}

	public int getBlockAndMetadata(int x, int y, int z) {
		int yi = (y >> 4) & 0xf;
		int offset = ((y & 0xf) << 8) | ((z & 0xf) << 4) | (x & 0xf);
		ChunkPosition chunkposition = new ChunkPosition(x, y, z);
		int lsb = ((this.lsbArray != null) && (this.lsbArray[yi] != null) && (this.lsbArray[yi].length != 0)) ? this.lsbArray[yi][offset]
				: 0;
		int msb = ((this.msbArray != null) && (this.msbArray[yi] != null) && (this.msbArray[yi].length != 0)) ? this.msbArray[yi][offset >> 1]
				: 0;
		int meta = ((this.metaArray != null) && (this.metaArray[yi] != null) && (this.metaArray[yi].length != 0)) ? this.metaArray[yi][offset >> 1]
				: 0;

		// check if the block has a tileentity if so use the blockdata in the
		// tileentity
		// (forgemultipart and carpenterblocks both save the block to be
		// rendered in the tileentity map)

		if (this.tileentityMap.containsKey(chunkposition)) {
			TileEntity value = (TileEntity) this.tileentityMap
					.get(chunkposition);
			int id = 0;

			// Get the Block from the carpenter TileEntity
			if (CarpenterMethod != null) {
				try {
					ItemStack itemStack = (ItemStack) CarpenterMethod.invoke(
							value, (byte) 6);
					if (itemStack != null) {
						ItemBlock itemBlock = (ItemBlock) itemStack.getItem();
						id = Block.getIdFromBlock(itemBlock.blockInstance);
						meta = itemStack.getMetadata();
					}
				} catch (IllegalArgumentException e) {
				} catch (IllegalAccessException e) {
				} catch (InvocationTargetException e) {
				}
			}
			if (FMPMethodParts != null) {
				try {
					for (Object temp : (List) FMPMethodParts.invoke(value)) {
						Object material = FMPMethodMaterial.invoke(temp);
						Block block = (Block) FMPFieldBlock.get(material);
						id = Block.getIdFromBlock(block);
						meta = (Integer) FMPFieldMeta.get(material);
					}

				} catch (IllegalArgumentException e) {
				} catch (IllegalAccessException e) {
				} catch (InvocationTargetException e) {
				}
			}
			if (id != 0) {
				lsb = (id & 255);
				if (id > 255) {
					msb = (id & 3840) >> 8;
				} else {
					msb = 0;
				}
			}
		}

		return ((offset & 1) == 1) ? ((msb & 0xf0) << 8) | ((lsb & 0xff) << 4)
				| ((meta & 0xf0) >> 4) : ((msb & 0x0f) << 12)
				| ((lsb & 0xff) << 4) | (meta & 0x0f);
	}

	// changed to use the NBTTagCompound that minecraft uses. this makes the
	// local way of saving anvill data the same as Minecraft world data
	private NBTTagCompound writeChunkToNBT() {
		NBTTagCompound nbttagcompound = new NBTTagCompound();
		NBTTagCompound nbttagcompound1 = new NBTTagCompound();
		nbttagcompound.setTag("Level", nbttagcompound1);

		nbttagcompound1.setByte("V", (byte) 1);
		nbttagcompound1.setInteger("xPos", this.x);
		nbttagcompound1.setInteger("zPos", this.z);

		NBTTagList nbttaglist = new NBTTagList();

		int i = 16;
		NBTTagCompound nbttagcompound2;

		for (int y = 0; y < i; ++y) {
			if ((this.lsbArray != null) && (this.lsbArray[y] != null)) {
				nbttagcompound2 = new NBTTagCompound();
				nbttagcompound2.setByte("Y", (byte) y);
				nbttagcompound2.setByteArray("Blocks", this.lsbArray[y]);

				if ((this.msbArray != null) && (this.msbArray[y] != null)) {
					nbttagcompound2.setByteArray("Add", this.msbArray[y]);
				}

				nbttagcompound2.setByteArray("Data", this.metaArray[y]);
				nbttaglist.appendTag(nbttagcompound2);
			}
		}

		nbttagcompound1.setTag("Sections", nbttaglist);
		nbttagcompound1.setByteArray("Biomes", this.biomeArray);

		NBTTagList nbttaglist2 = new NBTTagList();
		Iterator iterator1;

		NBTTagList nbttaglist3 = new NBTTagList();

		iterator1 = this.tileentityMap.values().iterator();

		int skipped = 0;
		
		while (iterator1.hasNext()) {
			TileEntity tileentity = (TileEntity) iterator1.next();
			nbttagcompound2 = new NBTTagCompound();
			try {
				tileentity.writeToNBT(nbttagcompound2);
				nbttaglist3.appendTag(nbttagcompound2);
			} catch (Exception e) {
				//ignoring errors here becouse they will happen.
				//mods might have server only code behind this method and thus we will fail when calling it.
				//this is not a direct problem, the tile entity will just get lost in the local mapwriter save of the world
			}
		}
		nbttagcompound1.setTag("TileEntities", nbttaglist3);

		return nbttagcompound;
	}

	public synchronized boolean write(RegionFileCache regionFileCache) {
		boolean error = false;
		RegionFile regionFile = regionFileCache.getRegionFile(this.x << 4,
				this.z << 4, this.dimension);
		if (!regionFile.isOpen()) {
			error = regionFile.open();
		}
		if (!error) {
			DataOutputStream dos = regionFile.getChunkDataOutputStream(
					this.x & 31, this.z & 31);
			if (dos != null) {
				// Nbt chunkNbt = this.getNbt();
				try {
					// RegionManager.logInfo("writing chunk (%d, %d) to region file",
					// this.x, this.z);
					// chunkNbt.writeElement(dos);
					// use minecraft build in save tool for saving the Anvil
					// Data
					CompressedStreamTools.write(writeChunkToNBT(), dos);
				} catch (IOException e) {
					RegionManager
							.logError(
									"%s: could not write chunk (%d, %d) to region file",
									e, this.x, this.z);
					error = true;
				} finally {
					try {
						dos.close();
					} catch (IOException e) {
						RegionManager.logError(
								"%s while closing chunk data output stream", e);
					}
				}
			} else {
				RegionManager
						.logError(
								"error: could not get output stream for chunk (%d, %d)",
								this.x, this.z);
			}
		} else {
			RegionManager.logError(
					"error: could not open region file for chunk (%d, %d)",
					this.x, this.z);
		}

		return error;
	}
}
