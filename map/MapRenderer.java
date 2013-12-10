package mapwriter.map;

import java.awt.Point;
import java.util.ArrayList;

import mapwriter.Mw;
import mapwriter.Render;
import mapwriter.api.IMwChunkOverlay;
import mapwriter.api.IMwDataProvider;
import mapwriter.api.MwAPI;
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
		this.mapView.setTextureSize(this.mw.textureSize);
		int regionZoomLevel = Math.max(0, this.mapView.getZoomLevel());
		
		MapViewRequest req = new MapViewRequest(this.mapView);
		this.mw.mapTexture.requestView(req, this.mw.executor, this.mw.regionManager);
		
		double tSize = (double) this.mw.textureSize;
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
		
		
		if ((this.mw.undergroundMode) && (regionZoomLevel == 0)) {
			// draw the underground map
			this.mw.undergroundMapTexture.requestView(this.mapView);
			Render.setColour(0xff000000);
			Render.drawRect(this.mapMode.x, this.mapMode.y, this.mapMode.w, this.mapMode.h);
			Render.setColourWithAlphaPercent(0xffffff, this.mapMode.alphaPercent);
			this.mw.undergroundMapTexture.bind();
			Render.drawTexturedRect(
					this.mapMode.x, this.mapMode.y, this.mapMode.w, this.mapMode.h,
					u, v, u + w, v + h
			);
		
		} else if (this.mw.mapTexture.isLoaded(req)) {
			// draw the surface map
			Render.setColourWithAlphaPercent(0xffffff, this.mapMode.alphaPercent);
			this.mw.mapTexture.bind();
			Render.drawTexturedRect(
					this.mapMode.x, this.mapMode.y, this.mapMode.w, this.mapMode.h,
					u, v, u + w, v + h
			);
			
		} else {
			// don't draw the map texture if the requested zoom level and dimension does not match
			// the zoom level and dimension of the regions currently loaded into the texture.
			// this prevents the map showing old regions while the new ones are loading.
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
		
		// draw north arrow
		if (this.mapMode.rotate) {
			
			Render.setColour(this.mapMode.borderColour);
			double r = this.mapMode.h / 2.0;
			Render.drawTriangle(4.0, -r, 0.0, -r - 4.0, -4.0, -r);
			Render.setColour(0xff00c000);
			r += 1.0;
			Render.drawTriangle(2.0, -r, 0.0, -r - 2.0, -2.0, -r);
		}
		
		// draw overlays from registered providers
   	 	//for (IMwDataProvider provider : MwAPI.getDataProviders())
		IMwDataProvider provider = MwAPI.getCurrentDataProvider();
		if (provider != null) {
			ArrayList<IMwChunkOverlay> overlays = provider.getChunksOverlay(this.mapView.getDimension(), this.mapView.getX(), this.mapView.getZ(), this.mapView.getMinX(), this.mapView.getMinZ(), this.mapView.getMaxX(), this.mapView.getMaxZ());
			if (overlays != null) {
   	 			for (IMwChunkOverlay overlay : overlays) {
   	 				paintChunk(mapMode, mapView, overlay);
   	 			}
			}
		}
		
		GL11.glLoadIdentity();
		GL11.glTranslatef((float) this.mapMode.xTranslation, (float) this.mapMode.yTranslation, -2000.0f);
		
		drawCoords(this.mw, this.mapMode);
		
		// some shader mods seem to need depth testing re-enabled
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glPopMatrix();
		
		if (provider != null) {
			GL11.glPushMatrix();			
			provider.onDraw(this.mapView, this.mapMode);
			GL11.glPopMatrix();			
		}
	}
	
	public static void paintChunk(MapMode mapMode, MapView mapView, IMwChunkOverlay overlay){
		int chunkX    = overlay.getCoordinates().x;
		int chunkZ    = overlay.getCoordinates().y;
		float filling = overlay.getFilling();
		
		Point.Double topCorner = mapMode.blockXZtoScreenXY(mapView, chunkX << 4, chunkZ << 4);
		Point.Double botCorner = mapMode.blockXZtoScreenXY(mapView, (chunkX + 1) << 4, (chunkZ + 1 << 4));

		topCorner.x = Math.max(mapMode.x,             topCorner.x);
		topCorner.x = Math.min(mapMode.x + mapMode.w, topCorner.x);
		topCorner.y = Math.max(mapMode.y,             topCorner.y);
		topCorner.y = Math.min(mapMode.y + mapMode.h, topCorner.y);
		
		botCorner.x = Math.max(mapMode.x,             botCorner.x);
		botCorner.x = Math.min(mapMode.x + mapMode.w, botCorner.x);
		botCorner.y = Math.max(mapMode.y,             botCorner.y);
		botCorner.y = Math.min(mapMode.y + mapMode.h, botCorner.y);		

		double sizeX = (botCorner.x - topCorner.x) * filling;
		double sizeY = (botCorner.y - topCorner.y) * filling;		
		double offsetX = ((botCorner.x - topCorner.x) - sizeX) / 2;
		double offsetY = ((botCorner.y - topCorner.y) - sizeY) / 2;	
		
		if (overlay.hasBorder()) {
			Render.setColour(overlay.getBorderColor());
			Render.drawRectBorder(topCorner.x + 1, topCorner.y + 1, botCorner.x - topCorner.x - 1, botCorner.y - topCorner.y - 1, overlay.getBorderWidth());
		}
		
		Render.setColour(overlay.getColor());
		Render.drawRect(topCorner.x + offsetX + 1, topCorner.y + offsetY + 1, sizeX - 1, sizeY - 1);
	}	
	
	public static void drawCoords(Mw mw, MapMode mapMode) {
		// draw coordinates
		if (mapMode.coordsEnabled && (mw.coordsMode > 0)) {
			
			GL11.glPushMatrix();
			GL11.glTranslatef(mapMode.textX, mapMode.textY, 0);
			if (mw.coordsMode == 1) {
				GL11.glScalef(0.5f, 0.5f, 1.0f);
			}
			Render.drawCentredString(0, 0, mapMode.textColour,
					"%d, %d, %d", mw.playerXInt, mw.playerYInt, mw.playerZInt);
			GL11.glPopMatrix();
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

