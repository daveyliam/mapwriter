package mapwriter.gui;

import java.awt.Point;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import mapwriter.Mw;
import mapwriter.api.IMwDataProvider;
import mapwriter.api.MwAPI;
import mapwriter.config.Config;
import mapwriter.config.WorldConfig;
import mapwriter.forge.MwKeyHandler;
import mapwriter.map.MapRenderer;
import mapwriter.map.MapView;
import mapwriter.map.Marker;
import mapwriter.map.mapmode.FullScreenMapMode;
import mapwriter.map.mapmode.MapMode;
import mapwriter.tasks.MergeTask;
import mapwriter.tasks.RebuildRegionsTask;
import mapwriter.util.Logging;
import mapwriter.util.Reference;
import mapwriter.util.Utils;
import mapwriter.util.VersionCheck;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@SideOnly(Side.CLIENT)
public class MwGui extends GuiScreen {
	private Mw mw;
	public MapMode mapMode;
	private MapView mapView;
	private MapRenderer map;

	private String[] HelpText1 = new String[]{				
			"Keys:",
			"",
		    "  Space",
			"  Delete",
			"  C",
			"  Home",
			"  End",
			"  N",
			"  T",
			"  P",
			"  R",
			"  U",
			"",
			"mw.gui.mwgui.helptext.1",
			"mw.gui.mwgui.helptext.2", 
			"mw.gui.mwgui.helptext.3", 
			"mw.gui.mwgui.helptext.4", 
			"mw.gui.mwgui.helptext.5", 
			"mw.gui.mwgui.helptext.6"
			};
	private String[] HelpText2 = new String[]{
			"",
			"",
			"mw.gui.mwgui.helptext.nextmarkergroup", 
			"mw.gui.mwgui.helptext.deletemarker",
			"mw.gui.mwgui.helptext.cyclecolour", 
			"mw.gui.mwgui.helptext.centermap",
			"mw.gui.mwgui.helptext.centermapplayer", 
			"mw.gui.mwgui.helptext.selectnextmarker",
			"mw.gui.mwgui.helptext.teleport",
			"mw.gui.mwgui.helptext.savepng",
			"mw.gui.mwgui.helptext.regenerate",
			"mw.gui.mwgui.helptext.undergroundmap"
			};
	
	private final static double PAN_FACTOR = 0.3D;

	private static final int menuY = 5;
	private static final int menuX = 5;

	private int mouseLeftHeld = 0;
	private int mouseLeftDragStartX = 0;
	private int mouseLeftDragStartY = 0;
	private double viewXStart;
	private double viewZStart;
	private Marker movingMarker = null;
	private int movingMarkerXStart = 0;
	private int movingMarkerZStart = 0;
	private int mouseBlockX = 0;
	private int mouseBlockY = 0;
	private int mouseBlockZ = 0;

	private MwGuiLabel helpLabel;
	private MwGuiLabel optionsLabel;
	private MwGuiLabel dimensionLabel;
	private MwGuiLabel groupLabel;
	private MwGuiLabel overlayLabel;
	private MwGuiLabel updateLabel;
	private MwGuiMarkerListOverlay MarkerOverlay;
	
	private MwGuiLabel helpTooltipLabel;
	private MwGuiLabel updateTooltipLabel;
	private MwGuiLabel statusLabel;
	private MwGuiLabel markerLabel;
	
	public static MwGui instance;

	
	private URI clickedLinkURI;

	public MwGui(Mw mw) {
		this.mw = mw;
		this.mapMode = new FullScreenMapMode();
		this.mapView = new MapView(this.mw, true);
		this.map = new MapRenderer(this.mw, this.mapMode, this.mapView);

		this.mapView.setDimension(this.mw.miniMap.view.getDimension());
		this.mapView.setViewCentreScaled(this.mw.playerX, this.mw.playerZ,
				this.mw.playerDimension);
		this.mapView.setZoomLevel(Config.fullScreenZoomLevel);

		this.helpLabel = new MwGuiLabel(this);
		this.optionsLabel = new MwGuiLabel(this);
		this.dimensionLabel = new MwGuiLabel(this);
		this.groupLabel = new MwGuiLabel(this);
		this.overlayLabel = new MwGuiLabel(this);
		this.updateLabel = new MwGuiLabel(this);
		this.helpTooltipLabel = new MwGuiLabel(this);
		this.updateTooltipLabel = new MwGuiLabel(this);
		this.statusLabel = new MwGuiLabel(this);
		this.markerLabel = new MwGuiLabel(this);
		
		this.MarkerOverlay = new MwGuiMarkerListOverlay(this, this.mw.markerManager);
		
		instance = this;
	}

