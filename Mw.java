package mapwriter;

import java.io.File;
import java.util.ArrayList;

import mapwriter.forge.MwConfig;
import mapwriter.forge.MwForge;
import mapwriter.forge.MwKeyHandler;
import mapwriter.map.MapTexture;
import mapwriter.map.MapView;
import mapwriter.map.Marker;
import mapwriter.map.MarkerManager;
import mapwriter.map.OverlayManager;
import mapwriter.map.Trail;
import mapwriter.region.BlockColours;
import mapwriter.region.RegionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.packet.Packet1Login;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.src.ModLoader;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/*

data transfers
---------------
chunk image (16x16 int[]) -> texture (512x512 GL texture)	| every chunk update
region file png -> texture (512x512 GL texture)				| on region load (slow, disk access)
texture (512x512 GL texture) -> region file png				| on region unload (slow, disk access)
chunk (Chunk object) -> anvil save file						| on chunk unload, separate thread handled by minecraft

background thread
------------------
performs all data transfers except Chunk->Anvil, which is handled by ThreadedFileIOBase in minecraft.
regions created in main thread when necessary, but filled from the background thread.

initialization
--------------
init()
  Called once only.
	- registers event and key handlers
	- loads configuration
	- inits marker manager
	- inits commands
	- inits chunkQueue

onClientLoggedIn()
  Called upon entry to each world.
	- inits executor
	- inits overlay
	- inits anvil save handler
	- inits regionMap

onConnectionClosed()
  Called on every exit from world.
	- closes chunkLoader
	- closes regionMap
	- closes overlay
	- saves markermanager
	- saves config
	- closes executor
	- flush chunkQueue

Every hook and event handler should be enclosed in an 'if (this.ready)' statement to ensure that all
components are initialised.
One exception is the fillChunk handler which adds chunks to the chunkQueue so that they can be processed
after initialization. This is so that no chunks are skipped if the chunks are loaded before the player is
logged in.
	
*/


/* TODO list
 * 
 * - Button to cycle through group selection
 * - Add marker in game hot key and GUI
 * - Save map as 8192x8192 tiles
 * - Fix mergeToImage exception for w or h of 0
 * - Add option to reduce map loading distance
 * - Dimension field on markers
 * - Rei's format marker reading and writing
 * - Save single player chunks to separate directory
 * - Convert to use own anvil writer implementation
 * - Allow using default texture pack for map with custom texture pack in game
 * - Cave map
 * - Death markers
 */

public class Mw {
	
	public Minecraft mc = null;
	
	public boolean ready = false;
	public boolean multiplayer = false;
	public int tickCounter = 0;
	//private int chunkCount = 0;
	public String worldName = "default";
	private String serverName = "default";
	private int serverPort = 0;
	private final File configDir;
	private final File saveDir;
	public File worldDir = null;
	public File imageDir = null;
	//private boolean closing = false;
	public MwConfig config;
	public MwConfig worldConfig = null;
	public boolean linearTextureScalingEnabled = true;
	public boolean coordsEnabled = false;
	public boolean teleportEnabled = true;
	public int chunksPerTick = 3;
	public int mapUpdateInterval = 100;
	public int maxChunkDistance = 16;
	public String teleportCommand = "tp";
	public int defaultTeleportHeight = 80;
	public static int maxZoom = 5;
	public static int minZoom = -5;
	public boolean useSavedBlockColours = false;
	
	public String blockColourSaveFileName = "MapWriterBlockColours.txt";
	
	private int textureSize = 2048;
	public int configTextureSize = 2048;
	
	// list of available dimensions
	public ArrayList<Integer> dimensionList = new ArrayList<Integer>();
	
	// player position and heading
	public double playerX = 0.0;
	public double playerZ = 0.0;
	public double playerY = 0.0;
	public int playerXInt = 0;
	public int playerYInt = 0;
	public int playerZInt = 0;
	public double playerHeading = 0.0;
	public int playerDimension = 0;
	public double mapRotationDegrees = 0.0;
	
	// constants
	public final static double PAN_FACTOR = 0.3D;
	public final static String catWorld = "world";
	public final static String catMarkers = "markers";
	public final static String catOptions = "options";
	public final static String worldDirConfigName = "mapwriter.cfg";
	
