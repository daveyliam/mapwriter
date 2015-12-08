package mapwriter.gui;

import java.util.List;

import mapwriter.Render;
import mapwriter.api.MwAPI;
import mapwriter.map.Marker;
import mapwriter.map.MarkerManager;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MwGuiMarkerDialogNew extends GuiScreen
{
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
	private ResourceLocation leftArrowTexture = new ResourceLocation(
			"mapwriter", "textures/map/arrow_text_left.png");
	private ResourceLocation rightArrowTexture = new ResourceLocation(
			"mapwriter", "textures/map/arrow_text_right.png");

	class ScrollableTextBox {
		public int x;
		public int y;
		public int width;
		// private int height;
		public int labelX;
		public int labelY;
		public int labelWidth;
		public int labelHeight;
		public String label;
		public boolean drawArrows = false;
		public int leftArrowX;
		public int rightArrowX;
		public int arrowsY;
		public int arrowsWidth = 7;
		public int arrowsHeight = 12;
		public int textFieldX;
		public int textFieldY;
		public int textFieldWidth;
		public int textFieldHeight = 12;
		public List<String> scrollableElements;
		public GuiTextField textField = null;

		ScrollableTextBox(int x, int y, int width, String label) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.label = label;
		}

		ScrollableTextBox(int x, int y, int width, String label,
				List<String> scrollableElements) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.label = label;
			this.scrollableElements = scrollableElements;
		}

		public void init() {
			this.textFieldX = this.x + this.arrowsWidth;
			this.textFieldY = this.y;
			this.textFieldWidth = this.width - this.arrowsWidth * 2 - 25;
			this.labelWidth = MwGuiMarkerDialogNew.this.fontRendererObj
					.getStringWidth(this.label);
			this.labelHeight = MwGuiMarkerDialogNew.this.fontRendererObj.FONT_HEIGHT;
			this.labelX = this.x - this.labelWidth - 4;
			this.labelY = this.y + this.labelHeight / 2 - 2;
			this.leftArrowX = this.x - 1;
			this.rightArrowX = this.textFieldX + this.textFieldWidth + 1;
			this.arrowsY = this.y;
			this.textField = new GuiTextField(
					MwGuiMarkerDialogNew.this.fontRendererObj, this.textFieldX,
					this.textFieldY, this.textFieldWidth, this.textFieldHeight);
			this.textField.setMaxStringLength(32);
		}

		public void draw() {
			MwGuiMarkerDialogNew screen = MwGuiMarkerDialogNew.this;
			screen.drawString(screen.fontRendererObj, this.label, this.labelX,
					this.labelY, 0xffffff);
			if (this.drawArrows) {
				screen.mc.renderEngine.bindTexture(screen.leftArrowTexture);
				Render.drawTexturedRect(this.leftArrowX, this.arrowsY,
						this.arrowsWidth, this.arrowsHeight, 0.0, 0.0, 1.0, 1.0);
				MwGuiMarkerDialogNew.this.mc.renderEngine
						.bindTexture(screen.rightArrowTexture);
				Render.drawTexturedRect(this.rightArrowX, this.arrowsY,
						this.arrowsWidth, this.arrowsHeight, 0.0, 0.0, 1.0, 1.0);
			}
			this.textField.drawTextBox();
			if (!this.validateTextFieldData()) {
				drawRect(this.textFieldX - 1, this.textFieldY - 1,
						this.textFieldX + this.textFieldWidth + 1,
						this.textFieldY, 
						0xff900000);
				drawRect(this.textFieldX - 1, this.textFieldY - 1,
						this.textFieldX, this.textFieldY + this.textFieldHeight	+ 1,
						0xff900000);
				drawRect(this.textFieldX + this.textFieldWidth + 1,
						this.textFieldY + this.textFieldHeight + 1,
						this.textFieldX,
						this.textFieldY + this.textFieldHeight, 
						0xff900000);
				drawRect(this.textFieldX + this.textFieldWidth + 1,
						this.textFieldY + this.textFieldHeight + 1,
						this.textFieldX + this.textFieldWidth, this.textFieldY,
						0xff900000);
			}
		}

		public void mouseClicked(int x, int y, int button) {
			int direction = this.posWithinArrows(x, y);
			if (direction != 0)
				this.textFieldScroll(direction);
			this.textField.mouseClicked(x, y, button);
		}

		public void setDrawArrows(boolean value) {
			this.drawArrows = value;
		}

		public void mouseDWheelScrolled(int x, int y, int direction) {
			if (posWithinTextField(x, y))
				textFieldScroll(-direction);
		}

		public boolean validateTextFieldData() {
			return this.textField.getText().length() > 0;
		}

		/**
		 * 
		 * @return Returns clicked arrow: 1 for right and -1 for left
		 */
		public int posWithinArrows(int x, int y) {
			if ((x >= this.leftArrowX) && (y >= this.arrowsY)
					&& (x <= this.arrowsWidth + this.leftArrowX)
					&& (y <= this.arrowsHeight + this.arrowsY))
				return -1;
			else if ((x >= this.rightArrowX) && (y >= this.arrowsY)
					&& (x <= this.arrowsWidth + this.rightArrowX)
					&& (y <= this.arrowsHeight + this.arrowsY))
				return 1;
			else
				return 0;
		}

		public boolean posWithinTextField(int x, int y) {
			return (x >= this.textFieldX) && (y >= this.textFieldY)
					&& (x <= this.textFieldWidth + this.textFieldX)
					&& (y <= this.textFieldHeight + this.textFieldY);
		}

		public void textFieldScroll(int direction) {
			if (this.scrollableElements != null) {
				int index = this.scrollableElements.indexOf(this.textField
						.getText().trim());
				if (direction > 0) {
					if (index == -1
							|| index == this.scrollableElements.size() - 1)
						index = 0;
					else
						index++;
				} else if (direction < 0) {
					if (index == -1 || index == 0)
						index = this.scrollableElements.size() - 1;
					else
						index--;
				}
				this.textField.setText(this.scrollableElements.get(index));
			}
		}
	}

	class ScrollableNumericTextBox extends ScrollableTextBox {

		public ScrollableNumericTextBox(int x, int y, int width, String label) {
			super(x, y, width, label);
		}

		@Override
		public void textFieldScroll(int direction) {
			if (this.validateTextFieldData()) {
				int value = this.getTextFieldIntValue();
				if (direction > 0)
					this.textField.setText("" + (value + 1));
				else if (direction < 0)
					this.textField.setText("" + (value - 1));
			}
		}

		public int getTextFieldIntValue() {
			return Integer.parseInt(this.textField.getText());
		}

		public void validateTextboxKeyTyped(char c, int key) {
			if ((c >= '0' && c <= '9') || key == Keyboard.KEY_BACK
					|| key == Keyboard.KEY_LEFT || key == Keyboard.KEY_RIGHT
					|| (c == '-' && (this.textField.getCursorPosition() == 0)))
				this.textField.textboxKeyTyped(c, key);
		}
	}

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
			this.markerName = scrollableTextBoxName.textField.getText();
		else
			inputCorrect = false;
		if (scrollableTextBoxGroup.validateTextFieldData())
			this.markerGroup = scrollableTextBoxGroup.textField.getText();
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
		this.scrollableTextBoxName = new ScrollableTextBox(x, y, width,
				I18n.format(this.editMarkerName));
		this.scrollableTextBoxName.init();
		this.scrollableTextBoxName.textField.setFocused(true);
		this.scrollableTextBoxName.textField.setText(this.markerName);
		this.scrollableTextBoxGroup = new ScrollableTextBox(x, y
				+ this.elementVSpacing, width, I18n.format(this.editMarkerGroup),
				this.markerManager.groupList);
		this.scrollableTextBoxGroup.init();
		this.scrollableTextBoxGroup.textField.setText(this.markerGroup);
		this.scrollableTextBoxGroup.setDrawArrows(true);
		this.scrollableNumericTextBoxX = new ScrollableNumericTextBox(x, y
				+ this.elementVSpacing * 2, width, I18n.format(this.editMarkerX));
		this.scrollableNumericTextBoxX.init();
		this.scrollableNumericTextBoxX.textField.setText("" + this.markerX);
		this.scrollableNumericTextBoxX.setDrawArrows(true);
		this.scrollableNumericTextBoxY = new ScrollableNumericTextBox(x, y
				+ this.elementVSpacing * 3, width, I18n.format(this.editMarkerY));
		this.scrollableNumericTextBoxY.init();
		this.scrollableNumericTextBoxY.textField.setText("" + this.markerY);
		this.scrollableNumericTextBoxY.setDrawArrows(true);
		this.scrollableNumericTextBoxZ = new ScrollableNumericTextBox(x, y
				+ this.elementVSpacing * 4, width, I18n.format(this.editMarkerZ));
		this.scrollableNumericTextBoxZ.init();
		this.scrollableNumericTextBoxZ.textField.setText("" + this.markerZ);
		this.scrollableNumericTextBoxZ.setDrawArrows(true);
	}

	public void drawScreen(int mouseX, int mouseY, float f) {
		if (this.parentScreen != null) {
			this.parentScreen.drawScreen(mouseX, mouseY, f);
		} else {
			this.drawDefaultBackground();
		}
		int w = this.width * this.dialogWidthPercent / 100;
		int y = (this.height - this.elementVSpacing * 5) / 2 + 2;
		drawRect(
				(this.width - w) / 2,
				(this.height - this.elementVSpacing * 7) / 2 - 4,
				(this.width - w) / 2 + w,
				(this.height - this.elementVSpacing * 7) / 2
						+ this.elementVSpacing * 6,
				0x80000000);
		this.drawCenteredString(
				this.fontRendererObj,
				I18n.format(this.title),
				(this.width) / 2, 
				(this.height - this.elementVSpacing * 6) / 2
						- this.elementVSpacing / 4,
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
	public void handleMouseInput() {
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

	protected void mouseClicked(int x, int y, int button) {
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
			
			if (this.scrollableTextBoxName.textField.isFocused())
			{
				thistextField = scrollableTextBoxName.textField;
				prevtextField = scrollableNumericTextBoxZ.textField;
				nexttextField = scrollableTextBoxGroup.textField;
			}
			else if (this.scrollableTextBoxGroup.textField.isFocused())
			{
				thistextField = scrollableTextBoxGroup.textField;
				prevtextField = scrollableTextBoxName.textField;
				nexttextField = scrollableNumericTextBoxX.textField;
			}
			else if (this.scrollableNumericTextBoxX.textField.isFocused())
			{
				thistextField = scrollableNumericTextBoxX.textField;
				prevtextField = scrollableTextBoxGroup.textField;
				nexttextField = scrollableNumericTextBoxY.textField;
			}
			else if (this.scrollableNumericTextBoxY.textField.isFocused())
			{
				thistextField = scrollableNumericTextBoxY.textField;
				prevtextField = scrollableNumericTextBoxX.textField;
				nexttextField = scrollableNumericTextBoxZ.textField;
			}
			else if (this.scrollableNumericTextBoxZ.textField.isFocused())
			{
				thistextField = scrollableNumericTextBoxZ.textField;
				prevtextField = scrollableNumericTextBoxY.textField;
				nexttextField = scrollableTextBoxName.textField;
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
			if (this.scrollableTextBoxName.textField.isFocused())
				this.scrollableTextBoxName.textField.textboxKeyTyped(c, key);
			else if (this.scrollableTextBoxGroup.textField.isFocused())
				this.scrollableTextBoxGroup.textField.textboxKeyTyped(c, key);
			else if (this.scrollableNumericTextBoxX.textField.isFocused())
				this.scrollableNumericTextBoxX.validateTextboxKeyTyped(c, key);
			else if (this.scrollableNumericTextBoxY.textField.isFocused())
				this.scrollableNumericTextBoxY.validateTextboxKeyTyped(c, key);
			else if (this.scrollableNumericTextBoxZ.textField.isFocused())
				this.scrollableNumericTextBoxZ.validateTextboxKeyTyped(c, key);
			break;
		}
	}
}