package mapwriter.map;

import java.awt.Point;
import java.util.ArrayList;

import mapwriter.Mw;
import mapwriter.api.IMwChunkOverlay;
import mapwriter.api.IMwDataProvider;
import mapwriter.api.MwAPI;
import mapwriter.config.Config;
import mapwriter.config.MapModeConfig;
import mapwriter.map.mapmode.MapMode;
import mapwriter.util.Reference;
import mapwriter.util.Render;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.BlockPos;

import org.lwjgl.opengl.GL11;

public class MapRenderer
{
	private Mw mw;
	private MapMode mapMode;
	private MapView mapView;
	// accessed by the MwGui to check whether the mouse cursor is near the
	// player arrow on the rendered map
	public Point.Double playerArrowScreenPos = new Point.Double(0, 0);
	private int textOffset = 12;
	private int textY = 0;
	private int textX = 0;
	
	public MapRenderer(Mw mw, MapMode mapMode, MapView mapView)
	{
		this.mw = mw;
		this.mapMode = mapMode;
		this.mapView = mapView;
	}

	private void drawMap()
	{

		int regionZoomLevel = Math.max(0, this.mapView.getZoomLevel());
		double tSize = this.mw.textureSize;
		double zoomScale = 1 << regionZoomLevel;

		// if the texture UV coordinates do not line up with the texture pixels
		// then the texture
		// will look blurry when it is drawn to the screen.
		// to fix this we round the texture coordinates to the nearest pixel
		// boundary.
		// this is unnecessary when zoomed in as the texture will be upscaled
		// and look blurry
		// anyway, so it is disabled in this case.
		// also the rounding causes the map to noticeably (and unpleasantly)
		// 'snap' to texture
		// pixel boundaries when zoomed in.

		double u, v, w, h;
		if ((!this.mapMode.config.circular) && (Config.mapPixelSnapEnabled) && (this.mapView.getZoomLevel() >= 0))
		{
			u = (Math.round(this.mapView.getMinX() / zoomScale) / tSize) % 1.0;
			v = (Math.round(this.mapView.getMinZ() / zoomScale) / tSize) % 1.0;
			w = Math.round(this.mapView.getWidth() / zoomScale) / tSize;
			h = Math.round(this.mapView.getHeight() / zoomScale) / tSize;
		}
		else
		{
			double tSizeInBlocks = tSize * zoomScale;
			u = (this.mapView.getMinX() / tSizeInBlocks) % 1.0;
			v = (this.mapView.getMinZ() / tSizeInBlocks) % 1.0;
			w = (this.mapView.getWidth() / tSizeInBlocks);
			h = (this.mapView.getHeight() / tSizeInBlocks);
		}
		GlStateManager.pushMatrix();

		if (this.mapMode.config.rotate && (this.mapMode.config.circular == true))
		{
			GlStateManager.rotate(this.mw.mapRotationDegrees, 0.0f, 0.0f, 1.0f);
		}
		if (this.mapMode.config.circular)
		{
			Render.setCircularStencil(0, 0, this.mapMode.h / 2.0);
		}

		if ((this.mapView.getUndergroundMode()) && (regionZoomLevel == 0))
		{
			// draw the underground map
			this.mw.undergroundMapTexture.requestView(this.mapView);
			// underground map needs to have a black background
			Render.setColourWithAlphaPercent(0x000000, this.mapMode.config.alphaPercent);
			Render.drawRect(this.mapMode.x, this.mapMode.y, this.mapMode.w, this.mapMode.h);
			Render.setColourWithAlphaPercent(0xffffff, this.mapMode.config.alphaPercent);
			this.mw.undergroundMapTexture.bind();
			Render.drawTexturedRect(this.mapMode.x, this.mapMode.y, this.mapMode.w, this.mapMode.h, u, v, u + w, v + h);
		}
		else
		{
			// draw the surface map
			MapViewRequest req = new MapViewRequest(this.mapView);
			this.mw.mapTexture.requestView(req, this.mw.executor, this.mw.regionManager);

			// draw the background texture
			if (!Config.backgroundTextureMode.equals(Config.backgroundModeStringArray[0]))
			{
				double bu1 = 0.0;
				double bu2 = 1.0;
				double bv1 = 0.0;
				double bv2 = 1.0;
				if (Config.backgroundTextureMode.equals(Config.backgroundModeStringArray[2]))
				{
					// background moves with map if mode is 2
					double bSize = tSize / 256.0;
					bu1 = u * bSize;
					bu2 = (u + w) * bSize;
					bv1 = v * bSize;
					bv2 = (v + h) * bSize;
				}
				this.mw.mc.renderEngine.bindTexture(Reference.backgroundTexture);
				Render.setColourWithAlphaPercent(0xffffff, this.mapMode.config.alphaPercent);
				Render.drawTexturedRect(this.mapMode.x, this.mapMode.y, this.mapMode.w, this.mapMode.h, bu1, bv1, bu2, bv2);
			}
			else
			{
				// mode 0, no background texture
				Render.setColourWithAlphaPercent(0x000000, this.mapMode.config.alphaPercent);
				Render.drawRect(this.mapMode.x, this.mapMode.y, this.mapMode.w, this.mapMode.h);
			}

			// only draw surface map if the request is loaded (view requests are
			// loaded by the background thread)
			if (this.mw.mapTexture.isLoaded(req))
			{
				this.mw.mapTexture.bind();
				Render.setColourWithAlphaPercent(0xffffff, this.mapMode.config.alphaPercent);
				Render.drawTexturedRect(this.mapMode.x, this.mapMode.y, this.mapMode.w, this.mapMode.h, u, v, u + w, v + h);
			}
		}

		// draw ProfMobius chunk overlay
		IMwDataProvider provider = this.drawOverlay();

		// overlay onDraw event
		if (provider != null)
		{
			GlStateManager.pushMatrix();
			provider.onDraw(this.mapView, this.mapMode);
			GlStateManager.popMatrix();
		}

		if (this.mapMode.config.circular)
		{
			Render.disableStencil();
		}
		GlStateManager.popMatrix();
	}