	public MwGui(Mw mw, int dim, int x, int z) {
		this(mw);
		this.mapView.setDimension(dim);
		this.mapView.setViewCentreScaled(x, z, dim);
		this.mapView.setZoomLevel(Config.fullScreenZoomLevel);
	}

	// called when gui is displayed and every time the screen
	// is resized
	public void initGui() {
	}

	// called when a button is pressed
	protected void actionPerformed(GuiButton button) {

	}
	
	// get a marker near the specified block pos if it exists.
	// the maxDistance is based on the view width so that you need to click
	// closer
	// to a marker when zoomed in to select it.
	public Marker getMarkerNearScreenPos(int x, int y) {
		Marker nearMarker = null;
		for (Marker marker : this.mw.markerManager.visibleMarkerList) {
			if (marker.screenPos != null) {
				if (marker.screenPos.distanceSq(x, y) < 6.0) {
					nearMarker = marker;
				}
			}
		}
		return nearMarker;
	}

	public int getHeightAtBlockPos(int bX, int bZ) {
		int bY = 0;
		int worldDimension = this.mw.mc.theWorld.provider.getDimensionId();
		if ((worldDimension == this.mapView.getDimension())
				&& (worldDimension != -1)) {
			bY = this.mw.mc.theWorld.getHeight(new BlockPos(bX, 0, bZ)).getY();
		}
		return bY;
	}

	public boolean isPlayerNearScreenPos(int x, int y) {
		Point.Double p = this.map.playerArrowScreenPos;
		return p.distanceSq(x, y) < 9.0;
	}

	public void deleteSelectedMarker() {
		if (this.mw.markerManager.selectedMarker != null) {
			// MwUtil.log("deleting marker %s",
			// this.mw.markerManager.selectedMarker.name);
			this.mw.markerManager
					.delMarker(this.mw.markerManager.selectedMarker);
			this.mw.markerManager.update();
			this.mw.markerManager.selectedMarker = null;
		}
	}

	public void mergeMapViewToImage() {
		this.mw.chunkManager.saveChunks();
		this.mw.executor.addTask(new MergeTask(this.mw.regionManager,
				(int) this.mapView.getX(), (int) this.mapView.getZ(),
				(int) this.mapView.getWidth(), (int) this.mapView.getHeight(),
				this.mapView.getDimension(), this.mw.worldDir, this.mw.worldDir
						.getName()));

		Utils.printBoth(I18n.format("mw.gui.mwgui.chatmsg.merge", new Object[0]) + " '" + this.mw.worldDir.getAbsolutePath()
				+ "'");
	}

	public void regenerateView() {
		Utils.printBoth(String.format(
				I18n.format("mw.gui.mwgui.chatmsg.regenmap.1", new Object[0]) + 
				" %dx%d " + 
				I18n.format("mw.gui.mwgui.chatmsg.regenmap.2", new Object[0]) +
				" (%d, %d)",
				(int) this.mapView.getWidth(), (int) this.mapView.getHeight(),
				(int) this.mapView.getMinX(), (int) this.mapView.getMinZ()));
		// this.mw.reloadBlockColours();
		this.mw.executor.addTask2(new RebuildRegionsTask(this.mw,
				(int) this.mapView.getMinX(), (int) this.mapView.getMinZ(),
				(int) this.mapView.getWidth(), (int) this.mapView.getHeight(),
				this.mapView.getDimension()));
	}

