package me.kevinnovak.messagetones;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;

public class ColorConverter {
	public MessageTones plugin;

	public ColorConverter(MessageTones plugin){
	     this.plugin = plugin;
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
    	return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(toConvert));
    }
    List<String> convertConfigList(String toConvert) {
        List<String> translatedColors = new ArrayList<String>();
        for (String stringToTranslate: plugin.getConfig().getStringList(toConvert)){
            translatedColors.add(ChatColor.translateAlternateColorCodes('&',stringToTranslate));
        }
        return translatedColors;
    }
}