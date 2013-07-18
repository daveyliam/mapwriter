package mapwriter.forge;

import java.util.EnumSet;

import mapwriter.Mw;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.KeyBindingRegistry.KeyHandler;
import cpw.mods.fml.common.TickType;

public class MwKeyHandler extends KeyHandler {

	private Mw mw;
	
	public static KeyBinding keyMapGui = new KeyBinding("Open Map GUI", Keyboard.KEY_M);
	public static KeyBinding keyNewMarker = new KeyBinding("New Marker", Keyboard.KEY_INSERT);
	public static KeyBinding keyMapMode = new KeyBinding("Next Map Mode", Keyboard.KEY_N);
	public static KeyBinding keyNextGroup = new KeyBinding("Next Marker Group", Keyboard.KEY_COMMA);
	public static KeyBinding keyTeleport = new KeyBinding("Teleport to Marker", Keyboard.KEY_PERIOD);
	public static KeyBinding keyZoomIn = new KeyBinding("Map Zoom In", Keyboard.KEY_PRIOR);
	public static KeyBinding keyZoomOut = new KeyBinding("Map Zoom Out", Keyboard.KEY_NEXT);
	
	private static KeyBinding[] keyBindings = new KeyBinding[] {
		keyMapGui, keyNewMarker, keyMapMode, keyNextGroup, keyTeleport, keyZoomIn, keyZoomOut};
	private static boolean[] keyBooleans = new boolean[] {
		false, false, false, false, false, false, false};
	
	public MwKeyHandler(Mw mw) {
		super(keyBindings, keyBooleans);
		this.mw = mw;
	}

	@Override
	public String getLabel() {
		return "Mw Key Bindings";
	}

	@Override
	public void keyDown(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd, boolean isRepeat) {
		if (types.contains(TickType.CLIENT) && (tickEnd)) {
			this.mw.onKeyDown(kb);
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
