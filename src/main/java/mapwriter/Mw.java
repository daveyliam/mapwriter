package mapwriter;

import java.io.File;

import mapwriter.config.Config;
import mapwriter.config.ConfigurationHandler;
import mapwriter.config.WorldConfig;
import mapwriter.forge.MwForge;
import mapwriter.forge.MwKeyHandler;
import mapwriter.gui.MwGui;
import mapwriter.gui.MwGuiMarkerDialog;
import mapwriter.gui.MwGuiMarkerDialogNew;
import mapwriter.map.MapTexture;
import mapwriter.map.MapView;
import mapwriter.map.Marker;
import mapwriter.map.MarkerManager;
import mapwriter.map.MiniMap;
import mapwriter.map.Trail;
import mapwriter.map.UndergroundTexture;
import mapwriter.overlay.OverlaySlime;
import mapwriter.region.BlockColours;
import mapwriter.region.RegionManager;
import mapwriter.tasks.CloseRegionManagerTask;
import mapwriter.util.Logging;
import mapwriter.util.Reference;
import mapwriter.util.Render;
import mapwriter.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.chunk.Chunk;

public class Mw
{
	public Minecraft mc = null;

	// directories
	private final File configDir;
	private final File saveDir;
	public File worldDir = null;
	public File imageDir = null;

	// flags and counters
	public boolean ready = false;
	// public boolean multiplayer = false;
	public int tickCounter = 0;

	public int textureSize = 2048;

	// player position and heading
	public double playerX = 0.0;
	public double playerZ = 0.0;
	public double playerY = 0.0;
	public int playerXInt = 0;
	public int playerYInt = 0;
	public int playerZInt = 0;
	public double playerHeading = 0.0;
	public int playerDimension = 0;
	public float mapRotationDegrees = 0.0f;

	// instances of components
	public MapTexture mapTexture = null;
	public UndergroundTexture undergroundMapTexture = null;
	public BackgroundExecutor executor = null;
	public MiniMap miniMap = null;
	public MarkerManager markerManager = null;
	public BlockColours blockColours = null;
	public RegionManager regionManager = null;
	public ChunkManager chunkManager = null;
	public Trail playerTrail = null;

	private static Mw instance;

	public static Mw getInstance()
	{
		if (instance == null)
		{
			synchronized (WorldConfig.class)
			{
				if (instance == null)
				{
					instance = new Mw();
				}
			}
		}

		return instance;
	}

	private Mw()
	{
		// client only initialization
		this.mc = Minecraft.getMinecraft();

		// create base save directory
		this.saveDir = new File(this.mc.mcDataDir, "saves");
		this.configDir = new File(this.mc.mcDataDir, "config");

		this.ready = false;

		RegionManager.logger = MwForge.logger;

		ConfigurationHandler.loadConfig();
	}

	public void setTextureSize()
	{
		if (Config.configTextureSize != this.textureSize)
		{
			int maxTextureSize = Render.getMaxTextureSize();
			int textureSize = 1024;
			while ((textureSize <= maxTextureSize) && (textureSize <= Config.configTextureSize))
			{
				textureSize *= 2;
			}
			textureSize /= 2;

			Logging.log("GL reported max texture size = %d", maxTextureSize);
			Logging.log("texture size from config = %d", Config.configTextureSize);
			Logging.log("setting map texture size to = %d", textureSize);

			this.textureSize = textureSize;
			if (this.ready)
			{
				// if we are already up and running need to close and
				// reinitialize the map texture and
				// region manager.
				this.reloadMapTexture();
			}
		}
	}

	// update the saved player position and orientation
	// called every tick
	public void updatePlayer()
	{
		// get player pos
		this.playerX = this.mc.thePlayer.posX;
		this.playerY = this.mc.thePlayer.posY;
		this.playerZ = this.mc.thePlayer.posZ;
		this.playerXInt = (int) Math.floor(this.playerX);
		this.playerYInt = (int) Math.floor(this.playerY);
		this.playerZInt = (int) Math.floor(this.playerZ);

		// rotationYaw of 0 points due north, we want it to point due east
		// instead
		// so add pi/2 radians (90 degrees)
		this.playerHeading = Math.toRadians(this.mc.thePlayer.rotationYaw) + (Math.PI / 2.0D);
		this.mapRotationDegrees = -this.mc.thePlayer.rotationYaw + 180;

		// set by onWorldLoad
		this.playerDimension = this.mc.theWorld.provider.getDimensionId();
		if (this.miniMap.view.getDimension() != this.playerDimension)
		{
			WorldConfig.getInstance().addDimension(this.playerDimension);
			this.miniMap.view.setDimension(this.playerDimension);
		}
	}

