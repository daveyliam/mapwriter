package mapwriter.forge;

import mapwriter.Mw;
import net.minecraft.client.settings.KeyBinding;

import java.util.EnumSet;

import org.lwjgl.input.Keyboard;

/*import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;*/

import cpw.mods.fml.client.registry.KeyBindingRegistry.KeyHandler;
import cpw.mods.fml.common.TickType;
import net.minecraftforge.common.Configuration;

public class MwKeyHandler extends KeyHandler {

	public static KeyBinding keyMapGui = new KeyBinding("Open Map GUI", Keyboard.KEY_M);
	public static KeyBinding keyNewMarker = new KeyBinding("New Marker", Keyboard.KEY_INSERT);
	public static KeyBinding keyMapMode = new KeyBinding("Next Map Mode", Keyboard.KEY_N);
	public static KeyBinding keyNextGroup = new KeyBinding("Next Marker Group", Keyboard.KEY_COMMA);
	public static KeyBinding keyTeleport = new KeyBinding("Teleport to Marker", Keyboard.KEY_PERIOD);
	public static KeyBinding keyZoomIn = new KeyBinding("Map Zoom In", Keyboard.KEY_PRIOR);
	public static KeyBinding keyZoomOut = new KeyBinding("Map Zoom Out", Keyboard.KEY_NEXT);
	public static KeyBinding keyUndergroundMode = new KeyBinding("Underground Mode", Keyboard.KEY_U);
	//public static KeyBinding keyQuickLargeMap = new KeyBinding("key.mw_quick_large_map", Keyboard.KEY_NONE);

    static KeyBinding[] keyBindings = new KeyBinding[] {
        keyMapGui, keyNewMarker, keyMapMode, keyNextGroup, keyTeleport, keyZoomIn, keyZoomOut, keyUndergroundMode};
    static boolean[] keyBooleans = new boolean[] {
        false, false, false, false, false, false, false, false};
	
	/*public MwKeyHandler(){
        ClientRegistry.registerKeyBinding(keyMapGui);
        ClientRegistry.registerKeyBinding(keyNewMarker);
        ClientRegistry.registerKeyBinding(keyMapMode);
        ClientRegistry.registerKeyBinding(keyNextGroup);
        ClientRegistry.registerKeyBinding(keyTeleport);
        ClientRegistry.registerKeyBinding(keyZoomIn);
        ClientRegistry.registerKeyBinding(keyZoomOut);
        ClientRegistry.registerKeyBinding(keyUndergroundMode);
	}*/
    
    public MwKeyHandler() {
        super(keyBindings, keyBooleans);
    }

    /*@SubscribeEvent
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
    }*/

    @Override
    public String getLabel() {
        return "MapWriter Key Bindings";
    }

    @Override
    public void keyDown(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd, boolean isRepeat) {
        if (types.contains(TickType.CLIENT)) {
            // make sure not in GUI element (e.g. chat box)
            if ((Mw.instance.mc.currentScreen == null) && (Mw.instance.ready) && (tickEnd)) {
                //System.out.format("client tick: %s key pressed\n", kb.keyDescription);
                
                Mw.instance.onKeyDown(kb);
            }
        }
    }

    @Override
    public void keyUp(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd) {
        // do nothing
    }

    @Override
    public EnumSet<TickType> ticks() {
        // keys should be handled in game in the client
        return EnumSet.of(TickType.CLIENT);
    }
}
