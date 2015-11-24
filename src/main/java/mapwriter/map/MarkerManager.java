package mapwriter.map;

import static org.lwjgl.opengl.ARBDepthClamp.GL_DEPTH_CLAMP;

import java.util.ArrayList;
import java.util.List;

import mapwriter.config.Config;
import mapwriter.config.WorldConfig;
import mapwriter.map.mapmode.MapMode;
import mapwriter.util.Logging;
import mapwriter.util.Reference;
import mapwriter.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraftforge.common.config.Configuration;

import org.lwjgl.opengl.GL11;

public class MarkerManager
{

	public List<Marker> markerList = new ArrayList<Marker>();
	public List<String> groupList = new ArrayList<String>();

	public List<Marker> visibleMarkerList = new ArrayList<Marker>();

	private String visibleGroupName = "none";

	public Marker selectedMarker = null;

	public MarkerManager()
	{
	}

	public void load(Configuration config, String category)
	{
		this.markerList.clear();

		if (config.hasCategory(category))
		{
			int markerCount = config.get(category, "markerCount", 0).getInt();
			this.visibleGroupName = config.get(category, "visibleGroup", "").getString();

			if (markerCount > 0)
			{
				for (int i = 0; i < markerCount; i++)
				{
					String key = "marker" + i;
					String value = config.get(category, key, "").getString();
					Marker marker = this.stringToMarker(value);
					if (marker != null)
					{
						this.addMarker(marker);
					}
					else
					{
						Logging.log("error: could not load " + key + " from config file");
					}
				}
			}
		}

		this.update();
	}

	public void save(Configuration config, String category)
	{
		config.removeCategory(config.getCategory(category));
		config.get(category, "markerCount", 0).set(this.markerList.size());
		config.get(category, "visibleGroup", "").set(this.visibleGroupName);

		int i = 0;
		for (Marker marker : this.markerList)
		{
			String key = "marker" + i;
			String value = this.markerToString(marker);
			config.get(category, key, "").set(value);
			i++;
		}
		
		if (config.hasChanged())
		{
			config.save();
		}
	}

	public void setVisibleGroupName(String groupName)
	{
		if (groupName != null)
		{
			this.visibleGroupName = Utils.mungeStringForConfig(groupName);
		}
		else
		{
			this.visibleGroupName = "none";
		}
	}

	public String getVisibleGroupName()
	{
		return this.visibleGroupName;
	}

	public void clear()
	{
		this.markerList.clear();
		this.groupList.clear();
		this.visibleMarkerList.clear();
		this.visibleGroupName = "none";
	}

	public String markerToString(Marker marker)
	{
		return String.format("%s:%d:%d:%d:%d:%06x:%s", marker.name, marker.x, marker.y, marker.z, marker.dimension, marker.colour & 0xffffff, marker.groupName);
	}

	public Marker stringToMarker(String s)
	{
		// new style delimited with colons
		String[] split = s.split(":");
		if (split.length != 7)
		{
			// old style was space delimited
			split = s.split(" ");
		}
		Marker marker = null;
		if (split.length == 7)
		{
			try
			{
				int x = Integer.parseInt(split[1]);
				int y = Integer.parseInt(split[2]);
				int z = Integer.parseInt(split[3]);
				int dimension = Integer.parseInt(split[4]);
				int colour = 0xff000000 | Integer.parseInt(split[5], 16);

				marker = new Marker(split[0], split[6], x, y, z, dimension, colour);

			}
			catch (NumberFormatException e)
			{
				marker = null;
			}
		}
		else
		{
			Logging.log("Marker.stringToMarker: invalid marker '%s'", s);
		}
		return marker;
	}

	public void addMarker(Marker marker)
	{
		this.markerList.add(marker);
		this.save(WorldConfig.getInstance().worldConfiguration, Reference.catMarkers);
	}
	
	public void addMarker(String name, String groupName, int x, int y, int z, int dimension, int colour)
	{
		this.addMarker(new Marker(name, groupName, x, y, z, dimension, colour));
	}

	// returns true if the marker exists in the arraylist.
	// safe to pass null.
	public boolean delMarker(Marker markerToDelete)
	{
		if (this.selectedMarker == markerToDelete)
		{
			this.selectedMarker = null;
		}
		boolean result = this.markerList.remove(markerToDelete);
		
		this.save(WorldConfig.getInstance().worldConfiguration, Reference.catMarkers);
		
		return result;
	}