	// instances of components
	public MapTexture mapTexture = null;
	public BackgroundExecutor executor = null;
	public OverlayManager overlayManager = null;
	public MarkerManager markerManager = null;
	public BlockColours blockColours = null;
	public RegionManager regionManager = null;
	public ChunkManager chunkManager = null;
	public Trail playerTrail = null;
	
	public Mw(MwConfig config) {
		// client only initialization
		this.mc = ModLoader.getMinecraftInstance();
		
		// load config
		this.config = config;
		
		// create base save directory
		this.saveDir = new File(Minecraft.getMinecraft().mcDataDir, "saves");
		this.configDir = new File(Minecraft.getMinecraft().mcDataDir, "config");
		
		this.ready = false;
		
		RegionManager.logger = MwForge.logger;
	}
	
	public String getWorldName() {
		String worldName;
		if (!this.multiplayer) {
			// cannot use this.mc.theWorld.getWorldInfo().getWorldName() as it
			// is set statically to "MpServer".
			IntegratedServer server = this.mc.getIntegratedServer();
			worldName = (server != null) ? server.getFolderName() : "sp_world";
		} else {
			// strip invalid characters from the server name so that it
			// can't be something malicious like '..\..\..\windows\'
			worldName = String.format("%s_%d", this.serverName, this.serverPort);
		}
		worldName = MwUtil.mungeString(worldName);
		// if something went wrong make sure the name is not blank
		// (causes crash on start up due to empty configuration section)
		if (worldName == "") {
			worldName = "default";
		}
		return worldName;
	}
	
	public void loadConfig() {
		this.config.load();
		this.linearTextureScalingEnabled = this.config.getOrSetBoolean(catOptions, "linearTextureScaling", true);
		this.useSavedBlockColours = this.config.getOrSetBoolean(catOptions, "useSavedBlockColours", false);
		this.teleportEnabled = this.config.getOrSetBoolean(catOptions, "teleportEnabled", this.teleportEnabled);
		this.chunksPerTick = this.config.getOrSetInt(catOptions, "chunksPerTick", this.chunksPerTick, 1, 64);
		this.teleportCommand = this.config.get(catOptions, "teleportCommand", this.teleportCommand).getString();
		this.coordsEnabled = this.config.getOrSetBoolean(catOptions, "coordsEnabled", this.coordsEnabled);
		this.maxChunkDistance = this.config.getOrSetInt(catOptions, "maxChunkDistance", this.maxChunkDistance, 1, 16);
		
		maxZoom = this.config.getOrSetInt(catOptions, "zoomOutLevels", maxZoom, 1, 256);
		minZoom = -this.config.getOrSetInt(catOptions, "zoomInLevels", -minZoom, 1, 256);
		
		this.configTextureSize = this.config.getOrSetInt(catOptions, "textureSize", this.configTextureSize, 1024, 8192);
		this.setTextureSize();
		
		// load markers from config
		File worldConfigFile = new File(this.worldDir, worldDirConfigName);
		this.worldConfig = new MwConfig(worldConfigFile);
		this.worldConfig.load();
		
		this.dimensionList.clear();
		this.worldConfig.getIntList(catWorld, "dimensionList", this.dimensionList);
		this.addDimension(0);
		this.cleanDimensionList();
	}
	
	public void saveConfig() {
		this.worldConfig.setIntList(catWorld, "dimensionList", this.dimensionList);
		this.config.setBoolean(catOptions, "linearTextureScaling", this.linearTextureScalingEnabled);
		this.config.setBoolean(catOptions, "useSavedBlockColours", this.useSavedBlockColours);
		this.config.setInt(catOptions, "textureSize", this.configTextureSize);
		this.config.setBoolean(catOptions, "coordsEnabled", this.coordsEnabled);
		this.config.setInt(catOptions, "maxChunkDistance", this.maxChunkDistance);
		
		// save config
		this.config.save();
		this.worldConfig.save();
	}
	
	public void setTextureSize() {
		if (this.configTextureSize != this.textureSize) {
			int maxTextureSize = Render.getMaxTextureSize();
			int textureSize = 1024;
			while ((textureSize <= maxTextureSize) && (textureSize <= this.configTextureSize)) {
				textureSize *= 2;
			}
			textureSize /= 2;
			
			MwUtil.log("GL reported max texture size = %d", maxTextureSize);
			MwUtil.log("texture size from config = %d", this.configTextureSize);
			MwUtil.log("setting map texture size to = %d", textureSize);
			
			this.textureSize = textureSize;
			if (this.ready) {
				// if we are already up and running need to close and reinitialize the map texture and
				// region manager.
				this.reloadMapTexture();
			}
		}
	}
	
