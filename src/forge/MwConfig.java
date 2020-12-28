package mapwriter.forge;

import java.io.File;
import java.util.List;

import mapwriter.MwUtil;
import net.minecraftforge.common.config.Configuration;

public class MwConfig extends Configuration {
	
	public MwConfig(File file) {
		super(file, true);
	}
	
	public boolean getOrSetBoolean(String category, String key, boolean defaultValue) {
		return this.get(category, key, defaultValue ? 1 : 0).getInt() != 0;
	}
	
	public void setBoolean(String category, String key, boolean value) {
		this.get(category, key, value).set(value ? 1 : 0);
	}
	
	public int getOrSetInt(String category, String key, int defaultValue, int minValue, int maxValue) {
		int value = this.get(category, key, defaultValue).getInt();
		return Math.min(Math.max(minValue, value), maxValue);
	}
	
	public void setInt(String category, String key, int value) {
		this.get(category, key, value).set(value);
	}
	
	public long getColour(String category, String key) {
		long value = -1;
		if (this.hasKey(category, key)) {
			try {
				String valueString = this.get(category, key, "").getString();
				if (valueString.length() > 0) {
					value = Long.parseLong(valueString, 16);
					value &= 0xffffffffL;
				}
			} catch (NumberFormatException e) {
				MwUtil.log("error: could not read colour from config file %s:%s", category, key);
				value = -1;
			}
		}
		return value;
	}
	
	public int getColour(String category, String key, int value) {
		long valueLong = this.getColour(category, key);
		if (valueLong >= 0L) {
			value = (int) (valueLong & 0xffffffffL);
		}
		return value;
	}
	
	public int getOrSetColour(String category, String key, int value) {
		long valueLong = this.getColour(category, key);
		if (valueLong >= 0L) {
			value = (int) (valueLong & 0xffffffffL);
		} else {
			this.setColour(category, key, value);
		}
		return value;
	}
	
	public void setColour(String category, String key, int n) {
		this.get(category, key, "00000000").set(String.format("%08x", n));
	}
	
	public void setColour(String category, String key, int n, String comment) {
		this.get(category, key, "00000000", comment).set(String.format("%08x", n));
	}
	
	public String getSingleWord(String category, String key) {
		String value = "";
		if (this.hasKey(category, key)) {
			value = this.get(category, key, value).getString().trim();
			int firstSpace = value.indexOf(' ');
			if (firstSpace >= 0) {
				value = value.substring(0, firstSpace);
			}
		}
		return value;
	}
	
	public void setSingleWord(String category, String key, String value, String comment) {
		if ((comment != null) && (comment.length() > 0)) {
			value = value + " # " + comment;
		}
		this.get(category, key, value).set(value);
	}
	
	public void getIntList(String category, String key, List<Integer> list) {
		// convert List of integers to integer array to pass as default value
		int size = list.size();
		int[] array = new int[size];
		for (int i = 0; i < size; i++) {
			array[i] = list.get(i);
		}
		
		// get integer array from config file
		int[] arrayFromConfig = null;
		try {
			arrayFromConfig = this.get(category, key, array).getIntList();
		} catch (Exception e) {
			e.printStackTrace();
			arrayFromConfig = null;
		}
		if (arrayFromConfig != null) {
			array = arrayFromConfig;
		}
		
		// convert integer array back to List of integers
		list.clear();
		for (int i = 0; i < array.length; i++) {
			list.add(array[i]);
		}
	}
	
	public void setIntList(String category, String key, List<Integer> list) {
		// convert List of integers to integer array
		int size = list.size();
		String[] array = new String[size];
		for (int i = 0; i < size; i++) {
			array[i] = list.get(i).toString();
		}
		// write integer array to config file
		try {
			this.get(category, key, array).set(array);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
