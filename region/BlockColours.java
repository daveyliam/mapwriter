package mapwriter.region;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;

public class BlockColours {
	
	public static final int MAX_BLOCKS = 4096;
	public static final int MAX_META = 16;
	public static final int MAX_BIOMES = 256;
	
	public static final String biomeSectionString = "[biomes]";
	public static final String blockSectionString = "[blocks]";
	
	private int[] bcArray = new int[MAX_BLOCKS * MAX_META];
	private int[] waterMultiplierArray = new int[MAX_BIOMES];
	private int[] grassMultiplierArray = new int[MAX_BIOMES];
	private int[] foliageMultiplierArray = new int[MAX_BIOMES];
	
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
		Arrays.fill(this.waterMultiplierArray, 0xffffff);
		Arrays.fill(this.grassMultiplierArray, 0xffffff);
		Arrays.fill(this.foliageMultiplierArray, 0xffffff);
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
	
	public void setBiomeWaterShading(int biomeID, int colour) {
		this.waterMultiplierArray[biomeID & 0xff] = colour;
	}
	
	public void setBiomeGrassShading(int biomeID, int colour) {
		this.grassMultiplierArray[biomeID & 0xff] = colour;
	}
	
	public void setBiomeFoliageShading(int biomeID, int colour) {
		this.foliageMultiplierArray[biomeID & 0xff] = colour;
	}
	
	private static BlockType getBlockTypeFromString(String typeString) {
		BlockType blockType = BlockType.NORMAL;
		if (typeString.equalsIgnoreCase("normal")) {
			blockType = BlockType.NORMAL;
		} else if (typeString.equalsIgnoreCase("grass")) {
			blockType = BlockType.GRASS;
		} else if (typeString.equalsIgnoreCase("leaves")) {
			blockType = BlockType.LEAVES;
		} else if (typeString.equalsIgnoreCase("foliage")) {
			blockType = BlockType.FOLIAGE;
		} else if (typeString.equalsIgnoreCase("water")) {
			blockType = BlockType.WATER;
		} else if (typeString.equalsIgnoreCase("opaque")) {
			blockType = BlockType.OPAQUE;
		} else {
			RegionManager.logWarning("unknown block type '%s'", typeString);
		}
		return blockType;
	}
	
	private static String getBlockTypeAsString(BlockType blockType) {
		String s = "normal";
		switch (blockType) {
		case NORMAL:
			s = "normal";
			break;
		case GRASS:
			s = "grass";
			break;
		case LEAVES:
			s = "leaves";
			break;
		case FOLIAGE:
			s = "foliage";
			break;
		case WATER:
			s = "water";
			break;
		case OPAQUE:
			s = "opaque";
			break;
		}
		return s;
	}
	
	public BlockType getBlockType(int blockAndMeta) {
		return this.blockTypeArray[blockAndMeta & 0xffff];
	}
	
	public BlockType getBlockType(int blockId, int meta) {
		return this.getBlockType(((blockId & 0xfff) << 4) | (meta & 0xf));
	}
	
