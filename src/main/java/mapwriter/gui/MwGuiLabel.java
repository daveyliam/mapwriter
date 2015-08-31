package mapwriter.gui;

import mapwriter.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

public class MwGuiLabel 
{
    	int x = 0, y = 0, w = 1, h = 12;
    	int spacingX = 10;
    	int spacingY = 2;
    	private FontRenderer fontRendererObj = Minecraft.getMinecraft().fontRendererObj;;
    	private final GuiScreen parentScreen;
    	
    	public MwGuiLabel(GuiScreen parentScreen) 
    	{
    		this.parentScreen = parentScreen;
    	}
    	
    	public void draw(String[] s1, String[] s2, int x, int y, Boolean Background, Boolean AllowFlip) {
    		this.x = x;
    		this.y = y;

    		int stringwidth = Utils.getMaxWidth(s1, s2);
    		this.w = stringwidth < parentScreen.width - 20 ?  stringwidth : parentScreen.width - 20;
    		
    		String str = Utils.stringArrayToString(s1);
    		this.h = fontRendererObj.splitStringWidth(str, parentScreen.width);
    		
    		if (AllowFlip)
    		{
	    		if (this.x + this.w + (spacingX * 2) > parentScreen.width)
	    		{
	    			this.x = this.x - this.w - spacingX - 5;
	    		}
	    		if (this.y + this.h + (spacingY * 2) > parentScreen.height)
	    		{
	    			this.y = this.y - this.h - spacingY;
	    		}
    		}
    		
    		if (this.y + this.h + (spacingY * 2) > parentScreen.height)
    		{
    			return;
    		}
    		
    		if(Background)
    		{
    			Gui.drawRect(this.x, this.y, this.x + this.w + spacingX, this.h + this.y + spacingY, 0x80000000);
    		}
    		
    		this.fontRendererObj.drawSplitString(str, this.x + 5 , this.y + 2, this.w, 0xffffff);
    		
    		if (s2 != null)
    		{
    			this.fontRendererObj.drawSplitString(Utils.stringArrayToString(s2), this.x + 65, this.y + 2, this.w, 0xffffff);
    		}
    	}
    	
    	public void drawToRightOf(MwGuiLabel label, String[] s, Boolean Background, Boolean AllowFlip) {
    		this.draw(s, null, label.x + label.w + spacingX + 2, label.y, Background, AllowFlip);
    	}
    	
    	public void drawToLeftOf(MwGuiLabel label, String[] s, Boolean Background, Boolean AllowFlip) {
    		this.draw(s, null, label.x - label.w - spacingX + 2, label.y, Background, AllowFlip);
    	}
    	
    	public void drawToBelowOf(MwGuiLabel label, String[] s, Boolean Background, Boolean AllowFlip) {
    		this.draw(s, null, label.x, label.y + label.h + spacingY + 2, Background, AllowFlip);
    	}
    	
    	public void drawToAboveOf(MwGuiLabel label, String[] s, Boolean Background, Boolean AllowFlip) {
    		this.draw(s, null, label.x, label.y + label.h + spacingY + 2, Background, AllowFlip);
    	}

    	public boolean posWithin(int x, int y) {
    		return (x >= this.x) && (y >= this.y) && (x <= (this.x + this.w)) && (y <= (this.y + this.h));
    	}
}
