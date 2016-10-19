package me.kevinnovak.messagetones;

import java.util.ArrayList;

import org.bukkit.entity.Player;

public class CommandHelp {
	private ArrayList<String> lines = new ArrayList<String>();
	private Player player = null;
	private String header = "";
	private String footer = "";
	private String pageLine = "";
	private String noLines = "";
	
	public CommandHelp(Player player, ArrayList<String> lines, String header, String footer, String pageLine, String noLines) {
		this.player = player;
		this.lines = lines;
		this.header = header;
		this.footer = footer;
		this.pageLine = pageLine;
		this.noLines = noLines;
	}
	
	public void print(int pageNum) {
		if (pageNum > Math.ceil((double)lines.size()/7)) {
			pageNum = 1;
		}
		player.sendMessage(header);
		if (!lines.isEmpty()) {
			for (int i=7*(pageNum-1); i<lines.size() && i<(7*pageNum); i++) {
				player.sendMessage(lines.get(i));
			}
			if (lines.size() > 7*pageNum) {
				int nextPageNum = pageNum + 1;
				player.sendMessage(pageLine.replace("{PAGE}", Integer.toString(nextPageNum)));
			}
		} else {
			player.sendMessage(noLines);
		}
		player.sendMessage(footer);
	}
}