	private void drawBorder()
	{
		if (this.mapMode.config.circular)
		{
			this.mw.mc.renderEngine.bindTexture(Reference.roundMapTexture);
		}
		else
		{
			this.mw.mc.renderEngine.bindTexture(Reference.squareMapTexture);
		}
		Render.setColour(0xffffffff);
		Render.drawTexturedRect(this.mapMode.x / 0.75, this.mapMode.y / 0.75, this.mapMode.w / 0.75, this.mapMode.h / 0.75, 0.0, 0.0, 1.0, 1.0);
	}

	private void drawPlayerArrow()
	{
		GlStateManager.pushMatrix();
		double scale = this.mapView.getDimensionScaling(this.mw.playerDimension);
		Point.Double p = this.mapMode.getClampedScreenXY(this.mapView, this.mw.playerX * scale, this.mw.playerZ * scale);
		this.playerArrowScreenPos.setLocation(p.x + this.mapMode.xTranslation, p.y + this.mapMode.yTranslation);

		// the arrow only needs to be rotated if the map is NOT rotated
		GlStateManager.translate(p.x, p.y, 0.0);
		if (!this.mapMode.config.rotate || (this.mapMode.config.circular == false))
		{
			GlStateManager.rotate(-this.mw.mapRotationDegrees, 0.0f, 0.0f, 1.0f);
		}

		double arrowSize = this.mapMode.config.playerArrowSize;
		Render.setColour(0xffffffff);
		this.mw.mc.renderEngine.bindTexture(Reference.playerArrowTexture);
		Render.drawTexturedRect(-arrowSize, -arrowSize, arrowSize * 2, arrowSize * 2, 0.0, 0.0, 1.0, 1.0);
		GlStateManager.popMatrix();
	}

	private void drawIcons()
	{
		GlStateManager.pushMatrix();

		if (this.mapMode.config.rotate && (this.mapMode.config.circular == true))
		{
			GlStateManager.rotate(this.mw.mapRotationDegrees, 0.0f, 0.0f, 1.0f);
		}

		// draw markers
		this.mw.markerManager.drawMarkers(this.mapMode, this.mapView);

		// draw player trail
		if (this.mw.playerTrail.enabled)
		{
			this.mw.playerTrail.draw(this.mapMode, this.mapView);
		}

		// draw north arrow
		if (this.mapMode.config.rotate)
		{
			double y = this.mapMode.h / 2.0;
			double arrowSize = this.mapMode.config.playerArrowSize;
			Render.setColour(0xffffffff);
			this.mw.mc.renderEngine.bindTexture(Reference.northArrowTexture);
			Render.drawTexturedRect(-arrowSize, -y - (arrowSize * 2), arrowSize * 2, arrowSize * 2, 0.0, 0.0, 1.0, 1.0);
		}
		GlStateManager.popMatrix();

		// outside of the matrix pop as theplayer arrow
		// needs to be drawn without rotation
		this.drawPlayerArrow();
	}

	private void drawStatusText()
	{
		this.textOffset = 12;
		this.textY = this.mapMode.textY;
		this.textX = this.mapMode.textX;
		drawCoords();
		drawBiomeName();
		drawUndergroundMode();
	}
	
	private void drawCoords()
	{
		// draw coordinates
		if (!this.mapMode.config.coordsMode.equals(MapModeConfig.coordsModeStringArray[0]))
		{
			GlStateManager.pushMatrix();
			GlStateManager.translate(this.textX, this.textY, 0);
			if (this.mapMode.config.coordsMode.equals(MapModeConfig.coordsModeStringArray[1]))
			{
				GlStateManager.scale(0.5f, 0.5f, 1.0f);	
				this.textOffset = (int)(this.textOffset * 0.5f);
			}
			Render.drawCentredString(0, 0, this.mapMode.textColour, "%d, %d, %d", this.mw.playerXInt, this.mw.playerYInt, this.mw.playerZInt);
			this.textY += this.textOffset;
			GlStateManager.popMatrix();
		}
	}
	
