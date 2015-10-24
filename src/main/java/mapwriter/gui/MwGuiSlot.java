package mapwriter.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Mouse;

@SideOnly(Side.CLIENT)
public abstract class MwGuiSlot
{
	protected final Minecraft mc;
	public int width;
	public int height;
	/** The top of the slot container. Affects the overlays and scrolling. */
	public int top;
	/** The bottom of the slot container. Affects the overlays and scrolling. */
	public int bottom;
	public int right;
	public int left;
	public final int scrollBarWidth = 6;
	/** The buttonID of the button used to scroll up */
	private int scrollUpButtonID;
	/** The buttonID of the button used to scroll down */
	private int scrollDownButtonID;
	protected int mouseX;
	protected int mouseY;
	protected boolean field_148163_i = true;
	/** Where the mouse was in the window when you first clicked to scroll */
	protected float initialClickY = -2.0F;
	/**
	 * What to multiply the amount you moved your mouse by (used for slowing
	 * down scrolling when over the items and not on the scroll bar)
	 */
	protected float scrollMultiplier;
	/** How far down this slot has been scrolled */
	protected float amountScrolled;
	/** The element in the list that was selected */
	protected int selectedElement = -1;
	/** The time when this button was last clicked. */
	protected long lastClicked;
	/** Set to true if a selected element in this gui will show an outline box */
	protected boolean showSelectionBox = true;
	protected boolean hasListHeader;
	public int headerPadding;
	private boolean enabled = false;
	int spacingY = 4;

	public MwGuiSlot(Minecraft mcIn, int width, int height, int topIn, int bottomIn, int left)
	{
		this.mc = mcIn;
		this.width = width;
		this.height = height;
		this.top = topIn;
		this.bottom = bottomIn;
		this.left = left;
		this.right = left + width;
	}

	public void setDimensions(int widthIn, int heightIn, int topIn, int bottomIn, int left)
	{
		this.width = widthIn;
		this.height = heightIn;
		this.top = topIn;
		this.bottom = bottomIn;
		this.left = left;
		this.right = left + widthIn;
	}

	public void setShowSelectionBox(boolean showSelectionBoxIn)
	{
		this.showSelectionBox = showSelectionBoxIn;
	}

	/**
	 * Sets hasListHeader and headerHeight. Params: hasListHeader, headerHeight.
	 * If hasListHeader is false headerHeight is set to 0.
	 */
	protected void setHasListHeader(boolean hasListHeaderIn, int headerPaddingIn)
	{
		this.hasListHeader = hasListHeaderIn;
		this.headerPadding = headerPaddingIn;

		if (!hasListHeaderIn)
		{
			this.headerPadding = 0;
		}
	}

	protected abstract int getSize();

