package mapwriter.region;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import mapwriter.util.Render;
import net.minecraft.block.Block;

public class BlockColours {

	public static final int MAX_BLOCKS = 4096;
	public static final int MAX_META = 16;
	public static final int MAX_BIOMES = 256;

	public static final String biomeSectionString = "[biomes]";
	public static final String blockSectionString = "[blocks]";

	// private int[] bcArray = new int[MAX_BLOCKS * MAX_META];
	private int[] waterMultiplierArray = new int[MAX_BIOMES];
	private int[] grassMultiplierArray = new int[MAX_BIOMES];
	private int[] foliageMultiplierArray = new int[MAX_BIOMES];
	private LinkedHashMap<String, BlockData> bcMap = new LinkedHashMap<String, BlockData>();
	private LinkedHashMap<String, BlockData> bcOverrideMap = new LinkedHashMap<String, BlockData>();

	public enum BlockType {
		NORMAL, GRASS, LEAVES, FOLIAGE, WATER, OPAQUE
	}

	public BlockColours() {
		Arrays.fill(this.waterMultiplierArray, 0xffffff);
		Arrays.fill(this.grassMultiplierArray, 0xffffff);
		Arrays.fill(this.foliageMultiplierArray, 0xffffff);
	}

	public String CombineBlockMeta(String BlockName, int meta) {
		return BlockName + " " + meta;
	}

	public String CombineBlockMeta(String BlockName, String meta) {
		return BlockName + " " + meta;
	}

	public int getColour(String BlockName, int meta) {
		String BlockAndMeta = CombineBlockMeta(BlockName, meta);
		String BlockAndWildcard = CombineBlockMeta(BlockName, "*");

		BlockData data = new BlockData();

		if (this.bcMap.containsKey(BlockAndMeta)) {
			data = this.bcMap.get(BlockAndMeta);
		} else if (this.bcMap.containsKey(BlockAndWildcard)) {
			data = this.bcMap.get(BlockAndWildcard);
		}
		return data.color;
	}

	public int getColour(int BlockAndMeta) {
		Block block = Block.getBlockById(BlockAndMeta >> 4);
		int meta = BlockAndMeta & 0xf;
		return getColour(block.delegate.name(), meta);
	}

	public void setColour(String BlockName, String meta, int colour) {
		String BlockAndMeta = CombineBlockMeta(BlockName, meta);

		if (meta.equals("*")) {
			for (int i = 0; i < 16; i++) {
				setColour(BlockName, String.valueOf(i), colour);
			}
		}

		if (this.bcMap.containsKey(BlockAndMeta)) {
			BlockData data = this.bcMap.get(BlockAndMeta);
			data.color = colour;
		} else {
			BlockData data = new BlockData();
			data.color = colour;
			this.bcMap.put(BlockAndMeta, data);
		}
	}

	private int getGrassColourMultiplier(int biome) {
		return (this.grassMultiplierArray != null) && (biome >= 0)
				&& (biome < this.grassMultiplierArray.length) ? this.grassMultiplierArray[biome]
				: 0xffffff;
	}

	private int getWaterColourMultiplier(int biome) {
		return (this.waterMultiplierArray != null) && (biome >= 0)
				&& (biome < this.waterMultiplierArray.length) ? this.waterMultiplierArray[biome]
				: 0xffffff;
	}

	private int getFoliageColourMultiplier(int biome) {
		return (this.foliageMultiplierArray != null) && (biome >= 0)
				&& (biome < this.foliageMultiplierArray.length) ? this.foliageMultiplierArray[biome]
				: 0xffffff;
	}

