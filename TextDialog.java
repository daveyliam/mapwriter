package mapwriter;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;

import org.lwjgl.input.Keyboard;

public class TextDialog extends Gui {
	String title;
	String text;
	String error;
	GuiTextField textField = null;
	boolean inputValid = false;
	boolean showError = false;
	boolean closed = false;
	int sw = 1;
	int sh = 1;
	FontRenderer fontRenderer;
	static final int textDialogWidthPercent = 50;
	static final int textDialogTitleY = 80;
	static final int textDialogY = 92;
	static final int textDialogErrorY = 108;
	
	public TextDialog(int sw, int sh, FontRenderer fontRenderer, String title, String text, String error) {
		this.title = title;
		this.text = text;
		this.error = error;
		this.update(sw, sh, fontRenderer);
	}
	
	private void newTextField() {
		if (this.textField != null) {
			this.text = this.textField.getText();
		}
    	int w = this.sw * textDialogWidthPercent / 100;
    	this.textField = new GuiTextField(this.fontRenderer,
    			(this.sw - w) / 2 + 5,
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
	
	public void update(int sw, int sh, FontRenderer fontRenderer) {
		this.sw = sw;
		this.sh = sh;
		this.fontRenderer = fontRenderer;
		this.newTextField();
	}
	
	public void draw() {
		int w = this.sw * textDialogWidthPercent / 100;
    	drawRect(
    			(this.sw - w) / 2,
    			textDialogTitleY - 4,
    			(this.sw - w) / 2 + w,
    			textDialogErrorY + 14,
    			0x80000000);
    	this.drawCenteredString(
    			this.fontRenderer,
        		this.title,
        		this.sw / 2,
        		textDialogTitleY,
        		0xffffff);
    	this.textField.drawTextBox();
    	if (this.showError) {
	    	this.drawCenteredString(
	    			this.fontRenderer,
	        		this.error,
	        		this.sw / 2,
	        		textDialogErrorY,
	        		0xffffff);
    	}
	}
	
	public void keyTyped(char c, int key) {
		switch (key) {
		case Keyboard.KEY_ESCAPE:
			this.closed = true;
			break;
			
		case Keyboard.KEY_RETURN:
			// when enter pressed, submit current input
			this.submit();
			break;
			
		default:
			// other characters are processed by the text box
			this.textField.textboxKeyTyped(c, key);
			this.text = this.textField.getText();
    		break;
		}
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
	
	public void submit() {
		this.closed = true;
	}
}