	// c is the ascii equivalent of the key typed.
	// key is the lwjgl key code.
	protected void keyTyped(char c, int key) {
		// MwUtil.log("MwGui.keyTyped(%c, %d)", c, key);
		switch (key) {
		case Keyboard.KEY_ESCAPE:
			this.exitGui();
			break;

		case Keyboard.KEY_DELETE:
			this.deleteSelectedMarker();
			break;

		case Keyboard.KEY_SPACE:
			// next marker group
			this.mw.markerManager.nextGroup();
			this.mw.markerManager.update();
			break;

		case Keyboard.KEY_C:
			// cycle selected marker colour
			if (this.mw.markerManager.selectedMarker != null) {
				this.mw.markerManager.selectedMarker.colourNext();
			}
			break;

		case Keyboard.KEY_N:
			// select next visible marker
			this.mw.markerManager.selectNextMarker();
			break;

		case Keyboard.KEY_HOME:
			// centre map on player
			this.mapView.setViewCentreScaled(this.mw.playerX, this.mw.playerZ,
					this.mw.playerDimension);
			break;

		case Keyboard.KEY_END:
			// centre map on selected marker
			if (this.mw.markerManager.selectedMarker != null) {
				this.mapView.setViewCentreScaled(
						this.mw.markerManager.selectedMarker.x,
						this.mw.markerManager.selectedMarker.z, 0);
			}
			break;

		case Keyboard.KEY_P:
			this.mergeMapViewToImage();
			this.exitGui();
			break;

		case Keyboard.KEY_T:
			if (this.mw.markerManager.selectedMarker != null) {
				this.mw.teleportToMarker(this.mw.markerManager.selectedMarker);
				this.exitGui();
			} else {
				this.mc.displayGuiScreen(new MwGuiTeleportDialog(this, this.mw,
						this.mapView, this.mouseBlockX,
						Config.defaultTeleportHeight, this.mouseBlockZ));
			}
			break;

		case Keyboard.KEY_LEFT:
			this.mapView.panView(-PAN_FACTOR, 0);
			break;
		case Keyboard.KEY_RIGHT:
			this.mapView.panView(PAN_FACTOR, 0);
			break;
		case Keyboard.KEY_UP:
			this.mapView.panView(0, -PAN_FACTOR);
			break;
		case Keyboard.KEY_DOWN:
			this.mapView.panView(0, PAN_FACTOR);
			break;

		case Keyboard.KEY_R:
			this.regenerateView();
			this.exitGui();
			break;

		default:
			if (key == MwKeyHandler.keyMapGui.getKeyCode()) {
				this.exitGui();
			} else if (key == MwKeyHandler.keyZoomIn.getKeyCode()) {
				this.mapView.adjustZoomLevel(-1);
			} else if (key == MwKeyHandler.keyZoomOut.getKeyCode()) {
				this.mapView.adjustZoomLevel(1);
			} else if (key == MwKeyHandler.keyNextGroup.getKeyCode()) {
				this.mw.markerManager.nextGroup();
				this.mw.markerManager.update();
			} else if (key == MwKeyHandler.keyUndergroundMode.getKeyCode()) {
				this.mw.toggleUndergroundMode();
				this.mapView.setUndergroundMode(Config.undergroundMode);
			}
			break;
		}
	}