	public void toggleMarkerMode()
	{
		this.markerManager.nextGroup();
		this.markerManager.update();
		this.mc.thePlayer.addChatMessage(new ChatComponentText("group " + this.markerManager.getVisibleGroupName() + " selected"));
	}

	// cheap and lazy way to teleport...
	public void teleportTo(int x, int y, int z)
	{
		if (Config.teleportEnabled)
		{
			this.mc.thePlayer.sendChatMessage(String.format("/%s %d %d %d", Config.teleportCommand, x, y, z));
		}
		else
		{
			Utils.printBoth("teleportation is disabled in mapwriter.cfg");
		}
	}

	public void warpTo(String name)
	{
		if (Config.teleportEnabled)
		{
			// MwUtil.printBoth(String.format("warping to %s", name));
			this.mc.thePlayer.sendChatMessage(String.format("/warp %s", name));
		}
		else
		{
			Utils.printBoth("teleportation is disabled in mapwriter.cfg");
		}
	}

	public void teleportToMapPos(MapView mapView, int x, int y, int z)
	{
		if (!Config.teleportCommand.equals("warp"))
		{
			double scale = mapView.getDimensionScaling(this.playerDimension);
			this.teleportTo((int) (x / scale), y, (int) (z / scale));
		}
		else
		{
			Utils.printBoth("teleport command is set to 'warp', can only warp to markers");
		}
	}

	public void teleportToMarker(Marker marker)
	{
		if (Config.teleportCommand.equals("warp"))
		{
			this.warpTo(marker.name);
		}
		else if (marker.dimension == this.playerDimension)
		{
			this.teleportTo(marker.x, marker.y, marker.z);
		}
		else
		{
			Utils.printBoth("cannot teleport to marker in different dimension");
		}
	}

	public void loadBlockColourOverrides(BlockColours bc)
	{
		File f = new File(this.configDir, Reference.blockColourOverridesFileName);
		if (f.isFile())
		{
			Logging.logInfo("loading block colour overrides file %s", f);
			bc.loadFromFile(f);
		}
		else
		{
			Logging.logInfo("recreating block colour overrides file %s", f);
			BlockColours.writeOverridesFile(f);
			if (f.isFile())
			{
				bc.loadFromFile(f);
			}
			else
			{
				Logging.logError("could not load block colour overrides from file %s", f);
			}
		}
	}

	public void saveBlockColours(BlockColours bc)
	{
		File f = new File(this.configDir, Reference.blockColourSaveFileName);
		Logging.logInfo("saving block colours to '%s'", f);
		bc.saveToFile(f);
	}

	public void reloadBlockColours()
	{
		BlockColours bc = new BlockColours();
		File f = new File(this.configDir, Reference.blockColourSaveFileName);
		if (Config.useSavedBlockColours && f.isFile())
		{
			// load block colours from file
			Logging.logInfo("loading block colours from %s", f);
			bc.loadFromFile(f);
			this.loadBlockColourOverrides(bc);
		}
		else
		{
			// generate block colours from current texture pack
			Logging.logInfo("generating block colours");
			BlockColourGen.genBlockColours(bc);
			// load overrides again to override block and biome colours
			this.loadBlockColourOverrides(bc);
			this.saveBlockColours(bc);
		}
		this.blockColours = bc;
	}

	public void reloadMapTexture()
	{
		this.executor.addTask(new CloseRegionManagerTask(this.regionManager));
		this.executor.close();
		MapTexture oldMapTexture = this.mapTexture;
		MapTexture newMapTexture = new MapTexture(this.textureSize, Config.linearTextureScaling);
		this.mapTexture = newMapTexture;
		if (oldMapTexture != null)
		{
			oldMapTexture.close();
		}
		this.executor = new BackgroundExecutor();
		this.regionManager = new RegionManager(this.worldDir, this.imageDir, this.blockColours, Config.zoomInLevels, Config.zoomOutLevels);

		UndergroundTexture oldTexture = this.undergroundMapTexture;
		UndergroundTexture newTexture = new UndergroundTexture(this, this.textureSize, Config.linearTextureScaling);
		this.undergroundMapTexture = newTexture;
		if (oldTexture != null)
		{
			this.undergroundMapTexture.close();
		}
	}

	public void toggleUndergroundMode()
	{
		Config.undergroundMode = !Config.undergroundMode;
		this.miniMap.view.setUndergroundMode(Config.undergroundMode);
	}

	// //////////////////////////////
	// Initialization and Cleanup
	// //////////////////////////////

