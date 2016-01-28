package mapwriter.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mapwriter.Mw;
import mapwriter.util.Reference;
import mapwriter.util.Utils;
import net.minecraftforge.common.config.Configuration;

public class WorldConfig
{
	private static WorldConfig instance = null;

	public Configuration worldConfiguration = null;

	// list of available dimensions
	public List<Integer> dimensionList = new ArrayList<Integer>();

	private WorldConfig()
	{
		// load world specific config file
		File worldConfigFile = new File(Mw.getInstance().worldDir, Reference.worldDirConfigName);
		this.worldConfiguration = new Configuration(worldConfigFile);

		this.InitDimensionList();
	}

	public static WorldConfig getInstance()
	{
		if (instance == null)
		{
			synchronized (WorldConfig.class)
			{
				if (instance == null)
				{
					instance = new WorldConfig();
				}
			}
		}

		return instance;
	}

	public void saveWorldConfig()
	{
		this.worldConfiguration.save();
	}

	// Dimension List
	public void InitDimensionList()
	{
		this.dimensionList.clear();
		this.worldConfiguration.get(Reference.catWorld, "dimensionList", Utils.integerListToIntArray(this.dimensionList));
		this.addDimension(0);
		this.cleanDimensionList();
	}

	public void addDimension(int dimension)
	{
		int i = this.dimensionList.indexOf(dimension);
		if (i < 0)
		{
			this.dimensionList.add(dimension);
		}
	}

	public void cleanDimensionList()
	{
		List<Integer> dimensionListCopy = new ArrayList<Integer>(this.dimensionList);
		this.dimensionList.clear();
		for (int dimension : dimensionListCopy)
		{
			this.addDimension(dimension);
		}
	}

}
