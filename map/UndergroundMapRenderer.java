package mapwriter.map;

import java.util.Arrays;

import mapwriter.Mw;
import mapwriter.Render;
import mapwriter.Texture;
import mapwriter.map.mapmode.MapMode;
import net.minecraft.block.Block;

import org.lwjgl.opengl.GL11;

public class UndergroundMapRenderer implements MapRenderer {
	private Mw mw;
	private MapMode mapMode;
	private Texture undergroundTexture;
	
	private double viewX = 0.0;
	private double viewZ = 0.0;
	private double viewW = 32.0;
	private double viewH = 32.0;
	
	private int startX = 0;
	private int startZ = 0;
	private boolean[] processedBlockFlags = new boolean[256];
	
	private static final int textureSize = 1024;
	
	public UndergroundMapRenderer(Mw mw, MapMode mapMode) {
		this.mw = mw;
		this.mapMode = mapMode;
		this.undergroundTexture = new Texture(textureSize, textureSize, 0xff000000, GL11.GL_NEAREST, GL11.GL_NEAREST, GL11.GL_REPEAT);
	}
	
	public void close() {
		this.undergroundTexture.close();
	}
	
	private boolean getBlockProcessedFlag(int x, int z) {
		int xi = x - this.startX + 8;
		int zi = z - this.startZ + 8;
		if (((xi & -16) == 0) && ((zi & -16) == 0)) {
			return this.processedBlockFlags[(zi << 4) | xi];
		} else {
			return true;
		}
	}
	
	private void setBlockProcessedFlag(int x, int z, boolean flag) {
		int xi = x - this.startX + 8;
		int zi = z - this.startZ + 8;
		if (((xi & -16) == 0) && ((zi & -16) == 0)) {
			this.processedBlockFlags[(zi << 4) | xi] = flag;
		}
	}
	
	private void clearBlockProcessedFlags() {
		Arrays.fill(this.processedBlockFlags, false);
	}
	
	private int getBlockColour(int x, int y, int z) {
		int airCount = 0;
		int lavaCount = 0;
		int waterCount = 0;
		for (int yi = 0; yi < 8; yi++) {
			int blockID = this.mw.mc.theWorld.getBlockId(x, y + yi, z);
			if (blockID == 0) {
					airCount++;
			} else if ((blockID == Block.waterMoving.blockID) || (blockID == Block.waterStill.blockID)) {
					waterCount++;
			} else if ((blockID == Block.lavaMoving.blockID) || (blockID == Block.lavaStill.blockID)) {
					lavaCount++;
			}
		}
		int colour = 0;
		if (lavaCount > 0) {
			colour = 0xff000000 | ((lavaCount * 32) << 16);
		} else if (waterCount > 0) {
			colour = 0xff000000 | ((waterCount * 32));
		} else {
			colour = 0xff000000 | (((8 - airCount) * 20) << 8);
		}
		return colour;
	}
	
	private void processBlock(int x, int y, int z) {
		this.setBlockProcessedFlag(x, z, true);
		
		int colour = this.getBlockColour(x, y, z);
		
		this.undergroundTexture.setRGB(x & (textureSize - 1), z & (textureSize - 1), colour);
		
		int blockID = this.mw.mc.theWorld.getBlockId(x, y, z);
		Block block = Block.blocksList[blockID];
		if ((block == null) || !block.isOpaqueCube()) {
			if (!this.getBlockProcessedFlag(x + 1, z)) {
				this.processBlock(x + 1, y, z);
			}
			if (!this.getBlockProcessedFlag(x - 1, z)) {
				this.processBlock(x - 1, y, z);
			}
			if (!this.getBlockProcessedFlag(x, z + 1)) {
				this.processBlock(x, y, z + 1);
			}
			if (!this.getBlockProcessedFlag(x, z - 1)) {
				this.processBlock(x, y, z - 1);
			}
		}
	}
	
	public void update() {
		int px = this.mw.playerXInt;
		int py = this.mw.playerYInt;
		int pz = this.mw.playerZInt;
		
		this.clearBlockProcessedFlags();
		this.startX = px;
		this.startZ = pz;
		this.processBlock(px, py, pz);
		
		this.undergroundTexture.updateTexture();
		
		this.viewX = this.mw.playerX - (this.viewW / 2.0);
		this.viewZ = this.mw.playerZ - (this.viewH / 2.0);
		
		this.mapMode.setScreenRes();
	}

	public void draw() {
		// underground view mode
		double tu1 = (this.viewX / textureSize) % 1.0;
		double tv1 = (this.viewZ / textureSize) % 1.0;
		double tu2 = tu1 + (this.viewW / textureSize);
		double tv2 = tv1 + (this.viewH / textureSize);
		
		GL11.glPushMatrix();
		GL11.glLoadIdentity();
		GL11.glTranslatef((float) this.mapMode.xTranslation, (float) this.mapMode.yTranslation, -2000.0f);
		if (this.mapMode.rotate) {
			GL11.glRotatef((float) this.mw.mapRotationDegrees, 0.0f, 0.0f, 1.0f);
		}
		if (this.mapMode.circular) {
			Render.setCircularStencil(0, 0, this.mapMode.h / 2.0);
		}
		
		Render.setColourWithAlphaPercent(0xffffff, this.mapMode.alphaPercent);
		this.undergroundTexture.bind();
		Render.drawTexturedRect(
				this.mapMode.x, this.mapMode.y, this.mapMode.w, this.mapMode.h,
				tu1, tv1, tu2, tv2);
		
		if (this.mapMode.circular) {
			Render.disableStencil();
		}
		
		StandardMapRenderer.drawBorder(this.mapMode);
		
		// only 15 blocks of the 16 block texture are shown, so rather than the player arrow being
		// drawn in the exact centre of the overlay area, it needs to be drawn 8/15 of the way in.
		double arrowX = this.mapMode.x + this.mapMode.w / 2;
		double arrowZ = this.mapMode.y + this.mapMode.h / 2;
		
		// draw player arrow
		Render.setColour(this.mapMode.playerArrowColour);
		Render.drawArrow(arrowX, arrowZ, this.mw.playerHeading, this.mapMode.playerArrowSize);
		
		GL11.glLoadIdentity();
		GL11.glTranslatef((float) this.mapMode.xTranslation, (float) this.mapMode.yTranslation, -2000.0f);
		
		StandardMapRenderer.drawCoords(this.mw, this.mapMode);
		
		// some shader mods seem to need depth testing re-enabled
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glPopMatrix();
	}
	
	/*public Point screenXYtoBlockXZ(int sx, int sy) {
		double withinMapX = ((double) (sx - this.mapMode.x)) / ((double) this.mapMode.w);
		double withinMapY = ((double) (sy - this.mapMode.y)) / ((double) this.mapMode.h);
		int bx = (int) Math.floor((this.viewX + (withinMapX * this.viewW)));
		int bz = (int) Math.floor((this.viewZ + (withinMapY * this.viewH)));
		return new Point(bx, bz);
	}*/
}
