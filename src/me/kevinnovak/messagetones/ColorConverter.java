package me.kevinnovak.messagetones;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

public class ColorConverter {
	private FileConfiguration config = null;
	
	public ColorConverter(FileConfiguration config) {
		this.config = config;
	}
	
    String convert(String toConvert) {
        return ChatColor.translateAlternateColorCodes('&', toConvert);
    }
    List<String> convert(List<String> toConvert) {
        List<String> translatedColors = new ArrayList<String>();
        for (String stringToTranslate: toConvert){
            translatedColors.add(ChatColor.translateAlternateColorCodes('&',stringToTranslate));
        }
        return translatedColors;
    }
    String convertConfig(String toConvert) {
    	return ChatColor.translateAlternateColorCodes('&', config.getString(toConvert));
    }
    List<String> convertConfigList(String toConvert) {
        List<String> translatedColors = new ArrayList<String>();
        for (String stringToTranslate: config.getStringList(toConvert)){
            translatedColors.add(ChatColor.translateAlternateColorCodes('&',stringToTranslate));
        }
        return translatedColors;
    }
}