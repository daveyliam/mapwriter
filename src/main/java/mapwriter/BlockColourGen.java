package mapwriter;

import mapwriter.region.BlockColours;
import mapwriter.region.BlockColours.BlockType;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.world.biome.BiomeGenBase;

import java.util.HashMap;

// Static class to generate BlockColours.
// This is separate from BlockColours because it needs to run in the GL rendering thread
// whereas the generated BlockColours object is used only in the background thread.
// So basically split to make it clear that BlockColourGen and the generated BlockColours
// must not have any interaction after it is generated.

public class BlockColourGen {
	
	private static int getIconMapColour(Texture terrainTexture, double minU, double maxU, double minV, double maxV) {
		// flipped icons have the U and V coords reversed (minU > maxU, minV > maxV).
		// thanks go to taelnia for fixing this. 
		int iconX = (int) Math.round(((float) terrainTexture.w) * Math.min(minU, maxU));
		int iconY = (int) Math.round(((float) terrainTexture.h) * Math.min(minV, maxV));
		int iconWidth = (int) Math.round(((float) terrainTexture.w) * Math.abs(maxU - minU));
		int iconHeight = (int) Math.round(((float) terrainTexture.h) * Math.abs(maxV - minV));
		
		int[] pixels = new int[iconWidth * iconHeight];
		
		//MwUtil.log("(%d, %d) %dx%d", iconX, iconY, iconWidth, iconHeight);

		terrainTexture.getRGB(iconX, iconY, iconWidth, iconHeight, pixels, 0, iconWidth);
		
		// need to use custom averaging routine rather than scaling down to one pixel to
		// stop transparent pixel colours being included in the average.
		return Render.getAverageColourOfArray(pixels);
	}
	
	private static int adjustBlockColourFromType(BlockColours bc, int blockAndMeta, int blockColour) {
		// for normal blocks multiply the block colour by the render colour.
		// for other blocks the block colour will be multiplied by the biome colour.
		int blockid = blockAndMeta >> 4;
		//Block block = (Block) Block.blockRegistry.getObjectById(blockid);
		Block block = Block.blocksList[blockid];
		BlockType blockType = bc.getBlockType(blockAndMeta);
		switch (blockType) {
		
		case OPAQUE:
			blockColour |= 0xff000000;
		case NORMAL:
			// fix crash when mods don't implement getRenderColor for all
			// block meta values.
			try {
				int renderColour = block.getRenderColor(blockAndMeta & 0xf);
				if (renderColour != 0xffffff) {
					blockColour = Render.multiplyColours(blockColour, 0xff000000 | renderColour);
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
		default:
			break;
		}
		return blockColour;
	}
	
	private static void genBiomeColours(BlockColours bc) {
		// generate array of foliage, grass, and water colour multipliers
		// for each biome.
		
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
	
	public static void genBlockColours(BlockColours bc) {
		
		MwUtil.log("generating block map colours from textures");
		
		double u1Last = 0;
		double u2Last = 0;
		double v1Last = 0;
		double v2Last = 0;
		int blockColourLast = 0;
		int e_count = 0;
		int b_count = 0;
		int s_count = 0;

		HashMap<String, Texture> terrainTextures = new HashMap<String, Texture>();

		for (int blockID = 0; blockID < Block.blocksList.length; blockID++) {
			for (int dv = 0; dv < 16; dv++) {
				
				int blockAndMeta = ((blockID & 0xfff) << 4) | (dv & 0xf);
				Block block = Block.blocksList[blockID];
				int blockColour = 0;
				
				if (block != null) {
					
					//IIcon icon = null;
					int icon = -1;
					try {
						icon = block.getBlockTextureFromSideAndMetadata(1, dv);
					} catch (Exception e) {
						//MwUtil.log("genFromTextures: exception caught when requesting block texture for %03x:%x", blockID, dv);
						//e.printStackTrace();
						e_count++;
					}

					if (icon >= 0) {
						// From RenderBlocks.renderTopFace. Gets UV coordinates for top face of block
						int texOffsetX = (icon & 15) << 4;
						int texOffsetY = icon & 240;
						double u1 = ((double)texOffsetX + block.getBlockBoundsMinX() * 16.0D) / 256.0D;
						double u2 = ((double)texOffsetX + block.getBlockBoundsMaxX() * 16.0D - 0.01D) / 256.0D;
						double v1 = ((double)texOffsetY + block.getBlockBoundsMinZ() * 16.0D) / 256.0D;
						double v2 = ((double)texOffsetY + block.getBlockBoundsMaxZ() * 16.0D - 0.01D) / 256.0D;
						
						if ((u1 == u1Last) && (u2 == u2Last) && (v1 == v1Last) && (v2 == v2Last)) {
							blockColour = blockColourLast;
							s_count++;
						} else {
							// Get Texture object needed by Block
							String textureFile = block.getTextureFile();
							Texture terrainTexture = terrainTextures.get(textureFile);

							if(terrainTexture == null) {
								// Texture object not created yet, create it
								int terrainTextureId = Minecraft.getMinecraft().renderEngine.getTexture(textureFile);

								if (terrainTextureId != 0) {
									//MwUtil.log("genFromTextures: loaded terrain texture '" + textureFile + "'");
									terrainTexture = new Texture(terrainTextureId);
									terrainTextures.put(textureFile, terrainTexture);
								}
							}

							if(terrainTexture == null) {
								//MwUtil.log("genFromTextures: could not get terrain texture for %03x:%x", blockID, dv);
								e_count++;
							}
							else {
								blockColour = getIconMapColour(terrainTexture, u1, u2, v1, v2);
								u1Last = u1;
								u2Last = u2;
								v1Last = v1;
								v2Last = v2;
								blockColourLast = blockColour;
								b_count++;
							}
						}
						//if (dv == 0)
						//	MwUtil.log("block %03x:%x colour = %08x", blockID, dv, blockColour);
					}
					
					// doesn't work as some leaves blocks aren't rendered using the biome
					// foliage colour
					//try {
					//	if (block.isLeaves(null, 0, 0, 0)) {
					//		bc.setBlockType(blockAndMeta, BlockType.LEAVES);
					//	}
					//} catch (NullPointerException e) {
					//}
					
					blockColour = adjustBlockColourFromType(bc, blockAndMeta, blockColour);
				}
				bc.setColour(blockAndMeta, blockColour);
			}
		}
		
		MwUtil.log("processed %d block textures, %d skipped, %d exceptions", b_count, s_count, e_count);
		
		genBiomeColours(bc);
	}
}
