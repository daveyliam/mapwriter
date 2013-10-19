package mapwriter.map;

import java.awt.Point;

import mapwriter.Mw;
import mapwriter.Render;
import mapwriter.map.mapmode.MapMode;

import org.lwjgl.opengl.GL11;

public class MapRenderer {
	private Mw mw;
	private MapMode mapMode;
	private MapView mapView;
	public Point.Double playerArrowScreenPos = new Point.Double(0, 0);
	
	public MapRenderer(Mw mw, MapMode mapMode, MapView mapView) {
		this.mw = mw;
		this.mapMode = mapMode;
		this.mapView = mapView;
	}
	
	public void draw() {
		
		this.mapMode.setScreenRes();
		this.mapView.setMapWH(this.mapMode);
		this.mapView.setTextureSize(this.mw.mapTexture.textureSize);
		
		MapViewRequest req = new MapViewRequest(this.mapView);
		this.mw.mapTexture.requestView(req, this.mw.executor, this.mw.regionManager);
		
		int regionZoomLevel = Math.max(0, this.mapView.getZoomLevel());
		
		double tSize = (double) this.mw.mapTexture.textureSize;
		double zoomScale = (double) (1 << regionZoomLevel);
		
		// if the texture UV coordinates do not line up with the texture pixels then the texture
		// will look blurry when it is rendered.
		// to fix this we round the texture coordinates to the nearest pixel boundary.
		// this is unnecessary when zoomed in as the texture will be upscaled and look blurry
		// anyway, so it is disabled in this case.
		// also the rounding causes the map to noticeably (and unpleasantly) 'snap' to texture
		// pixel boundaries when zoomed in.
		
		double u, v, w, h;
		
		if ((!this.mapMode.circular) && (this.mw.mapPixelSnapEnabled) && (this.mapView.getZoomLevel() >= 0)) {
			u = (Math.round(this.mapView.getMinX() / zoomScale) / tSize) % 1.0;
			v = (Math.round(this.mapView.getMinZ() / zoomScale) / tSize) % 1.0;
			w = Math.round(this.mapView.getWidth() / zoomScale) / tSize;
			h = Math.round(this.mapView.getHeight() / zoomScale) / tSize;
		} else {
			double tSizeInBlocks = tSize * zoomScale;
			u = (this.mapView.getMinX() / tSizeInBlocks) % 1.0;
			v = (this.mapView.getMinZ() / tSizeInBlocks) % 1.0;
			w = this.mapView.getWidth() / tSizeInBlocks;
			h = this.mapView.getHeight() / tSizeInBlocks;
		}
		
		GL11.glPushMatrix();
		GL11.glLoadIdentity();
		GL11.glTranslatef((float) this.mapMode.xTranslation, (float) this.mapMode.yTranslation, -2000.0f);
		if (this.mapMode.rotate) {
			GL11.glRotatef((float) this.mw.mapRotationDegrees, 0.0f, 0.0f, 1.0f);
		}
		if (this.mapMode.circular) {
			Render.setCircularStencil(0, 0, this.mapMode.h / 2.0);
		}
		
		// don't draw the map texture if the requested zoom level and dimension does not match
		// the zoom level and dimension of the regions currently loaded into the texture.
		// this prevents the map showing old regions while the new ones are loading.
		if (this.mw.mapTexture.isLoaded(req)) {
			Render.setColourWithAlphaPercent(0xffffff, this.mapMode.alphaPercent);
			this.mw.mapTexture.bind();
			Render.drawTexturedRect(this.mapMode.x, this.mapMode.y, this.mapMode.w, this.mapMode.h,
					u, v, u + w, v + h);
		} else {
			Render.setColourWithAlphaPercent(0x000000, this.mapMode.alphaPercent);
			Render.drawRect(this.mapMode.x, this.mapMode.y, this.mapMode.w, this.mapMode.h);
		}
		
		if (this.mapMode.circular) {
			Render.disableStencil();
		}
		
		drawBorder(this.mapMode);
		
		// draw markers
		this.mw.markerManager.drawMarkers(this.mapMode, this.mapView);
		
		// draw player trail
		if (this.mw.playerTrail.enabled) {
			this.mw.playerTrail.draw(this.mapMode, this.mapView);
		}
		
		// the position of the player within the map
		Point.Double arrow = this.getPlayerArrowPos();
		
		// draw player arrow
		Render.setColour(this.mapMode.playerArrowColour);
		Render.drawArrow(arrow.x, arrow.y, this.mw.playerHeading, this.mapMode.playerArrowSize);
		
		GL11.glLoadIdentity();
		GL11.glTranslatef((float) this.mapMode.xTranslation, (float) this.mapMode.yTranslation, -2000.0f);
		
		drawCoords(this.mw, this.mapMode);
		
		// some shader mods seem to need depth testing re-enabled
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glPopMatrix();
	}
	
	public static void drawCoords(Mw mw, MapMode mapMode) {
		// draw coordinates
		if (mw.coordsEnabled && mapMode.coordsEnabled) {
			Render.drawCentredString(mapMode.textX, mapMode.textY, mapMode.textColour,
					"%d, %d, %d", mw.playerXInt, mw.playerYInt, mw.playerZInt);
		}
	}
	
	public static void drawBorder(MapMode mapMode) {
		if ((mapMode.borderWidth > 0) && (mapMode.borderColour != 0)) {
			Render.setColour(mapMode.borderColour);
			if (mapMode.circular) {
				Render.drawCircleBorder(0.0, 0.0, mapMode.h / 2.0, mapMode.borderWidth);
			} else {
				Render.drawRectBorder(mapMode.x, mapMode.y, mapMode.w, mapMode.h, mapMode.borderWidth);
			}
		}
	}
	
	public Point.Double getPlayerArrowPos() {
		double scale = this.mapView.getDimensionScaling(this.mw.playerDimension);
		Point.Double p = this.mapMode.getClampedScreenXY(this.mapView, this.mw.playerX * scale, this.mw.playerZ * scale);
		this.playerArrowScreenPos.setLocation(p.x + this.mapMode.xTranslation, p.y + this.mapMode.yTranslation);
		return p;
	}
}

