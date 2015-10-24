package mapwriter.map;

import java.awt.Point;

import mapwriter.map.mapmode.MapMode;
import mapwriter.util.Render;
import mapwriter.util.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

public class Marker
{
	public final String name;
	public final String groupName;
	public int x;
	public int y;
	public int z;
	public int dimension;
	public int colour;

	public Point.Double screenPos = new Point.Double(0, 0);

	public Marker(String name, String groupName, int x, int y, int z, int dimension, int colour)
	{
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.dimension = dimension;
		this.colour = colour;
		this.groupName = groupName;
	}

	public String getString()
	{
		return String.format("%s %s (%d, %d, %d) %d %06x", this.name, this.groupName, this.x, this.y, this.z, this.dimension, this.colour & 0xffffff);
	}

	public void colourNext()
	{
		this.colour = Utils.getNextColour();
	}

	public void colourPrev()
	{
		this.colour = Utils.getPrevColour();
	}

	public void draw(MapMode mapMode, MapView mapView, int borderColour)
	{
		double scale = mapView.getDimensionScaling(this.dimension);
		Point.Double p = mapMode.getClampedScreenXY(mapView, this.x * scale, this.z * scale);
		this.screenPos.setLocation(p.x + mapMode.xTranslation, p.y + mapMode.yTranslation);

		// draw a coloured rectangle centered on the calculated (x, y)
		double mSize = mapMode.config.markerSize;
		double halfMSize = mapMode.config.markerSize / 2.0;
		Render.setColour(borderColour);
		Render.drawRect(p.x - halfMSize, p.y - halfMSize, mSize, mSize);
		Render.setColour(this.colour);
		Render.drawRect((p.x - halfMSize) + 0.5, (p.y - halfMSize) + 0.5, mSize - 1.0, mSize - 1.0);
	}

	// arraylist.contains was producing unexpected results in some situations
	// rather than figure out why i'll just control how two markers are compared
	@Override
	public boolean equals(final Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o instanceof Marker)
		{
			Marker m = (Marker) o;
			return (this.name == m.name) && (this.groupName == m.groupName) && (this.x == m.x) && (this.y == m.y) && (this.z == m.z) && (this.dimension == m.dimension);
		}
		return false;
	}

	public double getDistanceToMarker(Entity entityIn)
	{
		double d0 = this.x - entityIn.posX;
		double d1 = this.y - entityIn.posY;
		double d2 = this.z - entityIn.posZ;
		return MathHelper.sqrt_double((d0 * d0) + (d1 * d1) + (d2 * d2));
	}
}