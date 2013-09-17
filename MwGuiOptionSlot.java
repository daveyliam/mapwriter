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
	
	private int miniMapPositionIndex = 0;
	private String[] miniMapPositionStringArray = {
		"unchanged",
		"top right",
		"top left",
		"bottom right",
		"bottom left"
	};
	
	private GuiButton[] buttons = new GuiButton[9];
	
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
		case 5:
			this.buttons[i].displayString = "Map Colours: " + (this.mw.useSavedBlockColours ? "Frozen" : "Auto");
			break;
		case 6:
			this.buttons[i].displayString = "Max Draw Distance: " + Math.round(Math.sqrt(this.mw.maxChunkSaveDistSq));
			break;
		case 7:
			this.buttons[i].displayString = "Mini map size: " + this.mw.overlayManager.smallMapMode.heightPercent;
			break;
		case 8:
			this.buttons[i].displayString = "Mini map position: " + this.miniMapPositionStringArray[this.miniMapPositionIndex];
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
		case 5:
			// map colours
			this.mw.useSavedBlockColours = !this.mw.useSavedBlockColours;
			// reload block colours before saving in case player changed
			// texture packs before pressing button.
			this.mw.reloadBlockColours();
			
			if (this.mw.useSavedBlockColours) {
				// save current map colours
				this.mw.saveCurrentBlockColours();
			}
			break;
		case 6:
			// toggle max chunk save dist
			int d = Math.round((float) Math.sqrt(this.mw.maxChunkSaveDistSq));
			d += 32;
			if (d > 256) {
				d = 64;
			}
			this.mw.maxChunkSaveDistSq = d * d;
			break;
		case 7:
			this.mw.overlayManager.smallMapMode.toggleHeightPercent();
			break;
		case 8:
			this.miniMapPositionIndex++;
			if (this.miniMapPositionIndex >= this.miniMapPositionStringArray.length) {
				// don't go back to the "unchanged" setting
				this.miniMapPositionIndex = 1;
			}
			switch (this.miniMapPositionIndex) {
			case 1:
				// top right position
				this.mw.overlayManager.smallMapMode.setMargins(10, -1, -1, 10);
				this.mw.overlayManager.undergroundMapMode.setMargins(10, -1, -1, 10);
				break;
			case 2:
				// top left position
				this.mw.overlayManager.smallMapMode.setMargins(10, -1, 10, -1);
				this.mw.overlayManager.undergroundMapMode.setMargins(10, -1, 10, -1);
				break;
			case 3:
				// bottom right position
				this.mw.overlayManager.smallMapMode.setMargins(-1, 40, -1, 10);
				this.mw.overlayManager.undergroundMapMode.setMargins(-1, 40, -1, 10);
				break;
			case 4:
				// bottom left position
				this.mw.overlayManager.smallMapMode.setMargins(-1, 40, 10, -1);
				this.mw.overlayManager.undergroundMapMode.setMargins(-1, 40, 10, -1);
				break;
			default:
				break;
			}
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