	// update the saved player position and orientation
	// called every tick
	public void updatePlayer() {
		// get player pos
		this.playerX = (double) this.mc.thePlayer.posX;
		this.playerY = (double) this.mc.thePlayer.posY;
		this.playerZ = (double) this.mc.thePlayer.posZ;
		this.playerXInt = (int) Math.floor(this.playerX);
		this.playerYInt = (int) Math.floor(this.playerY);
		this.playerZInt = (int) Math.floor(this.playerZ);
		
		// rotationYaw of 0 points due north, we want it to point due east instead
		// so add pi/2 radians (90 degrees)
		this.playerHeading = Math.toRadians(this.mc.thePlayer.rotationYaw) + (Math.PI / 2.0D);
		this.mapRotationDegrees = -this.mc.thePlayer.rotationYaw + 180;
		
		// set by onWorldLoad
		//this.playerDimension = this.mc.theWorld.provider.dimensionId;
	}
	
	public void addDimension(int dimension) {
		int i = this.dimensionList.indexOf(dimension);
		if (i < 0) {
			this.dimensionList.add(dimension);
		}
	}
	
	public void cleanDimensionList() {
		ArrayList<Integer> dimensionListCopy = new ArrayList<Integer>(this.dimensionList);
		this.dimensionList.clear();
		for (int dimension : dimensionListCopy) {
			this.addDimension(dimension);
		}
	}
	
	public void toggleMarkerMode() {
		this.markerManager.nextGroup();
		this.markerManager.update();
		this.mc.thePlayer.addChatMessage("group " + this.markerManager.getVisibleGroupName() + " selected");
	}
	
	// cheap and lazy way to teleport...
	public void teleportTo(int x, int y, int z) {
		if (this.teleportEnabled) {
			this.mc.thePlayer.sendChatMessage(String.format("/%s %d %d %d", this.teleportCommand, x, y, z));
		} else {
			MwUtil.printBoth("teleportation is disabled in mapwriter.cfg");
		}
	}
	
	public void teleportToMapPos(MapView mapView, int x, int y, int z) {
		double scale = mapView.getDimensionScaling(this.playerDimension);
		this.teleportTo((int) (x / scale), y, (int) (z / scale));
	}
	
	public void teleportToMarker(Marker marker) {
		if (marker.dimension == this.playerDimension) {
			this.teleportTo(marker.x, marker.y, marker.z);
		} else {
			MwUtil.printBoth("cannot teleport to marker in different dimension");
		}
	}
	
	public void reloadBlockColours() {
		BlockColours bc;
		File f = new File(this.configDir, this.blockColourSaveFileName);
		if (this.useSavedBlockColours && f.isFile()) {
			// load block colours from file
			MwUtil.logInfo("loading block colours from %s", f);
			bc = BlockColours.loadFromFile(f);
		} else {
			// generate block colours from current texture pack
			MwUtil.logInfo("generating block colours");
			bc = BlockColourGen.genBlockColours(this, this.config);
		}
		this.blockColours = bc;
	}
	
	public void saveCurrentBlockColours() {
		File f = new File(this.configDir, this.blockColourSaveFileName);
		MwUtil.logInfo("saving block colours to '%s'", f);
		this.blockColours.saveToFile(f);
	}
	
	public void reloadMapTexture() {
		this.executor.addTask(new CloseRegionManagerTask(this.regionManager));
		this.executor.close();
		MapTexture oldMapTexture = this.mapTexture;
		this.mapTexture = new MapTexture(this.textureSize, this.linearTextureScalingEnabled);
		if (oldMapTexture != null) {
			oldMapTexture.close();
		}
		this.executor = new BackgroundExecutor();
		this.regionManager = new RegionManager(this.worldDir, this.imageDir, this.blockColours);
	}
	
	public void setCoords(boolean enabled) {
		this.coordsEnabled = enabled;
	}
	
	public boolean toggleCoords() {
		this.setCoords(!this.coordsEnabled);
		return this.coordsEnabled;
	}
	