	// deletes the first marker with matching name and group.
	// if null is passed as either name or group it means "any".
	public boolean delMarker(String name, String group)
	{
		Marker markerToDelete = null;
		for (Marker marker : this.markerList)
		{
			if (((name == null) || marker.name.equals(name)) && ((group == null) || marker.groupName.equals(group)))
			{
				markerToDelete = marker;
				break;
			}
		}
		// will return false if a marker matching the criteria is not found
		// (i.e. if markerToDelete is null)
		return this.delMarker(markerToDelete);
	}

	public void update()
	{
		this.visibleMarkerList.clear();
		this.groupList.clear();
		this.groupList.add("none");
		this.groupList.add("all");
		for (Marker marker : this.markerList)
		{
			if (marker.groupName.equals(this.visibleGroupName) || this.visibleGroupName.equals("all"))
			{
				this.visibleMarkerList.add(marker);
			}
			if (!this.groupList.contains(marker.groupName))
			{
				this.groupList.add(marker.groupName);
			}
		}
		if (!this.groupList.contains(this.visibleGroupName))
		{
			this.visibleGroupName = "none";
		}
	}

	public void nextGroup(int n)
	{
		if (this.groupList.size() > 0)
		{
			int i = this.groupList.indexOf(this.visibleGroupName);
			int size = this.groupList.size();
			if (i != -1)
			{
				i = (i + size + n) % size;
			}
			else
			{
				i = 0;
			}
			this.visibleGroupName = this.groupList.get(i);
		}
		else
		{
			this.visibleGroupName = "none";
			this.groupList.add("none");
		}
	}

	public void nextGroup()
	{
		this.nextGroup(1);
	}

	public int countMarkersInGroup(String group)
	{
		int count = 0;
		if (group.equals("all"))
		{
			count = this.markerList.size();
		}
		else
		{
			for (Marker marker : this.markerList)
			{
				if (marker.groupName.equals(group))
				{
					count++;
				}
			}
		}
		return count;
	}

	public void selectNextMarker()
	{
		if (this.visibleMarkerList.size() > 0)
		{
			int i = 0;
			if (this.selectedMarker != null)
			{
				i = this.visibleMarkerList.indexOf(this.selectedMarker);
				if (i == -1)
				{
					i = 0;
				}
			}
			i = (i + 1) % this.visibleMarkerList.size();
			this.selectedMarker = this.visibleMarkerList.get(i);
		}
		else
		{
			this.selectedMarker = null;
		}
	}

	public Marker getNearestMarker(int x, int z, int maxDistance)
	{
		int nearestDistance = maxDistance * maxDistance;
		Marker nearestMarker = null;
		for (Marker marker : this.visibleMarkerList)
		{
			int dx = x - marker.x;
			int dz = z - marker.z;
			int d = (dx * dx) + (dz * dz);
			if (d < nearestDistance)
			{
				nearestMarker = marker;
				nearestDistance = d;
			}
		}
		return nearestMarker;
	}

	public Marker getNearestMarkerInDirection(int x, int z, double desiredAngle)
	{
		int nearestDistance = 10000 * 10000;
		Marker nearestMarker = null;
		for (Marker marker : this.visibleMarkerList)
		{
			int dx = marker.x - x;
			int dz = marker.z - z;
			int d = (dx * dx) + (dz * dz);
			double angle = Math.atan2(dz, dx);
			// use cos instead of abs as it will wrap at 2 * Pi.
			// cos will be closer to 1.0 the closer desiredAngle and angle are.
			// 0.8 is the threshold corresponding to a maximum of
			// acos(0.8) = 37 degrees difference between the two angles.
			if ((Math.cos(desiredAngle - angle) > 0.8D) && (d < nearestDistance) && (d > 4))
			{
				nearestMarker = marker;
				nearestDistance = d;
			}
		}
		return nearestMarker;
	}

