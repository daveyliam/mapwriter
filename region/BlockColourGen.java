package mapwriter.region;

import mapwriter.Mw;
import mapwriter.MwUtil;
import mapwriter.Render;
import mapwriter.forge.MwConfig;
import mapwriter.map.Texture;
import mapwriter.region.BlockColours.BlockType;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Icon;
import net.minecraft.world.biome.BiomeGenBase;

// Static class to generate BlockColours.
// This is separate from BlockColours because it needs to run in the GL rendering thread
// whereas the generated BlockColours object is used only in the background thread.
// So basically split to make it clear that BlockColourGen and the generated BlockColours
// must not have any interaction after it is generated.

public class BlockColourGen {
	
	public static final String catBlockType = "blocktype";
	public static final String catBlockColour = "blockcolour";
	
	private static BlockColours bc = null;
		
	public static BlockColours genBlockColours(Mw mw, MwConfig config) {
		bc = new BlockColours();
		
		// load block types
		// must be called before genFromTextures
		getConfigBlockTypes(config);
		
		// generate block colours from textures
		genFromTextures(mw);
		// load block colour overrides
		getConfigBlockColours(config);
		
		//for (int i = 0; i < 256; i++) {
		//	MwUtil.log("block %03x:%x colour = %08x", i, 0, bc.getColour(i, 0));
		//}
		
		BlockColours bcReturn = bc;
		bc = null;
		
		return bcReturn;
	}
	
	private static void setBlockTypeFromString(int blockAndMeta, String typeString) {
		BlockType blockType = BlockType.NORMAL;
		if (typeString.equalsIgnoreCase("grass")) {
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
			MwUtil.log("unknown block type '%s' for block %03x:%01x", typeString, blockAndMeta >> 4, blockAndMeta & 0xf);
		}
		bc.setBlockTypeForBlockAndMeta(blockAndMeta, blockType);
	}
	
	private static void getConfigBlockColours(MwConfig config) {
		//MwUtil.log("reading block colour overrides");
		
		// set default overrides
		if (!config.hasKey(catBlockColour, "025"))
			config.setColour(catBlockColour, "025", 0x60ffff00, "make dandelions more yellow");
		if (!config.hasKey(catBlockColour, "026"))
			config.setColour(catBlockColour, "026", 0x60ff0000, "make roses more red");
		
		for (int blockID = 0; blockID < Block.blocksList.length; blockID++) {
			long defaultColour = -1;
			String key = String.format("%03x", blockID);
			defaultColour = config.getColour(catBlockColour, key);
			for (int dv = 0; dv < 16; dv++) {
				key = String.format("%03x:%01x", blockID, dv);
				// only "get" config value if it exists, otherwise an entry will
				// be created for every colour read
				long newColour = defaultColour;
				newColour = config.getColour(catBlockColour, key);
				if (newColour >= 0L) {
					bc.setColour(blockID, dv, (int) (newColour & 0xffffffffL));
				}
			}
		}
		config.addCustomCategoryComment(catBlockColour,
				"each entry should be of the form S:III:D=AARRGGBB\n\n" +
				"where:  III is the block ID as a 3 digit hex number\n" +
				"        D is the optional metadata value\n" +
				"        AA is 2 digit hex number for the alpha channel\n" +
				"        RR is 2 digit hex number for the red channel\n" +
				"        GG is 2 digit hex number for the blue channel\n" +
				"        BB is 2 digit hex number for the green channel\n\n" +
				" set the alpha channel to ff for opaque blocks.\n" +
				" if you do not add an alpha value then the block will be\n" +
				" completely transparent.\n\n" +
				" some example entries:\n" +
				"   S:002=ffff00ff   # to make grass purple\n" +
				"   S:003=80945d32   # to make dirt 50% translucent\n + " +
				"   S:011:2=ff0000ff # to make birch wood blue\n");
		
		//MwUtil.log("done reading block colour overrides");
	}
	
