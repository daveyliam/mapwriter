package mapwriter.region;

import java.util.Arrays;

import net.minecraft.world.biome.BiomeGenBase;

public class BlockColours {
	
	public static final int MAX_BLOCKS = 4096;
	public static final int MAX_META = 16;
	
	private int[] bcArray = new int[MAX_BLOCKS * MAX_META];
	private int[] waterMultiplierArray = null;
	private int[] grassMultiplierArray = null;
	private int[] foliageMultiplierArray = null;
	
	public enum BlockType {
		NORMAL,
		GRASS,
		LEAVES,
		FOLIAGE,
		WATER,
		OPAQUE
	}
	
	private BlockType[] blockTypeArray = new BlockType[MAX_BLOCKS * MAX_META];
	
	public BlockColours() {
		Arrays.fill(this.bcArray, 0);
		Arrays.fill(this.blockTypeArray, BlockType.NORMAL);
	}
	
	public int getColour(int blockAndMeta) {
		return this.bcArray[blockAndMeta & 0xffff];
	}
	
	public int getColour(int blockID, int dv) {
		return getColour(((blockID & 0xfff) << 4) | (dv & 0xf));
	}
	
	private int getGrassColourMultiplier(int biome) {
		return (this.grassMultiplierArray != null) && (biome >= 0) && (biome < this.grassMultiplierArray.length) ?
				this.grassMultiplierArray[biome] : 0xffffff;
	}
	
	private int getWaterColourMultiplier(int biome) {
		return (this.waterMultiplierArray != null) && (biome >= 0) && (biome < this.waterMultiplierArray.length) ?
				this.waterMultiplierArray[biome] : 0xffffff;
	}
	
	private int getFoliageColourMultiplier(int biome) {
		return (this.foliageMultiplierArray != null) && (biome >= 0) && (biome < this.foliageMultiplierArray.length) ?
				this.foliageMultiplierArray[biome] : 0xffffff;
	}
	
	public int getBiomeColour(int blockAndMeta, int biome) {
		blockAndMeta &= 0xffff;
		int colourMultiplier;
		switch(this.blockTypeArray[blockAndMeta]) {
		case GRASS:
			colourMultiplier = getGrassColourMultiplier(biome);
			break;
		case LEAVES:
		case FOLIAGE:
			colourMultiplier = getFoliageColourMultiplier(biome);
			break;
		case WATER:
			colourMultiplier = getWaterColourMultiplier(biome);
			break;
		default:
			colourMultiplier = 0xffffff;
			break;
		}
		return colourMultiplier;
	}
	
	public void clearBiomeArrays() {
		this.waterMultiplierArray = new int[BiomeGenBase.biomeList.length];
		this.grassMultiplierArray = new int[BiomeGenBase.biomeList.length];
		this.foliageMultiplierArray = new int[BiomeGenBase.biomeList.length];
	}
	
	public void setBiomeWaterShading(int biomeID, int colour) {
		this.waterMultiplierArray[biomeID & 0xff] = colour;
	}
	
	public void setBiomeGrassShading(int biomeID, int colour) {
		this.grassMultiplierArray[biomeID & 0xff] = colour;
	}
	
	public void setBiomeFoliageShading(int biomeID, int colour) {
		this.foliageMultiplierArray[biomeID & 0xff] = colour;
	}
	
	public BlockType getBlockType(int blockAndMeta) {
		return this.blockTypeArray[blockAndMeta & 0xffff];
	}
	
	public void setBlockTypeForBlockAndMeta(int blockAndMeta, BlockType type) {
		this.blockTypeArray[blockAndMeta & 0xffff] = type;
	}
	
	public void setBlockTypeForBlockID(int blockID, BlockType type) {
		for (int i = 0; i < 16; i++) {
			this.blockTypeArray[((blockID & 0xfff) << 4) | i] = type;
		}
	}
	
	public void setColour(int blockID, int dv, int colour) {
		this.bcArray[((blockID & 0xfff) << 4) | (dv & 0xf)] = colour;
	}
	
	public void setColour(int blockAndMeta, int colour) {
		this.bcArray[blockAndMeta & 0xffff] = colour;
	}
}