	/**
	 * The element in the slot that was clicked, boolean for whether it was
	 * double clicked or not
	 */
	protected abstract void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY, int mouseButton);

	/**
	 * Returns true if the element passed in is currently selected
	 */
	protected abstract boolean isSelected(int slotIndex);

	/**
	 *
	 * @param index
	 *            of the item
	 * @return the slotheigt of the specific item
	 */
	protected abstract int getSlotHeight(int index);

	/**
	 * Return the height of the content being scrolled
	 */
	protected int getContentHeight()
	{
		int slotHeight = this.headerPadding;
		for (int index = 0; index < this.getSize(); ++index)
		{
			slotHeight += this.getSlotHeight(index);
		}

		return slotHeight;
	}

	protected abstract void drawBackground();

	protected void slotOutOfBounds(int index, int p_178040_2_, int p_178040_3_)
	{
	}

	protected abstract void drawSlot(int entryID, int x, int y, int h, int mouseXIn, int mouseYIn);

	/**
	 * Handles drawing a list's header row.
	 */
	protected void drawListHeader(int x, int y, Tessellator tess)
	{
	}

	protected void func_148132_a(int p_148132_1_, int p_148132_2_)
	{
	}

	protected void func_148142_b(int p_148142_1_, int p_148142_2_)
	{
	}

	public int getSlotIndexFromScreenCoords(int x, int y)
	{
		int k = this.left + ((this.width / 2) - (this.getListWidth() / 2));
		int l = (this.left + ((this.width / 2) + (this.getListWidth() / 2))) - this.scrollBarWidth;
		int i1 = (y - this.top - this.headerPadding) + (int) this.amountScrolled;

		int yStart = 0;
		int yStop = yStart + this.getSlotHeight(0);

		for (int index = 0; index < this.getSize(); ++index)
		{
			if ((i1 >= yStart) && (i1 <= yStop))
			{
				return (x < this.getScrollBarX()) && (x >= k) && (x <= l) && (i1 >= 0) ? index : -1;
			}
			yStart = yStop;
			yStop += this.getSlotHeight(index + 1);
		}
		return -1;
	}

	/**
	 * Registers the IDs that can be used for the scrollbar's up/down buttons.
	 */
	public void registerScrollButtons(int scrollUpButtonIDIn, int scrollDownButtonIDIn)
	{
		this.scrollUpButtonID = scrollUpButtonIDIn;
		this.scrollDownButtonID = scrollDownButtonIDIn;
	}

	/**
	 * Stop the thing from scrolling out of bounds
	 */
	protected void bindAmountScrolled()
	{
		int i = this.func_148135_f();

		if (i < 0)
		{
			i /= 2;
		}

		if (!this.field_148163_i && (i < 0))
		{
			i = 0;
		}

		this.amountScrolled = MathHelper.clamp_float(this.amountScrolled, 0.0F, i);
	}

	public int func_148135_f()
	{
		return Math.max(0, this.getContentHeight() - (this.bottom - this.top));
	}

	/**
	 * Returns the amountScrolled field as an integer.
	 */
	public int getAmountScrolled()
	{
		return (int) this.amountScrolled;
	}

	public boolean isMouseYWithinSlotBounds()
	{
		return (this.mouseY >= this.top) && (this.mouseY <= this.bottom) && (this.mouseX >= this.left) && (this.mouseX <= this.right);
	}

	public boolean isMouseXWithinSlotBounds()
	{
		return (this.mouseX >= this.left) && (this.mouseX <= this.right);
	}

	public boolean isMouseInField()
	{
		return this.isMouseYWithinSlotBounds() && this.isMouseXWithinSlotBounds();
	}

	/**
	 * Scrolls the slot by the given amount. A positive value scrolls down, and
	 * a negative value scrolls up.
	 */
	public void scrollBy(int amount)
	{
		this.amountScrolled += amount;
		this.bindAmountScrolled();
		this.initialClickY = -2.0F;
	}

	public void actionPerformed(GuiButton button)
	{
		if (button.enabled)
		{
			int slotHeight = this.getAverageSlotHeight();
			if (button.id == this.scrollUpButtonID)
			{
				this.amountScrolled -= (slotHeight * 2) / 3;
				this.initialClickY = -2.0F;
				this.bindAmountScrolled();
			}
			else if (button.id == this.scrollDownButtonID)
			{
				this.amountScrolled += (slotHeight * 2) / 3;
				this.initialClickY = -2.0F;
				this.bindAmountScrolled();
			}
		}
	}

	public void drawScreen(int mouseXIn, int mouseYIn, float f)
	{
		if (this.getEnabled())
		{
			this.mouseX = mouseXIn;
			this.mouseY = mouseYIn;
			this.drawBackground();
			int k = this.getScrollBarX();
			int l = k + this.scrollBarWidth;
			this.bindAmountScrolled();
			GlStateManager.disableLighting();
			GlStateManager.disableFog();
			Tessellator tessellator = Tessellator.getInstance();
			WorldRenderer worldrenderer = tessellator.getWorldRenderer();
			this.drawContainerBackground(tessellator);
			int i1 = this.left + ((this.width / 2) - (this.getListWidth() / 2));
			int j1 = this.top - (int) this.amountScrolled;

			if (this.hasListHeader)
			{
				this.drawListHeader(i1, j1, tessellator);
			}

			this.drawSelectionBox(i1, j1, mouseXIn, mouseYIn);
			GlStateManager.disableDepth();

			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(770, 771, 0, 1);
			GlStateManager.disableAlpha();
			GlStateManager.shadeModel(7425);
			GlStateManager.disableTexture2D();

			int k1 = this.func_148135_f();

			if (k1 > 0)
			{
				int l1 = ((this.bottom - this.top) * (this.bottom - this.top)) / this.getContentHeight();
				l1 = MathHelper.clamp_int(l1, 32, this.bottom - this.top - 8);
				int i2 = (((int) this.amountScrolled * (this.bottom - this.top - l1)) / k1) + this.top;

				if (i2 < this.top)
				{
					i2 = this.top;
				}
				// draw the scrollbar
				worldrenderer.startDrawingQuads();
				worldrenderer.setColorRGBA_I(0, 255);
				worldrenderer.addVertexWithUV(k, this.bottom, 0.0D, 0.0D, 1.0D);
				worldrenderer.addVertexWithUV(l, this.bottom, 0.0D, 1.0D, 1.0D);
				worldrenderer.addVertexWithUV(l, this.top, 0.0D, 1.0D, 0.0D);
				worldrenderer.addVertexWithUV(k, this.top, 0.0D, 0.0D, 0.0D);
				tessellator.draw();
				worldrenderer.startDrawingQuads();
				worldrenderer.setColorRGBA_I(8421504, 255);
				worldrenderer.addVertexWithUV(k, i2 + l1, 0.0D, 0.0D, 1.0D);
				worldrenderer.addVertexWithUV(l, i2 + l1, 0.0D, 1.0D, 1.0D);
				worldrenderer.addVertexWithUV(l, i2, 0.0D, 1.0D, 0.0D);
				worldrenderer.addVertexWithUV(k, i2, 0.0D, 0.0D, 0.0D);
				tessellator.draw();
				worldrenderer.startDrawingQuads();
				worldrenderer.setColorRGBA_I(12632256, 255);
				worldrenderer.addVertexWithUV(k, (i2 + l1) - 1, 0.0D, 0.0D, 1.0D);
				worldrenderer.addVertexWithUV(l - 1, (i2 + l1) - 1, 0.0D, 1.0D, 1.0D);
				worldrenderer.addVertexWithUV(l - 1, i2, 0.0D, 1.0D, 0.0D);
				worldrenderer.addVertexWithUV(k, i2, 0.0D, 0.0D, 0.0D);
				tessellator.draw();
			}

			this.func_148142_b(mouseXIn, mouseYIn);
			GlStateManager.enableTexture2D();
			GlStateManager.shadeModel(7424);
			GlStateManager.enableAlpha();
			GlStateManager.disableBlend();
		}
	}

	public void handleMouseInput()
	{
		if (this.isMouseInField())
		{
			if (Mouse.isButtonDown(0) && this.getEnabled())
			{
				if (this.initialClickY == -1.0F)
				{
					boolean flag = true;

					if ((this.mouseY >= this.top) && (this.mouseY <= this.bottom))
					{
						int i = ((this.width / 2) - (this.getListWidth() / 2)) + this.left;
						int j = (((this.width / 2) + (this.getListWidth() / 2)) + this.left) - this.scrollBarWidth;
						int k = (this.mouseY - this.top - this.headerPadding) + (int) this.amountScrolled;
						int l = this.getSlotIndexFromScreenCoords(this.mouseX, this.mouseY);

						if ((this.mouseX >= i) && (this.mouseX <= j) && (l >= 0) && (k >= 0) && (l < this.getSize()))
						{
							boolean flag1 = (l == this.selectedElement) && ((Minecraft.getSystemTime() - this.lastClicked) < 250L);
							this.elementClicked(l, flag1, this.mouseX, this.mouseY, 0);
							this.selectedElement = l;
							this.lastClicked = Minecraft.getSystemTime();
						}
						else if ((this.mouseX >= i) && (this.mouseX <= j) && (k < 0))
						{
							this.func_148132_a(this.mouseX - i, (this.mouseY - this.top) + (int) this.amountScrolled);
							flag = false;
						}

						int i2 = this.getScrollBarX();
						int i1 = i2 + this.scrollBarWidth;

						if ((this.mouseX >= i2) && (this.mouseX <= i1))
						{
							this.scrollMultiplier = -1.0F;
							int j1 = this.func_148135_f();

							if (j1 < 1)
							{
								j1 = 1;
							}

							int k1 = (int) ((float) ((this.bottom - this.top) * (this.bottom - this.top)) / (float) this.getContentHeight());
							k1 = MathHelper.clamp_int(k1, 32, this.bottom - this.top - 8);
							this.scrollMultiplier /= (float) (this.bottom - this.top - k1) / (float) j1;
						}
						else
						{
							this.scrollMultiplier = 1.0F;
						}

						if (flag)
						{
							this.initialClickY = this.mouseY;
						}
						else
						{
							this.initialClickY = -2.0F;
						}
					}
					else
					{
						this.initialClickY = -2.0F;
					}
				}
				else if (this.initialClickY >= 0.0F)
				{
					this.amountScrolled -= (this.mouseY - this.initialClickY) * this.scrollMultiplier;
					this.initialClickY = this.mouseY;
				}
			}
			else
			{
				this.initialClickY = -1.0F;
			}

			if (Mouse.isButtonDown(1) && this.getEnabled())
			{
				if (this.initialClickY == -1.0F)
				{
					if ((this.mouseY >= this.top) && (this.mouseY <= this.bottom))
					{
						int i = ((this.width / 2) - (this.getListWidth() / 2)) + this.left;
						int j = (((this.width / 2) + (this.getListWidth() / 2)) + this.left) - this.scrollBarWidth;
						int k = (this.mouseY - this.top - this.headerPadding) + (int) this.amountScrolled;
						int l = this.getSlotIndexFromScreenCoords(this.mouseX, this.mouseY);

						if ((this.mouseX >= i) && (this.mouseX <= j) && (l >= 0) && (k >= 0) && (l < this.getSize()))
						{
							boolean flag1 = (l == this.selectedElement) && ((Minecraft.getSystemTime() - this.lastClicked) < 250L);
							this.elementClicked(l, flag1, this.mouseX, this.mouseY, 1);
							this.selectedElement = l;
							this.lastClicked = Minecraft.getSystemTime();
						}
						else if ((this.mouseX >= i) && (this.mouseX <= j) && (k < 0))
						{
							this.func_148132_a(this.mouseX - i, (this.mouseY - this.top) + (int) this.amountScrolled);
						}
					}
				}
			}

			int l1 = Mouse.getEventDWheel();

			if (l1 != 0)
			{
				if (l1 > 0)
				{
					l1 = -1;
				}
				else if (l1 < 0)
				{
					l1 = 1;
				}

				this.amountScrolled += (l1 * this.getAverageSlotHeight()) / 2;
			}
		}
	}

	public void setEnabled(boolean enabledIn)
	{
		this.enabled = enabledIn;
	}

	public boolean getEnabled()
	{
		return this.enabled;
	}

	/**
	 * Gets the width of the list
	 */
	public int getListWidth()
	{
		return this.width;
	}

	/**
	 * Draws the selection box around the selected slot element.
	 */
	protected void drawSelectionBox(int x, int y, int mouseXIn, int mouseYIn)
	{
		int i1 = this.getSize();
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer worldrenderer = tessellator.getWorldRenderer();

		int yTotal = y + this.headerPadding;

		for (int index = 0; index < i1; ++index)
		{
			int slotHeight = this.getSlotHeight(index);

			if (((yTotal + slotHeight) > this.bottom) || (yTotal < this.top))
			{
				this.slotOutOfBounds(index, x, yTotal);
				yTotal += slotHeight;
				continue;
			}

			if (this.showSelectionBox && this.isSelected(index))
			{
				int xLeft = this.left + ((this.width / 2) - (this.getListWidth() / 2));
				int xRight = (this.left + ((this.width / 2) + (this.getListWidth() / 2))) - this.scrollBarWidth;

				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
				GlStateManager.disableTexture2D();
				worldrenderer.startDrawingQuads();
				worldrenderer.setColorOpaque_I(8421504);
				worldrenderer.addVertexWithUV(xLeft, yTotal + slotHeight, 0.0D, 0.0D, 1.0D);
				worldrenderer.addVertexWithUV(xRight, yTotal + slotHeight, 0.0D, 1.0D, 1.0D);
				worldrenderer.addVertexWithUV(xRight, (yTotal), 0.0D, 1.0D, 0.0D);
				worldrenderer.addVertexWithUV(xLeft, (yTotal), 0.0D, 0.0D, 0.0D);
				worldrenderer.setColorOpaque_I(0);
				worldrenderer.addVertexWithUV(xLeft + 1, (yTotal + slotHeight) - 1, 0.0D, 0.0D, 1.0D);
				worldrenderer.addVertexWithUV(xRight - 1, (yTotal + slotHeight) - 1, 0.0D, 1.0D, 1.0D);
				worldrenderer.addVertexWithUV(xRight - 1, yTotal + 1, 0.0D, 1.0D, 0.0D);
				worldrenderer.addVertexWithUV(xLeft + 1, yTotal + 1, 0.0D, 0.0D, 0.0D);
				tessellator.draw();
				GlStateManager.enableTexture2D();
			}

			this.drawSlot(index, x + 4, yTotal + this.spacingY, slotHeight, mouseXIn, mouseYIn);

			yTotal += slotHeight;
		}
	}

	protected int getScrollBarX()
	{
		return (this.left + this.width) - this.scrollBarWidth;
	}

	/**
	 * Sets the left and right bounds of the slot. Param is the left bound,
	 * right is calculated as left + width.
	 */
	public void setSlotXBoundsFromLeft(int leftIn)
	{
		this.left = leftIn;
		this.right = leftIn + this.width;
	}

	public int getAverageSlotHeight()
	{
		int height = 0;
		int biggest = 0;
		for (int index = 0; index < this.getSize(); ++index)
		{
			int slotheight = this.getSlotHeight(index);
			height += slotheight;

			biggest = biggest < slotheight ? slotheight : biggest;
		}

		return height > 0 ? height / this.getSize() : 9;
	}

	protected void drawContainerBackground(Tessellator tessellator)
	{
		Gui.drawRect(this.left, this.top, this.right, this.bottom, 0x80000000);
	}
}