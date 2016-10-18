package me.kevinnovak.messagetones;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.bukkit.entity.Player;

public class CommandHelp {
	private ArrayList<String> lines = new ArrayList<String>();
	private Player player = null;
	private ColorConverter colorConv = null;
	private LinkedHashMap<String, String> permissionDesc = null;
	
	public CommandHelp(Player player, ColorConverter colorConv, LinkedHashMap<String, String> permissionDesc) {
		this.player = player;
		this.colorConv = colorConv;
		this.permissionDesc = permissionDesc;
	}
	
	private void evaluate() {
		for (String permission : permissionDesc.keySet()) {
			if (player.hasPermission(permission)) {
				lines.add(colorConv.convertConfig(permissionDesc.get(permission)));
			}
		}
	}
	
	public void print(int pageNum) {
		this.evaluate();
		if (pageNum > Math.ceil((double)lines.size()/7)) {
			pageNum = 1;
		}
		player.sendMessage(colorConv.convert(colorConv.convertConfig("helpHeader")));
		if (!lines.isEmpty()) {
			for (int i=7*(pageNum-1); i<lines.size() && i<(7*pageNum); i++) {
				player.sendMessage(colorConv.convert(lines.get(i)));
			}
			if (lines.size() > 7*pageNum) {
				int nextPageNum = pageNum + 1;
				player.sendMessage(colorConv.convertConfig("helpPage").replace("{PAGE}", Integer.toString(nextPageNum)));
			}
		} else {
			player.sendMessage(colorConv.convertConfig("helpNoCommands"));
		}
		player.sendMessage(colorConv.convert(colorConv.convertConfig("helpFooter")));
	}
}