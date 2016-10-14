package me.kevinnovak.messagetones;

import org.bukkit.Sound;

public class ConvertSound {
    // ======================
    // Convert Sound
    // ======================
    Sound convertSound(int soundID) throws InterruptedException {
		Sound[] soundArray = Sound.values();
//		for (int i = 0; i < soundArray.length; i++) {
//			int j = i+1;
//			Bukkit.getServer().getLogger().info("[MessageTones] ID: " + j + " Sound: " + soundArray[i].name());
//			TimeUnit.MILLISECONDS.sleep(10);
//		}
    	soundID = soundID - 1;
		if(soundID >= 0 && soundArray.length > soundID && soundArray[soundID] != null) {
//			Bukkit.getServer().getLogger().info("[MessageTones] Sound: " + soundArray[soundID].name());
			return soundArray[soundID];
		}
		return null;
    } 
}