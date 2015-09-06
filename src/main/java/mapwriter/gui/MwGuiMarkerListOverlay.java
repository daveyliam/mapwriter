package mapwriter.gui;

import mapwriter.map.Marker;
import mapwriter.map.MarkerManager;
import mapwriter.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public class MwGuiMarkerListOverlay extends MwGuiSlot
{	
	private final GuiScreen parentScreen;
	private final MarkerManager markerManager;
	
	public static int listWidth = 95;
	public static int ListY = 10;
	private int height;
	private int x;
	
	public MwGuiMarkerListOverlay(GuiScreen parentScreen, MarkerManager markerManager)
	{
		super(
				Minecraft.getMinecraft(),		//mcIn
				listWidth, 						//width
				parentScreen.height - 20, 		//height
				ListY, 							//topIn
				10 + parentScreen.height - 20, 	//bottomIn
				parentScreen.width - 110		//left
				);
		
		this.x = parentScreen.width - 110;
		this.height = parentScreen.height - 20;
		
		this.parentScreen = parentScreen;
		this.markerManager = markerManager;
	}

	@Override
	protected int getSlotHeight(int index)
	{
		String str = Utils.stringArrayToString(getLabelString(index));
		int height = mc.fontRendererObj.splitStringWidth(str, this.listWidth - 6);
		
		height += this.spacingY * 2;
		
		return height;
	}
	
	protected String[] getLabelString(int index)
	{
		Marker m = markerManager.visibleMarkerList.get(index);
		
		String[] text = new String[2];
		text[0] = m.name;	
		text[1] = String.format(
				"(%d, %d, %d)",
				m.x, 
				m.y, 
				m.z);
		return text;
	}
	
	@Override
	protected int getSize() 
	{
		return markerManager.visibleMarkerList.size();
	}

	@Override
	protected void elementClicked(int slotIndex, boolean isDoubleClick,	int mouseX, int mouseY, int mouseButton) 
	{
		markerManager.selectedMarker = markerManager.visibleMarkerList.get(slotIndex);
		if (mouseButton == 1)
		{
			if (parentScreen instanceof MwGui)
			{
				((MwGui)parentScreen).openMarkerGui(markerManager.selectedMarker, mouseX, mouseY);;
			}
		}
		if (mouseButton == 0 && isDoubleClick)
		{
			if (parentScreen instanceof MwGui)
			{
				((MwGui)parentScreen).centerOnSelectedMarker();
			}
		}
	}

	@Override
	protected boolean isSelected(int slotIndex) 
	{
		return markerManager.selectedMarker == markerManager.visibleMarkerList.get(slotIndex);
	}

	@Override
	protected void drawBackground() 
	{	
	}
	
	@Override
	protected void drawSlot(int entryID, int x, int y, int slotHeight, int mouseXIn, int mouseYIn) 
	{
		MwGuiLabel label = new MwGuiLabel();

		label.draw(getLabelString(entryID), null, x, y, false, false, this.listWidth, this.height);
	}
	
	@Override
    public void setDimensions(int widthIn, int heightIn, int topIn, int bottomIn, int left)
    {
		this.x = parentScreen.width - 110;
		this.height = parentScreen.height - 20;
		
        super.setDimensions(widthIn, heightIn, topIn, bottomIn, left);
    }
}
