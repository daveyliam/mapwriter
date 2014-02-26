package mapwriter.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import org.lwjgl.input.Keyboard;

public class MwGuiTextDialog extends GuiScreen {

	private final GuiScreen parentScreen;
	
	String title;
	String text;
	String error;
	GuiTextField textField = null;
	boolean inputValid = false;
	boolean showError = false;
	boolean backToGameOnSubmit = false;
	static final int textDialogWidthPercent = 50;
	static final int textDialogTitleY = 80;
	static final int textDialogY = 92;
	static final int textDialogErrorY = 108;
	
	public MwGuiTextDialog(GuiScreen parentScreen, String title, String text, String error) {
		this.parentScreen = parentScreen;
		this.title = title;
		this.text = text;
		this.error = error;
	}
	
	private void newTextField() {
		if (this.textField != null) {
			this.text = this.textField.getText();
		}
    	int w = this.width * textDialogWidthPercent / 100;
    	this.textField = new GuiTextField(this.fontRendererObj,
    			(this.width - w) / 2 + 5,
    			textDialogY,
    			w - 10,
    			12);
		this.textField.setMaxStringLength(32);
		this.textField.setFocused(true);
		this.textField.setCanLoseFocus(false);
		//this.textField.setEnableBackgroundDrawing(false);
        this.textField.setText(this.text);
    }
	
	public void setText(String s) {
		this.textField.setText(s);
		this.text = s;
	}
	
	public String getInputAsString() {
		String s = this.textField.getText().trim();
		this.inputValid = (s.length() > 0);
		this.showError = !this.inputValid;
		return s;
	}
	
	public int getInputAsInt() {
		String s = this.getInputAsString();
		int value = 0;
		try {
			value = Integer.parseInt(s);
			this.inputValid = true;
			this.showError = false;
		} catch (NumberFormatException e) {
			this.inputValid = false;
			this.showError = true;
		}
		return value;
	}
	
	public int getInputAsHexInt() {
		String s = this.getInputAsString();
		int value = 0;
		try {
			value = Integer.parseInt(s, 16);
			this.inputValid = true;
			this.showError = false;
		} catch (NumberFormatException e) {
			this.inputValid = false;
			this.showError = true;
		}
		return value;
	}
	
	public boolean submit() {
		return false;
	}
	
	public void initGui() {
		this.newTextField();
    }

    public void drawScreen(int mouseX, int mouseY, float f) {
        
    	if (this.parentScreen != null) {
    		this.parentScreen.drawScreen(mouseX, mouseY, f);
    	} else {
    		this.drawDefaultBackground();
    	}
        
        int w = this.width * textDialogWidthPercent / 100;
    	drawRect(
    			(this.width - w) / 2,
    			textDialogTitleY - 4,
    			(this.width - w) / 2 + w,
    			textDialogErrorY + 14,
    			0x80000000);
    	this.drawCenteredString(
    			this.fontRendererObj,
        		this.title,
        		this.width / 2,
        		textDialogTitleY,
        		0xffffff);
    	this.textField.drawTextBox();
    	if (this.showError) {
	    	this.drawCenteredString(
	    			this.fontRendererObj,
	        		this.error,
	        		this.width / 2,
	        		textDialogErrorY,
	        		0xffffff);
    	}
        
        super.drawScreen(mouseX, mouseY, f);
    }

    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
    }

    protected void keyTyped(char c, int key) {
    	switch (key) {
		case Keyboard.KEY_ESCAPE:
			this.mc.displayGuiScreen(this.parentScreen);
			break;
			
		case Keyboard.KEY_RETURN:
			// when enter pressed, submit current input
			if (this.submit()) {
				if (!this.backToGameOnSubmit) {
					this.mc.displayGuiScreen(this.parentScreen);
				} else {
					this.mc.displayGuiScreen(null);
				}
			}
			break;
			
		default:
			// other characters are processed by the text box
			this.textField.textboxKeyTyped(c, key);
			this.text = this.textField.getText();
    		break;
		}
    }
}
