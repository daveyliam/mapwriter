package mapwriter.map;

import mapwriter.Mw;
import mapwriter.Render;
import mapwriter.map.mapmode.MapMode;
import net.minecraft.block.Block;

import org.lwjgl.opengl.GL11;

public class UndergroundMapRenderer implements MapRenderer {
	private Mw mw;
	private MapMode mapMode;
	private Texture undergroundTexture;
	
	private double viewX = 0.0;
	private double viewZ = 0.0;
	private double viewW = 15.0;
	private double viewH = 15.0;
	
	public UndergroundMapRenderer(Mw mw, MapMode mapMode) {
		this.mw = mw;
		this.mapMode = mapMode;
		this.undergroundTexture = new Texture(16, 16, 0xff000000, GL11.GL_NEAREST, GL11.GL_NEAREST, GL11.GL_REPEAT);
	}
	
	public void close() {
		this.undergroundTexture.close();
	}
	
	public void update() {
		double px = this.mw.playerX;
		double py = this.mw.playerY;
		double pz = this.mw.playerZ;
		
		for (int z = 0; z < 16; z++) {
			for (int x = 0; x < 16; x++) {
				int airCount = 0;
				int lavaCount = 0;
				int waterCount = 0;
				int bx = x + (int) Math.round(px - 8.0);
				int bz = z + (int) Math.round(pz - 8.0);
				for (int y = 0; y < 8; y++) {
					int by = y + (int) (py - 4.0);
					int blockID = this.mw.mc.theWorld.getBlockId(bx, by, bz);
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
				this.undergroundTexture.setRGB(bx & 0xf, bz & 0xf, colour);
			}
		}
		this.undergroundTexture.updateTexture();
		
		this.viewX = this.mw.playerX - 7.5;
		this.viewZ = this.mw.playerZ - 7.5;
		this.viewW = 15.0;
		this.viewH = 15.0;
		
		this.mapMode.setScreenRes();
	}

	public void draw() {
		// underground view mode
		double tu1 = (this.viewX % 16.0) / 16.0;
		double tv1 = (this.viewZ % 16.0) / 16.0;
		double tu2 = tu1 + (this.viewW / 16.0);
		double tv2 = tv1 + (this.viewH / 16.0);
		
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
		
		Render.disableStencil();
		
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
