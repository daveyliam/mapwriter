package mapwriter.gui;

import mapwriter.util.Utils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;

public class ScrollableColorSelector extends ScrollableField
{
	private String editRed = "mw.gui.ScrollableColorSelector.Red";
	private String editGreen = "mw.gui.ScrollableColorSelector.Green";
	private String editBlue = "mw.gui.ScrollableColorSelector.Blue";

	private ScrollableNumericTextBox ScrollableNumericTextBoxColourRed;
	private ScrollableNumericTextBox ScrollableNumericTextBoxColourGreen;
	private ScrollableNumericTextBox ScrollableNumericTextBoxColourBlue;

	private int colour = 0;

	private int colourFieldX = 0;
	private int colourFieldY = 0;
	private int colourFieldW = 0;
	private int colourFieldH = 0;

	private int y;

	public ScrollableColorSelector(int x, int y, int width, String label, FontRenderer fontrendererObj)
	{
		super(x, y + MwGuiMarkerDialogNew.elementVSpacing, width, label, fontrendererObj);
		this.y = y;
		this.init();
	}

	private void init()
	{
		int textboxWidth = 16;
		int x1 = this.x + ScrollableField.arrowsWidth + this.fontrendererObj.getStringWidth(I18n.format(this.editGreen, new Object[0])) + 4;
		int w = (ScrollableField.arrowsWidth * 2) + this.fontrendererObj.getStringWidth("999") + textboxWidth;

		this.ScrollableNumericTextBoxColourRed = new ScrollableNumericTextBox(x1, this.y, w, I18n.format(this.editRed, new Object[0]), this.fontrendererObj);
		this.ScrollableNumericTextBoxColourRed.setDrawArrows(true);
		this.ScrollableNumericTextBoxColourRed.setMaxValue(255);
		this.ScrollableNumericTextBoxColourRed.setMinValue(0);

		this.ScrollableNumericTextBoxColourGreen = new ScrollableNumericTextBox(x1, this.y + MwGuiMarkerDialogNew.elementVSpacing, w, I18n.format(this.editGreen, new Object[0]), this.fontrendererObj);
		this.ScrollableNumericTextBoxColourGreen.setDrawArrows(true);
		this.ScrollableNumericTextBoxColourGreen.setMaxValue(255);
		this.ScrollableNumericTextBoxColourGreen.setMinValue(0);

		this.ScrollableNumericTextBoxColourBlue = new ScrollableNumericTextBox(x1, this.y + (MwGuiMarkerDialogNew.elementVSpacing * 2), w, I18n.format(this.editBlue, new Object[0]), this.fontrendererObj);
		this.ScrollableNumericTextBoxColourBlue.setDrawArrows(true);
		this.ScrollableNumericTextBoxColourBlue.setMaxValue(255);
		this.ScrollableNumericTextBoxColourBlue.setMinValue(0);

		this.colourFieldX = x1 + w + 2;
		this.colourFieldY = this.y + 6;
		this.colourFieldW = this.width - w - (ScrollableField.arrowsWidth * 2) - this.fontrendererObj.getStringWidth(I18n.format(this.editGreen, new Object[0])) - 8;
		this.colourFieldH = MwGuiMarkerDialogNew.elementVSpacing * 2;
	}

	@Override
	public void nextElement()
	{
		this.setColor(Utils.getNextColour());
	}

	@Override
	public void previousElement()
	{
		this.setColor(Utils.getPrevColour());
	}

	@Override
	public void setFocused(Boolean focus)
	{
		this.ScrollableNumericTextBoxColourRed.setFocused(focus);
	}

	@Override
	public Boolean isFocused()
	{
		if (this.ScrollableNumericTextBoxColourRed.isFocused() || this.ScrollableNumericTextBoxColourGreen.isFocused() || this.ScrollableNumericTextBoxColourBlue.isFocused())
		{
			return true;
		}
		return false;
	}

	public boolean validateColorData()
	{
		return ((this.ScrollableNumericTextBoxColourRed.getText().length() > 0) && (this.ScrollableNumericTextBoxColourGreen.getText().length() > 0) && (this.ScrollableNumericTextBoxColourBlue.getText().length() > 0));
	}

	public int getColor()
	{
		return this.colour;
	}