	public void load()
	{

		if (this.ready)
		{
			return;
		}

		if ((this.mc.theWorld == null) || (this.mc.thePlayer == null))
		{
			Logging.log("Mw.load: world or player is null, cannot load yet");
			return;
		}

		Logging.log("Mw.load: loading...");

		// get world and image directories
		File saveDir = this.saveDir;
		if (Config.saveDirOverride.length() > 0)
		{
			File d = new File(Config.saveDirOverride);
			if (d.isDirectory())
			{
				saveDir = d;
			}
			else
			{
				Logging.log("error: no such directory %s", Config.saveDirOverride);
			}
		}

		if (!this.mc.isSingleplayer())
		{
			this.worldDir = new File(new File(saveDir, "mapwriter_mp_worlds"), Utils.getWorldName());
		}
		else
		{
			this.worldDir = new File(new File(saveDir, "mapwriter_sp_worlds"), Utils.getWorldName());
		}

		// create directories
		this.imageDir = new File(this.worldDir, "images");
		if (!this.imageDir.exists())
		{
			this.imageDir.mkdirs();
		}
		if (!this.imageDir.isDirectory())
		{
			Logging.log("Mapwriter: ERROR: could not create images directory '%s'", this.imageDir.getPath());
		}

		this.tickCounter = 0;

		// this.multiplayer = !this.mc.isIntegratedServerRunning();

		// marker manager only depends on the config being loaded
		this.markerManager = new MarkerManager();
		this.markerManager.load(WorldConfig.getInstance().worldConfiguration, Reference.catMarkers);

		this.playerTrail = new Trail(this, Reference.PlayerTrailName);

		// executor does not depend on anything
		this.executor = new BackgroundExecutor();

		// mapTexture depends on config being loaded
		this.mapTexture = new MapTexture(this.textureSize, Config.linearTextureScaling);
		this.undergroundMapTexture = new UndergroundTexture(this, this.textureSize, Config.linearTextureScaling);
		// this.reloadBlockColours();
		// region manager depends on config, mapTexture, and block colours
		this.regionManager = new RegionManager(this.worldDir, this.imageDir, this.blockColours, Config.zoomInLevels, Config.zoomOutLevels);
		// overlay manager depends on mapTexture
		this.miniMap = new MiniMap(this);
		this.miniMap.view.setDimension(this.mc.thePlayer.dimension);

		this.chunkManager = new ChunkManager(this);

		this.ready = true;

		// if (!zoomLevelsExist) {
		// printBoth("recreating zoom levels");
		// this.regionManager.recreateAllZoomLevels();
		// }
	}

	public void close()
	{

		Logging.log("Mw.close: closing...");

		if (this.ready)
		{
			this.ready = false;

			this.chunkManager.close();
			this.chunkManager = null;

			// close all loaded regions, saving modified images.
			// this will create extra tasks that need to be completed.
			this.executor.addTask(new CloseRegionManagerTask(this.regionManager));
			this.regionManager = null;

			Logging.log("waiting for %d tasks to finish...", this.executor.tasksRemaining());
			if (this.executor.close())
			{
				Logging.log("error: timeout waiting for tasks to finish");
			}
			Logging.log("done");

			this.playerTrail.close();

			this.markerManager.save(WorldConfig.getInstance().worldConfiguration, Reference.catMarkers);
			this.markerManager.clear();

			// close overlay
			this.miniMap.close();
			this.miniMap = null;

			this.undergroundMapTexture.close();
			this.mapTexture.close();

			WorldConfig.getInstance().saveWorldConfig();
			// this.saveConfig();

			this.tickCounter = 0;

			OverlaySlime.reset(); // Reset the state so the seed will be asked
			// again when we log in
		}
	}

	// //////////////////////////////
	// Event handlers
	// //////////////////////////////

	public void onTick()
	{
		this.load();
		if (this.ready && (this.mc.thePlayer != null))
		{

			this.setTextureSize();

			this.updatePlayer();

			if (Config.undergroundMode && ((this.tickCounter % 30) == 0))
			{
				this.undergroundMapTexture.update();
			}

			if (!(this.mc.currentScreen instanceof MwGui))
			{
				// if in game (no gui screen) center the minimap on the player
				// and render it.
				this.miniMap.view.setViewCentreScaled(this.playerX, this.playerZ, this.playerDimension);
				this.miniMap.drawCurrentMap();
			}

			// process background tasks
			int maxTasks = 50;
			while (!this.executor.processTaskQueue() && (maxTasks > 0))
			{
				maxTasks--;
			}

			this.chunkManager.onTick();

			// update GL texture of mapTexture if updated
			this.mapTexture.processTextureUpdates();

			// let the renderEngine know we have changed the bound texture.
			// this.mc.renderEngine.resetBoundTexture();

			// if (this.tickCounter % 100 == 0) {
			// MwUtil.log("tick %d", this.tickCounter);
			// }
			this.playerTrail.onTick();

			this.tickCounter++;
		}
	}