	public void setBlockType(int blockId, int meta, BlockType type) {
		this.blockTypeArray[((blockId & 0xfff) << 4) | (meta & 0xf)] = type;
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
	
	public static int getColourFromString(String s) {
		return (int) (Long.parseLong(s, 16) & 0xffffffffL);
	}
	
	//
	// Methods for loading block colours from file:
	//
	
	// read biome colour multiplier values.
	// line format is:
	//   biome <biomeId> <waterMultiplier> <grassMultiplier> <foliageMultiplier>
	// the biome id and colour multipliers are in hex.
	// accepts "*" wildcard for biome id (meaning for all biomes).
	private static void loadBiomeLine(BlockColours bc, String[] split) {
		try {
			int startBiomeId = 0;
			int endBiomeId = MAX_BIOMES;
			if (!split[1].equals("*")) {
				startBiomeId = Integer.parseInt(split[1], 16);
				endBiomeId = startBiomeId + 1;
			}
			
			if ((startBiomeId >= 0) && (startBiomeId < MAX_BIOMES)) {
				int waterMultiplier = getColourFromString(split[2]) & 0xffffff;
				int grassMultiplier = getColourFromString(split[3]) & 0xffffff;
				int foliageMultiplier = getColourFromString(split[4]) & 0xffffff;
				
				for (int biomeId = startBiomeId; biomeId < endBiomeId; biomeId++) {
					bc.setBiomeWaterShading(biomeId, waterMultiplier);
					bc.setBiomeGrassShading(biomeId, grassMultiplier);
					bc.setBiomeFoliageShading(biomeId, foliageMultiplier);
				}
			} else {
				RegionManager.logWarning("biome ID '%02x' out of range", startBiomeId);
			}
			
		} catch (NumberFormatException e) {
			RegionManager.logWarning("invalid biome colour line '%s %s %s %s %s'", split[0], split[1], split[2], split[3], split[4]);
		}
	}
	
	// read block colour values.
	// line format is:
	//   block <blockId> <blockMeta> <colour>
	// the biome id, meta value, and colour code are in hex.
	// accepts "*" wildcard for biome id and meta (meaning for all blocks and/or meta values).
	private static void loadBlockLine(BlockColours bc, String[] split, boolean isBlockColourLine) {
		try {
			int startBlockId = 0;
			int endBlockId = MAX_BLOCKS;
			if (!split[1].equals("*")) {
				startBlockId = Integer.parseInt(split[1], 16);
				endBlockId = startBlockId + 1;
			}
			
			int startBlockMeta = 0;
			int endBlockMeta = MAX_META;
			if (!split[2].equals("*")) {
				startBlockMeta = Integer.parseInt(split[2], 16);
				endBlockMeta = startBlockMeta + 1;
			}
			
			if ((startBlockId >= 0) && (startBlockId < MAX_BLOCKS) && (startBlockMeta >= 0) && (startBlockMeta < MAX_META)) {
				if (isBlockColourLine) {
					// block colour line
					int colour = getColourFromString(split[3]);
					
					for (int blockId = startBlockId; blockId < endBlockId; blockId++) {
						for (int blockMeta = startBlockMeta; blockMeta < endBlockMeta; blockMeta++) {
							bc.setColour(blockId, blockMeta, colour);
						}
					}
				} else {
					// block type line
					BlockType type = getBlockTypeFromString(split[3]);
					
					for (int blockId = startBlockId; blockId < endBlockId; blockId++) {
						for (int blockMeta = startBlockMeta; blockMeta < endBlockMeta; blockMeta++) {
							bc.setBlockType(blockId, blockMeta, type);
						}
					}
				}
			}
			
		} catch (NumberFormatException e) {
			RegionManager.logWarning("invalid block colour line '%s %s %s %s'", split[0], split[1], split[2], split[3]);
		}
	}
	
	// load block colours and biome colour multipliers from a file.
	public static BlockColours loadFromFile(File f) {
		BlockColours bc = new BlockColours();
		
		Scanner fin = null;
		try {
			fin = new Scanner(new FileReader(f));
			
			while (fin.hasNextLine()) {
				String line = fin.nextLine().trim();
				if (line.length() > 0) {
					String[] lineSplit = line.split(" ");
					if (lineSplit[0].equals("biome") && (lineSplit.length == 5)) {
						loadBiomeLine(bc, lineSplit);
					} else if (lineSplit[0].equals("block") && (lineSplit.length == 4)) {
						loadBlockLine(bc, lineSplit, true);
					} else if (lineSplit[0].equals("blocktype") && (lineSplit.length == 4)) {
						loadBlockLine(bc, lineSplit, false);
					} else {
						RegionManager.logWarning("invalid map colour line '%s'", line);
					}
				}
			}
		} catch (IOException e) {
			RegionManager.logError("loading block colours: no such file '%s'", f);
			
		} finally {
			if (fin != null) {
				fin.close();
			}
		}
		
		return bc;
	}
	
	//
	// Methods for saving block colours to file.
	//
	
	// save biome colour multipliers to a file.
	public void saveBiomes(Writer fout) throws IOException {
		fout.write("biome * ffffff ffffff ffffff\n");
		
		for (int biomeId = 0; biomeId < MAX_BIOMES; biomeId++) {
			int waterMultiplier = this.getWaterColourMultiplier(biomeId) & 0xffffff;
			int grassMultiplier = this.getGrassColourMultiplier(biomeId) & 0xffffff;
			int foliageMultiplier = this.getFoliageColourMultiplier(biomeId) & 0xffffff;
			
			// don't add lines that are covered by the default.
			if ((waterMultiplier != 0xffffff) || (grassMultiplier != 0xffffff) || (foliageMultiplier != 0xffffff)) {
				fout.write(String.format("biome %02x %06x %06x %06x\n", biomeId, waterMultiplier, grassMultiplier, foliageMultiplier));
			}
		}
	}
	
	private static String getMostOccurringKey(HashMap<String, Integer> map, String defaultItem) {
		// find the most commonly occurring key in a hash map.
		// only return a key if there is more than 1.
		int maxCount = 1;
		String mostOccurringKey = defaultItem;
		for (Entry<String, Integer> entry : map.entrySet()) {
			String key = entry.getKey();
			int count = entry.getValue();
			
			if (count > maxCount) {
				maxCount = count;
				mostOccurringKey = key;
			}
		}
		
		return mostOccurringKey;
	}
	
	// to use the least number of lines possible find the most commonly occurring
	// item for the 16 different meta values of a block.
	// an 'item' is either a block colour or a block type.
	// the most commonly occurring item is then used as the wildcard entry for
	// the block, and all non matching items added afterwards.
	private static void writeMinimalBlockLines(Writer fout, String lineStart, String[] items, String defaultItem) throws IOException {
		
		HashMap<String, Integer> frequencyMap = new HashMap<String, Integer>();
		
		// first count the number of occurrences of each item.
		for (String item : items) {
			int count = 0;
			if (frequencyMap.containsKey(item)) {
				count = frequencyMap.get(item);
			}
			frequencyMap.put(item, count + 1);
		}
		
		// then find the most commonly occurring item.
		String mostOccurringItem = getMostOccurringKey(frequencyMap, defaultItem);
		
		// only add a wildcard line if it actually saves lines.
		if (!mostOccurringItem.equals(defaultItem)) {
			fout.write(String.format("%s * %s\n", lineStart, mostOccurringItem));
		}
		
		// add lines for items that don't match the wildcard line.
		for (int i = 0; i < items.length; i++) {
			if (!items[i].equals(mostOccurringItem) && !items[i].equals(defaultItem)) {
				fout.write(String.format("%s %x %s\n", lineStart, i, items[i]));
			}
		}
	}
	
	public void saveBlocks(Writer fout) throws IOException {
		fout.write("block * * 00000000\n");
		
		String[] colours = new String[MAX_META];
		
		for (int blockId = 0; blockId < MAX_BLOCKS; blockId++) {
			// build a 16 element list of block colours
			for (int meta = 0; meta < MAX_META; meta++) {
				colours[meta] = String.format("%08x", this.getColour(blockId, meta));
			}
			// write a minimal representation to the file
			String lineStart = String.format("block %03x", blockId);
			writeMinimalBlockLines(fout, lineStart, colours, "00000000");
		}
	}
	
	public void saveBlockTypes(Writer fout) throws IOException {
		fout.write("blocktype * * normal\n");
		
		String[] blockTypes = new String[MAX_META];
		
		for (int blockId = 0; blockId < MAX_BLOCKS; blockId++) {
			// build a 16 element list of block types
			for (int meta = 0; meta < MAX_META; meta++) {
				BlockType bt = this.getBlockType(blockId, meta);
				blockTypes[meta] = getBlockTypeAsString(bt);
			}
			// write a minimal representation to the file
			String lineStart = String.format("blocktype %03x", blockId);
			writeMinimalBlockLines(fout, lineStart, blockTypes, "normal");
		}
	}
	
	// save block colours and biome colour multipliers to a file.
	public void saveToFile(File f) {
		Writer fout = null;
		try {
			fout = new OutputStreamWriter(new FileOutputStream(f));
			this.saveBiomes(fout);
			this.saveBlockTypes(fout);
			this.saveBlocks(fout);
			
		} catch (IOException e) {
			RegionManager.logError("saving block colours: could not write to '%s'", f);
			
		} finally {
			if (fout != null) {
				try {
					fout.close();
				} catch (IOException e) {}
			}
		}
		
	}
}
