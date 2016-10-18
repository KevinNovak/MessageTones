package me.kevinnovak.messagetones;

import org.bukkit.Sound;

public class ConvertSound {
    // ======================
    // Convert Sound
    // ======================
    Sound convertSound(int soundID) throws InterruptedException {
		Sound[] soundArray = Sound.values();
    	soundID = soundID - 1;
		if(soundID >= 0 && soundArray.length > soundID && soundArray[soundID] != null) {
			return soundArray[soundID];
		}
		return null;
    } 
}