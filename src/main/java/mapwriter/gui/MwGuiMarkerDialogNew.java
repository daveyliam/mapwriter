package mapwriter.gui;

import java.io.IOException;

import mapwriter.api.MwAPI;
import mapwriter.map.Marker;
import mapwriter.map.MarkerManager;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@net.minecraftforge.fml.relauncher.SideOnly(Side.CLIENT)
public class MwGuiMarkerDialogNew extends GuiScreen {
	private final GuiScreen parentScreen;
	String title = "";
	String titleNew = "mw.gui.mwguimarkerdialognew.title.new";
	String titleEdit = "mw.gui.mwguimarkerdialognew.title.edit";
	private String editMarkerName = "mw.gui.mwguimarkerdialognew.editMarkerName";
	private String editMarkerGroup = "mw.gui.mwguimarkerdialognew.editMarkerGroup";
	private String editMarkerX = "mw.gui.mwguimarkerdialognew.editMarkerX";
	private String editMarkerY = "mw.gui.mwguimarkerdialognew.editMarkerY";
	private String editMarkerZ = "mw.gui.mwguimarkerdialognew.editMarkerZ";
	ScrollableTextBox scrollableTextBoxName = null;
	ScrollableTextBox scrollableTextBoxGroup = null;
	ScrollableNumericTextBox scrollableNumericTextBoxX = null;
	ScrollableNumericTextBox scrollableNumericTextBoxY = null;
	ScrollableNumericTextBox scrollableNumericTextBoxZ = null;
	boolean backToGameOnSubmit = false;
	static final int dialogWidthPercent = 40;
	static final int elementVSpacing = 20;
	private final MarkerManager markerManager;
	private Marker editingMarker;
	private String markerName = "";
	private String markerGroup = "";
	private int markerX = 0;
	private int markerY = 80;
	private int markerZ = 0;
	private int dimension = 0;

	
	public MwGuiMarkerDialogNew(GuiScreen parentScreen,
			MarkerManager markerManager, String markerName, String markerGroup,
			int x, int y, int z, int dimension) {
		this.markerManager = markerManager;
		this.markerName = markerName;
		this.markerGroup = markerGroup;
		this.markerX = x;
		this.markerY = y;
		this.markerZ = z;
		this.editingMarker = null;
		this.dimension = dimension;
		this.parentScreen = parentScreen;
		this.title = this.titleNew;
	}

	public MwGuiMarkerDialogNew(GuiScreen parentScreen,
			MarkerManager markerManager, Marker editingMarker) {
		this.markerManager = markerManager;
		this.editingMarker = editingMarker;
		this.markerName = editingMarker.name;
		this.markerGroup = editingMarker.groupName;
		this.markerX = editingMarker.x;
		this.markerY = editingMarker.y;
		this.markerZ = editingMarker.z;
		this.dimension = editingMarker.dimension;
		this.parentScreen = parentScreen;
		this.title = this.titleEdit;
	}

	public boolean submit() {
		boolean inputCorrect = true;
		if (scrollableTextBoxName.validateTextFieldData())
			this.markerName = scrollableTextBoxName.getText();
		else
			inputCorrect = false;
		if (scrollableTextBoxGroup.validateTextFieldData())
			this.markerGroup = scrollableTextBoxGroup.getText();
		else
			inputCorrect = false;
		if (scrollableNumericTextBoxX.validateTextFieldData())
			this.markerX = scrollableNumericTextBoxX.getTextFieldIntValue();
		else
			inputCorrect = false;
		if (scrollableNumericTextBoxY.validateTextFieldData())
			this.markerY = scrollableNumericTextBoxY.getTextFieldIntValue();
		else
			inputCorrect = false;
		if (scrollableNumericTextBoxZ.validateTextFieldData())
			this.markerZ = scrollableNumericTextBoxZ.getTextFieldIntValue();
		else
			inputCorrect = false;
		if (inputCorrect) {
			int colour = Marker.getCurrentColour();
			if (this.editingMarker != null) {
				colour = this.editingMarker.colour;
				this.markerManager.delMarker(this.editingMarker);
				this.editingMarker = null;
			}
			this.markerManager.addMarker(this.markerName, this.markerGroup,
					this.markerX, this.markerY, this.markerZ, this.dimension,
					colour);
			this.markerManager.setVisibleGroupName(this.markerGroup);
			this.markerManager.update();
		}
		return inputCorrect;
	}

