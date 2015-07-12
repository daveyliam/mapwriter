package mapwriter;

import mapwriter.region.BlockColours;
import mapwriter.region.BlockColours.BlockType;
import mapwriter.util.Logging;
import mapwriter.util.Render;
import mapwriter.util.Texture;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ColorizerFoliage;
import net.minecraft.world.ColorizerGrass;
import net.minecraft.world.biome.BiomeGenBase;

// Static class to generate BlockColours.
// This is separate from BlockColours because it needs to run in the GL rendering thread
// whereas the generated BlockColours object is used only in the background thread.
// So basically split to make it clear that BlockColourGen and the generated BlockColours
// must not have any interaction after it is generated.

public class BlockColourGen {

	private static int getIconMapColour(TextureAtlasSprite icon,
			Texture terrainTexture) {
		// flipped icons have the U and V coords reversed (minU > maxU, minV >
		// maxV).
		// thanks go to taelnia for fixing this.
		int iconX = (int) Math.round(((float) terrainTexture.w)
				* Math.min(icon.getMinU(), icon.getMaxU()));
		int iconY = (int) Math.round(((float) terrainTexture.h)
				* Math.min(icon.getMinV(), icon.getMaxV()));
		int iconWidth = (int) Math.round(((float) terrainTexture.w)
				* Math.abs(icon.getMaxU() - icon.getMinU()));
		int iconHeight = (int) Math.round(((float) terrainTexture.h)
				* Math.abs(icon.getMaxV() - icon.getMinV()));

		int[] pixels = new int[iconWidth * iconHeight];

		// MwUtil.log("(%d, %d) %dx%d", iconX, iconY, iconWidth, iconHeight);

		terrainTexture.getRGB(iconX, iconY, iconWidth, iconHeight, pixels, 0,
				iconWidth, icon);

		// need to use custom averaging routine rather than scaling down to one
		// pixel to
		// stop transparent pixel colours being included in the average.
		return Render.getAverageColourOfArray(pixels);
	}

	private static void genBiomeColours(BlockColours bc) {
		// generate array of foliage, grass, and water colour multipliers
		// for each biome.

		for (int i = 0; i < BiomeGenBase.getBiomeGenArray().length; i++) {
			if (BiomeGenBase.getBiomeGenArray()[i] != null) {
				bc.setBiomeWaterShading(i, BiomeGenBase.getBiomeGenArray()[i]
						.getWaterColorMultiplier() & 0xffffff);

				double temp = MathHelper.clamp_float(
						BiomeGenBase.getBiomeGenArray()[i].temperature, 0.0F,
						1.0F);
				double rain = MathHelper
						.clamp_float(
								BiomeGenBase.getBiomeGenArray()[i].rainfall,
								0.0F, 1.0F);
				int grasscolor = ColorizerGrass.getGrassColor(temp, rain);
				int foliagecolor = ColorizerFoliage.getFoliageColor(temp, rain);

				bc.setBiomeGrassShading(i, grasscolor & 0xffffff);
				bc.setBiomeFoliageShading(i, foliagecolor & 0xffffff);
			} else {
				bc.setBiomeWaterShading(i, 0xffffff);
				bc.setBiomeGrassShading(i, 0xffffff);
				bc.setBiomeFoliageShading(i, 0xffffff);
			}
		}
	}

	public static void genBlockColours(BlockColours bc) {

		Logging.log("generating block map colours from textures");

		// copy terrain texture to MwRender pixel bytebuffer

		// bind the terrain texture
		// Minecraft.getMinecraft().func_110434_K().func_110577_a(TextureMap.field_110575_b);
		// get the bound texture id
		// int terrainTextureId = Render.getBoundTextureId();

		int terrainTextureId = Minecraft.getMinecraft().renderEngine
				.getTexture(TextureMap.locationBlocksTexture).getGlTextureId();

		// create texture object from the currently bound GL texture
		if (terrainTextureId == 0) {
			Logging.log("error: could get terrain texture ID");
			return;
		}
		Texture terrainTexture = new Texture(terrainTextureId);

		double u1Last = 0;
		double u2Last = 0;
		double v1Last = 0;
		double v2Last = 0;
		int blockColourLast = 0;
		int e_count = 0;
		int b_count = 0;
		int s_count = 0;

		for (Object oblock : Block.blockRegistry) {
			Block block = (Block) oblock;
			int blockID = Block.getIdFromBlock(block);
			if (blockID == 0) {
				continue;
			}
			for (int dv = 0; dv < 16; dv++) {

				int blockAndMeta = ((blockID & 0xfff) << 4) | (dv & 0xf);
				int blockColour = 0;

				if (block != null) {

					TextureAtlasSprite icon = null;
					try {
						icon = Minecraft.getMinecraft()
								.getBlockRendererDispatcher()
								.getBlockModelShapes()
								.getTexture(block.getStateFromMeta(dv));
					} catch (Exception e) {
						// MwUtil.log("genFromTextures: exception caught when requesting block texture for %03x:%x",
						// blockID, dv);
						// e.printStackTrace();
						e_count++;
					}

					if (icon != null) {
						double u1 = icon.getMinU();
						double u2 = icon.getMaxU();
						double v1 = icon.getMinV();
						double v2 = icon.getMaxV();

						if ((u1 == u1Last) && (u2 == u2Last) && (v1 == v1Last)
								&& (v2 == v2Last)) {
							blockColour = blockColourLast;
							s_count++;
						} else {
							blockColour = getIconMapColour(icon, terrainTexture);
							// request icon with meta 16, carpenterblocks uses
							// this method to get the real texture
							// this makes the carpenterblocks render as brown
							// blocks on the map
							// FIXME:check how carpenterblocks fixes this
							// if (blockColour == 0)
							// {
							// icon = block.getIcon(1, 16);
							// blockColour = getIconMapColour(icon,
							// terrainTexture);
							// }
							
							u1Last = u1;
							u2Last = u2;
							v1Last = v1;
							v2Last = v2;
							blockColourLast = blockColour;
							b_count++;
						}
					}
				}
				bc.setColour(block.delegate.name(), String.valueOf(dv),
						blockColour);
			}
		}

		Logging.log("processed %d block textures, %d skipped, %d exceptions",
				b_count, s_count, e_count);

		genBiomeColours(bc);
	}
}
