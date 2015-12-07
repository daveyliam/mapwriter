package mapwriter.gui;

import java.awt.Point;

import mapwriter.Mw;
import mapwriter.MwUtil;
import mapwriter.api.IMwDataProvider;
import mapwriter.api.MwAPI;
import mapwriter.forge.MwKeyHandler;
import mapwriter.map.MapRenderer;
import mapwriter.map.MapView;
import mapwriter.map.Marker;
import mapwriter.map.mapmode.FullScreenMapMode;
import mapwriter.map.mapmode.MapMode;
import mapwriter.tasks.MergeTask;
import mapwriter.tasks.RebuildRegionsTask;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MwGui extends GuiScreen {
	private Mw mw;
    private MapMode mapMode;
    private MapView mapView;
    private MapRenderer map;
    
	private final static double PAN_FACTOR = 0.3D;
    
    private static final int menuY = 5;
    private static final int menuX = 5;
    
    private int mouseLeftHeld = 0;
    //private int mouseRightHeld = 0;
    //private int mouseMiddleHeld = 0;
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

    private int exit = 0;
    
    private Label helpLabel;
    private Label optionsLabel;
    private Label dimensionLabel;
    private Label groupLabel;
    private Label overlayLabel;
    
    class Label {
    	int x = 0, y = 0, w = 1, h = 12;
    	public Label() {
    	}
    	
    	public void draw(int x, int y, String s) {
    		this.x = x;
    		this.y = y;
    		this.w = MwGui.this.fontRendererObj.getStringWidth(s) + 4;
    		MwGui.drawRect(this.x, this.y, this.x + this.w, this.y + this.h, 0x80000000);
    		MwGui.this.drawString(MwGui.this.fontRendererObj, s, this.x + 2, this.y + 2, 0xffffff);
    	}
    	
    	public void drawToRightOf(Label label, String s) {
    		this.draw(label.x + label.w + 5, label.y, s);
    	}
    	
    	public boolean posWithin(int x, int y) {
    		return (x >= this.x) && (y >= this.y) && (x <= (this.x + this.w)) && (y <= (this.y + this.h));
    	}
    }
    
    public MwGui(Mw mw) {
    	this.mw = mw;
    	this.mapMode = new FullScreenMapMode(mw.config);
    	this.mapView = new MapView(this.mw);
    	this.map = new MapRenderer(this.mw, this.mapMode, this.mapView);
    	
    	this.mapView.setDimension(this.mw.miniMap.view.getDimension());
    	this.mapView.setViewCentreScaled(this.mw.playerX, this.mw.playerZ, this.mw.playerDimension);
    	this.mapView.setZoomLevel(0);
    	
    	this.helpLabel = new Label();
    	this.optionsLabel = new Label();
    	this.dimensionLabel = new Label();
    	this.groupLabel = new Label();
    	this.overlayLabel = new Label();
    }

    public MwGui(Mw mw, int dim, int x, int z){
    	this(mw);
    	this.mapView.setDimension(dim);
    	this.mapView.setViewCentreScaled(x, z, dim);
    	this.mapView.setZoomLevel(0);    	
    }
    
    // called when gui is displayed and every time the screen
    // is resized
    public void initGui() {
    }

    // called when a button is pressed
    protected void actionPerformed(GuiButton button) {
    	
    }
    
    public void exitGui() {
    	//MwUtil.log("closing GUI");
    	// set the mini map dimension to the GUI map dimension when closing
    	this.mw.miniMap.view.setDimension(this.mapView.getDimension());
    	this.mapMode.close();
    	Keyboard.enableRepeatEvents(false);
    	this.mc.displayGuiScreen((GuiScreen) null);
        this.mc.setIngameFocus();
        this.mc.getSoundHandler().resumeSounds();
    }
    
    // get a marker near the specified block pos if it exists.
    // the maxDistance is based on the view width so that you need to click closer
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
    	int worldDimension = this.mw.mc.theWorld.provider.dimensionId;
    	if ((worldDimension == this.mapView.getDimension()) && (worldDimension != -1)) {
    		bY = this.mw.mc.theWorld.getChunkFromBlockCoords(bX, bZ).getHeightValue(bX & 0xf, bZ & 0xf);
    	}
    	return bY;
    }
    
    public boolean isPlayerNearScreenPos(int x, int y) {
    	Point.Double p = this.map.playerArrowScreenPos;
        return p.distanceSq(x, y) < 9.0;
    }
    
    public void deleteSelectedMarker() {
    	if (this.mw.markerManager.selectedMarker != null) {
    		//MwUtil.log("deleting marker %s", this.mw.markerManager.selectedMarker.name);
    		this.mw.markerManager.delMarker(this.mw.markerManager.selectedMarker);
    		this.mw.markerManager.update();
    		this.mw.markerManager.selectedMarker = null;
    	}
    }
    
    public void mergeMapViewToImage() {
			this.mw.chunkManager.saveChunks();
			this.mw.executor.addTask(new MergeTask(this.mw.regionManager,
					(int) this.mapView.getX(),
					(int) this.mapView.getZ(),
					(int) this.mapView.getWidth(),
					(int) this.mapView.getHeight(),
					this.mapView.getDimension(),
					this.mw.worldDir,
					this.mw.worldDir.getName()));
			
			MwUtil.printBoth(I18n.format("mw.gui.mwgui.chatmsg.merge") + " '" + this.mw.worldDir.getAbsolutePath() + "'");
    }
    
    public void regenerateView() {
    	MwUtil.printBoth(String.format(
				I18n.format("mw.gui.mwgui.chatmsg.regenmap.1") + " %dx%d " + I18n.format("mw.gui.mwgui.chatmsg.regenmap.2") + " (%d, %d)",
				(int) this.mapView.getWidth(),
				(int) this.mapView.getHeight(),
				(int) this.mapView.getMinX(),
				(int) this.mapView.getMinZ()));
		this.mw.reloadBlockColours();
		this.mw.executor.addTask(new RebuildRegionsTask(
				this.mw,
				(int) this.mapView.getMinX(),
				(int) this.mapView.getMinZ(),
				(int) this.mapView.getWidth(),
				(int) this.mapView.getHeight(),
				this.mapView.getDimension()));
    }
    
    // c is the ascii equivalent of the key typed.
    // key is the lwjgl key code.
    protected void keyTyped(char c, int key) {
    	//MwUtil.log("MwGui.keyTyped(%c, %d)", c, key);
		switch(key) {
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
        	this.mapView.setViewCentreScaled(this.mw.playerX, this.mw.playerZ, this.mw.playerDimension);
        	break;
        
		case Keyboard.KEY_END:
        	// centre map on selected marker
        	if (this.mw.markerManager.selectedMarker != null) {
        		this.mapView.setViewCentreScaled(
        			this.mw.markerManager.selectedMarker.x,
        			this.mw.markerManager.selectedMarker.z,
        			0
        		);
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
        		this.mc.displayGuiScreen(
        			new MwGuiTeleportDialog(
        				this,
        				this.mw,
        				this.mapView,
        				this.mouseBlockX,
        				this.mw.defaultTeleportHeight,
        				this.mouseBlockZ
        			)
        		);
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
			
		//case Keyboard.KEY_9:
		//	MwUtil.log("refreshing maptexture");
		//	this.mw.mapTexture.updateTexture();
		//	break;
		
		default:
			if (key == MwKeyHandler.keyMapGui.getKeyCode()) {
				// exit on the next tick
    			this.exit = 1;
    		} else if (key == MwKeyHandler.keyZoomIn.getKeyCode()) {
    			this.mapView.adjustZoomLevel(-1);
    		} else if (key == MwKeyHandler.keyZoomOut.getKeyCode()) {
    			this.mapView.adjustZoomLevel(1);
    		} else if (key == MwKeyHandler.keyNextGroup.getKeyCode()) {
    			this.mw.markerManager.nextGroup();
	        	this.mw.markerManager.update();
    		} else if (key == MwKeyHandler.keyUndergroundMode.getKeyCode()) {
    			this.mw.toggleUndergroundMode();
    			this.mapView.setUndergroundMode(this.mw.undergroundMode);
			}
			break;
        }
    }
    
    // override GuiScreen's handleMouseInput to process
    // the scroll wheel.
    @Override
    public void handleMouseInput() {
    	if (MwAPI.getCurrentDataProvider() != null && MwAPI.getCurrentDataProvider().onMouseInput(this.mapView, this.mapMode))
    		return;
    	
    	int x = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
    	int direction = Mouse.getEventDWheel();
    	if (direction != 0) {
    		this.mouseDWheelScrolled(x, y, direction);
    	}
    	super.handleMouseInput();
    }
    
    // mouse button clicked. 0 = LMB, 1 = RMB, 2 = MMB
    protected void mouseClicked(int x, int y, int button) {
    	//MwUtil.log("MwGui.mouseClicked(%d, %d, %d)", x, y, button);
    	
    	//int bX = this.mouseToBlockX(x);
		//int bZ = this.mouseToBlockZ(y);
		//int bY = this.getHeightAtBlockPos(bX, bZ);
    	
    	Marker marker = this.getMarkerNearScreenPos(x, y);
    	Marker prevMarker = this.mw.markerManager.selectedMarker;
    	
    	if (button == 0) {
    		if (this.dimensionLabel.posWithin(x, y)) {
    			this.mc.displayGuiScreen(
        			new MwGuiDimensionDialog(
        				this,
        				this.mw,
        				this.mapView,
        				this.mapView.getDimension()
        			)
        		);
    		} else if (this.optionsLabel.posWithin(x, y)) {
    			this.mc.displayGuiScreen(new MwGuiOptions(this, this.mw));
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
    		//this.mouseRightHeld = 1;
			if ((marker != null) && (prevMarker == marker)) {
    			// right clicked previously selected marker.
    			// edit the marker
				this.mc.displayGuiScreen(
        			new MwGuiMarkerDialog(
        				this,
        				this.mw.markerManager,
        				marker
        			)
        		);
    			
    		} else if (marker == null) {
    			// open new marker dialog
    			String group = this.mw.markerManager.getVisibleGroupName();
        		if (group.equals("none")) {
        			group = I18n.format("mw.gui.mwgui.group");
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
        			my = (this.mouseBlockY > 0) ? this.mouseBlockY : this.mw.defaultTeleportHeight;
        			mz = this.mouseBlockZ;
        		}
        		
        		if (mw.newMarkerDialog)
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
            		this.mc.displayGuiScreen(
                			new MwGuiMarkerDialog(
                				this,
                				this.mw.markerManager,
                				"",
                				group,
                				mx, my, mz,
                				this.mapView.getDimension()
                			)
                		);
        		}
        		

    		}
    	}

    	else if (button == 2) {
    		Point blockPoint = this.mapMode.screenXYtoBlockXZ(this.mapView, x, y);
	
    		IMwDataProvider provider = MwAPI.getCurrentDataProvider();
    			if (provider != null)    			
    				provider.onMiddleClick(this.mapView.getDimension(), blockPoint.x, blockPoint.y, this.mapView);
    	}
    	
    	this.viewXStart = this.mapView.getX();
		this.viewZStart = this.mapView.getZ();
		//this.viewSizeStart = this.mapManager.getViewSize();
    }

    // mouse button released. 0 = LMB, 1 = RMB, 2 = MMB
    // not called on mouse movement.
    protected void mouseReleased(int x, int y, int button) {
    	//MwUtil.log("MwGui.mouseMovedOrUp(%d, %d, %d)", x, y, button);
    	if (button == 0) {
    		this.mouseLeftHeld = 0;
    		this.movingMarker = null;
    	} else if (button == 1) {
    		//this.mouseRightHeld = 0;
    	}
    }
    
    // zoom on mouse direction wheel scroll
    public void mouseDWheelScrolled(int x, int y, int direction) {
    	Marker marker = this.getMarkerNearScreenPos(x, y);
    	if ((marker != null) && (marker == this.mw.markerManager.selectedMarker)) {
    		if (direction > 0) {
    			marker.colourNext();
    		} else {
    			marker.colourPrev();
    		}
    		
    	} else if (this.dimensionLabel.posWithin(x, y)) {
    		int n = (direction > 0) ? 1 : -1;
	    	this.mapView.nextDimension(this.mw.dimensionList, n);
	    	
    	} else if (this.groupLabel.posWithin(x, y)) {
    		int n = (direction > 0) ? 1 : -1;
    		this.mw.markerManager.nextGroup(n);
    		this.mw.markerManager.update();
    	} else if (this.overlayLabel.posWithin(x, y)) {
    		int n = (direction > 0) ? 1 : -1;
			if (MwAPI.getCurrentDataProvider() != null)
				MwAPI.getCurrentDataProvider().onOverlayDeactivated(this.mapView);    		
    		
    		if (n == 1)
    			MwAPI.setNextProvider();
    		else
    			MwAPI.setPrevProvider();

			if (MwAPI.getCurrentDataProvider() != null)
				MwAPI.getCurrentDataProvider().onOverlayActivated(this.mapView);    		
    		
    	} else {
    		int zF = (direction > 0) ? -1 : 1;
    		this.mapView.zoomToPoint(this.mapView.getZoomLevel() + zF, this.mouseBlockX, this.mouseBlockZ);
    	}
    }

    // called every frame
    public void updateScreen() {
    	//MwUtil.log("MwGui.updateScreen() " + Thread.currentThread().getName());
    	// need to wait one tick before exiting so that the game doesn't
    	// handle the 'm' key and re-open the gui.
    	// there should be a better way.
    	if (this.exit > 0) {
    		this.exit++;
    	}
    	if (this.exit > 2) {
    		this.exitGui();
    	}
        super.updateScreen();
    }
    
    public void drawStatus(int bX, int bY, int bZ) {
    	 String s;
    	 if (bY != 0) {
          	s = I18n.format("mw.gui.mwgui.status.cursor", bX, bY, bZ);
          } else {
          	s = I18n.format("mw.gui.mwgui.status.cursorNoY", bX, bZ);
          }
    	 if (this.mc.theWorld != null) {
    		 if (!this.mc.theWorld.getChunkFromBlockCoords(bX, bZ).isEmpty()) {
    			 s += String.format(", " + I18n.format("mw.gui.mwgui.status.biome", this.mc.theWorld.getBiomeGenForCoords(bX, bZ).biomeName));
    		 }
    	 }
         
         /*if (this.mw.markerManager.selectedMarker != null) {
         	s += ", current marker: " + this.mw.markerManager.selectedMarker.name;
         }*/
    	 
    	 IMwDataProvider provider = MwAPI.getCurrentDataProvider();
 			if (provider != null)    	 
 				s += provider.getStatusString(this.mapView.getDimension(), bX, bY, bZ);
    	 
         drawRect(10, this.height - 21, this.width - 20, this.height - 6, 0x80000000);
         this.drawCenteredString(this.fontRendererObj,
         		s, this.width / 2, this.height - 18, 0xffffff);
    }
    
    public void drawHelp() {
    	drawRect(10, 20, this.width - 20, this.height - 30, 0x80000000);
    	this.fontRendererObj.drawSplitString(
    			I18n.format("mw.gui.mwgui.keys") + ":\n\n" + 
    			"  Space\n" +
    			"  Delete\n" +
    			"  C\n" +
    			"  Home\n" +
    			"  End\n" +
    			"  N\n" +
    			"  T\n" +
    			"  P\n" +
    			"  R\n" +
    			"  U\n\n" +
    			I18n.format("mw.gui.mwgui.helptext.1") + "\n" +
    			I18n.format("mw.gui.mwgui.helptext.2") + "\n" +
    			I18n.format("mw.gui.mwgui.helptext.3") + "\n" +
    			I18n.format("mw.gui.mwgui.helptext.4") + "\n" +
    			I18n.format("mw.gui.mwgui.helptext.5") + "\n" +
    			I18n.format("mw.gui.mwgui.helptext.6") + "\n",
    			15, 24, this.width - 30, 0xffffff);
    	this.fontRendererObj.drawSplitString(
    			"| " + I18n.format("mw.gui.mwgui.helptext.nextmarkergroup") + "\n" +
    			"| " + I18n.format("mw.gui.mwgui.helptext.deletemarker") + "\n" +
    			"| " + I18n.format("mw.gui.mwgui.helptext.cyclecolour") + "\n" +
    			"| " + I18n.format("mw.gui.mwgui.helptext.centermap") + "\n" +
    			"| " + I18n.format("mw.gui.mwgui.helptext.centermapplayer") + "\n" +
    			"| " + I18n.format("mw.gui.mwgui.helptext.selectnextmarker") + "\n" +
    			"| " + I18n.format("mw.gui.mwgui.helptext.teleport") + "\n" +
    			"| " + I18n.format("mw.gui.mwgui.helptext.savepng") + "\n" +
    			"| " + I18n.format("mw.gui.mwgui.helptext.regenerate") + "\n" +
    			"| " + I18n.format("mw.gui.mwgui.helptext.undergroundmap") + "\n",
    			75, 42, this.width - 90, 0xffffff);
    }
    
    public void drawMouseOverHint(int x, int y, String title, int mX, int mY, int mZ) {
    	String desc = String.format("(%d, %d, %d)", mX, mY, mZ);
    	int stringW = Math.max(
    			this.fontRendererObj.getStringWidth(title),
    			this.fontRendererObj.getStringWidth(desc));
    	
    	x = Math.min(x, this.width - (stringW + 16));
    	y = Math.min(Math.max(10, y), this.height - 14);
    	
    	drawRect(x + 8, y - 10, x + stringW + 16, y + 14, 0x80000000);
    	this.drawString(this.fontRendererObj,
    			title,
    			x + 10, y - 8, 0xffffff);
    	this.drawString(this.fontRendererObj,
    			desc,
    			x + 10, y + 4, 0xcccccc);
    }
    
    // also called every frame
    public void drawScreen(int mouseX, int mouseY, float f) {
    	
        this.drawDefaultBackground();
        double xOffset = 0.0;
        double yOffset = 0.0;
        //double zoomFactor = 1.0;

    	if (this.mouseLeftHeld > 2) {
    		xOffset = (this.mouseLeftDragStartX - mouseX) * this.mapView.getWidth() / this.mapMode.w;
    		yOffset = (this.mouseLeftDragStartY - mouseY) * this.mapView.getHeight() / this.mapMode.h;
    		
    		if (this.movingMarker != null) {
    			double scale = this.mapView.getDimensionScaling(this.movingMarker.dimension);
        		this.movingMarker.x = this.movingMarkerXStart - (int) (xOffset / scale);
        		this.movingMarker.z = this.movingMarkerZStart - (int) (yOffset / scale);
    		} else {
	    		this.mapView.setViewCentre(this.viewXStart + xOffset, this.viewZStart + yOffset);
    		}
    	}
    	
        if (this.mouseLeftHeld > 0) {
        	this.mouseLeftHeld++;
        }
        
        // draw the map
        this.map.draw();
        
        // let the renderEngine know we have changed the texture.
    	//this.mc.renderEngine.resetBoundTexture();
        
        // get the block the mouse is currently hovering over
    	Point p = this.mapMode.screenXYtoBlockXZ(this.mapView, mouseX, mouseY);
        this.mouseBlockX = p.x;
        this.mouseBlockZ = p.y;
        this.mouseBlockY = this.getHeightAtBlockPos(this.mouseBlockX, this.mouseBlockZ);
        
        // draw name of marker under mouse cursor
        Marker marker = this.getMarkerNearScreenPos(mouseX, mouseY);
        if (marker != null) {
        	this.drawMouseOverHint(mouseX, mouseY, marker.name, marker.x, marker.y, marker.z);
        }
        
        // draw name of player under mouse cursor
        if (this.isPlayerNearScreenPos(mouseX, mouseY)) {
        	this.drawMouseOverHint(mouseX, mouseY, this.mc.thePlayer.getDisplayName(),
        			this.mw.playerXInt,
					this.mw.playerYInt,
					this.mw.playerZInt);
        }
        
        // draw status message
       this.drawStatus(this.mouseBlockX, this.mouseBlockY, this.mouseBlockZ);
        
        // draw labels
       this.helpLabel.draw(menuX, menuY, "[" + I18n.format("mw.gui.mwgui.help") + "]");
       this.optionsLabel.drawToRightOf(this.helpLabel, "[" + I18n.format("mw.gui.mwgui.options") + "]");
       String dimString = String.format("[" + I18n.format("mw.gui.mwgui.dimension", this.mapView.getDimension()) + "]");
       this.dimensionLabel.drawToRightOf(this.optionsLabel, dimString);
       String groupString = String.format("[" + I18n.format("mw.gui.mwgui.group") + ": %s]", this.mw.markerManager.getVisibleGroupName());
       this.groupLabel.drawToRightOf(this.dimensionLabel, groupString);
       String overlayString = String.format("[" + I18n.format("mw.gui.mwgui.overlay", MwAPI.getCurrentProviderName()) + "]");
       this.overlayLabel.drawToRightOf(this.groupLabel, overlayString);
        
        // help message on mouse over
		if (this.helpLabel.posWithin(mouseX, mouseY)) {
		    this.drawHelp();
		}
        
        super.drawScreen(mouseX, mouseY, f);
    }
}

