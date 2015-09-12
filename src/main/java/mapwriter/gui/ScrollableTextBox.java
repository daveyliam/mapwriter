package mapwriter.gui;

import java.util.List;

import mapwriter.util.Reference;
import mapwriter.util.Render;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.texture.TextureManager;

import org.lwjgl.input.Keyboard;

public class ScrollableTextBox extends ScrollableField
	{		
		public int textFieldX;
		public int textFieldY;
		public int textFieldWidth;
		private static int textFieldHeight = 12;
		
		public List<String> scrollableElements;
		
		protected GuiTextField textField;
		
		ScrollableTextBox(int x, int y, int width, String label, FontRenderer fontrendererObj) 
		{	
			super(x, y, width, label, fontrendererObj);
			this.init();
		}

		ScrollableTextBox(int x, int y, int width, String label, List<String> scrollableElements, FontRenderer fontrendererObj) 
		{			
			super(x, y, width, label, fontrendererObj);
			this.scrollableElements = scrollableElements;
			this.init();
		}

		private void init() 
		{
			this.textFieldX = this.x + this.arrowsWidth + 3;
			this.textFieldY = this.y;
			this.textFieldWidth = this.width - 5 - ScrollableField.arrowsWidth * 2;
			
			textField = new GuiTextField(
					0,
					this.fontrendererObj,
					this.textFieldX,
					this.textFieldY, 
					this.textFieldWidth, 
					this.textFieldHeight
					);
			
			textField.setMaxStringLength(32);

		}

		@Override
		public void draw() 
		{
			super.draw();
			this.textField.drawTextBox();
			if (!this.validateTextFieldData()) 
			{
				//draw a red rectangle over the textbox to indicate that the text is invallid
				int x1 = this.textFieldX - 1;
				int y1 = this.textFieldY - 1;
				int x2 = this.textFieldX + this.textFieldWidth;
				int y2 = this.textFieldY + ScrollableTextBox.textFieldHeight;
				int colour = 0xff900000;
						
				this.drawHorizontalLine(x1, x2, y1, colour);
				this.drawHorizontalLine(x1, x2, y2, colour);
				
				this.drawVerticalLine(x1, y1, y2, colour);
				this.drawVerticalLine(x2, y1, y2, colour);
			}
		}

		@Override
		public void mouseClicked(int x, int y, int button)
		{
			super.mouseClicked(x, y, button);
			this.textField.mouseClicked(x, y, button);		
		}
		
		public void mouseDWheelScrolled(int x, int y, int direction) {
			if (posWithinTextField(x, y))
				textFieldScroll(-direction);
		}

		public boolean validateTextFieldData() {
			return this.getText().length() > 0;
		}

		public boolean posWithinTextField(int x, int y) {
			return (x >= this.textFieldX) && (y >= this.textFieldY)
					&& (x <= this.textFieldWidth + this.textFieldX)
					&& (y <= ScrollableTextBox.textFieldHeight + this.textFieldY);
		}

		public void textFieldScroll(int direction) {
			if (this.scrollableElements != null) {
				int index = this.scrollableElements.indexOf( this.getText().trim());
				if (direction > 0) {
					if (index == -1
							|| index == this.scrollableElements.size() - 1)
						index = 0;
					else
						index++;
				} else if (direction < 0) {
					if (index == -1 || index == 0)
						index = this.scrollableElements.size() - 1;
					else
						index--;
				}
				 this.textField.setText(this.scrollableElements.get(index));
			}
		}

		@Override
		public void nextElement() 
		{
			this.textFieldScroll(1);
		}

		@Override
		public void previousElement() 
		{
			this.textFieldScroll(-1);			
		}
	
		public String getText()
		{
			return this.textField.getText();
		}
		
		public void setText(String text)
		{
			this.textField.setText(text);
		}
	
		public void setFocused(Boolean focus)
		{
			this.textField.setFocused(focus);
			this.textField.setSelectionPos(0);
		}
		
		public Boolean isFocused()
		{
			return this.textField.isFocused();
		}
		
		public void KeyTyped(char c, int key)
		{
			this.textField.textboxKeyTyped(c, key);
		}
		
		public int getCursorPosition()
		{
			return this.textField.getCursorPosition();
		}
		
		public void setCursorPositionEnd()
		{
			this.textField.setCursorPositionEnd();
		}
	}

class ScrollableNumericTextBox extends ScrollableTextBox {

	public int maxValue = -1;
	public int minValue = -1;
	
	public ScrollableNumericTextBox(int x, int y, int width, String label, FontRenderer fontrendererObj) {
		super(x, y, width, label, fontrendererObj);
	}

	@Override
	public void textFieldScroll(int direction) 
	{
		int newValue = 0;
		if (this.validateTextFieldData()) 
		{
			newValue = this.getTextFieldIntValue();
			if (direction > 0)
			{
				if(this.maxValue < 0 || newValue + 1 <= this.maxValue)
				{
					newValue += 1;
				}
			}
			else if (direction < 0)
			{
				if(this.minValue < 0 || newValue - 1 >= this.minValue)
				{
					newValue -= 1;
				}
			}
		}
			this.setText(newValue);
	}

	public int getTextFieldIntValue() 
	{
	     try 
	     {
	    	 return Integer.parseInt(this.getText());
	     }
	     catch (NumberFormatException e) 
	     {
	    	 return 0;
	     }  
	}
	 
	public void setText(int num)
	{
		if(this.maxValue < 0 || num <= this.maxValue || num >= this.minValue )
		{
			this.setText(Integer.toString(num));	
		}
	}

	@Override
	public void KeyTyped(char c, int key) 
	{
		if ((c >= '0' && c <= '9') || key == Keyboard.KEY_BACK
				|| key == Keyboard.KEY_LEFT || key == Keyboard.KEY_RIGHT
				|| (c == '-' && (this.getCursorPosition() == 0))
				)
		{
			if (Character.isDigit(c) && (this.maxValue > -1 && Integer.parseInt(this.getText() + c) > this.maxValue))
			{
				return;
			}
			super.KeyTyped(c, key);
		}
	}
	
	public void setMaxValue(int max)
	{
		this.maxValue = max;
		this.textField.setMaxStringLength(Integer.toString(max).length());
	}
	
	public void setMinValue(int min)
	{
		this.minValue = min;
	}
}

