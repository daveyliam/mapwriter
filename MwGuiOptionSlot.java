package mapwriter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

public class MwGuiOptionSlot extends GuiSlot {
	
	//private GuiScreen parentScreen;
	private Minecraft mc;
	private Mw mw;
	
	private int mouseX = 0;
	private int mouseY = 0;
	
	private GuiButton[] buttons = new GuiButton[5];
	
    static final ResourceLocation WIDGET_TEXTURE_LOC = new ResourceLocation("textures/gui/widgets.png");
	
	public void updateButtonLabel(int i) {
		switch(i) {
		case 0:
			this.buttons[i].displayString = "Draw coords: " + this.mw.coordsEnabled;
			break;
		case 1:
			this.buttons[i].displayString = "Circular mode: " + this.mw.overlayManager.smallMapMode.circular;
			break;
		case 2:
			this.buttons[i].displayString = "Texture size: " + this.mw.configTextureSize;
			break;
		case 3:	
			this.buttons[i].displayString = "Texture scaling: " + (this.mw.linearTextureScalingEnabled ? "linear" : "nearest");
			break;
		case 4:
			this.buttons[i].displayString = "Trail Markers: " + (this.mw.playerTrail.enabled);
			break;
		default:
			break;
		}
	}
    
	public MwGuiOptionSlot(GuiScreen parentScreen, Minecraft mc, Mw mw) {
		// GuiSlot(minecraft, width, height, top, bottom, slotHeight)
		super(mc, parentScreen.width, parentScreen.height, 16, parentScreen.height - 32, 25);
		//this.parentScreen = parentScreen;
		this.mw = mw;
		this.mc = mc;
		for (int i = 0; i < this.buttons.length; i++) {
			this.buttons[i] = new GuiButton(300 + i, 0, 0, "");
			this.updateButtonLabel(i);
		}
	}
	
	protected boolean keyTyped(char c, int k) {
		return false;
	}
	
	@Override
	protected int getSize() {
		// number of slots
		return this.buttons.length;
	}

	@Override
	protected void elementClicked(int i, boolean doubleClicked) {
		switch(i) {
		case 0:
	        // toggle coords
			this.mw.toggleCoords();
			break;
		case 1:
			// toggle circular
			this.mw.overlayManager.toggleRotating();
			break;
		case 2:
			// toggle texture size
			this.mw.configTextureSize *= 2;
			if (this.mw.configTextureSize > 4096) {
				this.mw.configTextureSize = 1024;
			}
			break;
		case 3:
			// linear scaling
			this.mw.linearTextureScalingEnabled = !this.mw.linearTextureScalingEnabled;
			this.mw.mapTexture.setLinearScaling(this.mw.linearTextureScalingEnabled);
			break;
		case 4:
			// player trail
			this.mw.playerTrail.enabled = !this.mw.playerTrail.enabled;
			break;
		default:
			break;
		}
		this.updateButtonLabel(i);
	}

	@Override
    public void drawScreen(int mouseX, int mouseY, float f) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        super.drawScreen(mouseX, mouseY, f);
    }
	
	@Override
	protected boolean isSelected(int i) {
		return false;
	}

	@Override
	protected void drawBackground() {
	}

	@Override
	protected void drawSlot(int i, int x, int y, int height, Tessellator tessellator) {
		GuiButton button = buttons[i];
		button.xPosition = x;
		button.yPosition = y;
		button.drawButton(this.mc, this.mouseX, this.mouseY);
	}
}