	public void drawMarkers(MapMode mapMode, MapView mapView)
	{
		for (Marker marker : this.visibleMarkerList)
		{
			// only draw markers that were set in the current dimension
			if (mapView.getDimension() == marker.dimension)
			{
				marker.draw(mapMode, mapView, 0xff000000);
			}
		}
		if (this.selectedMarker != null)
		{
			this.selectedMarker.draw(mapMode, mapView, 0xffffffff);
		}
	}

	public void drawMarkersWorld(float partialTicks)
	{
		if (!Config.drawMarkersInWorld && !Config.drawMarkersNameInWorld)
		{
			return;
		}

		for (Marker m : this.visibleMarkerList)
		{
			if (m.dimension == Minecraft.getMinecraft().thePlayer.dimension)
			{
				if (Config.drawMarkersInWorld)
				{
					this.drawBeam(m, partialTicks);
				}
				if (Config.drawMarkersNameInWorld)
				{
					this.drawLabel(m);
				}
			}
		}
	}

	public void drawBeam(Marker m, float partialTicks)
	{
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer worldrenderer = tessellator.getWorldRenderer();

		float f2 = Minecraft.getMinecraft().theWorld.getTotalWorldTime() + partialTicks;
		double d3 = f2 * 0.025D * -1.5D;
		// the height of the beam always to the max height
		double d17 = 255.0D;

		double x = m.x - TileEntityRendererDispatcher.staticPlayerX;
		double y = 0.0D - TileEntityRendererDispatcher.staticPlayerY;
		double z = m.z - TileEntityRendererDispatcher.staticPlayerZ;

		GlStateManager.pushMatrix();
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.depthMask(false);

		worldrenderer.startDrawingQuads();
		// size of the square from middle to edge
		double d4 = 0.2D;

		double d5 = 0.5D + (Math.cos(d3 + 2.356194490192345D) * d4);
		double d6 = 0.5D + (Math.sin(d3 + 2.356194490192345D) * d4);
		double d7 = 0.5D + (Math.cos(d3 + (Math.PI / 4D)) * d4);
		double d8 = 0.5D + (Math.sin(d3 + (Math.PI / 4D)) * d4);
		double d9 = 0.5D + (Math.cos(d3 + 3.9269908169872414D) * d4);
		double d10 = 0.5D + (Math.sin(d3 + 3.9269908169872414D) * d4);
		double d11 = 0.5D + (Math.cos(d3 + 5.497787143782138D) * d4);
		double d12 = 0.5D + (Math.sin(d3 + 5.497787143782138D) * d4);

		worldrenderer.setColorRGBA_I(m.colour, 50);

		worldrenderer.addVertex(x + d5, y + d17, z + d6);
		worldrenderer.addVertex(x + d5, y, z + d6);
		worldrenderer.addVertex(x + d7, y, z + d8);
		worldrenderer.addVertex(x + d7, y + d17, z + d8);
		worldrenderer.addVertex(x + d11, y + d17, z + d12);
		worldrenderer.addVertex(x + d11, y, z + d12);
		worldrenderer.addVertex(x + d9, y, z + d10);
		worldrenderer.addVertex(x + d9, y + d17, z + d10);
		worldrenderer.addVertex(x + d7, y + d17, z + d8);
		worldrenderer.addVertex(x + d7, y, z + d8);
		worldrenderer.addVertex(x + d11, y, z + d12);
		worldrenderer.addVertex(x + d11, y + d17, z + d12);
		worldrenderer.addVertex(x + d9, y + d17, z + d10);
		worldrenderer.addVertex(x + d9, y, z + d10);
		worldrenderer.addVertex(x + d5, y, z + d6);
		worldrenderer.addVertex(x + d5, y + d17, z + d6);
		tessellator.draw();

		worldrenderer.startDrawingQuads();
		worldrenderer.setColorRGBA_I(m.colour, 50);
		// size of the square from middle to edge
		d4 = 0.5D;

		d5 = 0.5D + (Math.sin(d3 + 2.356194490192345D) * d4);
		d6 = 0.5D + (Math.cos(d3 + 2.356194490192345D) * d4);
		d7 = 0.5D + (Math.sin(d3 + (Math.PI / 4D)) * d4);
		d8 = 0.5D + (Math.cos(d3 + (Math.PI / 4D)) * d4);
		d9 = 0.5D + (Math.sin(d3 + 3.9269908169872414D) * d4);
		d10 = 0.5D + (Math.cos(d3 + 3.9269908169872414D) * d4);
		d11 = 0.5D + (Math.sin(d3 + 5.497787143782138D) * d4);
		d12 = 0.5D + (Math.cos(d3 + 5.497787143782138D) * d4);

		worldrenderer.addVertex(x + d5, y + d17, z + d6);
		worldrenderer.addVertex(x + d5, y, z + d6);
		worldrenderer.addVertex(x + d7, y, z + d8);
		worldrenderer.addVertex(x + d7, y + d17, z + d8);
		worldrenderer.addVertex(x + d11, y + d17, z + d12);
		worldrenderer.addVertex(x + d11, y, z + d12);
		worldrenderer.addVertex(x + d9, y, z + d10);
		worldrenderer.addVertex(x + d9, y + d17, z + d10);
		worldrenderer.addVertex(x + d7, y + d17, z + d8);
		worldrenderer.addVertex(x + d7, y, z + d8);
		worldrenderer.addVertex(x + d11, y, z + d12);
		worldrenderer.addVertex(x + d11, y + d17, z + d12);
		worldrenderer.addVertex(x + d9, y + d17, z + d10);
		worldrenderer.addVertex(x + d9, y, z + d10);
		worldrenderer.addVertex(x + d5, y, z + d6);
		worldrenderer.addVertex(x + d5, y + d17, z + d6);
		tessellator.draw();

		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		GlStateManager.depthMask(true);
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
	}

