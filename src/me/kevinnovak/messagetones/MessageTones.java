package me.kevinnovak.messagetones;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class MessageTones extends JavaPlugin implements Listener{
    public File replyFile = new File(getDataFolder()+"/replies.yml");
    public FileConfiguration replyData = YamlConfiguration.loadConfiguration(replyFile);
    
    // ======================
    // Enable
    // ======================
    public void onEnable() {
        saveDefaultConfig();
        try {
            replyData.save(replyFile);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        if (getConfig().getBoolean("metrics")) {
            try {
                MetricsLite metrics = new MetricsLite(this);
                metrics.start();
                Bukkit.getServer().getLogger().info("[MessageTones] Metrics Enabled!");
            } catch (IOException e) {
                Bukkit.getServer().getLogger().info("[MessageTones] Failed to Start Metrics.");
            }
        } else {
            Bukkit.getServer().getLogger().info("[MessageTones] Metrics Disabled.");
        }
        Bukkit.getServer().getLogger().info("[MessageTones] Plugin Enabled!");
    }
    
    // ======================
    // Disable
    // ======================
    public void onDisable() {
        replyFile.delete();
        Bukkit.getServer().getLogger().info("[MessageTones] Plugin Disabled!");
    }
    
    // =========================
    // Convert String in Config
    // =========================
    String convertedLang(String toConvert) {
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString(toConvert));
    }
    
    // =========================
    // Find Player
    // =========================
    Player findPlayer(String toFind) {
        String toFindLower = toFind.toLowerCase();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            String onlineLower = onlinePlayer.getName().toLowerCase();
            if (onlineLower.contains(toFindLower)) {
                return onlinePlayer;
            }
        }
        return null;
    }
    
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        String[] args = e.getMessage().split(" ");
        if (args.length >= 2){
            String cmd = args[0];
            String target = args[1];
            // =========================
            // /msg
            // =========================
            if (cmd.equals("/msg") || cmd.equals("/m") || cmd.equals("/t") || cmd.equals("/tell") || cmd.equals("/whisper") || cmd.equals("/w")) {
                if (target.length() > 1) {
                    Player targetPlayer = findPlayer(target);
                    if (targetPlayer == null) {
                        return;
                    } else {
                        replyData.set(targetPlayer.getName() + ".Reply", e.getPlayer().getName());
                        try {
                            replyData.save(replyFile);
                        } catch (IOException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                        targetPlayer.playSound(targetPlayer.getLocation(),Sound.NOTE_PLING,1,0);
                        return;
                    }
                }
            }
            // =========================
            // /r
            // =========================
            if (cmd.equals("/r") || cmd.equals("/reply")) {
                // if send command player exists in replies.yml file
                if (replyData.getString(e.getPlayer().getName() + ".Reply") == null) {
                    return;
                }
                // if reply to player exists on server
                Player targetPlayer = Bukkit.getServer().getPlayer(replyData.getString(e.getPlayer().getName() + ".Reply"));
                if (targetPlayer == null) {
                    return;
                }
                // save a new reply to player
                e.getPlayer().sendMessage(targetPlayer.getName());
                replyData.set(targetPlayer.getName() + ".Reply", e.getPlayer().getName());
                try {
                    replyData.save(replyFile);
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                // play sound
                targetPlayer.playSound(targetPlayer.getLocation(),Sound.NOTE_PLING,1,0);
                return;
            }
        }
    }
    
    // ======================
    // Commands
    // ======================
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        // ======================
        // Console
        // ======================
        if (!(sender instanceof Player)) {
            sender.sendMessage(convertedLang("noconsole"));
            return true;
        }
        Player player = (Player) sender;
        // ======================
        // /ding
        // ======================
        if(cmd.getName().equalsIgnoreCase("ding")) {
               player.playSound(player.getLocation(),Sound.NOTE_PLING,1,0);
        }
        
        return true;
    }
}
