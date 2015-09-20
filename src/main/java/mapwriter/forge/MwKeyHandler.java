package mapwriter.forge;

import java.util.ArrayList;

import mapwriter.Mw;
import modwarriors.notenoughkeys.api.Api;
import modwarriors.notenoughkeys.api.KeyBindingPressedEvent;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.Optional;

public class MwKeyHandler {
	
	public static KeyBinding keyMapGui = new KeyBinding("key.mw_open_gui", Keyboard.KEY_M, "Mapwriter");
	public static KeyBinding keyNewMarker = new KeyBinding("key.mw_new_marker", Keyboard.KEY_INSERT, "Mapwriter");
	public static KeyBinding keyMapMode = new KeyBinding("key.mw_next_map_mode", Keyboard.KEY_N, "Mapwriter");
	public static KeyBinding keyNextGroup = new KeyBinding("key.mw_next_marker_group", Keyboard.KEY_COMMA, "Mapwriter");
	public static KeyBinding keyTeleport = new KeyBinding("key.mw_teleport", Keyboard.KEY_PERIOD, "Mapwriter");
	public static KeyBinding keyZoomIn = new KeyBinding("key.mw_zoom_in", Keyboard.KEY_PRIOR, "Mapwriter");
	public static KeyBinding keyZoomOut = new KeyBinding("key.mw_zoom_out", Keyboard.KEY_NEXT, "Mapwriter");
	public static KeyBinding keyUndergroundMode = new KeyBinding("key.mw_underground_mode", Keyboard.KEY_U, "Mapwriter");
	//public static KeyBinding keyQuickLargeMap = new KeyBinding("key.mw_quick_large_map", Keyboard.KEY_NONE);
	
	public final KeyBinding[] keys = 
		{
			keyMapGui,
			keyNewMarker,
			keyMapMode,
			keyNextGroup,
			keyTeleport,
			keyZoomIn,
			keyZoomOut,
			keyUndergroundMode
	};
	
	public MwKeyHandler()
	{
		ArrayList<String> listKeyDescs = new ArrayList<String>();
		// Register bindings
		for (KeyBinding key : this.keys)
		{
			if (key != null)
			{
				ClientRegistry.registerKeyBinding(key);
			}
			listKeyDescs.add(key.getKeyDescription());
			}
		
		if (Loader.isModLoaded("notenoughkeys"))
		{
			Api.registerMod("MapWriter", listKeyDescs.toArray(new String[0]));
		}
	}
	
	@SubscribeEvent
	public void keyEvent(InputEvent.KeyInputEvent event)
	{
		if (!Loader.isModLoaded("notenoughkeys"))
		{
			this.checkKeys();
		}
	}
	
	@Optional.Method(modid = "notenoughkeys")
	@SubscribeEvent
	public void keyEventSpecial(KeyBindingPressedEvent event) 
	{
		if (event.isKeyBindingPressed)
		{
			Mw.instance.onKeyDown(event.keyBinding);
		}
	}
	
	private void checkKeys() 
	{
		for (KeyBinding key : keys) 
		{
			if (key != null && key.isPressed()) 
			{
				Mw.instance.onKeyDown(key);
			}
		}
	}
}