	public void initGui() {
		int labelsWidth = this.fontRendererObj.getStringWidth("Group");
		int width = this.width * dialogWidthPercent / 100 - labelsWidth;
		int x = (this.width - width) / 2 + labelsWidth;
		int y = (this.height - elementVSpacing * 5) / 2;
		
		this.scrollableTextBoxName = new ScrollableTextBox(x, y, width,	I18n.format(this.editMarkerName, new Object[0]), this.fontRendererObj);
		this.scrollableTextBoxName.setFocused(true);
		this.scrollableTextBoxName.setText(this.markerName);
		
		this.scrollableTextBoxGroup = new ScrollableTextBox(x, y
				+ MwGuiMarkerDialogNew.elementVSpacing, width, I18n.format(this.editMarkerGroup, new Object[0]),
				this.markerManager.groupList, this.fontRendererObj);
		this.scrollableTextBoxGroup.setText(this.markerGroup);
		this.scrollableTextBoxGroup.setDrawArrows(true);
		
		this.scrollableNumericTextBoxX = new ScrollableNumericTextBox(x, y
				+ MwGuiMarkerDialogNew.elementVSpacing * 2, width, I18n.format(this.editMarkerX, new Object[0]), this.fontRendererObj);
		this.scrollableNumericTextBoxX.setText("" + this.markerX);
		this.scrollableNumericTextBoxX.setDrawArrows(true);
		
		this.scrollableNumericTextBoxY = new ScrollableNumericTextBox(x, y
				+ MwGuiMarkerDialogNew.elementVSpacing * 3, width, I18n.format(this.editMarkerY, new Object[0]), this.fontRendererObj);
		this.scrollableNumericTextBoxY.setText("" + this.markerY);
		this.scrollableNumericTextBoxY.setDrawArrows(true);
		
		this.scrollableNumericTextBoxZ = new ScrollableNumericTextBox(x, y
				+ MwGuiMarkerDialogNew.elementVSpacing * 4, width, I18n.format(this.editMarkerZ, new Object[0]), this.fontRendererObj);
		this.scrollableNumericTextBoxZ.setText("" + this.markerZ);
		this.scrollableNumericTextBoxZ.setDrawArrows(true);
	}

	public void drawScreen(int mouseX, int mouseY, float f) {
		if (this.parentScreen != null) {
			this.parentScreen.drawScreen(mouseX, mouseY, f);
		} else {
			this.drawDefaultBackground();
		}
		int w = this.width * MwGuiMarkerDialogNew.dialogWidthPercent / 100;
		//int y = (this.height - MwGuiMarkerDialogNew.elementVSpacing * 5) / 2 + 2;
		drawRect(
				(this.width - w) / 2,
				(this.height - MwGuiMarkerDialogNew.elementVSpacing * 7) / 2 - 4,
				(this.width - w) / 2 + w,
				(this.height - MwGuiMarkerDialogNew.elementVSpacing * 7) / 2
						+ MwGuiMarkerDialogNew.elementVSpacing * 6,
				0x80000000);
		this.drawCenteredString(
				this.fontRendererObj,
				I18n.format(this.title, new Object[0]),
				(this.width) / 2, 
				(this.height - MwGuiMarkerDialogNew.elementVSpacing * 6) / 2
						- MwGuiMarkerDialogNew.elementVSpacing / 4,
				0xffffff);
		this.scrollableTextBoxName.draw();
		this.scrollableTextBoxGroup.draw();
		this.scrollableNumericTextBoxX.draw();
		this.scrollableNumericTextBoxY.draw();
		this.scrollableNumericTextBoxZ.draw();
		super.drawScreen(mouseX, mouseY, f);
	}

