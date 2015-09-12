package mapwriter.gui;

import mapwriter.util.Reference;
import mapwriter.util.Render;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.TextureManager;

public abstract class ScrollableField extends Gui
{
	public int x;
	public int y;
	public int width;
	
	public int labelX;
	public int labelY;
	public int labelWidth;
	public int labelHeight;
	public String label;
	
	private boolean drawArrows = false;
	private int leftArrowX;
	private int rightArrowX;
	private int arrowsY;
	public static int arrowsWidth = 7;
	private int arrowsHeight = 12;
	
	public final FontRenderer fontrendererObj;
	
	public ScrollableField(int x, int y, int width, String label, FontRenderer fontrendererObj)
	{
		this.x = x;
		this.y = y;
		this.width = width;
		
		this.fontrendererObj = fontrendererObj;
		this.label = label;
		
		this.leftArrowX = this.x + 1;
		this.rightArrowX = this.x + (this.width - ScrollableField.arrowsWidth);
		this.arrowsY = this.y;
		
		this.labelWidth = fontrendererObj.getStringWidth(this.label);
		this.labelHeight = this.fontrendererObj.FONT_HEIGHT;
		this.labelX = this.x - this.labelWidth;
		this.labelY = this.y + this.labelHeight / 2 - 2;
	}
	
	public void draw()
	{
		TextureManager renderEngine = Minecraft.getMinecraft().renderEngine;
		//Render.drawRectBorder(labelX, y, width + this.labelWidth + 4, this.arrowsHeight, 2);
		
		//draw the description label
		drawString(this.fontrendererObj, this.label, this.labelX, this.labelY, 0xffffff);
		
		if (this.drawArrows) 
		{
			renderEngine.bindTexture(Reference.leftArrowTexture);
			Render.drawTexturedRect(
					this.leftArrowX, 
					this.arrowsY,
					ScrollableField.arrowsWidth, 
					this.arrowsHeight, 
					0.0, 
					0.0, 
					1.0, 
					1.0
					);
			renderEngine.bindTexture(Reference.rightArrowTexture);
			Render.drawTexturedRect(
					this.rightArrowX, 
					this.arrowsY,
					ScrollableField.arrowsWidth, 
					this.arrowsHeight, 
					0.0, 
					0.0, 
					1.0, 
					1.0
					);
		}
	}
	
	public void setDrawArrows(boolean value) {
		this.drawArrows = value;
	}

	public void mouseClicked(int x, int y, int button) {
		int direction = this.posWithinArrows(x, y);
		if (direction == 1)
		{
			this.nextElement();
		}
		else if(direction == -1)
		{
			this.previousElement();
		}
	}
	
	/**
	 * 
	 * @return Returns clicked arrow: 1 for right and -1 for left
	 */
	public int posWithinArrows(int x, int y) {
		if ((x >= this.leftArrowX) && (y >= this.arrowsY)
				&& (x <= ScrollableField.arrowsWidth + this.leftArrowX)
				&& (y <= this.arrowsHeight + this.arrowsY))
			return -1;
		else if ((x >= this.rightArrowX) && (y >= this.arrowsY)
				&& (x <= ScrollableField.arrowsWidth + this.rightArrowX)
				&& (y <= this.arrowsHeight + this.arrowsY))
			return 1;
		else
			return 0;
	}

	public abstract void nextElement();
	public abstract void previousElement();
	public abstract void setFocused(Boolean focus);
	public abstract Boolean isFocused();
}