	private void drawBiomeName()
	{
		if (!this.mapMode.config.biomeMode.equals(MapModeConfig.coordsModeStringArray[0]))
		{
			GlStateManager.pushMatrix();
			GlStateManager.translate(this.textX, this.textY, 0);
			if (this.mapMode.config.biomeMode.equals(MapModeConfig.coordsModeStringArray[1]))
			{
				GlStateManager.scale(0.5f, 0.5f, 1.0f);
				this.textOffset = (int)(this.textOffset * 0.5f);
			}
			Render.drawCentredString(0, 0, this.mapMode.textColour, this.mw.playerBiome);
			this.textY += this.textOffset;
			GlStateManager.popMatrix();
		}
	}

	private void drawUndergroundMode()
	{
		if (Config.undergroundMode)
		{
			GlStateManager.pushMatrix();
			GlStateManager.translate(this.textX, this.textY, 0);
			GlStateManager.scale(0.5f, 0.5f, 1.0f);
			this.textOffset = (int)(this.textOffset * 0.5f);
			Render.drawCentredString(0, 0, this.mapMode.textColour, "underground mode");
			this.textY += this.textOffset;
			GlStateManager.popMatrix();
		}
	}
	
	private IMwDataProvider drawOverlay()
	{
		// draw overlays from registered providers
		// for (IMwDataProvider provider : MwAPI.getDataProviders())
		IMwDataProvider provider = MwAPI.getCurrentDataProvider();
		if (provider != null)
		{
			ArrayList<IMwChunkOverlay> overlays = provider.getChunksOverlay(this.mapView.getDimension(), this.mapView.getX(), this.mapView.getZ(), this.mapView.getMinX(), this.mapView.getMinZ(), this.mapView.getMaxX(), this.mapView.getMaxZ());
			if (overlays != null)
			{
				for (IMwChunkOverlay overlay : overlays)
				{
					paintChunk(this.mapMode, this.mapView, overlay);
				}
			}
		}
		return provider;
	}

	public void draw()
	{
		this.mapMode.updateMargin();
		this.mapMode.setScreenRes();
		this.mapView.setMapWH(this.mapMode);
		this.mapView.setTextureSize(this.mw.textureSize);

		GlStateManager.pushMatrix();
		GlStateManager.loadIdentity();

		// translate to center of minimap
		// z is -2000 so that it is drawn above the 3D world, but below GUI
		// elements which are typically at -3000
		GlStateManager.translate(this.mapMode.xTranslation, this.mapMode.yTranslation, -2000.0);

		// draw background, the map texture, and enabled overlays
		this.drawMap();

		if (this.mapMode.config.borderMode)
		{
			this.drawBorder();
		}
		this.drawIcons();

		this.drawStatusText();

		// some shader mods seem to need depth testing re-enabled
		GlStateManager.enableDepth();
		GlStateManager.popMatrix();
	}

	private static void paintChunk(MapMode mapMode, MapView mapView, IMwChunkOverlay overlay)
	{
		int chunkX = overlay.getCoordinates().x;
		int chunkZ = overlay.getCoordinates().y;
		float filling = overlay.getFilling();

		Point.Double topCorner = mapMode.blockXZtoScreenXY(mapView, chunkX << 4, chunkZ << 4);
		Point.Double botCorner = mapMode.blockXZtoScreenXY(mapView, (chunkX + 1) << 4, ((chunkZ + 1) << 4));

		topCorner.x = Math.max(mapMode.x, topCorner.x);
		topCorner.x = Math.min(mapMode.x + mapMode.w, topCorner.x);
		topCorner.y = Math.max(mapMode.y, topCorner.y);
		topCorner.y = Math.min(mapMode.y + mapMode.h, topCorner.y);

		botCorner.x = Math.max(mapMode.x, botCorner.x);
		botCorner.x = Math.min(mapMode.x + mapMode.w, botCorner.x);
		botCorner.y = Math.max(mapMode.y, botCorner.y);
		botCorner.y = Math.min(mapMode.y + mapMode.h, botCorner.y);

		double sizeX = (botCorner.x - topCorner.x) * filling;
		double sizeY = (botCorner.y - topCorner.y) * filling;
		double offsetX = ((botCorner.x - topCorner.x) - sizeX) / 2;
		double offsetY = ((botCorner.y - topCorner.y) - sizeY) / 2;

		if (overlay.hasBorder())
		{
			Render.setColour(overlay.getBorderColor());
			Render.drawRectBorder(topCorner.x + 1, topCorner.y + 1, botCorner.x - topCorner.x - 1, botCorner.y - topCorner.y - 1, overlay.getBorderWidth());
		}

		Render.setColour(overlay.getColor());
		Render.drawRect(topCorner.x + offsetX + 1, topCorner.y + offsetY + 1, sizeX - 1, sizeY - 1);
	}

	public MapMode getMapMode()
	{
		return this.mapMode;
	}
}
