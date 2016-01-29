package me.kevinnovak.messagetones;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

public class MessageTones extends JavaPlugin implements Listener {
    ConvertSound other = new ConvertSound();

    // =========================
    // Enable
    // =========================
    @Override
    public void onEnable() {
        
        // send message if ProtocolLib is detected or not
        if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            Bukkit.getServer().getLogger().info("[MessageTones] ProtocolLib Detected!");
            if (getConfig().getBoolean("msgEnabled")) {
                // start ProtocolLib
                startProtocolLib(); 
            }
        } else {
            Bukkit.getServer().getLogger().info("[MessageTones] ProtocolLib Not Detected!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // copy over default config file if doesn't exist
        saveDefaultConfig();
        
        // register events
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        
        // send message if Metrics is enabled or not
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
        
        // send message if plugin is enabled
        Bukkit.getServer().getLogger().info("[MessageTones] Plugin Enabled!");
    }
    
    // ======================
    // Disable
    // ======================
    public void onDisable() {
        Bukkit.getServer().getLogger().info("[MessageTones] Plugin Disabled!");
    }
      
    // =========================
    // Broadcast
    // =========================
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        if (!getConfig().getBoolean("broadcastEnabled")) {
            return;
        }
        if (!e.getPlayer().hasPermission("messagetones.broadcast")) {
            return;
        }
        String[] args = e.getMessage().split(" ");
        if (args.length <= 1){
            return;
        }
        String cmd = args[0];
        if (cmd.equals("/" + getConfig().getString("broadcastCommand"))) {
            for(Player player : Bukkit.getOnlinePlayers()){
                player.playSound(player.getLocation(), other.convertSound(getConfig().getInt("broadcastSound")), (float) getConfig().getDouble("broadcastVolume"), (float) getConfig().getDouble("broadcastPitch"));
            }
        }
    }
   
