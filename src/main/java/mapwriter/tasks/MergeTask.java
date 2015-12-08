package mapwriter.tasks;

import java.io.File;

import mapwriter.region.MergeToImage;
import mapwriter.region.RegionManager;
import mapwriter.util.Utils;
import net.minecraft.client.resources.I18n;

public class MergeTask extends Task
{

	final RegionManager regionManager;
	final File outputDir;
	final String basename;
	final int x, z, w, h, dimension;
	String msg = "";

	public MergeTask(RegionManager regionManager, int x, int z, int w, int h, int dimension, File outputDir, String basename)
	{
		this.regionManager = regionManager;
		this.x = x;
		this.z = z;
		this.w = w;
		this.h = h;
		this.dimension = dimension;
		this.outputDir = outputDir;
		this.basename = basename;
	}

	@Override
	public void run()
	{
		int count = MergeToImage.merge(this.regionManager, this.x, this.z, this.w, this.h, this.dimension, this.outputDir, this.basename);
		if (count > 0)
		{
			this.msg = I18n.format("mw.task.mergetask.chatmsg.merge.done", this.outputDir);
		}
		else
		{
			this.msg = I18n.format("mw.task.mergetask.chatmsg.merge.error", this.outputDir);
		}
	}

	@Override
	public void onComplete()
	{
		Utils.printBoth(this.msg);
	}

	@Override
	public boolean CheckForDuplicate()
	{
		return false;
	}

}
