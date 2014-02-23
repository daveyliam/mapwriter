package mapwriter.gui;

import mapwriter.Mw;
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
	private static final String[] miniMapPositionStringArray = {
		"unchanged",
		"top right",
		"top left",
		"bottom right",
		"bottom left"
	};
	private static final String[] coordsModeStringArray = {
		"disabled",
		"small",
		"large"
	};
	private static final String[] backgroundModeStringArray = {
		"none",
		"static",
		"panning"
	};
	
	private GuiButton[] buttons = new GuiButton[12];
	
    static final ResourceLocation WIDGET_TEXTURE_LOC = new ResourceLocation("textures/gui/widgets.png");
	
	public void updateButtonLabel(int i) {
		switch(i) {
		case 0:
			this.buttons[i].displayString = "Draw coords: " + coordsModeStringArray[this.mw.coordsMode];
			break;
		case 1:
			this.buttons[i].displayString = "Circular mode: " + this.mw.miniMap.smallMapMode.circular;
			break;
		case 2:
			this.buttons[i].displayString = "Texture size: " + this.mw.configTextureSize;
			break;
		case 3:	
			this.buttons[i].displayString = "Texture scaling: " + (this.mw.linearTextureScalingEnabled ? "linear" : "nearest");
			break;
		case 4:
			this.buttons[i].displayString = "Trail markers: " + (this.mw.playerTrail.enabled);
			break;
		case 5:
			this.buttons[i].displayString = "Map colours: " + (this.mw.useSavedBlockColours ? "frozen" : "auto");
			break;
		case 6:
			this.buttons[i].displayString = "Max draw distance: " + Math.round(Math.sqrt(this.mw.maxChunkSaveDistSq));
			break;
		case 7:
			this.buttons[i].displayString = "Mini map size: " + this.mw.miniMap.smallMapMode.heightPercent;
			break;
		case 8:
			this.buttons[i].displayString = "Mini map position: " + miniMapPositionStringArray[this.miniMapPositionIndex];
			break;
		case 9:
			this.buttons[i].displayString = "Map pixel snapping: " + (this.mw.mapPixelSnapEnabled ? "enabled" : "disabled");
			break;
		case 10:
			this.buttons[i].displayString = "Max death markers: " + this.mw.maxDeathMarkers;
			break;
		case 11:
			this.buttons[i].displayString = "Background mode: " + backgroundModeStringArray[this.mw.backgroundTextureMode];
			break;
		//case 11:
		//	this.buttons[i].displayString = "Map Lighting: " + (this.mw.lightingEnabled ? "enabled" : "disabled");
		//	break;	
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
	protected void elementClicked(int i, boolean doubleClicked, int x, int y) {
		switch(i) {
		case 0:
	        // toggle coords
			this.mw.toggleCoords();
			break;
		case 1:
			// toggle circular
			this.mw.miniMap.toggleRotating();
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
			//this.mw.undergroundMapTexture.setLinearScaling(this.mw.linearTextureScalingEnabled);
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
			this.mw.miniMap.smallMapMode.toggleHeightPercent();
			break;
		case 8:
			this.miniMapPositionIndex++;
			if (this.miniMapPositionIndex >= miniMapPositionStringArray.length) {
				// don't go back to the "unchanged" setting
				this.miniMapPositionIndex = 1;
			}
			switch (this.miniMapPositionIndex) {
			case 1:
				// top right position
				this.mw.miniMap.smallMapMode.setMargins(10, -1, -1, 10);
				break;
			case 2:
				// top left position
				this.mw.miniMap.smallMapMode.setMargins(10, -1, 10, -1);
				break;
			case 3:
				// bottom right position
				this.mw.miniMap.smallMapMode.setMargins(-1, 40, -1, 10);
				break;
			case 4:
				// bottom left position
				this.mw.miniMap.smallMapMode.setMargins(-1, 40, 10, -1);
				break;
			default:
				break;
			}
		case 9:
			// map scroll pixel snapping
			this.mw.mapPixelSnapEnabled = !this.mw.mapPixelSnapEnabled;
			break;
		case 10:
			// max death markers
			this.mw.maxDeathMarkers++;
			if (this.mw.maxDeathMarkers > 10) {
				this.mw.maxDeathMarkers = 0;
			}
			break;
		case 11:
			// background texture mode
			this.mw.backgroundTextureMode = (this.mw.backgroundTextureMode + 1) % 3;
			break;
		//case 11:
		//	// lighting
		//	this.mw.lightingEnabled = !this.mw.lightingEnabled;
		//	break;
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
    protected void drawSlot(int i, int x, int y, int i4, Tessellator tessellator, int i5, int i6){
        GuiButton button = buttons[i];
        button.xPosition = x;
        button.yPosition = y;
        button.drawButton(this.mc, this.mouseX, this.mouseY);
    }
}
