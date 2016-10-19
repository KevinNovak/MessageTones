package me.kevinnovak.messagetones;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class CustomSound {
	private String friendlyName;
	private String commandName;
	private Sound sound;
	private float pitch;
	private float volume;
	private Boolean enabled;
	private Boolean defaultOn;
	private String dataName;
	private String statusMessage;
	private String toggleMessage;
	private String testMessage;
	private String helpMessage;
	private String infoTestSound;
	private String infoToggleSound;
	
	public CustomSound(String friendlyName, String commandName, Sound sound, float pitch, float volume, Boolean enabled, Boolean defaultOn, String dataName, String testMessage, String statusMessage, String toggleMessage, String helpMessage, String infoTestSound, String infoToggleSound) {
		this.friendlyName = friendlyName;
		this.commandName = commandName;
		this.sound = sound;
		this.pitch = pitch;
		this.volume = volume;
		this.enabled = enabled;
		this.defaultOn = defaultOn;
		this.dataName = dataName;
		this.testMessage = testMessage;
		this.statusMessage = statusMessage;
		this.toggleMessage = toggleMessage;
		this.helpMessage = helpMessage;
		this.infoTestSound = infoTestSound;
		this.infoToggleSound = infoToggleSound;
	}
	
	public String getFriendlyName() {
		return friendlyName;
	}
	
	public String getCommandName() {
		return commandName;
	}
	
	public Boolean isEnabled() {
		return enabled;
	}
	
	public Boolean isDefaultOn() {
		return defaultOn;
	}
	
	public String getDataName() {
		return dataName;
	}
	
	public String getTogglePerm() {
		return "messagetones.toggle." + commandName;
	}
	
	public void playSound(Player player) {
		player.playSound(player.getLocation(), sound, volume, pitch);
	}
	public String getHelpTestMessage() {
		return helpMessage.replace("{COMMAND}", "/mt " + commandName).replace("{INFO}", infoTestSound).replace("{SOUND}", friendlyName);
	}
	public String getHelpToggleMessage() {
		return helpMessage.replace("{COMMAND}", "/mt " + commandName + " [on/off]").replace("{INFO}", infoToggleSound).replace("{SOUND}", friendlyName);
	}
	public void printTestMessage(Player player) {
		player.sendMessage(testMessage.replace("{SOUND}", friendlyName));
	}
	
	public void printToggleMessage(Player player, String toggledStatus) {
		player.sendMessage(toggleMessage.replace("{SOUND}", friendlyName).replace("{STATUS}", toggledStatus));
	}
	
	public void printStatusMessage(Player player, String currentStatus) {
		player.sendMessage(statusMessage.replace("{SOUND}", friendlyName).replace("{STATUS}", currentStatus));
	}
}