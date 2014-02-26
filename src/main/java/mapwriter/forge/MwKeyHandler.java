package mapwriter.forge;

import mapwriter.Mw;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;

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
	
	public MwKeyHandler(){
        ClientRegistry.registerKeyBinding(keyMapGui);
        ClientRegistry.registerKeyBinding(keyNewMarker);
        ClientRegistry.registerKeyBinding(keyMapMode);
        ClientRegistry.registerKeyBinding(keyNextGroup);
        ClientRegistry.registerKeyBinding(keyTeleport);
        ClientRegistry.registerKeyBinding(keyZoomIn);
        ClientRegistry.registerKeyBinding(keyZoomOut);
        ClientRegistry.registerKeyBinding(keyUndergroundMode);
	}

    @SubscribeEvent
    public void onKeyPress(InputEvent.KeyInputEvent event){
        if(keyMapGui.getIsKeyPressed()){
            KeyBinding.setKeyBindState(keyMapGui.getKeyCode(), false);
            Mw.instance.onKeyDown(keyMapGui);
        }
        if(keyNewMarker.getIsKeyPressed()){
            KeyBinding.setKeyBindState(keyNewMarker.getKeyCode(), false);
            Mw.instance.onKeyDown(keyNewMarker);
        }
        if(keyMapMode.getIsKeyPressed()){
            KeyBinding.setKeyBindState(keyMapMode.getKeyCode(), false);
            Mw.instance.onKeyDown(keyMapMode);
        }
        if(keyNextGroup.getIsKeyPressed()){
            KeyBinding.setKeyBindState(keyNextGroup.getKeyCode(), false);
            Mw.instance.onKeyDown(keyNextGroup);
        }
        if(keyTeleport.getIsKeyPressed()){
            KeyBinding.setKeyBindState(keyTeleport.getKeyCode(), false);
            Mw.instance.onKeyDown(keyTeleport);
        }
        if(keyZoomIn.getIsKeyPressed()){
            KeyBinding.setKeyBindState(keyZoomIn.getKeyCode(), false);
            Mw.instance.onKeyDown(keyZoomIn);
        }
        if(keyZoomOut.getIsKeyPressed()){
            KeyBinding.setKeyBindState(keyZoomOut.getKeyCode(), false);
            Mw.instance.onKeyDown(keyZoomOut);
        }
        if(keyUndergroundMode.getIsKeyPressed()){
            KeyBinding.setKeyBindState(keyUndergroundMode.getKeyCode(), false);
            Mw.instance.onKeyDown(keyUndergroundMode);
        }
    }
}
