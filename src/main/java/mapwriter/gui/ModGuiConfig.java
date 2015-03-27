package mapwriter.gui;

import java.util.ArrayList;
import java.util.List;

import mapwriter.handler.ConfigurationHandler;
import mapwriter.util.Reference;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.ConfigGuiType;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

public class ModGuiConfig extends GuiConfig
{
    public ModGuiConfig(GuiScreen guiScreen)
    {
        super(guiScreen,
                new ConfigElement(ConfigurationHandler.configuration.getCategory(Reference.catOptions)).getChildElements(),
                Reference.MOD_ID,
                false,
                false,
                GuiConfig.getAbridgedConfigPath(ConfigurationHandler.configuration.toString()));
    }
    
    private static List<IConfigElement> getConfigElements()
    {
    	List<IConfigElement> list = new ArrayList<IConfigElement>();
    	
    	return list;
    }

}
