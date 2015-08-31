package mapwriter.gui;

import mapwriter.map.Marker;
import mapwriter.map.MarkerManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public class MwGuiMarkerListOverlay extends GuiScreen
{	
	private final GuiScreen parentScreen;
	private final MarkerManager markerManager;
	
	//TODO: Make the list scrollable and add button toggle on off
	
	public MwGuiMarkerListOverlay(GuiScreen parentScreen, MarkerManager markerManager)
	{
		this.parentScreen = parentScreen;
		this.markerManager = markerManager;
		this.fontRendererObj = Minecraft.getMinecraft().fontRendererObj;
	}
	
	public void draw()
	{
		int x = parentScreen.width - 110;
		int y = 10;
		int w = 110 - 5;
		int h = parentScreen.height - 20;
		Boolean first = true;
		MwGuiLabel prevLabel = new MwGuiLabel(this);
		
		this.width = w;
		this.height = h;
		
		drawRect(x, y, x + w, h + y, 0x80000000);
		y += 5;
		for (Marker m : markerManager.visibleMarkerList)
		{

			MwGuiLabel label = new MwGuiLabel(this);
			
			String[] text = new String[2];
			text[0] = m.name;	
			text[1] = String.format(
					"(%d, %d, %d)",
					m.x, 
					m.y, 
					m.z);
			if (first)
			{
				label.draw(text, null, x, y, false, false);	
				first = false;
			}
			else
			{
				label.drawToBelowOf(prevLabel, text, false, false);
			}
			prevLabel = label;
			
			if (label.y + label.h +  label.h> parentScreen.height)
			{
				break;
			}
		}
	}
}