	private static void getConfigBlockTypes(MwConfig config) {
		//MwUtil.log("reading block types");
		
		// set default overrides
		config.setSingleWord(catBlockType, "002", "grass", "grass block");
		config.setSingleWord(catBlockType, "008", "water", "still water block");
		config.setSingleWord(catBlockType, "009", "water", "flowing water block");
		config.setSingleWord(catBlockType, "012", "leaves", "leaves block");
		config.setSingleWord(catBlockType, "01f", "grass", "tall grass block");
		config.setSingleWord(catBlockType, "06a", "foliage", "vines block");
		config.setSingleWord(catBlockType, "0a9", "grass", "biomes o plenty holy grass");
		config.setSingleWord(catBlockType, "780", "grass", "biomes o plenty plant");
		config.setSingleWord(catBlockType, "783", "opaque", "biomes o plenty leaves 1");
		config.setSingleWord(catBlockType, "784", "opaque", "biomes o plenty leaves 2");
		config.setSingleWord(catBlockType, "785", "foliage", "biomes o plenty foliage");
		config.setSingleWord(catBlockType, "786", "opaque", "biomes o plenty fruit leaf block");
		config.setSingleWord(catBlockType, "78c", "foliage", "biomes o plenty tree moss");
		config.setSingleWord(catBlockType, "7aa", "leaves", "biomes o plenty colorized leaves");
		config.setSingleWord(catBlockType, "874", "leaves", "twilight forest leaves");
		config.setSingleWord(catBlockType, "881", "leaves", "twilight forest magic leaves");
		config.setSingleWord(catBlockType, "89c", "leaves", "extrabiomesXL green leaves");
		config.setSingleWord(catBlockType, "898", "opaque", "extrabiomesXL autumn leaves");
		
		for (int blockID = 0; blockID < Block.blocksList.length; blockID++) {
			String defaultType = "";
			String key = String.format("%03x", blockID);
			defaultType = config.getSingleWord(catBlockType, key);
			//MwUtil.log("read block type from config: block %03x = %s",
			//		blockID, defaultType);
			for (int dv = 0; dv < 16; dv++) {
				key = String.format("%03x:%01x", blockID, dv);
				// only "get" config value if it exists, otherwise an entry will
				// be created for every colour read
				String typeString = defaultType;
				String s = config.getSingleWord(catBlockType, key);
				if (s.length() > 0) {
					typeString = s;
				}
				if (typeString.length() > 0) {
					//MwUtil.log("setting block type from config: block %03x:%01x = %s",
					//		blockID, dv, typeString);
					setBlockTypeFromString(blockID << 4 | dv, typeString);
				}
			}
		}
		config.addCustomCategoryComment(catBlockType,
				"each entry should be of the form S:III:D=typeString\n\n" +
				"where:  III is the block ID as a 3 digit hex number\n" +
				"        D is the optional metadata value\n" +
				"        typeString is the block type\n\n" +
				"typeString must be one of:\n" +
				"  grass   | block coloured with biome grass colour\n" +
				"  leaves  | block coloured with biome leaves colour and set to opaque\n" +
				"  foliage | block coloured with biome leaves colour\n" +
				"  water   | block coloured with biome water colour\n" +
				"  opaque  | block set to opaque only, no biome shading\n");
		
		//MwUtil.log("done reading block type overrides");
	}
	
	private static int getIconMapColour(Icon icon, Texture terrainTexture) {
		//MwUtil.log("block %03x:%01x: (%f, %f, %f, %f)", blockID, dv, u1, v1, u2, v2); 
		
		int iconX = icon.getOriginX();
		int iconY = icon.getOriginY();
		float iconWidthF = ((float) icon.getSheetWidth()) * (icon.getMaxU() - icon.getMinU());
		float iconHeightF = ((float) icon.getSheetHeight()) * (icon.getMaxV() - icon.getMinV());
		int iconWidth = (int) (iconWidthF);
		int iconHeight = (int) (iconHeightF);
		
		int[] pixels = new int[iconWidth * iconHeight];
		
		terrainTexture.getRGB(iconX, iconY, iconWidth, iconHeight, pixels, 0, iconWidth);
		
		// need to use custom averaging routine rather than scaling down to one pixel to
		// stop transparent pixel colours being included in the average.
		return Render.getAverageColourOfArray(pixels);
	}
	
