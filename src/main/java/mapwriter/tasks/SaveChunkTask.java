package mapwriter.tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import mapwriter.region.MwChunk;
import mapwriter.region.RegionManager;

public class SaveChunkTask extends Task
{
	private MwChunk chunk;
	private RegionManager regionManager;
	private AtomicBoolean Running = new AtomicBoolean();
	private static HashMap<Long, SaveChunkTask> chunksUpdating = new HashMap<Long, SaveChunkTask>();

	public SaveChunkTask(MwChunk chunk, RegionManager regionManager)
	{
		this.chunk = chunk;
		this.regionManager = regionManager;
	}

	@Override
	public void run()
	{
		this.Running.set(true);
		this.chunk.write(this.regionManager.regionFileCache);
	}

	@Override
	public void onComplete()
	{
		Long coords = this.chunk.getCoordIntPair();
		SaveChunkTask.chunksUpdating.remove(coords);
		this.Running.set(false);
	}

	@Override
	public boolean CheckForDuplicate()
	{
		Long coords = this.chunk.getCoordIntPair();

		if (!SaveChunkTask.chunksUpdating.containsKey(coords))
		{
			SaveChunkTask.chunksUpdating.put(coords, this);
			return false;
		}
		else
		{
			SaveChunkTask task2 = (SaveChunkTask) SaveChunkTask.chunksUpdating.get(coords);
			if (task2.Running.get() == false)
			{
				task2.UpdateChunkData(this.chunk, this.regionManager);
			}
			else
			{
				SaveChunkTask.chunksUpdating.put(coords, this);
				return false;
			}
		}
		return true;
	}

	public void UpdateChunkData(MwChunk chunk, RegionManager regionManager)
	{
		this.chunk = chunk;
		this.regionManager = regionManager;
	}
}
