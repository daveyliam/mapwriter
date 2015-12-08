package mapwriter.gui;

import mapwriter.Mw;
import mapwriter.map.MapView;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MwGuiTeleportDialog extends MwGuiTextDialog {

	final Mw mw;
	final MapView mapView;
	final int teleportX, teleportZ;
	
	public MwGuiTeleportDialog(GuiScreen parentScreen, Mw mw, MapView mapView, int x, int y, int z)
	{
		super(parentScreen, I18n.format("mw.gui.mwguimarkerdialognew.title") + ":", Integer.toString(y), I18n.format("mw.gui.mwguimarkerdialognew.error"));
		this.mw = mw;
		this.mapView = mapView;
		this.teleportX = x;
		this.teleportZ = z;
		this.backToGameOnSubmit = true;
	}
		
	@Override
	public boolean submit() {
		boolean done = false;
		int height = this.getInputAsInt();
		if (this.inputValid) {
    		height = Math.min(Math.max(0, height), 255);
    		this.mw.defaultTeleportHeight = height;
    		this.mw.teleportToMapPos(this.mapView, this.teleportX, height, this.teleportZ);
    		done = true;
		}
		return done;
	}
}