	// override GuiScreen's handleMouseInput to process
	// the scroll wheel.
	@Override
	public void handleMouseInput() throws IOException {
		if (MwAPI.getCurrentDataProvider() != null)
			return;
		int x = Mouse.getEventX() * this.width / this.mc.displayWidth;
		int y = this.height - Mouse.getEventY() * this.height
				/ this.mc.displayHeight - 1;
		int direction = Mouse.getEventDWheel();
		if (direction != 0) {
			this.mouseDWheelScrolled(x, y, direction);
		}
		super.handleMouseInput();
	}

	public void mouseDWheelScrolled(int x, int y, int direction) {
		this.scrollableTextBoxName.mouseDWheelScrolled(x, y, direction);
		this.scrollableTextBoxGroup.mouseDWheelScrolled(x, y, direction);
		this.scrollableNumericTextBoxX.mouseDWheelScrolled(x, y, direction);
		this.scrollableNumericTextBoxY.mouseDWheelScrolled(x, y, direction);
		this.scrollableNumericTextBoxZ.mouseDWheelScrolled(x, y, direction);
	}

	protected void mouseClicked(int x, int y, int button) throws IOException {
		super.mouseClicked(x, y, button);
		this.scrollableTextBoxName.mouseClicked(x, y, button);
		this.scrollableTextBoxGroup.mouseClicked(x, y, button);
		this.scrollableNumericTextBoxX.mouseClicked(x, y, button);
		this.scrollableNumericTextBoxY.mouseClicked(x, y, button);
		this.scrollableNumericTextBoxZ.mouseClicked(x, y, button);
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
		case Keyboard.KEY_TAB:
			GuiTextField thistextField = null;
			GuiTextField prevtextField = null;
			GuiTextField nexttextField = null;
			
			if (this.scrollableTextBoxName.isFocused())
			{
				thistextField = scrollableTextBoxName;
				prevtextField = scrollableNumericTextBoxZ;
				nexttextField = scrollableTextBoxGroup;
			}
			else if (this.scrollableTextBoxGroup.isFocused())
			{
				thistextField = scrollableTextBoxGroup;
				prevtextField = scrollableTextBoxName;
				nexttextField = scrollableNumericTextBoxX;
			}
			else if (this.scrollableNumericTextBoxX.isFocused())
			{
				thistextField = scrollableNumericTextBoxX;
				prevtextField = scrollableTextBoxGroup;
				nexttextField = scrollableNumericTextBoxY;
			}
			else if (this.scrollableNumericTextBoxY.isFocused())
			{
				thistextField = scrollableNumericTextBoxY;
				prevtextField = scrollableNumericTextBoxX;
				nexttextField = scrollableNumericTextBoxZ;
			}
			else if (this.scrollableNumericTextBoxZ.isFocused())
			{
				thistextField = scrollableNumericTextBoxZ;
				prevtextField = scrollableNumericTextBoxY;
				nexttextField = scrollableTextBoxName;
			}
			
			thistextField.setFocused(false);
			thistextField.setCursorPositionEnd();
			if (Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54))
			{
				prevtextField.setFocused(true);
				prevtextField.setSelectionPos(0);
			}
			else
			{
				nexttextField.setFocused(true);
				nexttextField.setSelectionPos(0);
			}
			
			break;
		default:
			if (this.scrollableTextBoxName.isFocused())
				this.scrollableTextBoxName.textboxKeyTyped(c, key);
			else if (this.scrollableTextBoxGroup.isFocused())
				this.scrollableTextBoxGroup.textboxKeyTyped(c, key);
			else if (this.scrollableNumericTextBoxX.isFocused())
				this.scrollableNumericTextBoxX.validateTextboxKeyTyped(c, key);
			else if (this.scrollableNumericTextBoxY.isFocused())
				this.scrollableNumericTextBoxY.validateTextboxKeyTyped(c, key);
			else if (this.scrollableNumericTextBoxZ.isFocused())
				this.scrollableNumericTextBoxZ.validateTextboxKeyTyped(c, key);
			break;
		}
	}
}