    // =========================
    // Login
    // =========================
    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPermission("messagetones.owner")) {
            if (!getConfig().getBoolean("joinOwnerEnabled")) {
                return;
            }
            for(Player player : Bukkit.getOnlinePlayers()){
                player.playSound(player.getLocation(), other.convertSound(getConfig().getInt("joinOwnerSound")), (float) getConfig().getDouble("joinOwnerVolume"), (float) getConfig().getDouble("joinOwnerPitch"));
            }
            return;
        } else if (event.getPlayer().hasPermission("messagetones.admin")) {
            if (!getConfig().getBoolean("joinAdminEnabled")) {
                return;
            }
            for(Player player : Bukkit.getOnlinePlayers()){
                player.playSound(player.getLocation(), other.convertSound(getConfig().getInt("joinAdminSound")), (float) getConfig().getDouble("joinAdminVolume"), (float) getConfig().getDouble("joinAdminPitch"));
            }
            return;
        } else {
            if (!getConfig().getBoolean("joinPlayerEnabled")) {
                return;
            }
            for(Player player : Bukkit.getOnlinePlayers()){
                player.playSound(player.getLocation(), other.convertSound(getConfig().getInt("joinPlayerSound")), (float) getConfig().getDouble("joinPlayerVolume"), (float) getConfig().getDouble("joinPlayerPitch"));
            }
            return;
        }
    }
    
    // =========================
    // Hotbar
    // =========================
    @EventHandler
    public void hotbarSwitch(PlayerItemHeldEvent event) {
        if (!getConfig().getBoolean("hotbarEnabled")) {
            return;
        }
        Player player = event.getPlayer();
        player.playSound(player.getLocation(), other.convertSound(getConfig().getInt("hotbarSound")), (float) getConfig().getDouble("hotbarVolume"), (float) getConfig().getDouble("hotbarPitch"));
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
        if(cmd.getName().equalsIgnoreCase("mt")) {
            if(args.length == 0) {
                List<String> list = getConfig().getStringList("help");
                List<String> convertedList = convertedLang(list);
                player.sendMessage(convertedList.toArray(new String[convertedList.size()]));
                return true;
            }
            if (args[0].equalsIgnoreCase("message")) {
                player.playSound(player.getLocation(), other.convertSound(getConfig().getInt("msgSound")), (float) getConfig().getDouble("msgVolume"), (float) getConfig().getDouble("msgPitch"));
                player.sendMessage(convertedLang("testmessage"));
                return true;
            } else if (args[0].equalsIgnoreCase("broadcast")) {
                player.playSound(player.getLocation(), other.convertSound(getConfig().getInt("broadcastSound")), (float) getConfig().getDouble("broadcastVolume"), (float) getConfig().getDouble("broadcastPitch"));
                player.sendMessage(convertedLang("testbroadcast"));
                return true;
            } else if (args[0].equalsIgnoreCase("playerjoin")) {
                player.playSound(player.getLocation(), other.convertSound(getConfig().getInt("joinPlayerSound")), (float) getConfig().getDouble("joinPlayerVolume"), (float) getConfig().getDouble("joinPlayerPitch"));
                player.sendMessage(convertedLang("testplayerjoin"));
                return true;
            } else if (args[0].equalsIgnoreCase("adminjoin")) {
                player.playSound(player.getLocation(), other.convertSound(getConfig().getInt("joinAdminSound")), (float) getConfig().getDouble("joinAdminVolume"), (float) getConfig().getDouble("joinAdminPitch"));
                player.sendMessage(convertedLang("testadminjoin"));
                return true;
            } else if (args[0].equalsIgnoreCase("ownerjoin")) {
                player.playSound(player.getLocation(), other.convertSound(getConfig().getInt("joinOwnerSound")), (float) getConfig().getDouble("joinOwnerVolume"), (float) getConfig().getDouble("joinOwnerPitch"));
                player.sendMessage(convertedLang("testownerjoin"));
                return true;
            } else {
                List<String> list = getConfig().getStringList("help");
                List<String> convertedList = convertedLang(list);
                player.sendMessage(convertedList.toArray(new String[convertedList.size()]));
                return true;
            }
        }

        return true;
    }
    
    // =========================
    // Convert String in Config
    // =========================
    String convertedLang(String toConvert) {
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString(toConvert));
    }
    List<String> convertedLang(List<String> toConvert) {
        List<String> translatedColors = new ArrayList<String>();
        for (String stringToTranslate: toConvert){
            translatedColors.add(ChatColor.translateAlternateColorCodes('&',stringToTranslate));
             
        }
        return translatedColors;
    }
    
    // =========================
    // ProtocolLib
    // =========================
    private interface Processor {
        public Object process(Object value, Object parent);
    }
    void startProtocolLib() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(this, PacketType.Play.Server.CHAT) {
                    private JSONParser parser = new JSONParser();
                    
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        PacketContainer packet = event.getPacket();
                        StructureModifier<WrappedChatComponent> componets = packet.getChatComponents();
                        
                        try {
                            Object data = parser.parse(componets.read(0).getJson());
                            final boolean[] result = new boolean[1];
                            
                            transformPrimitives(data, null, new Processor() {
                                @Override
                                public Object process(Object value, Object parent) {
                                    if (value instanceof String) {
                                        String stripped = ChatColor.stripColor((String) value);

                                        if (stripped.contains(getConfig().getString("msgTrigger"))) {
                                            event.getPlayer().playSound(event.getPlayer().getLocation(), other.convertSound(getConfig().getInt("msgSound")), (float) getConfig().getDouble("msgVolume"), (float) getConfig().getDouble("msgPitch"));
                                            result[0] = true;
                                        }
                                    }
                                    return value;
                                }
                            });
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }
    private Object transformPrimitives(Object value, Object parent, Processor processor) {
        // Check its type
        if (value instanceof JSONObject) {
            return transformPrimitives((JSONObject) value, processor);
        } else if (value instanceof JSONArray) {
            return transformPrimitives((JSONArray) value, processor);
        } else {
            return processor.process(value, parent);
        }
    }
    @SuppressWarnings("unchecked")
    private JSONObject transformPrimitives(JSONObject source, Processor processor) {
        for (Object key : source.keySet().toArray()) {
            Object value = source.get(key);
            source.put(key, transformPrimitives(value, source, processor));
        }
        return source;
    }
    @SuppressWarnings("unchecked")
    private JSONArray transformPrimitives(JSONArray source, Processor processor) {
        for (int i = 0; i < source.size(); i++) {
            Object value = source.get(i);
            source.set(i, transformPrimitives(value, source, processor));
        }
        return source;
    }
}