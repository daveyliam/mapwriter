package mapwriter.map.mapmode;

import java.awt.Point;

import mapwriter.forge.MwConfig;
import mapwriter.map.MapView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public class MapMode {
	private final MwConfig config;
	public final String configCategory;
	
	private int sw = 320;
	private int sh = 240;
	private int dw = 320;
	private int dh = 240;
	
	// calculated before every frame drawn by updateMapDimensions
	public int xTranslation = 0;
	public int yTranslation = 0;
	public int x = -50;
	public int y = -50;
	public int w = 50;
	public int h = 50;
	
	// config settings
	public boolean enabled = true;
	public boolean rotate = false;
	public boolean circular = false;
	public int borderWidth = 0;
	public int borderColour = 0;
	public int playerArrowSize = 5;
	public int markerSize = 5;
	public int playerArrowColour = 0xffff0000;
	public int alphaPercent = 100;
	
	public int marginTop = 0;
	public int marginBottom = 0;
	public int marginLeft = 0;
	public int marginRight = 0;
	public int heightPercent = -1;
	
	public boolean coordsEnabled = false;
	
	public int textX = 0;
	public int textY = 0;
	public int textColour = 0xffffffff;
	
	public MapMode(MwConfig config, String configCategory) {
		this.config = config;
		this.configCategory = configCategory;
	}
	
	public void loadConfig() {
		// get options from config file
		this.enabled = this.config.getOrSetBoolean(this.configCategory, "enabled", this.enabled);
		this.coordsEnabled = this.config.getOrSetBoolean(this.configCategory, "drawCoords", this.coordsEnabled);
		this.playerArrowSize = this.config.getOrSetInt(this.configCategory, "playerArrowSize", this.playerArrowSize, 1, 20);
		this.markerSize = this.config.getOrSetInt(this.configCategory, "markerSize", this.markerSize, 1, 20);
		this.alphaPercent = this.config.getOrSetInt(this.configCategory, "alphaPercent", this.alphaPercent, 0, 100);
		this.borderWidth = this.config.getOrSetInt(this.configCategory, "borderWidth", this.borderWidth, 0, 50);
		this.borderColour = this.config.getOrSetColour(this.configCategory, "borderColour", this.borderColour);
		this.playerArrowColour = this.config.getOrSetColour(this.configCategory, "playerArrowColour", this.playerArrowColour);
		
		this.heightPercent = this.config.getOrSetInt(this.configCategory, "heightPercent", this.heightPercent, 0, 100);
		this.marginTop = this.config.getOrSetInt(this.configCategory, "marginTop", this.marginTop, -1, 320);
		this.marginBottom = this.config.getOrSetInt(this.configCategory, "marginBottom", this.marginBottom, -1, 320);
		this.marginLeft = this.config.getOrSetInt(this.configCategory, "marginLeft", this.marginLeft, -1, 320);
		this.marginRight = this.config.getOrSetInt(this.configCategory, "marginRight", this.marginRight, -1, 320);
		
		this.rotate = this.config.getOrSetBoolean(this.configCategory, "rotate", this.rotate);
		this.circular = this.config.getOrSetBoolean(this.configCategory, "circular", this.circular);
	}
	
	public void close() {
		this.config.get(this.configCategory, "drawCoords", 0).set(this.coordsEnabled ? 1 : 0);
		this.config.get(this.configCategory, "rotate", 0).set(this.rotate ? 1 : 0);
		this.config.get(this.configCategory, "circular", 0).set(this.circular ? 1 : 0);
	}
	
	public void setScreenRes(int dw, int dh, int sw, int sh) {
		if ((dw != this.dw) || (dh != this.dh) ||(sw != this.sw) || (sh != this.sh)) {
			this.dw = dw;
			this.dh = dh;
			this.sw = sw;
			this.sh = sh;
			this.update();
		}
	}
	
	public void setScreenRes() {
		Minecraft mc = Minecraft.getMinecraft();
		ScaledResolution sRes = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
		this.setScreenRes(mc.displayWidth, mc.displayHeight, sRes.getScaledWidth(), sRes.getScaledHeight());
	}
	
	private void update() {
		int size = (this.sh * this.heightPercent) / 100;
		int x, y;
		
		// calculate map x position and width
		if ((this.marginLeft >= 0) && (this.marginRight >= 0)) {
			x = this.marginLeft;
			this.w = this.sw - this.marginLeft - this.marginRight;
		} else if (this.marginLeft >= 0) {
			x = this.marginLeft;
			this.w = size;
		} else if (this.marginRight >= 0) {
			x = this.sw - size - this.marginRight;
			this.w = size;
		} else {
			x = (this.sw - size) / 2;
			this.w = size;
		}
		
		// calculate map y position and height
		if ((this.marginTop >= 0) && (this.marginBottom >= 0)) {
			y = this.marginTop;
			this.h = this.sh - this.marginTop - this.marginBottom;
		} else if (this.marginTop >= 0) {
			y = this.marginTop;
			this.h = size;
		} else if (this.marginBottom >= 0) {
			y = this.sh - size - this.marginBottom;
			this.h = size;
		} else {
			y = (this.sh - size) / 2;
			this.h = size;
		}
		
		int halfW = this.w / 2;
		int halfH = this.h / 2;
		this.xTranslation = x + halfW;
		this.yTranslation = y + halfH;
		this.x = -halfW;
		this.y = -halfH;
		
		if (this.circular) {
			this.w = this.h;
			this.x = -halfH;
		}
		
		// calculate coords display location
		this.textX = 0;
		this.textY = this.y + this.h + 4;
		
		//MwUtil.log("MapMode: map = %d %d %d %d, screen = %d %d", this.x, this.y, this.w, this.h, this.sw, this.sh);
		//MwUtil.log("MapMode: margins = left %d, right %d, top %d, bottom %d, size = %d",
		//		this.marginLeft, this.marginRight, this.marginTop, this.marginBottom, size);
	}
	
	public void setCoords(boolean enabled) {
		this.coordsEnabled = enabled;
	}
	
	public boolean toggleCoords() {
		this.setCoords(!this.coordsEnabled);
		return this.coordsEnabled;
	}
	
	public void setRotating(boolean enabled) {
		this.rotate = enabled;
		this.circular = enabled;
		this.update();
	}
	
	public boolean toggleRotating() {
		this.setRotating(!this.rotate);
		return this.rotate;
	}
	
	public Point screenXYtoBlockXZ(MapView mapView, int sx, int sy) {
		double withinMapX = ((double) (sx - this.xTranslation - this.x)) / ((double) this.w);
		double withinMapY = ((double) (sy - this.yTranslation - this.y)) / ((double) this.h);
		int bx = (int) Math.floor((mapView.getMinX() + (withinMapX * mapView.w)));
		int bz = (int) Math.floor((mapView.getMinZ() + (withinMapY * mapView.h)));
		return new Point(bx, bz);
	}
	
	public Point.Double getClampedScreenXY(MapView mapView, double bX, double bZ) {
		double xRel = (bX - mapView.getX()) / mapView.w;
		double zRel = (bZ - mapView.getZ()) / mapView.h;
		double limit = 0.49;
		
		if (!this.circular) {
			if (xRel < -limit) {
				zRel = -limit * zRel / xRel;
				xRel = -limit;
			}
			if (xRel > limit) {
				zRel = limit * zRel / xRel;
				xRel = limit;
			}
			if (zRel < -limit) {
				xRel = -limit * xRel / zRel;
				zRel = -limit;
			}
			if (zRel > limit) {
				xRel = limit * xRel / zRel;
				zRel = limit;
			}
			if (xRel < -limit) {
				zRel = -limit * zRel / xRel;
				xRel = -limit;
			}
			if (xRel > limit) {
				zRel = limit * zRel / xRel;
				xRel = limit;
			}
		} else {
			double dSq = (xRel * xRel) + (zRel * zRel);
			if (dSq > (limit * limit)) {
				double a = Math.atan2(zRel, xRel);
				xRel = limit * Math.cos(a);
				zRel = limit * Math.sin(a);
			}
		}
		
		// multiply by the overlay size and add the overlay position to
		// get the position within the overlay in screen coordinates
		return new Point.Double(
				this.x + (this.w * (xRel + 0.5)),
				this.y + (this.h * (zRel + 0.5)));
	}
}
