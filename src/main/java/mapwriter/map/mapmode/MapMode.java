package mapwriter.map.mapmode;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import mapwriter.config.MapModeConfig;
import mapwriter.gui.ModGuiConfig.ModBooleanEntry;
import mapwriter.handler.ConfigurationHandler;
import mapwriter.map.MapView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.client.config.ConfigGuiType;
import net.minecraftforge.fml.client.config.DummyConfigElement;
import net.minecraftforge.fml.client.config.IConfigElement;

public class MapMode {
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
	
	public int textX = 0;
	public int textY = 0;
	public int textColour = 0xffffffff;
	
	public MapModeConfig config;
	
	public MapMode(MapModeConfig config) {
		this.config = config;
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
		this.config.marginTop = marginTop;
		this.config.marginBottom = marginBottom;
		this.config.marginLeft = marginLeft;
		this.config.marginRight = marginRight;
		this.update();
	}
	
	public void setHeightPercent(int heightPercent) {
		this.config.heightPercent = heightPercent;
		this.update();
	}
	
	public void toggleHeightPercent() {
		int i = (this.config.heightPercent / 5) + 1;
		if (i > 12) {
			i = 1;
		}
		this.setHeightPercent(i * 5);
	}
	
	private void update() {
		int size = (this.sh * this.config.heightPercent) / 100;
		int x, y;
		
		// calculate map x position and width
		if ((this.config.marginLeft >= 0) && (this.config.marginRight >= 0)) {
			x = this.config.marginLeft;
			this.w = this.sw - this.config.marginLeft - this.config.marginRight;
		} else if (this.config.marginLeft >= 0) {
			x = this.config.marginLeft;
			this.w = size;
		} else if (this.config.marginRight >= 0) {
			x = this.sw - size - this.config.marginRight;
			this.w = size;
		} else {
			x = (this.sw - size) / 2;
			this.w = size;
		}
		
		// calculate map y position and height
		if ((this.config.marginTop >= 0) && (this.config.marginBottom >= 0)) {
			y = this.config.marginTop;
			this.h = this.sh - this.config.marginTop - this.config.marginBottom;
		} else if (this.config.marginTop >= 0) {
			y = this.config.marginTop;
			this.h = size;
		} else if (this.config.marginBottom >= 0) {
			y = this.sh - size - this.config.marginBottom;
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
		
		if (this.config.circular) {
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
		this.config.rotate = enabled;
		this.config.circular = enabled;
		this.update();
	}
	
	public boolean toggleRotating() {
		this.setRotating(!this.config.rotate);
		return this.config.rotate;
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
		
		if (!this.config.circular) {
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
