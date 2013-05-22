package mapwriter.forge;

import java.util.EnumSet;

import mapwriter.Mw;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;

public class MwTickHandler implements ITickHandler {

	Mw mw;
	
	public MwTickHandler(Mw mw) {
		this.mw = mw;
	}
	
	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData) {
	}

	@Override
	public void tickEnd(EnumSet<TickType> type, Object... tickData) {
		this.mw.onTick();
	}

	@Override
	public EnumSet<TickType> ticks() {
		return EnumSet.of(TickType.RENDER);
	}

	@Override
	public String getLabel() {
		return "MapWriter";
	}

}
