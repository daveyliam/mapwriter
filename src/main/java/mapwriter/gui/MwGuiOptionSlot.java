package mapwriter.gui;

import mapwriter.Mw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

public class MwGuiOptionSlot extends GuiSlot {
	
	//private GuiScreen parentScreen;
	private Minecraft mc;
	private Mw mw;
	
	private int mouseX = 0;
	private int mouseY = 0;
	
	private int miniMapPositionIndex = 0;
	private static final String[] miniMapPositionStringArray = {
		I18n.format("mw.gui.mwguioptionslot.miniMapPosition.unchanged"),
		I18n.format("mw.gui.mwguioptionslot.miniMapPosition.topRight"),
		I18n.format("mw.gui.mwguioptionslot.miniMapPosition.topLeft"),
		I18n.format("mw.gui.mwguioptionslot.miniMapPosition.bottomRight"),
		I18n.format("mw.gui.mwguioptionslot.miniMapPosition.bottomLeft")
	};
	private static final String[] coordsModeStringArray = {
		I18n.format("mw.gui.mwguioptionslot.drawCoords.disabled"),
		I18n.format("mw.gui.mwguioptionslot.drawCoords.small"),
		I18n.format("mw.gui.mwguioptionslot.drawCoords.large")
	};
	private static final String[] backgroundModeStringArray = {
		I18n.format("mw.gui.mwguioptionslot.backgroundMode.none"),
		I18n.format("mw.gui.mwguioptionslot.backgroundMode.static"),
		I18n.format("mw.gui.mwguioptionslot.backgroundMode.panning")
	};
	
	private GuiButton[] buttons = new GuiButton[13];
	
    static final ResourceLocation WIDGET_TEXTURE_LOC = new ResourceLocation("textures/gui/widgets.png");
	
	public void updateButtonLabel(int i) {
		switch(i) {
		case 0:
			this.buttons[i].displayString = I18n.format("mw.gui.mwguioptionslot.drawCoords", coordsModeStringArray[this.mw.coordsMode]);
			break;
		case 1:
			this.buttons[i].displayString = I18n.format("mw.gui.mwguioptionslot.circularMode", this.mw.miniMap.smallMapMode.circular);
			break;
		case 2:
			this.buttons[i].displayString = I18n.format("mw.gui.mwguioptionslot.textureSize", this.mw.configTextureSize);
			break;
		case 3:	
			this.buttons[i].displayString = I18n.format("mw.gui.mwguioptionslot.textureScaling", (this.mw.linearTextureScalingEnabled ? I18n.format("mw.gui.mwguioptionslot.textureScaling.linear") : I18n.format("mw.gui.mwguioptionslot.textureScaling.nearest")));
			break;
		case 4:
			this.buttons[i].displayString = I18n.format("mw.gui.mwguioptionslot.trailMarkers", (this.mw.playerTrail.enabled));
			break;
		case 5:
			this.buttons[i].displayString = I18n.format("mw.gui.mwguioptionslot.mapColours", (this.mw.useSavedBlockColours ? I18n.format("mw.gui.mwguioptionslot.mapColours.frozen") : I18n.format("mw.gui.mwguioptionslot.mapColours.auto")));
			break;
		case 6:
			this.buttons[i].displayString = I18n.format("mw.gui.mwguioptionslot.maxDrawDistance", Math.round(Math.sqrt(this.mw.maxChunkSaveDistSq)));
			break;
		case 7:
			this.buttons[i].displayString = I18n.format("mw.gui.mwguioptionslot.miniMapSize", this.mw.miniMap.smallMapMode.heightPercent);
			break;
		case 8:
			this.buttons[i].displayString = I18n.format("mw.gui.mwguioptionslot.miniMapPosition", miniMapPositionStringArray[this.miniMapPositionIndex]);
			break;
		case 9:
			this.buttons[i].displayString = I18n.format("mw.gui.mwguioptionslot.mapPixelSnapping", (this.mw.mapPixelSnapEnabled ? I18n.format("mw.gui.mwguioptionslot.mapPixelSnapping.enabled") : I18n.format("mw.gui.mwguioptionslot.mapPixelSnapping.disabled")));
			break;
		case 10:
			this.buttons[i].displayString = I18n.format("mw.gui.mwguioptionslot.maxDeathMarkers", this.mw.maxDeathMarkers);
			break;
		case 11:
			this.buttons[i].displayString = I18n.format("mw.gui.mwguioptionslot.backgroundMode", backgroundModeStringArray[this.mw.backgroundTextureMode]);
			break;
		//case 11:
		//	this.buttons[i].displayString = "Map Lighting: " + (this.mw.lightingEnabled ? "enabled" : "disabled");
		//	break;	
		case 12:
			this.buttons[i].displayString = I18n.format("mw.gui.mwguioptionslot.oldNewMarkerDialog", (this.mw.newMarkerDialog ? I18n.format("mw.gui.mwguioptionslot.oldNewMarkerDialog.new") : I18n.format("mw.gui.mwguioptionslot.oldNewMarkerDialog.old")));
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
		case 12:
			this.mw.newMarkerDialog = !this.mw.newMarkerDialog;
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
    protected void drawSlot(int i, int x, int y, int i4, Tessellator tessellator, int i5, int i6){
        GuiButton button = buttons[i];
        button.xPosition = x;
        button.yPosition = y;
        button.drawButton(this.mc, this.mouseX, this.mouseY);
    }
}
