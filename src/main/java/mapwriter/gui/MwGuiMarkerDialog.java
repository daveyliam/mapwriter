package mapwriter.gui;

import mapwriter.map.Marker;
import mapwriter.map.MarkerManager;
import net.minecraft.client.gui.GuiScreen;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MwGuiMarkerDialog extends MwGuiTextDialog {
    private final MarkerManager markerManager;
	private Marker editingMarker;
	private String markerName = "name";
    private String markerGroup = "group";
    private int markerX = 0;
    private int markerY = 80;
    private int markerZ = 0;
    private int state = 0;
    private int dimension = 0;
    
    public MwGuiMarkerDialog(GuiScreen parentScreen, MarkerManager markerManager, String markerName, String markerGroup, int x, int y, int z, int dimension) {
        super(parentScreen, "Marker Name:", markerName, "marker must have a name");
		this.markerManager = markerManager;
		this.markerName = markerName;
		this.markerGroup = markerGroup;
		this.markerX = x;
		this.markerY = y;
		this.markerZ = z;
		this.editingMarker = null;
		this.dimension = dimension;
    }
    
    public MwGuiMarkerDialog(GuiScreen parentScreen, MarkerManager markerManager, Marker editingMarker) {
        super(parentScreen, "Edit Marker Name:", editingMarker.name, "marker must have a name");
        this.markerManager = markerManager;
		this.editingMarker = editingMarker;
		this.markerName = editingMarker.name;
		this.markerGroup = editingMarker.groupName;
		this.markerX = editingMarker.x;
		this.markerY = editingMarker.y;
		this.markerZ = editingMarker.z;
		this.dimension = editingMarker.dimension;
    }
    	
	@Override
	public boolean submit() {
		boolean done = false;
		switch(this.state) {
		case 0:
			this.markerName = this.getInputAsString();
			if (this.inputValid) {
				this.title = "Marker Group:";
				this.setText(this.markerGroup);
				this.error = "marker must have a group name";
				this.state++;
			}
			break;
		case 1:
			this.markerGroup = this.getInputAsString();
			if (this.inputValid) {
				this.title = "Marker X:";
				this.setText("" + this.markerX);
				this.error = "invalid value";
				this.state++;
			}
			break;
		case 2:
			this.markerX = this.getInputAsInt();
			if (this.inputValid) {
				this.title = "Marker Y:";
				this.setText("" + this.markerY);
				this.error = "invalid value";
				this.state++;
			}
			break;
		case 3:
			this.markerY = this.getInputAsInt();
			if (this.inputValid) {
				this.title = "Marker Z:";
				this.setText("" + this.markerZ);
				this.error = "invalid value";
				this.state++;
			}
			break;
		case 4:
			this.markerZ = this.getInputAsInt();
			if (this.inputValid) {
				done = true;
				int colour = Marker.getCurrentColour();
	    		if (this.editingMarker != null) {
	    			colour = this.editingMarker.colour;
	    			this.markerManager.delMarker(this.editingMarker);
	    			this.editingMarker = null;
	    		}
	    		this.markerManager.addMarker(this.markerName, this.markerGroup,
						this.markerX, this.markerY, this.markerZ,
						this.dimension, colour);
				this.markerManager.setVisibleGroupName(this.markerGroup);
				this.markerManager.update();
			}
			break;
		}
		return done;
	}
}
