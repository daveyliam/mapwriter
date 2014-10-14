package mapwriter.api;

import java.util.ArrayList;

import mapwriter.map.MapView;
import mapwriter.map.mapmode.MapMode;

public interface IMwDataProvider {
	public ArrayList<IMwChunkOverlay> getChunksOverlay(int dim, double centerX, double centerZ, double minX, double minZ, double maxX, double maxZ);
	
	//Returns what should be added to the status bar by the addon.
	public String getStatusString(int dim, int bX, int bY, int bZ);
	
	//Call back for middle click.
	public void   onMiddleClick(int dim, int bX, int bZ, MapView mapview);
	
	//Callback for dimension change on the map
	public void onDimensionChanged(int dimension, MapView mapview);
	
	public void onMapCenterChanged(double vX, double vZ, MapView mapview);
	
	public void onZoomChanged(int level, MapView mapview);	
	
	public void onOverlayActivated(MapView mapview);

	public void onOverlayDeactivated(MapView mapview);	
	
	public void onDraw(MapView mapview, MapMode mapmode);
	
	public boolean onMouseInput(MapView mapview, MapMode mapmode);
}
