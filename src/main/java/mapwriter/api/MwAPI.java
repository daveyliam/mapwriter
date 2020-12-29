package mapwriter.api;

import java.util.ArrayList;
import java.util.Collection;

import com.google.common.collect.HashBiMap;

public class MwAPI {

	private static HashBiMap<String, IMwDataProvider> dataProviders = HashBiMap.create();
	private static IMwDataProvider currentProvider = null;
	private static ArrayList<String> providerKeys = new ArrayList<String>();
	
	public static void registerDataProvider(String name, IMwDataProvider handler){
		dataProviders.put(name, handler);
		providerKeys.add(name);
	}
	
	public static Collection<IMwDataProvider> getDataProviders(){
		return dataProviders.values();
	}
	
	// Returns the data provider based on its name //
	public static IMwDataProvider getDataProvider(String name){
		return dataProviders.get(name);
	}

	// Returns the name based on the data provider //
	public static String getProviderName(IMwDataProvider provider){
		return dataProviders.inverse().get(provider);
	}	
	
	public static IMwDataProvider getCurrentDataProvider(){
		return currentProvider;
	}

	public static String getCurrentProviderName(){
		if (currentProvider != null)
			return getProviderName(currentProvider);
		else
			return "None";
	}
	
	public static IMwDataProvider setCurrentDataProvider(String name){
		currentProvider    = dataProviders.get(name);
		return currentProvider;		
	}
	
	public static IMwDataProvider setCurrentDataProvider(IMwDataProvider provider){
		currentProvider = provider;
		return currentProvider;		
	}
	
	public static IMwDataProvider setNextProvider(){
		if (currentProvider != null){
			int    index   = providerKeys.indexOf(getCurrentProviderName());
			if (index + 1 >= providerKeys.size()){
				currentProvider = null;
			} else {
				String nextKey  = providerKeys.get(index + 1);
				currentProvider = getDataProvider(nextKey);
			}
		} else {
			if (providerKeys.size() > 0)
				currentProvider = getDataProvider(providerKeys.get(0));
		}
		return currentProvider;
	}
	
	public static IMwDataProvider setPrevProvider(){
		if (currentProvider != null){
			int    index   = providerKeys.indexOf(getCurrentProviderName());
			if (index - 1 < 0){
				currentProvider = null;
			} else {
				String prevKey  = providerKeys.get(index - 1);
				currentProvider = getDataProvider(prevKey);
			}
		} else {
			if (providerKeys.size() > 0)
				currentProvider = getDataProvider(providerKeys.get(providerKeys.size() - 1));
		}
		return currentProvider;
	}	
}