	public int getBiomeColour(String BlockName, int meta, int biome) {
		int colourMultiplier = 0xffffff;

		if (this.bcMap.containsKey(CombineBlockMeta(BlockName, meta))) {
			switch (this.bcMap.get(CombineBlockMeta(BlockName, meta)).type) {
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
		}
		return colourMultiplier;
	}

	public int getBiomeColour(int BlockAndMeta, int biome) {
		Block block = Block.getBlockById(BlockAndMeta >> 4);
		int meta = BlockAndMeta & 0xf;
		return getBiomeColour(block.delegate.name(), meta, biome);
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

	public BlockType getBlockType(String BlockName, int meta) {
		String BlockAndMeta = CombineBlockMeta(BlockName, meta);
		String BlockAndWildcard = CombineBlockMeta(BlockName, "*");

		BlockData data = new BlockData();

		if (this.bcMap.containsKey(BlockAndMeta)) {
			data = this.bcMap.get(BlockAndMeta);
		} else if (this.bcMap.containsKey(BlockAndWildcard)) {
			data = this.bcMap.get(BlockAndWildcard);
		}
		return data.type;
	}

	public BlockType getBlockType(int BlockAndMeta) {
		Block block = Block.getBlockById(BlockAndMeta >> 4);
		int meta = BlockAndMeta & 0xf;
		return getBlockType(block.delegate.name(), meta);
	}

	public void setBlockType(String BlockName, String meta, BlockType type) {
		String BlockAndMeta = CombineBlockMeta(BlockName, meta);

		if (meta.equals("*")) {
			for (int i = 0; i < 16; i++) {
				setBlockType(BlockName, String.valueOf(i), type);
			}
			return;
		}

		if (this.bcMap.containsKey(BlockAndMeta)) {
			BlockData data = this.bcMap.get(BlockAndMeta);
			data.type = type;
			data.color = adjustBlockColourFromType(BlockName, meta, type,
					data.color);
		} else {
			BlockData data = new BlockData();
			data.type = type;
			this.bcMap.put(BlockAndMeta, data);
		}
	}

	private static int adjustBlockColourFromType(String BlockName, String meta,
			BlockType type, int blockColour) {
		// for normal blocks multiply the block colour by the render colour.
		// for other blocks the block colour will be multiplied by the biome
		// colour.
		Block block = Block.getBlockFromName(BlockName);

		switch (type) {

		case OPAQUE:
			blockColour |= 0xff000000;
		case NORMAL:
			// fix crash when mods don't implement getRenderColor for all
			// block meta values.
			try {
				int renderColour = block.getRenderColor(block
						.getStateFromMeta(Integer.parseInt(meta) & 0xf));
				if (renderColour != 0xffffff) {
					blockColour = Render.multiplyColours(blockColour,
							0xff000000 | renderColour);
				}
			} catch (RuntimeException e) {
				// do nothing
			}
			break;
		case LEAVES:
			// leaves look weird on the map if they are not opaque.
			// they also look too dark if the render colour is applied.
			blockColour |= 0xff000000;
			break;
		case GRASS:
			//the icon returns the dirt texture so hardcode it to the grey undertexture.
			blockColour = 0xff9b9b9b;
		default:
			break;
		}
		return blockColour;
	}

	public static int getColourFromString(String s) {
		return (int) (Long.parseLong(s, 16) & 0xffffffffL);
	}

	//
	// Methods for loading block colours from file:
	//

	// read biome colour multiplier values.
	// line format is:
	// biome <biomeId> <waterMultiplier> <grassMultiplier> <foliageMultiplier>
	// accepts "*" wildcard for biome id (meaning for all biomes).
	private void loadBiomeLine(String[] split) {
		try {
			int startBiomeId = 0;
			int endBiomeId = MAX_BIOMES;
			if (!split[1].equals("*")) {
				startBiomeId = Integer.parseInt(split[1]);
				endBiomeId = startBiomeId + 1;
			}

			if ((startBiomeId >= 0) && (startBiomeId < MAX_BIOMES)) {
				int waterMultiplier = getColourFromString(split[2]) & 0xffffff;
				int grassMultiplier = getColourFromString(split[3]) & 0xffffff;
				int foliageMultiplier = getColourFromString(split[4]) & 0xffffff;

				for (int biomeId = startBiomeId; biomeId < endBiomeId; biomeId++) {
					this.setBiomeWaterShading(biomeId, waterMultiplier);
					this.setBiomeGrassShading(biomeId, grassMultiplier);
					this.setBiomeFoliageShading(biomeId, foliageMultiplier);
				}
			} else {
				RegionManager.logWarning("biome ID '%d' out of range",
						startBiomeId);
			}

		} catch (NumberFormatException e) {
			RegionManager.logWarning(
					"invalid biome colour line '%s %s %s %s %s'", split[0],
					split[1], split[2], split[3], split[4]);
		}
	}

	// read block colour values.
	// line format is:
	// block <blockName> <blockMeta> <colour>
	// the biome id, meta value, and colour code are in hex.
	// accepts "*" wildcard for biome id and meta (meaning for all blocks and/or
	// meta values).
	private void loadBlockLine(String[] split) {
		try {
			// block colour line
			int colour = getColourFromString(split[3]);
			this.setColour(split[1], split[2], colour);

		} catch (NumberFormatException e) {
			RegionManager.logWarning("invalid block colour line '%s %s %s %s'",
					split[0], split[1], split[2], split[3]);
		}
	}

	private void loadBlockTypeLine(String[] split) {
		try {
			// block type line
			BlockType type = getBlockTypeFromString(split[3]);
			this.setBlockType(split[1], split[2], type);
		} catch (NumberFormatException e) {
			RegionManager.logWarning("invalid block colour line '%s %s %s %s'",
					split[0], split[1], split[2], split[3]);
		}
	}

	public void loadFromFile(File f) {
		Scanner fin = null;
		try {
			fin = new Scanner(new FileReader(f));

			while (fin.hasNextLine()) {
				// get next line and remove comments (part of line after #)
				String line = fin.nextLine().split("#")[0].trim();
				if (line.length() > 0) {
					String[] lineSplit = line.split(" ");
					if (lineSplit[0].equals("biome") && (lineSplit.length == 5)) {
						this.loadBiomeLine(lineSplit);
					} else if (lineSplit[0].equals("block")
							&& (lineSplit.length == 4)) {
						this.loadBlockLine(lineSplit);
					} else if (lineSplit[0].equals("blocktype")
							&& (lineSplit.length == 4)) {
						this.loadBlockTypeLine(lineSplit);
					} else {
						RegionManager.logWarning(
								"invalid map colour line '%s'", line);
					}
				}
			}
		} catch (IOException e) {
			RegionManager.logError("loading block colours: no such file '%s'",
					f);

		} finally {
			if (fin != null) {
				fin.close();
			}
		}
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
			if ((waterMultiplier != 0xffffff) || (grassMultiplier != 0xffffff)
					|| (foliageMultiplier != 0xffffff)) {
				fout.write(String.format("biome %d %06x %06x %06x\n", biomeId,
						waterMultiplier, grassMultiplier, foliageMultiplier));
			}
		}
	}

	private static String getMostOccurringKey(Map<String, Integer> map,
			String defaultItem) {
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

	// to use the least number of lines possible find the most commonly
	// occurring
	// item for the different meta values of a block.
	// an 'item' is either a block colour or a block type.
	// the most commonly occurring item is then used as the wildcard entry for
	// the block, and all non matching items added afterwards.
	private static void writeMinimalBlockLines(Writer fout, String lineStart,
			List<String> items, String defaultItem) throws IOException {

		Map<String, Integer> frequencyMap = new HashMap<String, Integer>();

		// first count the number of occurrences of each item.
		for (String item : items) {
			int count = 0;
			if (frequencyMap.containsKey(item)) {
				count = frequencyMap.get(item);
			}
			frequencyMap.put(item, count + 1);
		}

		// then find the most commonly occurring item.
		String mostOccurringItem = getMostOccurringKey(frequencyMap,
				defaultItem);

		// only add a wildcard line if it actually saves lines.
		if (!mostOccurringItem.equals(defaultItem)) {
			fout.write(String.format("%s * %s\n", lineStart, mostOccurringItem));
		}

		// add lines for items that don't match the wildcard line.

		int meta = 0;
		for (String s : items) {
			if (!s.equals(mostOccurringItem) && !s.equals(defaultItem)) {
				fout.write(String.format("%s %d %s\n", lineStart, meta, s));
			}
			meta++;
		}
	}

	public void saveBlocks(Writer fout) throws IOException {
		fout.write("block * * 00000000\n");

		String LastBlock = "";
		List<String> colours = new ArrayList<String>();

		for (Map.Entry<String, BlockData> entry : bcMap.entrySet()) {
			String[] BlockAndMeta = entry.getKey().split(" ");
			String block = BlockAndMeta[0];
			String meta = BlockAndMeta[1];

			String color = String.format("%08x", entry.getValue().color);

			if (!LastBlock.equals(block) && !LastBlock.isEmpty()) {
				String lineStart = String.format("block %s", LastBlock);
				writeMinimalBlockLines(fout, lineStart, colours, "00000000");

				colours.clear();
			}

			colours.add(color);
			LastBlock = block;
		}
	}

	public void saveBlockTypes(Writer fout) throws IOException {
		fout.write("blocktype * * normal\n");

		String LastBlock = "";
		List<String> blockTypes = new ArrayList<String>();

		for (Map.Entry<String, BlockData> entry : bcMap.entrySet()) {
			String[] BlockAndMeta = entry.getKey().split(" ");
			String block = BlockAndMeta[0];
			String meta = BlockAndMeta[1];

			String Type = getBlockTypeAsString(entry.getValue().type);

			if (!LastBlock.equals(block) && !LastBlock.isEmpty()) {
				String lineStart = String.format("blocktype %s", LastBlock);
				writeMinimalBlockLines(fout, lineStart, blockTypes,
						getBlockTypeAsString(BlockType.NORMAL));

				blockTypes.clear();
			}

			blockTypes.add(Type);
			LastBlock = block;
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
			RegionManager.logError(
					"saving block colours: could not write to '%s'", f);

		} finally {
			if (fout != null) {
				try {
					fout.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public static void writeOverridesFile(File f) {
		Writer fout = null;
		try {
			fout = new OutputStreamWriter(new FileOutputStream(f));

			fout.write("block minecraft:yellow_flower * 60ffff00	# make dandelions more yellow\n"
					+ "block minecraft:red_flower 0 60ff0000		# make poppy more red\n"
					+ "block minecraft:red_flower 1 601c92d6		# make Blue Orchid more red\n"
					+ "block minecraft:red_flower 2 60b865fb		# make Allium more red\n"
					+ "block minecraft:red_flower 3 60e4eaf2		# make Azure Bluet more red\n"
					+ "block minecraft:red_flower 4 60d33a17		# make Red Tulip more red\n"
					+ "block minecraft:red_flower 5 60e17124		# make Orange Tulip more red\n"
					+ "block minecraft:red_flower 6 60ffffff		# make White Tulip more red\n"
					+ "block minecraft:red_flower 7 60eabeea		# make Pink Tulip more red\n"
					+ "block minecraft:red_flower 8 60eae6ad		# make Oxeye Daisy more red\n"
					+ "block minecraft:double_plant 0 60ffff00		# make Sunflower more Yellow-orrange\n"
					+ "block minecraft:double_plant 1 d09f78a4		# make Lilac more pink\n"
					+ "block minecraft:double_plant 4 60ff0000		# make Rose Bush more red\n"
					+ "block minecraft:double_plant 5 d0e3b8f7		# make Peony more red\n"
					+ "blocktype minecraft:grass * grass			# grass block\n"
					+ "blocktype minecraft:flowing_water * water	# flowing water block\n"
					+ "blocktype minecraft:water * water			# still water block\n"
					+ "blocktype minecraft:leaves * leaves    		# leaves block\n"
					+ "blocktype minecraft:leaves 1 opaque    		# pine leaves (not biome colorized)\n"
					+ "blocktype minecraft:leaves 2 opaque    		# birch leaves (not biome colorized)\n"
					+ "blocktype minecraft:tallgrass * grass     	# tall grass block\n"
					+ "blocktype minecraft:vine * foliage  			# vines block\n");
			//TODO: Find out the names and readd these overwrites
			// + "blocktype 169 * grass    						# biomes o plenty holy grass\n"
			// + "blocktype 1920 * grass   						# biomes o plenty plant\n"
			// + "blocktype 1923 * opaque  						# biomes o plenty leaves 1\n"
			// + "blocktype 1924 * opaque  						# biomes o plenty leaves 2\n"
			// + "blocktype 1925 * foliage 						# biomes o plenty foliage\n"
			// + "blocktype 1926 * opaque  						# biomes o plenty fruit leaf block\n"
			// + "blocktype 1932 * foliage 						# biomes o plenty tree moss\n"
			// + "blocktype 1962 * leaves  						# biomes o plenty colorized leaves\n"
			// + "blocktype 2164 * leaves  						# twilight forest leaves\n"
			// + "blocktype 2177 * leaves  						# twilight forest magic leaves\n"
			// + "blocktype 2204 * leaves  						# extrabiomesXL green leaves\n"
			// + "blocktype 2200 * opaque  						# extrabiomesXL autumn leaves\n"
			// + "blocktype 3257 * opaque  						# natura berry bush\n"
			// + "blocktype 3272 * opaque  						# natura darkwood leaves\n"
			// + "blocktype 3259 * leaves  						# natura flora leaves\n"
			// + "blocktype 3278 * opaque 						# natura rare leaves\n"
			// + "blocktype 3258 * opaque  						# natura sakura leaves\n"
		} catch (IOException e) {
			RegionManager.logError(
					"saving block overrides: could not write to '%s'", f);

		} finally {
			if (fout != null) {
				try {
					fout.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public class BlockData {
		public int color = 0;
		public BlockType type = BlockType.NORMAL;
	}
}