	private static int adjustBlockColourFromType(int blockAndMeta, int blockColour) {
		// for normal blocks multiply the block colour by the render colour.
		// for other blocks the block colour will be multiplied by the biome colour.
		Block block = Block.blocksList[blockAndMeta >> 4];
		BlockType blockType = bc.getBlockType(blockAndMeta);
		switch (blockType) {
		case NORMAL:
			int renderColour = block.getRenderColor(blockAndMeta & 0xf);
			if (renderColour != 0xffffff) {
				blockColour = Render.multiplyColours(blockColour, 0xff000000 | renderColour);
			}
			break;
		case LEAVES:
		case OPAQUE:
			blockColour |= 0xff000000;
			break;
		default:
			break;
		}
		return blockColour;
	}
	
	private static void genFromTextures(Mw mw) {
		
		MwUtil.log("generating block map colours from textures");

		// copy terrain texture to MwRender pixel bytebuffer
		int terrainTextureID = Minecraft.getMinecraft().renderEngine.getTexture("/terrain.png");
		Texture terrainTexture = new Texture(terrainTextureID);
		
		double u1Last = 0;
		double u2Last = 0;
		double v1Last = 0;
		double v2Last = 0;
		int blockColourLast = 0;
		int e_count = 0;
		int b_count = 0;
		int s_count = 0;
		
		for (int blockID = 0; blockID < Block.blocksList.length; blockID++) {
			for (int dv = 0; dv < 16; dv++) {
				
				int blockAndMeta = ((blockID & 0xfff) << 4) | (dv & 0xf);
				Block block = Block.blocksList[blockID];
				int blockColour = 0;
				
				if (block != null) {
					
					Icon icon = null;
					try {
						icon = block.getIcon(1, dv);
					} catch (Exception e) {
						//MwUtil.log("genFromTextures: exception caught when requesting block texture for %03x:%x", blockID, dv);
						//e.printStackTrace();
						e_count++;
					}
					
					if (icon != null) {
						double u1 = icon.getMinU();
						double u2 = icon.getMaxU();
						double v1 = icon.getMinV();
						double v2 = icon.getMaxV();
						
						if ((u1 == u1Last) && (u2 == u2Last) && (v1 == v1Last) && (v2 == v2Last)) {
							blockColour = blockColourLast;
							s_count++;
						} else {
							blockColour = getIconMapColour(icon, terrainTexture);
							u1Last = u1;
							u2Last = u2;
							v1Last = v1;
							v2Last = v2;
							blockColourLast = blockColour;
							b_count++;
						}
						//if (dv == 0)
						//	MwUtil.log("block %03x:%x colour = %08x", blockID, dv, blockColour);
					}
					blockColour = adjustBlockColourFromType(blockAndMeta, blockColour);
				}	
				bc.setColour(blockAndMeta, blockColour);
			}
		}
		
		MwUtil.log("processed %d block textures, %d skipped, %d exceptions", b_count, s_count, e_count);
		
		genBiomeColours();
	}
	
	private static void genBiomeColours() {
		// generate array of foliage, grass, and water colour multipliers
		// for each biome.
		bc.clearBiomeArrays();
		
		for (int i = 0; i < BiomeGenBase.biomeList.length; i++) {
			if (BiomeGenBase.biomeList[i] != null) {
				bc.setBiomeWaterShading(i, BiomeGenBase.biomeList[i].getWaterColorMultiplier() & 0xffffff);
				bc.setBiomeGrassShading(i, BiomeGenBase.biomeList[i].getBiomeGrassColor() & 0xffffff);
				bc.setBiomeFoliageShading(i, BiomeGenBase.biomeList[i].getBiomeFoliageColor() & 0xffffff);
			} else {
				bc.setBiomeWaterShading(i, 0xffffff);
				bc.setBiomeGrassShading(i, 0xffffff);
				bc.setBiomeFoliageShading(i, 0xffffff);
			}
		}
	}
}
