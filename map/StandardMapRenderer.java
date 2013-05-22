package mapwriter.map;

import java.awt.Point;

import org.lwjgl.opengl.GL11;

import mapwriter.Mw;
import mapwriter.Render;
import mapwriter.map.mapmode.MapMode;

public class StandardMapRenderer implements MapRenderer {
	private Mw mw;
	private MapTexture mapTexture;
	public MarkerManager markerManager;
	private MapMode mapMode;
	private MapView mapView;
	public Point.Double playerArrowScreenPos = new Point.Double(0, 0);
	
	public StandardMapRenderer(Mw mw, MapTexture mapTexture, MarkerManager markerManager, MapMode mapMode, MapView mapView) {
		this.mw = mw;
		this.mapTexture = mapTexture;
		this.markerManager = markerManager;
		this.mapMode = mapMode;
		this.mapView = mapView;
	}
	
	public void close() {
	}
	
	public void update() {
		this.mapMode.setScreenRes();
		this.mapView.setAspect(this.mapMode);
		this.mapTexture.update(this.mw.regionManager, this.mapView);
	}
	
	public void draw() {	
		int regionZoomLevel = Math.max(0, this.mapView.getZoomLevel());
		double tSize = (double) (Mw.TEXTURE_SIZE << regionZoomLevel);
		
		double u = (this.mapView.getMinX() % tSize) / tSize;
		double v = (this.mapView.getMinZ() % tSize) / tSize;
		double w = this.mapView.getWidth() / tSize;
		double h = this.mapView.getHeight() / tSize;
		
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
		this.mapTexture.bind();
		Render.drawTexturedRect(this.mapMode.x, this.mapMode.y, this.mapMode.w, this.mapMode.h,
				u, v, u + w, v + h);
		
		Render.disableStencil();
		
		this.drawBorder(this.mapMode);
		
		// draw markers
		this.drawMarkers();
		
		// the position of the player within the map
		Point.Double arrow = this.getPlayerArrowPos();
		
		// draw player arrow
		Render.setColour(this.mapMode.playerArrowColour);
		Render.drawArrow(arrow.x, arrow.y, this.mw.playerHeading, this.mapMode.playerArrowSize);
		
		GL11.glLoadIdentity();
		GL11.glTranslatef((float) this.mapMode.xTranslation, (float) this.mapMode.yTranslation, -2000.0f);
		
		drawCoords(this.mw, this.mapMode);
		
		GL11.glPopMatrix();
	}
	
	public static void drawCoords(Mw mw, MapMode mapMode) {
		// draw coordinates
		if (mapMode.coordsEnabled) {
			Render.drawCentredString(mapMode.textX, mapMode.textY, mapMode.textColour,
					"%d %d %d", mw.playerXInt, mw.playerYInt, mw.playerZInt);
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
	
	private void drawMarkers() {
		for (Marker marker : this.markerManager.visibleMarkerList) {
			this.drawMarker(marker, 0xff000000);
		}
		if (this.markerManager.selectedMarker != null) {
			this.drawMarker(this.markerManager.selectedMarker, 0xffffffff);
		}
	}
	
	private void drawMarker(Marker marker, int borderColour) {
		Point.Double p;
		// markers are always specified by their overworld coordinates
		if (this.mapView.getDimension() == -1) {
			p = this.mapMode.getClampedScreenXY(this.mapView, marker.x / 8, marker.z / 8);
		} else {
			p = this.mapMode.getClampedScreenXY(this.mapView, marker.x, marker.z);
		}
		marker.screenPos.setLocation(p.x + this.mapMode.xTranslation, p.y + this.mapMode.yTranslation);
		
		// draw a coloured 2x2 rectangle centered on the calculated (x, y)
		double mSize = this.mapMode.markerSize;
		double halfMSize = this.mapMode.markerSize / 2.0;
		Render.setColour(borderColour);
		Render.drawRect(p.x - halfMSize, p.y - halfMSize, mSize, mSize);
		Render.setColour(marker.colour);
		Render.drawRect(p.x - halfMSize + 0.5, p.y - halfMSize + 0.5, mSize - 1.0, mSize - 1.0);
	}
	
	public Point.Double getPlayerArrowPos() {
		double scale = this.mapView.getDimensionScaling(this.mw.playerDimension);
		Point.Double p = this.mapMode.getClampedScreenXY(this.mapView, this.mw.playerX * scale, this.mw.playerZ * scale);
		this.playerArrowScreenPos.setLocation(p.x + this.mapMode.xTranslation, p.y + this.mapMode.yTranslation);
		return p;
	}
}