	// override GuiScreen's handleMouseInput to process
	// the scroll wheel.
	@Override
	public void handleMouseInput() throws IOException {
		if (MwAPI.getCurrentDataProvider() != null
				&& MwAPI.getCurrentDataProvider().onMouseInput(this.mapView,
						this.mapMode))
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

	// mouse button clicked. 0 = LMB, 1 = RMB, 2 = MMB
	protected void mouseClicked(int x, int y, int button) {
		Marker marker = this.getMarkerNearScreenPos(x, y);
		Marker prevMarker = this.mw.markerManager.selectedMarker;

		if (button == 0) {
			if (this.dimensionLabel.posWithin(x, y)) {
				this.mc.displayGuiScreen(new MwGuiDimensionDialog(this,
						this.mw, this.mapView, this.mapView.getDimension()));
			} else if (this.optionsLabel.posWithin(x, y)) {
				try {
					GuiScreen newScreen = ModGuiConfig.class.getConstructor(
							GuiScreen.class).newInstance(this);
					this.mc.displayGuiScreen(newScreen);
				} catch (Exception e) {
					Logging.logError(
							"There was a critical issue trying to build the config GUI for %s",
							Reference.MOD_ID);
				}

				// this.mc.displayGuiScreen(new MwGuiOptions(this, this.mw));
			} else if (this.updateLabel.posWithin(x, y)) {
				URI uri;

				if (!this.mc.gameSettings.chatLinks) {
					return;
				}

				try {
					uri = new URI(VersionCheck.getUpdateURL());

					if (!Reference.PROTOCOLS.contains(uri.getScheme().toLowerCase())) {
						throw new URISyntaxException(uri.toString(),
								"Unsupported protocol: "
										+ uri.getScheme().toLowerCase());
					}

					if (this.mc.gameSettings.chatLinksPrompt) {
						this.clickedLinkURI = uri;
						this.mc.displayGuiScreen(new GuiConfirmOpenLink(this,
								uri.toString(), 31102009, false));
					} else {
						Utils.openWebLink(uri);
					}
				} catch (URISyntaxException urisyntaxexception) {
					Logging.logError("Can\'t open url for %s",
							urisyntaxexception);
				}
			} else {
				this.mouseLeftHeld = 1;
				this.mouseLeftDragStartX = x;
				this.mouseLeftDragStartY = y;
				this.mw.markerManager.selectedMarker = marker;

				if ((marker != null) && (prevMarker == marker)) {
					// clicked previously selected marker.
					// start moving the marker.
					this.movingMarker = marker;
					this.movingMarkerXStart = marker.x;
					this.movingMarkerZStart = marker.z;
				}
			}

		} else if (button == 1) {
			// this.mouseRightHeld = 1;
			if ((marker != null) && (prevMarker == marker)) {
				// right clicked previously selected marker.
				// edit the marker
				if (Config.newMarkerDialog)
        		{
            		this.mc.displayGuiScreen(
                			new MwGuiMarkerDialogNew(this,
            						this.mw.markerManager, marker
                			)
                		);	
        		}
        		else
        		{
				this.mc.displayGuiScreen(new MwGuiMarkerDialog(this,
						this.mw.markerManager, marker));
        		}

			} else if (marker == null) {
				// open new marker dialog
				String group = this.mw.markerManager.getVisibleGroupName();
				if (group.equals("none")) {
					group = I18n.format("mw.gui.mwgui.group", new Object[0]);
				}

				int mx, my, mz;
				if (this.isPlayerNearScreenPos(x, y)) {
					// marker at player's locations
					mx = this.mw.playerXInt;
					my = this.mw.playerYInt;
					mz = this.mw.playerZInt;

				} else {
					// marker at mouse pointer location
					mx = this.mouseBlockX;
					my = (this.mouseBlockY > 0) ? this.mouseBlockY
							: Config.defaultTeleportHeight;
					mz = this.mouseBlockZ;
				}
			if (Config.newMarkerDialog)
        		{
            		this.mc.displayGuiScreen(
                			new MwGuiMarkerDialogNew(
                				this,
                				this.mw.markerManager,
                				"",
                				group,
                				mx, my, mz,
                				this.mapView.getDimension()
                			)
                		);	
        		}
        		else
        		{
				this.mc.displayGuiScreen(new MwGuiMarkerDialog(this,
						this.mw.markerManager, "", group, mx, my, mz,
						this.mapView.getDimension()));
			}
		}
	}

		else if (button == 2) {
			Point blockPoint = this.mapMode.screenXYtoBlockXZ(this.mapView, x,
					y);

			IMwDataProvider provider = MwAPI.getCurrentDataProvider();
			if (provider != null)
				provider.onMiddleClick(this.mapView.getDimension(),
						blockPoint.x, blockPoint.y, this.mapView);
		}

		this.viewXStart = this.mapView.getX();
		this.viewZStart = this.mapView.getZ();
		// this.viewSizeStart = this.mapManager.getViewSize();
	}

	// mouse button released. 0 = LMB, 1 = RMB, 2 = MMB
	// not called on mouse movement.
	protected void mouseReleased(int x, int y, int button) {
		// MwUtil.log("MwGui.mouseMovedOrUp(%d, %d, %d)", x, y, button);
		if (button == 0) {
			this.mouseLeftHeld = 0;
			this.movingMarker = null;
		} else if (button == 1) {
			// this.mouseRightHeld = 0;
		}
	}

	// zoom on mouse direction wheel scroll
	public void mouseDWheelScrolled(int x, int y, int direction) {
		Marker marker = this.getMarkerNearScreenPos(x, y);
		if ((marker != null)
				&& (marker == this.mw.markerManager.selectedMarker)) {
			if (direction > 0) {
				marker.colourNext();
			} else {
				marker.colourPrev();
			}

		} else if (this.dimensionLabel.posWithin(x, y)) {
			int n = (direction > 0) ? 1 : -1;
			this.mapView.nextDimension(WorldConfig.getInstance().dimensionList,
					n);

		} else if (this.groupLabel.posWithin(x, y)) {
			int n = (direction > 0) ? 1 : -1;
			this.mw.markerManager.nextGroup(n);
			this.mw.markerManager.update();
		} else if (this.overlayLabel.posWithin(x, y)) {
			int n = (direction > 0) ? 1 : -1;
			if (MwAPI.getCurrentDataProvider() != null)
				MwAPI.getCurrentDataProvider().onOverlayDeactivated(
						this.mapView);

			if (n == 1)
				MwAPI.setNextProvider();
			else
				MwAPI.setPrevProvider();

			if (MwAPI.getCurrentDataProvider() != null)
				MwAPI.getCurrentDataProvider().onOverlayActivated(this.mapView);

		} else {
			int zF = (direction > 0) ? -1 : 1;
			this.mapView.zoomToPoint(this.mapView.getZoomLevel() + zF,
					this.mouseBlockX, this.mouseBlockZ);
			Config.fullScreenZoomLevel = this.mapView.getZoomLevel();
		}
	}

	//closes this gui
	public void exitGui()
	{
		this.mc.displayGuiScreen((GuiScreen)null);
	}
	
    /**
     * Called when the screen is unloaded. Used to disable keyboard repeat events
     */
    public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
		this.mw.miniMap.view.setDimension(this.mapView.getDimension());
		Keyboard.enableRepeatEvents(false);
    }
	
