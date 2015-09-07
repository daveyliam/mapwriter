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
    	private Boolean Background;
    	private Boolean AllowFlip;
    	private int parentWidth;
    	private int parentHeight;
    	private String str1;
    	private String str2;
    	private String[] s1;
    	private String[] s2;
    	
    	private MwGuiLabel label;
    	private Side side = Side.none;
    	
    	private enum Side
    	{
    		left,right,top,bottom,none
    	}
    	
    	public MwGuiLabel(String[] s1, String[] s2, int x, int y, Boolean Background, Boolean AllowFlip, int parentWidth, int parentHeight) 
    	{	
    		this.Background = Background;
    		this.AllowFlip = AllowFlip;
    		
    		this.parentWidth = parentWidth;
    		this.parentHeight = parentHeight;
    		
    		this.setCoords(x, y);
    		this.setText(s1, s2);
    	}
    	
    	public void draw() 
    	{   
    		this.updateCoords();
    		if (str1 != null)
    		{
	    		if(Background)
	    		{
	    			Gui.drawRect(this.x - spacingX, this.y - this.spacingY, this.x + this.w + this.spacingX, this.h + this.y + this.spacingY, 0x80000000);
	    		}
	    		
	    		this.fontRendererObj.drawSplitString(str1, this.x , this.y, this.w, 0xffffff);
	    		
	    		if (this.str2 != null)
	    		{
	    			this.fontRendererObj.drawSplitString(str2, this.x + 65, this.y, this.w, 0xffffff);
	    		}
    		}
    	}
    	
    	public void drawToRightOf(MwGuiLabel label) 
    	{
    		this.label = label;
    		this.side = Side.right;
    	}
    	
    	public void drawToLeftOf(MwGuiLabel label) {
    		this.label = label;
    		this.side = Side.left;
    	}
    	
    	public void drawToBelowOf(MwGuiLabel label) {
    		this.label = label;
    		this.side = Side.bottom;
    	}
    	
    	public void drawToAboveOf(MwGuiLabel label) {
    		this.label = label;
    		this.side = Side.top;
    	}

    	private void updateCoords()
    	{
    	      switch (this.side) {
              case left:
            	  this.setCoords(this.label.x - (this.w + (2 * spacingX) + 2), this.label.y);
                  break;
                      
              case right:
            	  this.setCoords(this.label.x + this.label.w + (2 * spacingX) + 2, this.label.y);
                  break;
                           
              case bottom:
            	  this.setCoords(this.label.x, this.label.y + this.label.h + (2 * spacingY) + 2);
                  break;
               
              case top:
            	  this.setCoords(this.label.x, this.label.y - (this.h + (2 * spacingY) + 2));
                  break;
                  
              default:
            	  break;
    	      }
    	}
    	
    	public boolean posWithin(int x, int y) {
    		return (x >= this.x + spacingX) && (y >= this.y + spacingY) && (x <= (this.x + this.w + spacingX)) && (y <= (this.y + this.h + spacingY));
    	}

    	public void setDrawBackground(boolean enable)
    	{
    		Background = enable;	
    	}
    	public boolean getDrawBackground()
    	{
    		return Background;	
    	}
    	
    	public void setAllowFlip(boolean enable)
    	{
    		Background = enable;	
    	}
    	public boolean getAllowFlip()
    	{
    		return Background;	
    	}
    	
    	public int getparentWidth()
    	{
    		return parentWidth;	
    	}

    	public int getparentHeight()
    	{
    		return parentHeight;	
    	}
    	
    	public void setCoords(int x, int y)
    	{
    		if (AllowFlip)
    		{
	    		if (x + this.w + this.spacingX > this.parentWidth)
	    		{
	    			this.x = x - this.w - this.spacingX - 5;
	    		}
	    		else
	    		{
	        		this.x = x;
	    		}
	    		if (y + this.h + this.spacingY > this.parentHeight)
	    		{
	    			this.y = y - this.h - this.spacingY;
	    		}
	    		else
	    		{
	        		this.y = y;  			
	    		}
    		}
    		else
    		{
    			this.x = x;
    			this.y = y;	  
    		}
    	}
    	
    	public void setParentWidthAndHeight(int width, int height)
    	{
    		this.parentWidth = width;
    		this.parentHeight = height;
    		
    		this.updateWidthAndHeight();
    	}
    	
    	public void setText(String[] s1, String[] s2)
    	{
    		this.s1 = s1;
    		this.s2 = s2;
    		this.UpdateStrings();
    	}
    	
    	private void UpdateStrings()
    	{
    		if (s1 != null && s1.length >0)
    		{
    			str1 = Utils.stringArrayToString(this.s1);
    		}
    		if (s2 != null && s2.length >0)
    		{
    			str2 = Utils.stringArrayToString(this.s2);
    		}
    		this.updateWidthAndHeight();
    	}
    	private void updateWidthAndHeight()
    	{
    		if (s1 != null)
    		{
    			int stringwidth = Utils.getMaxWidth(s1, s2);
    			this.w = stringwidth < parentWidth - 20 ?  stringwidth : parentWidth - 20;
    			this.h = fontRendererObj.splitStringWidth(str1, parentWidth > 0 ? parentWidth : 10);
    		}
    	}
}