	////////////////////////////////
	// Event handling methods
	////////////////////////////////
	
	// single player connection opened event
	public void onConnectionOpened() {
		MwUtil.log("connection opened to integrated server");
		this.multiplayer = false;
	}
	
	// multi player connection opened event
	public void onConnectionOpened(String server, int port) {
		MwUtil.log("connection opened to remote server: %s %d", server, port);
		
		// set worldname to server_hostname.server_port
		this.serverName = server;
		this.serverPort = port;
		
		this.multiplayer = true;
	}
	
	public void onClientLoggedIn(Packet1Login login) {
		MwUtil.log("onClientLoggedIn: dimension = %d", login.dimension);
		
		this.worldName = this.getWorldName();
		
		// create directories
		if (this.multiplayer) {
			this.worldDir = new File(new File(this.saveDir, "mapwriter_mp_worlds"), this.worldName);
		} else {
			this.worldDir = new File(new File(this.saveDir, "mapwriter_sp_worlds"), this.worldName);
		}
		
		this.imageDir = new File(this.worldDir, "images");
		if (!this.imageDir.exists()) {
			this.imageDir.mkdirs();
		}
		if (!this.imageDir.isDirectory()) {
			MwUtil.log("Mapwriter: ERROR: could not create images directory '%s'", this.imageDir.getPath());
		}
		
		// create directories for zoom levels 1..n
		//boolean zoomLevelsExist = true;
		for (int i = 1; i <= maxZoom; i++) {
			File zDir = new File(imageDir, "z" + i);
			//zoomLevelsExist &= zDir.exists();
			zDir.mkdirs();
		}
		
		this.tickCounter = 0;
		
		this.loadConfig();
		//this.multiplayer = !this.mc.isIntegratedServerRunning();
		
		// marker manager only depends on the config being loaded
		this.markerManager = new MarkerManager();
		this.markerManager.load(this.worldConfig, catMarkers);
		
		this.playerTrail = new Trail(this, "player");
		
		// executor does not depend on anything
		this.executor = new BackgroundExecutor();
		
		// mapTexture depends on config being loaded
		this.mapTexture = new MapTexture(this.textureSize, this.linearTextureScalingEnabled);
		this.reloadBlockColours();
		// region manager depends on config, mapTexture, and block colours
		this.regionManager = new RegionManager(this.worldDir, this.imageDir, this.blockColours);
		// overlay manager depends on mapTexture
		this.overlayManager = new OverlayManager(this);
		this.overlayManager.overlayView.setDimension(login.dimension);
		
		this.chunkManager = new ChunkManager(this);
		
		this.ready = true;
		
		//if (!zoomLevelsExist) {
			//printBoth("recreating zoom levels");
			//this.regionManager.recreateAllZoomLevels();
		//}
	}
	
	public void onWorldLoad(World world) {
		//MwUtil.log("onWorldLoad: %s, name %s, dimension %d",
		//		world,
		//		world.getWorldInfo().getWorldName(),
		//		world.provider.dimensionId);
		
		this.playerDimension = world.provider.dimensionId;
		if (this.ready) {
			this.addDimension(this.playerDimension);
			this.overlayManager.overlayView.setDimension(this.playerDimension);
		}
	}
	
	public void onWorldUnload(World world) {
		//MwUtil.log("onWorldUnload: %s, name %s, dimension %d",
		//		world,
		//		world.getWorldInfo().getWorldName(),
		//		world.provider.dimensionId);
	}
	
	public void onConnectionClosed() {
		
		MwUtil.log("connection closed");
		
		if (this.ready) {
			this.ready = false;
			
			this.chunkManager.close();
			this.chunkManager = null;
			
			// close all loaded regions, saving modified images.
			// this will create extra tasks that need to be completed.
			this.executor.addTask(new CloseRegionManagerTask(this.regionManager));
			this.regionManager = null;
			
			MwUtil.log("waiting for %d tasks to finish...", this.executor.tasksRemaining());
			if (this.executor.close()) {
				MwUtil.log("error: timeout waiting for tasks to finish");
			}
			MwUtil.log("done");
			
			this.playerTrail.close();
			
			this.markerManager.save(this.worldConfig, catMarkers);
			this.markerManager.clear();
			
			// close overlay
			this.overlayManager.close();
			this.overlayManager = null;
			
			this.mapTexture.close();
			
			this.saveConfig();
			
			this.tickCounter = 0;
		}
	}
	