	public void setColor(int colour)
	{
		this.colour = colour;

		int red = (colour >> 16) & 0xff;
		int green = (colour >> 8) & 0xff;
		int blue = (colour) & 0xff;
		this.ScrollableNumericTextBoxColourRed.setText(red);
		this.ScrollableNumericTextBoxColourGreen.setText(green);
		this.ScrollableNumericTextBoxColourBlue.setText(blue);
	}

	private void UpdateColour()
	{
		int colour = 0xff << 24;
		colour += this.ScrollableNumericTextBoxColourRed.getTextFieldIntValue() << 16;
		colour += this.ScrollableNumericTextBoxColourGreen.getTextFieldIntValue() << 8;
		colour += this.ScrollableNumericTextBoxColourBlue.getTextFieldIntValue();
		this.colour = colour;
	}

	@Override
	public void mouseClicked(int x, int y, int button)
	{
		super.mouseClicked(x, y, button);
		this.ScrollableNumericTextBoxColourRed.mouseClicked(x, y, button);
		this.ScrollableNumericTextBoxColourGreen.mouseClicked(x, y, button);
		this.ScrollableNumericTextBoxColourBlue.mouseClicked(x, y, button);
	}

	public void mouseDWheelScrolled(int x, int y, int direction)
	{
		this.ScrollableNumericTextBoxColourRed.mouseDWheelScrolled(x, y, direction);
		this.ScrollableNumericTextBoxColourGreen.mouseDWheelScrolled(x, y, direction);
		this.ScrollableNumericTextBoxColourBlue.mouseDWheelScrolled(x, y, direction);

		if (this.posWithinColourField(x, y))
		{
			this.colourFieldScroll(-direction);
		}
	}

	public boolean posWithinColourField(int x, int y)
	{
		return (x >= this.colourFieldX) && (y >= this.colourFieldY) && (x <= (this.colourFieldW + this.colourFieldX)) && (y <= (this.colourFieldH + this.colourFieldY));
	}

	public void colourFieldScroll(int direction)
	{
		if (direction > 0)
		{
			this.nextElement();
		}
		else if (direction < 0)
		{
			this.previousElement();
		}
	}

	public void KeyTyped(char c, int key)
	{
		this.ScrollableNumericTextBoxColourRed.KeyTyped(c, key);
		this.ScrollableNumericTextBoxColourGreen.KeyTyped(c, key);
		this.ScrollableNumericTextBoxColourBlue.KeyTyped(c, key);
	}

	@Override
	public void draw()
	{
		super.draw();
		this.ScrollableNumericTextBoxColourRed.draw();
		this.ScrollableNumericTextBoxColourGreen.draw();
		this.ScrollableNumericTextBoxColourBlue.draw();

		this.UpdateColour();

		Gui.drawRect(this.colourFieldX - 1, this.colourFieldY - 1, this.colourFieldX + this.colourFieldW + 1, this.colourFieldY + this.colourFieldH + 1, 0xff000000);
		Gui.drawRect(this.colourFieldX, this.colourFieldY, this.colourFieldX + this.colourFieldW, this.colourFieldY + this.colourFieldH, this.colour);
	}

	public ScrollableField thisField()
	{
		if (this.ScrollableNumericTextBoxColourRed.isFocused())
		{
			return this.ScrollableNumericTextBoxColourRed;
		}
		if (this.ScrollableNumericTextBoxColourGreen.isFocused())
		{
			return this.ScrollableNumericTextBoxColourGreen;
		}
		if (this.ScrollableNumericTextBoxColourBlue.isFocused())
		{
			return this.ScrollableNumericTextBoxColourBlue;
		}
		return this.ScrollableNumericTextBoxColourRed;
	}

	public ScrollableField nextField(ScrollableField field)
	{
		if (this.ScrollableNumericTextBoxColourRed.isFocused())
		{
			return this.ScrollableNumericTextBoxColourGreen;
		}
		if (this.ScrollableNumericTextBoxColourGreen.isFocused())
		{
			return this.ScrollableNumericTextBoxColourBlue;
		}
		return field;
	}

	public ScrollableField prevField(ScrollableField field)
	{
		if (this.ScrollableNumericTextBoxColourGreen.isFocused())
		{
			return this.ScrollableNumericTextBoxColourRed;
		}
		if (this.ScrollableNumericTextBoxColourBlue.isFocused())
		{
			return this.ScrollableNumericTextBoxColourGreen;
		}
		return field;
	}
}
