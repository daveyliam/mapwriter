package mapwriter;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class MwGuiOptions extends GuiScreen {
	
	private final Mw mw;
	private final GuiScreen parentScreen;
	
	public MwGuiOptions(Mw mw, GuiScreen parentScreen) {
		this.mw = mw;
		this.parentScreen = parentScreen;
	}
	
    @SuppressWarnings("unchecked")
	public void initGui() {
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(1, 0, 0, this.getDrawCoordsString()));
        this.buttonList.add(new GuiButton(2, 0, 0, this.getCircularModeString()));
        this.buttonList.add(new GuiButton(5, 0, 0, this.getTrailString()));
        this.buttonList.add(new GuiButton(3, 0, 0, this.getTextureSizeString()));
        this.buttonList.add(new GuiButton(4, 0, 0, this.getTextureScalingString()));
        this.buttonList.add(new GuiButton(0, 0, 0, "Done"));
        
        int y = Math.max(60, this.height / 4);
    	int x = this.width / 2 - 100;
        for (Object o : this.buttonList) {
        	GuiButton button = (GuiButton) o;
        	button.xPosition = x;
        	button.yPosition = y;
        	y += 25;
        }
    }
    
    String getDrawCoordsString() {
    	return "Draw coords: " + this.mw.coordsEnabled;
    }
    
    String getCircularModeString() {
    	return "Circular mode: " + this.mw.overlayManager.smallMapMode.circular;
    }
    
    String getTextureSizeString() {
    	return "Texture size: " + this.mw.configTextureSize;
    }
    
    String getTextureScalingString() {
    	return "Texture scaling: " + (this.mw.linearTextureScalingEnabled ? "linear" : "nearest");
    }
    
    String getTrailString() {
    	return "Trail Markers: " + (this.mw.playerTrail.enabled);
    }
    
    protected void actionPerformed(GuiButton button) {
		switch (button.id) {
		case 0:
			// done
			// reconfigure texture size
			this.mw.setTextureSize();
			this.mc.displayGuiScreen(this.parentScreen);
			break;
		case 1:
	        // toggle coords
			this.mw.toggleCoords();
			button.displayString = this.getDrawCoordsString();
			break;
		case 2:
			// toggle circular
			this.mw.overlayManager.toggleRotating();
			button.displayString = this.getCircularModeString();
			break;
		case 3:
			// toggle texture size
			this.mw.configTextureSize *= 2;
			if (this.mw.configTextureSize > 4096) {
				this.mw.configTextureSize = 1024;
			}
			button.displayString = this.getTextureSizeString();
			break;
		case 4:
			// linear scaling
			this.mw.linearTextureScalingEnabled = !this.mw.linearTextureScalingEnabled;
			this.mw.mapTexture.setLinearScaling(this.mw.linearTextureScalingEnabled);
			button.displayString = this.getTextureScalingString();
			break;
		case 5:
			// player trail
			this.mw.playerTrail.enabled = !this.mw.playerTrail.enabled;
			button.displayString = this.getTrailString();
			break;
		default:
			break;
		}
	}

    public void updateScreen() {
        super.updateScreen();
    }

    public void drawScreen(int mouseX, int mouseY, float f) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, "MapWriter Options", this.width / 2, 40, 0xffffff);
        super.drawScreen(mouseX, mouseY, f);
    }
}