	public void onTick() {
		if (this.ready && (this.mc.thePlayer != null)) {
			
			this.updatePlayer();
			if (this.mc.currentScreen == null) {
				this.overlayManager.overlayView.setViewCentreScaled(this.playerX, this.playerZ, this.playerDimension);
				this.overlayManager.drawCurrentMap();
			}
			
			// process background tasks
			int maxTasks = 50;
			while (!this.executor.processTaskQueue() && (maxTasks > 0)) {
				maxTasks--;
			}
			
			this.chunkManager.onTick();
			
			// let the renderEngine know we have changed the bound texture.
	    	//this.mc.renderEngine.resetBoundTexture();
			
	    	//if (this.tickCounter % 100 == 0) {
	    	//	MwUtil.log("tick %d", this.tickCounter);
	    	//}
	    	this.playerTrail.onTick();
	    	
			this.tickCounter++;
		}
	}
	
	// add chunk to the set of loaded chunks
	public void onChunkLoad(Chunk chunk) {
		if (this.ready && (chunk != null) && (chunk.worldObj instanceof net.minecraft.client.multiplayer.WorldClient)) {
			this.chunkManager.addChunk(chunk);
		}
	}
	
	// remove chunk from the set of loaded chunks.
	// convert to mwchunk and write chunk to region file if in multiplayer.
	public void onChunkUnload(Chunk chunk) {
		if (this.ready && (chunk != null) && (chunk.worldObj instanceof net.minecraft.client.multiplayer.WorldClient)) {
			this.chunkManager.removeChunk(chunk);
		}
	}
	
	// not currently called by anything.
	// need to find a entity death event that works client side.
	/*public void onPlayerDeath() {
		if (this.ready) {
			this.updatePlayer();
			boolean error = false;
			while (!error && (this.markerManager.countMarkersInGroup("playerDeaths") > 2)) {
				error = this.markerManager.delMarker("diedHere", "playerDeaths");
			}
			this.markerManager.addMarker("diedHere", "playerDeaths", this.playerXInt, this.playerYInt, this.playerZInt, 0xffff0000);
			this.markerManager.setVisibleGroupName("playerDeaths");
			this.markerManager.update();
		}
	}*/
	
	public void onKeyDown(KeyBinding kb) {
		// make sure not in GUI element (e.g. chat box)
		if ((this.mc.currentScreen == null) && (this.ready)) {
			//Mw.log("client tick: %s key pressed", kb.keyDescription);
			
			if (kb == MwKeyHandler.keyMapMode) {
				// map mode toggle
				this.overlayManager.nextOverlayMode(1);
				
			} else if (kb == MwKeyHandler.keyMapGui) {
				// open map gui
				this.mc.displayGuiScreen(new MwGui(this));
			
			} else if (kb == MwKeyHandler.keyNewMarker) {
				// open new marker dialog
				String group = this.markerManager.getVisibleGroupName();
        		if (group.equals("none")) {
        			group = "group";
        		}
				this.mc.displayGuiScreen(
					new MwGuiMarkerDialog(
						null,
						this.markerManager,
						"",
						group,
						this.playerXInt,
						this.playerYInt,
						this.playerZInt,
						this.playerDimension
					)
				);
			
			} else if (kb == MwKeyHandler.keyNextGroup) {
				// toggle marker mode
				this.markerManager.nextGroup();
				this.markerManager.update();
				this.mc.thePlayer.addChatMessage("group " + this.markerManager.getVisibleGroupName() + " selected");
				
			} else if (kb == MwKeyHandler.keyTeleport) {
				// set or remove marker
				Marker marker = this.markerManager.getNearestMarkerInDirection(
						this.playerXInt,
						this.playerZInt,
						this.playerHeading);
				if (marker != null) {
					this.teleportToMarker(marker);						
				}
			} else if (kb == MwKeyHandler.keyZoomIn) {
				// zoom in
				this.overlayManager.overlayView.adjustZoomLevel(-1);
			} else if (kb == MwKeyHandler.keyZoomOut) {
				// zoom out
				this.overlayManager.overlayView.adjustZoomLevel(1);
			}
		}
	}
}
