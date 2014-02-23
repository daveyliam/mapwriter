package mapwriter.map;

import mapwriter.region.Region;

public class MapViewRequest {
	public final int xMin, xMax, zMin, zMax, zoomLevel, dimension;
	
	public MapViewRequest(MapView view) {
		this.zoomLevel = view.getRegionZoomLevel();
		int size = Region.SIZE << this.zoomLevel;
		this.xMin = ((int) view.getMinX()) & (-size);
		this.zMin = ((int) view.getMinZ()) & (-size);
		this.xMax = ((int) view.getMaxX()) & (-size);
		this.zMax = ((int) view.getMaxZ()) & (-size);
		this.dimension = view.getDimension();
	}
	
	public boolean equals(MapViewRequest req) {
		return (req != null) &&
				(req.zoomLevel == this.zoomLevel) &&
				(req.dimension == this.dimension) &&
				(req.xMin == this.xMin) &&
				(req.xMax == this.xMax) &&
				(req.zMin == this.zMin) &&
				(req.zMax == this.zMax);
	}
	
	public boolean mostlyEquals(MapViewRequest req) {
		return (req != null) &&
				(req.zoomLevel == this.zoomLevel) &&
				(req.dimension == this.dimension);
	}
}