	public void drawLabel(Marker m)
	{
		float growFactor = 0.17F;
		Minecraft mc = Minecraft.getMinecraft();
		RenderManager renderManager = mc.getRenderManager();
		FontRenderer fontrenderer = mc.fontRendererObj;

		double x = (0.5D + m.x) - TileEntityRendererDispatcher.staticPlayerX;
		double y = (0.5D + m.y) - TileEntityRendererDispatcher.staticPlayerY;
		double z = (0.5D + m.z) - TileEntityRendererDispatcher.staticPlayerZ;

		double distance = m.getDistanceToMarker(renderManager.livingPlayer);

		String strText = m.name;
		String strDistance = " (" + (int) distance + "m)";

		int strTextWidth = fontrenderer.getStringWidth(strText) / 2;
		int strDistanceWidth = fontrenderer.getStringWidth(strDistance) / 2;
		int offstet = 9;

		float f = (float) (1.0F + ((distance) * growFactor));
		float f1 = 0.016666668F * f;

		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		GL11.glNormal3f(0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
		GlStateManager.scale(-f1, -f1, f1);
		GlStateManager.disableLighting();
		GlStateManager.depthMask(false);
		GlStateManager.disableDepth();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GL11.glEnable(GL_DEPTH_CLAMP);

		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer worldrenderer = tessellator.getWorldRenderer();

		GlStateManager.disableTexture2D();

		worldrenderer.startDrawingQuads();
		worldrenderer.setColorRGBA_I(m.colour, 64);
		worldrenderer.addVertex(-strTextWidth - 1, (-1), 0.0D);
		worldrenderer.addVertex(-strTextWidth - 1, (8), 0.0D);
		worldrenderer.addVertex(strTextWidth + 1, (8), 0.0D);
		worldrenderer.addVertex(strTextWidth + 1, (-1), 0.0D);
		tessellator.draw();

		worldrenderer.startDrawingQuads();
		worldrenderer.setColorRGBA_I(m.colour, 64);
		worldrenderer.addVertex(-strDistanceWidth - 1, -1 + offstet, 0.0D);
		worldrenderer.addVertex(-strDistanceWidth - 1, 8 + offstet, 0.0D);
		worldrenderer.addVertex(strDistanceWidth + 1, 8 + offstet, 0.0D);
		worldrenderer.addVertex(strDistanceWidth + 1, -1 + offstet, 0.0D);
		tessellator.draw();

		GlStateManager.enableTexture2D();
		GlStateManager.depthMask(true);

		fontrenderer.drawString(strText, -strTextWidth, 0, -1);
		fontrenderer.drawString(strDistance, -strDistanceWidth, offstet, -1);

		GL11.glDisable(GL_DEPTH_CLAMP);
		GlStateManager.enableDepth();
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.popMatrix();
	}
}
