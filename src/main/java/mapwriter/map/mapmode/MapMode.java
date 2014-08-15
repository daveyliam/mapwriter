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
	private double screenScalingFactor = 1.0;
	
	// calculated before every frame drawn by updateMapDimensions
	public int xTranslation = 0;
	public int yTranslation = 0;
	public int x = -25;
	public int y = -25;
	public int w = 50;
	public int h = 50;
	public int wPixels = 50;
	public int hPixels = 50;
	
	// config settings
	public boolean enabled = true;
	public boolean rotate = true;
	public boolean circular = true;
	public boolean coordsEnabled = false;
	public int borderMode = 1;
	public int playerArrowSize = 5;
	public int markerSize = 5;
	public int trailMarkerSize = 3;
	public int alphaPercent = 100;
	
	public int marginTop = 0;
	public int marginBottom = 0;
	public int marginLeft = 0;
	public int marginRight = 0;
	public int heightPercent = -1;
	
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
		this.playerArrowSize = this.config.getOrSetInt(this.configCategory, "playerArrowSize", this.playerArrowSize, 1, 20);
		this.markerSize = this.config.getOrSetInt(this.configCategory, "markerSize", this.markerSize, 1, 20);
		this.alphaPercent = this.config.getOrSetInt(this.configCategory, "alphaPercent", this.alphaPercent, 0, 100);
		
		this.heightPercent = this.config.getOrSetInt(this.configCategory, "heightPercent", this.heightPercent, 0, 100);
		this.marginTop = this.config.getOrSetInt(this.configCategory, "marginTop", this.marginTop, -1, 320);
		this.marginBottom = this.config.getOrSetInt(this.configCategory, "marginBottom", this.marginBottom, -1, 320);
		this.marginLeft = this.config.getOrSetInt(this.configCategory, "marginLeft", this.marginLeft, -1, 320);
		this.marginRight = this.config.getOrSetInt(this.configCategory, "marginRight", this.marginRight, -1, 320);
		
		this.rotate = this.config.getOrSetBoolean(this.configCategory, "rotate", this.rotate);
		this.circular = this.config.getOrSetBoolean(this.configCategory, "circular", this.circular);
		this.coordsEnabled = this.config.getOrSetBoolean(this.configCategory, "coordsEnabled", this.coordsEnabled);
		this.borderMode = this.config.getOrSetInt(this.configCategory, "borderMode", this.borderMode, 0, 1);
		
		this.trailMarkerSize = Math.max(1, this.markerSize - 1);
	}
	
	public void saveConfig() {
		this.config.setBoolean(this.configCategory, "enabled", this.enabled);
		this.config.setInt(this.configCategory, "heightPercent", this.heightPercent);
		this.config.setInt(this.configCategory, "marginTop", this.marginTop);
		this.config.setInt(this.configCategory, "marginBottom", this.marginBottom);
		this.config.setInt(this.configCategory, "marginLeft", this.marginLeft);
		this.config.setInt(this.configCategory, "marginRight", this.marginRight);
		this.config.setBoolean(this.configCategory, "rotate", this.rotate);
		this.config.setBoolean(this.configCategory, "circular", this.circular);
	}
	
	public void close() {
		this.saveConfig();
	}
	
	public void setScreenRes(int dw, int dh, int sw, int sh, double scaling) {
		if ((sw != this.sw) || (sh != this.sh) || (scaling != this.screenScalingFactor)) {
			this.sw = sw;
			this.sh = sh;
			this.screenScalingFactor = scaling;
			this.update();
		}
	}
	
	public void setScreenRes() {
		Minecraft mc = Minecraft.getMinecraft();
		ScaledResolution sRes = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
		this.setScreenRes(mc.displayWidth, mc.displayHeight, sRes.getScaledWidth(), sRes.getScaledHeight(), sRes.getScaleFactor());
	}
	
	public void setMargins(int marginTop, int marginBottom, int marginLeft, int marginRight) {
		this.marginTop = marginTop;
		this.marginBottom = marginBottom;
		this.marginLeft = marginLeft;
		this.marginRight = marginRight;
		this.update();
	}
	
	public void setHeightPercent(int heightPercent) {
		this.heightPercent = heightPercent;
		this.update();
	}
	
	public void toggleHeightPercent() {
		int i = (this.heightPercent / 5) + 1;
		if (i > 12) {
			i = 1;
		}
		this.setHeightPercent(i * 5);
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
		
		// make sure width and height are multiples of 2
		this.w &= -2;
		this.h &= -2;
		
		this.xTranslation = x + (this.w >> 1);
		this.yTranslation = y + (this.h >> 1);
		
		if (this.circular) {
			this.w = this.h;
		}
		
		this.x = -(this.w >> 1);
		this.y = -(this.h >> 1);
		
		this.wPixels = (int) Math.round(((double) this.w) * this.screenScalingFactor);
		this.hPixels = (int) Math.round(((double) this.h) * this.screenScalingFactor);
		
		// calculate coords display location
		this.textX = 0;
		this.textY = (this.h >> 1) + 4;
		
		//MwUtil.log("MapMode: map = %d %d %d %d, screen = %d %d", this.x, this.y, this.w, this.h, this.sw, this.sh);
		//MwUtil.log("MapMode: margins = left %d, right %d, top %d, bottom %d, size = %d",
		//		this.marginLeft, this.marginRight, this.marginTop, this.marginBottom, size);
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
		double withinMapX = ((double) (sx - this.xTranslation)) / ((double) this.w);
		double withinMapY = ((double) (sy - this.yTranslation)) / ((double) this.h);
		int bx = (int) Math.floor((mapView.getX() + (withinMapX * mapView.getWidth())));
		int bz = (int) Math.floor((mapView.getZ() + (withinMapY * mapView.getHeight())));
		return new Point(bx, bz);
	}
	
	public Point.Double blockXZtoScreenXY(MapView mapView, double bX, double bZ) {
		double xNorm = (bX - mapView.getX()) / mapView.getWidth();
		double zNorm = (bZ - mapView.getZ()) / mapView.getHeight();
		return new Point.Double(this.w * xNorm, this.h * zNorm);
	}
	
	public Point.Double getClampedScreenXY(MapView mapView, double bX, double bZ) {
		double xRel = (bX - mapView.getX()) / mapView.getWidth();
		double zRel = (bZ - mapView.getZ()) / mapView.getHeight();
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
		return new Point.Double(this.w * xRel, this.h * zRel);
	}
}
