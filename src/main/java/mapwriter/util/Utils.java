package mapwriter.util;

import java.util.List;

public class Utils 
{
	public static int[] integerListToIntArray(List<Integer> list)
	{
		// convert List of integers to integer array
		int size = list.size();
		int[] array = new int[size];
		for (int i = 0; i < size; i++) 
		{
			array[i] = list.get(i);
		}
		
		return array;
	}
	
	public static String[] StringListToIntArray(List<String> list)
	{
		// convert List of integers to integer array
		int size = list.size();
		String[] array = new String[size];
		for (int i = 0; i < size; i++) 
		{
			array[i] = list.get(i);
		}
		return array;
	}
}