	// called every frame
	public void updateScreen() {
		super.updateScreen();
	}

	public void drawStatus(int bX, int bY, int bZ) 
	{
		StringBuilder builder = new StringBuilder();
		if (bY != 0) {
			builder.append(String.format(I18n.format("mw.gui.mwgui.status.cursor", new Object[0]) + ": (%d, %d, %d)", bX, bY, bZ));
		} else {
			builder.append(String.format(I18n.format("mw.gui.mwgui.status.cursor", new Object[0]) + ": (%d, ?, %d)", bX, bZ));
		}
		
		if (this.mc.theWorld != null) {
			if (!this.mc.theWorld.getChunkFromBlockCoords(new BlockPos(bX, 0, bZ)).isEmpty()) 
			{
				builder.append(", " );
				builder.append(I18n.format("mw.gui.mwgui.status.biome", new Object[0]));
				builder.append(String.format(": %s", this.mc.theWorld.getBiomeGenForCoords(new BlockPos(bX, 0, bZ)).biomeName));
			}
		}

		IMwDataProvider provider = MwAPI.getCurrentDataProvider();
		if (provider != null)
		{
			builder.append(provider.getStatusString(this.mapView.getDimension(), bX, bY, bZ));
		}
		String s = builder.toString();
		int x = this.width / 2 - 10 - (this.fontRendererObj.getStringWidth(s)/ 2);
		
		statusLabel.draw(new String[]{builder.toString()}, null, x, this.height - 21, true, false);
	}