	// add chunk to the set of loaded chunks
	public void onChunkLoad(Chunk chunk)
	{
		this.load();
		if ((chunk != null) && (chunk.getWorld() instanceof net.minecraft.client.multiplayer.WorldClient))
		{
			if (this.ready)
			{
				this.chunkManager.addChunk(chunk);
			}
			else
			{
				Logging.logInfo("missed chunk (%d, %d)", chunk.xPosition, chunk.zPosition);
			}
		}
	}

	// remove chunk from the set of loaded chunks.
	// convert to mwchunk and write chunk to region file if in multiplayer.
	public void onChunkUnload(Chunk chunk)
	{
		if (this.ready && (chunk != null) && (chunk.getWorld() instanceof net.minecraft.client.multiplayer.WorldClient))
		{
			this.chunkManager.removeChunk(chunk);
		}
	}

	// from onTick when mc.currentScreen is an instance of GuiGameOver
	// it's the only option to detect death client side
	public void onPlayerDeath(EntityPlayerMP player)
	{
		if (this.ready && (Config.maxDeathMarkers > 0))
		{
			this.updatePlayer();
			int deleteCount = (this.markerManager.countMarkersInGroup("playerDeaths") - Config.maxDeathMarkers) + 1;
			for (int i = 0; i < deleteCount; i++)
			{
				// delete the first marker found in the group "playerDeaths".
				// as new markers are only ever appended to the marker list this
				// will delete the
				// earliest death marker added.
				this.markerManager.delMarker(null, "playerDeaths");
			}

			this.markerManager.addMarker(Utils.getCurrentDateString(), "playerDeaths", this.playerXInt, this.playerYInt, this.playerZInt, this.playerDimension, 0xffff0000);
			this.markerManager.setVisibleGroupName("playerDeaths");
			this.markerManager.update();
		}
	}

	public void onKeyDown(KeyBinding kb)
	{
		// make sure not in GUI element (e.g. chat box)
		if ((this.mc.currentScreen == null) && (this.ready))
		{
			// Mw.log("client tick: %s key pressed", kb.keyDescription);

			if (kb == MwKeyHandler.keyMapMode)
			{
				// map mode toggle
				this.miniMap.nextOverlayMode(1);

			}
			else if (kb == MwKeyHandler.keyMapGui)
			{
				// open map gui
				this.mc.displayGuiScreen(new MwGui(this));

			}
			else if (kb == MwKeyHandler.keyNewMarker)
			{
				// open new marker dialog
				String group = this.markerManager.getVisibleGroupName();
				if (group.equals("none"))
				{
					group = "group";
				}
				if (Config.newMarkerDialog)
				{
					this.mc.displayGuiScreen(new MwGuiMarkerDialogNew(null, this.markerManager, "", group, this.playerXInt, this.playerYInt, this.playerZInt, this.playerDimension));
				}
				else
				{
					this.mc.displayGuiScreen(new MwGuiMarkerDialog(null, this.markerManager, "", group, this.playerXInt, this.playerYInt, this.playerZInt, this.playerDimension));
				}
			}
			else if (kb == MwKeyHandler.keyNextGroup)
			{
				// toggle marker mode
				this.markerManager.nextGroup();
				this.markerManager.update();
				this.mc.thePlayer.addChatMessage(new ChatComponentText("group " + this.markerManager.getVisibleGroupName() + " selected"));

			}
			else if (kb == MwKeyHandler.keyTeleport)
			{
				// set or remove marker
				Marker marker = this.markerManager.getNearestMarkerInDirection(this.playerXInt, this.playerZInt, this.playerHeading);
				if (marker != null)
				{
					this.teleportToMarker(marker);
				}
			}
			else if (kb == MwKeyHandler.keyZoomIn)
			{
				// zoom in
				this.miniMap.view.adjustZoomLevel(-1);
			}
			else if (kb == MwKeyHandler.keyZoomOut)
			{
				// zoom out
				this.miniMap.view.adjustZoomLevel(1);
			}
			else if (kb == MwKeyHandler.keyUndergroundMode)
			{
				this.toggleUndergroundMode();
			}
		}
	}
}
