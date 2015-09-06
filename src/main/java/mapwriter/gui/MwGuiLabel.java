package mapwriter.gui;

import mapwriter.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

public class MwGuiLabel 
{
    	int x = 0, y = 0, w = 1, h = 12;
    	static int spacingX = 4;
    	static int spacingY = 2;
    	private FontRenderer fontRendererObj = Minecraft.getMinecraft().fontRendererObj;;
    	
    	
    	public MwGuiLabel() 
    	{	
    	}
    	//TODO: remove all vars to own methods
    	public void draw(String[] s1, String[] s2, int x, int y, Boolean Background, Boolean AllowFlip, int parentWidth, int parentHeight) {
    		this.x = x;
    		this.y = y;

    		int stringwidth = Utils.getMaxWidth(s1, s2);
    		this.w = stringwidth < parentWidth - 20 ?  stringwidth : parentWidth - 20;
    		
    		String str = Utils.stringArrayToString(s1);
    		this.h = fontRendererObj.splitStringWidth(str, parentWidth);
    		
    		if (AllowFlip)
    		{
	    		if (this.x + this.w + spacingX > parentWidth)
	    		{
	    			this.x = this.x - this.w - spacingX - 5;
	    		}
	    		if (this.y + this.h + spacingY > parentHeight)
	    		{
	    			this.y = this.y - this.h - spacingY;
	    		}
    		}
    		
    		if(Background)
    		{
    			Gui.drawRect(this.x - spacingX, this.y - spacingY, this.x + this.w + spacingX, this.h + this.y + spacingY, 0x80000000);
    		}
    		
    		this.fontRendererObj.drawSplitString(str, this.x , this.y, this.w, 0xffffff);
    		
    		if (s2 != null)
    		{
    			this.fontRendererObj.drawSplitString(Utils.stringArrayToString(s2), this.x + 65, this.y, this.w, 0xffffff);
    		}
    	}
    	
    	public void drawToRightOf(MwGuiLabel label, String[] s, Boolean Background, Boolean AllowFlip, int parentWidth, int parentHeight) {
    		this.draw(s, null, label.x + label.w + (2 * spacingX) + 2, label.y, Background, AllowFlip, parentWidth, parentHeight);
    	}
    	
    	public void drawToLeftOf(MwGuiLabel label, String[] s, Boolean Background, Boolean AllowFlip, int parentWidth, int parentHeight) {
    		this.draw(s, null, label.x - label.w - (2 * spacingX) + 2, label.y, Background, AllowFlip, parentWidth, parentHeight);
    	}
    	
    	public void drawToBelowOf(MwGuiLabel label, String[] s, Boolean Background, Boolean AllowFlip, int parentWidth, int parentHeight) {
    		this.draw(s, null, label.x, label.y + label.h + (2 * spacingY) + 2, Background, AllowFlip, parentWidth, parentHeight);
    	}
    	
    	public void drawToAboveOf(MwGuiLabel label, String[] s, Boolean Background, Boolean AllowFlip, int parentWidth, int parentHeight) {
    		this.draw(s, null, label.x, label.y + label.h + (2 * spacingY) + 2, Background, AllowFlip, parentWidth, parentHeight);
    	}

    	public boolean posWithin(int x, int y) {
    		return (x >= this.x + spacingX) && (y >= this.y + spacingY) && (x <= (this.x + this.w + spacingX)) && (y <= (this.y + this.h + spacingY));
    	}
}