	// also called every frame
	public void drawScreen(int mouseX, int mouseY, float f) {

		this.drawDefaultBackground();
		double xOffset = 0.0;
		double yOffset = 0.0;
		// double zoomFactor = 1.0;

		if (this.mouseLeftHeld > 2) {
			xOffset = (this.mouseLeftDragStartX - mouseX)
					* this.mapView.getWidth() / this.mapMode.w;
			yOffset = (this.mouseLeftDragStartY - mouseY)
					* this.mapView.getHeight() / this.mapMode.h;

			if (this.movingMarker != null) {
				double scale = this.mapView
						.getDimensionScaling(this.movingMarker.dimension);
				this.movingMarker.x = this.movingMarkerXStart
						- (int) (xOffset / scale);
				this.movingMarker.z = this.movingMarkerZStart
						- (int) (yOffset / scale);
			} else {
				this.mapView.setViewCentre(this.viewXStart + xOffset,
						this.viewZStart + yOffset);
			}
		}

		if (this.mouseLeftHeld > 0) {
			this.mouseLeftHeld++;
		}

		// draw the map
		this.map.draw();

		// let the renderEngine know we have changed the texture.
		// this.mc.renderEngine.resetBoundTexture();

		// get the block the mouse is currently hovering over
		Point p = this.mapMode.screenXYtoBlockXZ(this.mapView, mouseX, mouseY);
		this.mouseBlockX = p.x;
		this.mouseBlockZ = p.y;
		this.mouseBlockY = this.getHeightAtBlockPos(this.mouseBlockX,
				this.mouseBlockZ);

		// draw name of marker under mouse cursor
		Marker marker = this.getMarkerNearScreenPos(mouseX, mouseY);
		if (marker != null) {
			markerLabel.draw(new String[]{
					marker.name, 
					String.format("(%d, %d, %d)",
							marker.x, 
							marker.y, 
							marker.z)},
							null,
							mouseX + 5, 
							mouseY,
							true,
							true
							);
		}
		
		// draw name of selected marker
		if (this.mw.markerManager.selectedMarker != null) {
			markerLabel.draw(new String[]{
					this.mw.markerManager.selectedMarker.name, 
					String.format("(%d, %d, %d)",
							this.mw.markerManager.selectedMarker.x, 
							this.mw.markerManager.selectedMarker.y, 
							this.mw.markerManager.selectedMarker.z)},
							null,
							(int)this.mw.markerManager.selectedMarker.screenPos.x + 5, 
							(int)this.mw.markerManager.selectedMarker.screenPos.y, 
							true,
							true
							);
		}

		// draw name of player under mouse cursor
		if (this.isPlayerNearScreenPos(mouseX, mouseY)) {
			markerLabel.draw(new String[]{
					this.mc.thePlayer.getDisplayNameString(), 
					String.format("(%d, %d, %d)", 
					this.mw.playerXInt, 
					this.mw.playerYInt, 
					this.mw.playerZInt)},
					null,
					mouseX + 5, 
					mouseY, 
					true,
					true
					);
		}

		// draw status message
		this.drawStatus(this.mouseBlockX, this.mouseBlockY, this.mouseBlockZ);

		// draw labels
		this.helpLabel.draw(new String[]{"[" + I18n.format("mw.gui.mwgui.help", new Object[0]) + "]"}, null, menuX, menuY, true, false);
		this.optionsLabel.drawToRightOf(this.helpLabel, new String[]{"[" + I18n.format("mw.gui.mwgui.options", new Object[0]) + "]"}, true, false);
		String dimString = String.format("[" + I18n.format("mw.gui.mwgui.dimension", new Object[0]) + ": %d]",
				this.mapView.getDimension());
		this.dimensionLabel.drawToRightOf(this.optionsLabel, new String[]{dimString}, true, false);
		String groupString = String.format("[" + I18n.format("mw.gui.mwgui.group", new Object[0]) + ": %s]",
				this.mw.markerManager.getVisibleGroupName());
		this.groupLabel.drawToRightOf(this.dimensionLabel, new String[]{groupString}, true, false);
		String overlayString = String.format("[" + I18n.format("mw.gui.mwgui.overlay", new Object[0]) + ": %s]",
				MwAPI.getCurrentProviderName());
		this.overlayLabel.drawToRightOf(this.groupLabel, new String[]{overlayString}, true, false);

		if (!VersionCheck.isLatestVersion()) {
			String updateString = String.format("[" + I18n.format("mw.gui.mwgui.newversion", new Object[0]) + ": %s]",
					VersionCheck.getLatestVersion());
			this.updateLabel.drawToRightOf(this.overlayLabel, new String[]{updateString}, true, false);
		}

		// help message on mouse over
		if (this.helpLabel.posWithin(mouseX, mouseY)) {
			helpTooltipLabel.draw(HelpText1,HelpText2, 10 , 20, true, false);
		}
		if (this.updateLabel.posWithin(mouseX, mouseY)) {
			updateTooltipLabel.draw(new String[]{VersionCheck.getUpdateURL()},null, 10 , 20, true, false);
		}

		MarkerOverlay.draw();
		
		super.drawScreen(mouseX, mouseY, f);
	}

	public void confirmClicked(boolean result, int id) {
		if (id == 31102009) {
			if (result) {
				Utils.openWebLink(this.clickedLinkURI);
			}

			this.clickedLinkURI = null;
			this.mc.displayGuiScreen(this);
		}
	}
}
