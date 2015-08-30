package mapwriter.gui;

import java.util.List;

import org.lwjgl.input.Keyboard;

import mapwriter.util.Reference;
import mapwriter.util.Render;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.texture.TextureManager;

public class ScrollableTextBox extends GuiTextField
	{
		public int x;
		public int y;
		public int width;
		// private int height;
		public int labelX;
		public int labelY;
		public int labelWidth;
		public int labelHeight;
		public String label;
		public boolean drawArrows = false;
		public int leftArrowX;
		public int rightArrowX;
		public int arrowsY;
		public static int arrowsWidth = 7;
		public int arrowsHeight = 12;
		public int textFieldX;
		public int textFieldY;
		public int textFieldWidth;
		public static int textFieldHeight = 12;
		public List<String> scrollableElements;
		private final FontRenderer fontRendererInstance;
		
		ScrollableTextBox(int x, int y, int width, String label, FontRenderer fontrendererObj) 
		{
			super(			
				0,
				fontrendererObj, 
				x + arrowsWidth,
				y, 
				width - arrowsWidth * 2 - 25, 
				textFieldHeight
				);
			
			this.x = x;
			this.y = y;
			this.width = width;
			this.label = label;
			this.fontRendererInstance = fontrendererObj;
			
			this.init();
		}

		ScrollableTextBox(int x, int y, int width, String label, List<String> scrollableElements, FontRenderer fontrendererObj) 
		{
			super(			
				0,
				fontrendererObj, 
				x + arrowsWidth,
				y, 
				width - arrowsWidth * 2 - 25, 
				textFieldHeight
				);
			
			this.x = x;
			this.y = y;
			this.width = width;
			this.label = label;
			this.scrollableElements = scrollableElements;
			this.fontRendererInstance = fontrendererObj;
			this.init();
		}

		private void init() {
			setMaxStringLength(32);
			this.textFieldX = x + arrowsWidth;
			this.textFieldY = this.y;
			this.textFieldWidth = width - arrowsWidth * 2 - 25;
			this.labelWidth = fontRendererInstance
					.getStringWidth(this.label);
			this.labelHeight = this.fontRendererInstance.FONT_HEIGHT;
			this.labelX = this.x - this.labelWidth - 4;
			this.labelY = this.y + this.labelHeight / 2 - 2;
			this.leftArrowX = this.x - 1;
			this.rightArrowX = this.textFieldX + this.textFieldWidth + 1;
			this.arrowsY = this.y;
		}

		public void draw() {
			drawString(this.fontRendererInstance, this.label, this.labelX, this.labelY, 0xffffff);
			if (this.drawArrows) 
			{
				TextureManager renderEngine = Minecraft.getMinecraft().renderEngine;
				renderEngine.bindTexture(Reference.leftArrowTexture);
				Render.drawTexturedRect(
						this.leftArrowX, 
						this.arrowsY,
						this.arrowsWidth, 
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
						this.arrowsWidth, 
						this.arrowsHeight, 
						0.0, 
						0.0, 
						1.0, 
						1.0
						);
			}
			drawTextBox();
			if (!this.validateTextFieldData()) {
				drawRect(this.textFieldX - 1, this.textFieldY - 1,
						this.textFieldX + this.textFieldWidth + 1,
						this.textFieldY, 
						0xff900000);
				drawRect(this.textFieldX - 1, this.textFieldY - 1,
						this.textFieldX, this.textFieldY + this.textFieldHeight	+ 1,
						0xff900000);
				drawRect(this.textFieldX + this.textFieldWidth + 1,
						this.textFieldY + this.textFieldHeight + 1,
						this.textFieldX,
						this.textFieldY + this.textFieldHeight, 
						0xff900000);
				drawRect(this.textFieldX + this.textFieldWidth + 1,
						this.textFieldY + this.textFieldHeight + 1,
						this.textFieldX + this.textFieldWidth, this.textFieldY,
						0xff900000);
			}
		}

		public void mouseClicked(int x, int y, int button) {
			int direction = this.posWithinArrows(x, y);
			if (direction != 0)
				this.textFieldScroll(direction);
			super.mouseClicked(x, y, button);
		}

		public void setDrawArrows(boolean value) {
			this.drawArrows = value;
		}

		public void mouseDWheelScrolled(int x, int y, int direction) {
			if (posWithinTextField(x, y))
				textFieldScroll(-direction);
		}

		public boolean validateTextFieldData() {
			return getText().length() > 0;
		}

		/**
		 * 
		 * @return Returns clicked arrow: 1 for right and -1 for left
		 */
		public int posWithinArrows(int x, int y) {
			if ((x >= this.leftArrowX) && (y >= this.arrowsY)
					&& (x <= this.arrowsWidth + this.leftArrowX)
					&& (y <= this.arrowsHeight + this.arrowsY))
				return -1;
			else if ((x >= this.rightArrowX) && (y >= this.arrowsY)
					&& (x <= this.arrowsWidth + this.rightArrowX)
					&& (y <= this.arrowsHeight + this.arrowsY))
				return 1;
			else
				return 0;
		}

		public boolean posWithinTextField(int x, int y) {
			return (x >= this.textFieldX) && (y >= this.textFieldY)
					&& (x <= this.textFieldWidth + this.textFieldX)
					&& (y <= this.textFieldHeight + this.textFieldY);
		}

		public void textFieldScroll(int direction) {
			if (this.scrollableElements != null) {
				int index = this.scrollableElements.indexOf(getText().trim());
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
				setText(this.scrollableElements.get(index));
			}
		}
	}

class ScrollableNumericTextBox extends ScrollableTextBox {

	public ScrollableNumericTextBox(int x, int y, int width, String label, FontRenderer fontrendererObj) {
		super(x, y, width, label, fontrendererObj);
	}

	@Override
	public void textFieldScroll(int direction) {
		if (this.validateTextFieldData()) {
			int value = this.getTextFieldIntValue();
			if (direction > 0)
				setText("" + (value + 1));
			else if (direction < 0)
				setText("" + (value - 1));
		}
	}

	public int getTextFieldIntValue() {
		return Integer.parseInt(getText());
	}

	public void validateTextboxKeyTyped(char c, int key) {
		if ((c >= '0' && c <= '9') || key == Keyboard.KEY_BACK
				|| key == Keyboard.KEY_LEFT || key == Keyboard.KEY_RIGHT
				|| (c == '-' && (getCursorPosition() == 0)))
			textboxKeyTyped(c, key);
	}
}

