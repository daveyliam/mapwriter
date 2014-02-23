package mapwriter.gui;

import mapwriter.Mw;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class MwGuiOptions extends GuiScreen {
	
	private final Mw mw;
	private final GuiScreen parentScreen;
	private MwGuiOptionSlot optionSlot = null;
	
	public MwGuiOptions(GuiScreen parentScreen, Mw mw) {
		this.mw = mw;
		this.parentScreen = parentScreen;
	}
	
    @SuppressWarnings("unchecked")
	public void initGui() {
    	this.optionSlot = new MwGuiOptionSlot(this, this.mc, this.mw);
        this.optionSlot.registerScrollButtons(7, 8);
        
        this.buttonList.add(new GuiButton(200, (this.width / 2) - 50, this.height - 28, 100, 20, "Done"));
    }
    
    protected void actionPerformed(GuiButton button) {
		if (button.id == 200) {
			// done
			// reconfigure texture size
			this.mw.setTextureSize();
			this.mc.displayGuiScreen(this.parentScreen);
		}
	}

    public void drawScreen(int mouseX, int mouseY, float f) {
        this.drawDefaultBackground();
        this.optionSlot.drawScreen(mouseX, mouseY, f);
        this.drawCenteredString(this.fontRendererObj, "MapWriter Options", this.width / 2, 10, 0xffffff);
        super.drawScreen(mouseX, mouseY, f);
    }

    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
    }

    protected void keyTyped(char c, int k) {
        if (this.optionSlot.keyTyped(c, k)) {
            super.keyTyped(c, k);
        }
    }
}
