package me.kevinnovak.messagetones;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.google.common.io.ByteStreams;

public class MessageTones extends JavaPlugin implements Listener {
    ConvertSound other = new ConvertSound();
    int version = 0;
    
    // =========================
    // Enable
    // =========================
    @Override
    public void onEnable() {
        // copy over default config file if doesn't exist
        saveDefaultConfig();
        
        // register events
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
    	
        String fullVersion = Bukkit.getServer().getVersion();
        Pattern versionPattern = Pattern.compile("\\d[.](\\d{1,2})[.]\\d{1,2}");
        Matcher versionMatcher = versionPattern.matcher(fullVersion);
        if (versionMatcher.find()) {
        	version = Integer.parseInt(versionMatcher.group(1));
        }
        
        if (getConfig().getBoolean("copied") != true) {
        	InputStream in = null;
        	if (version <= 8) {
	    		in = getClass().getResourceAsStream("/config-1.8.yml");
	    		Bukkit.getServer().getLogger().info("[MessageTones] Creating config.yml for Minecraft 1.8");
	    	} else if (version == 9) {
	    		in = getClass().getResourceAsStream("/config-1.9.yml");
	    		Bukkit.getServer().getLogger().info("[MessageTones] Creating config.yml for Minecraft 1.9");
	    	} else {
	    		in = getClass().getResourceAsStream("/config-1.10.yml");
	    		Bukkit.getServer().getLogger().info("[MessageTones] Creating config.yml for Minecraft 1.10");
	    	}
        	File targetFile = new File(this.getDataFolder() + File.separator + "config.yml");
        	OutputStream out = null;
			try {
				out = new FileOutputStream(targetFile);
				ByteStreams.copy(in, out);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			reloadConfig();
        }
    	
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
        
        if (version > 7 && version < 100) {
        	Bukkit.getServer().getLogger().info("[MessageTones] Using Minecraft 1." + version + " sounds.");
        } else {
        	Bukkit.getServer().getLogger().warning("[MessageTones] Unknown Minecraft version.");
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
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) throws InterruptedException {
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
    public void playerJoin(PlayerJoinEvent event) throws InterruptedException {
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
    public void hotbarSwitch(PlayerItemHeldEvent event) throws InterruptedException {
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
                try {
					player.playSound(player.getLocation(), other.convertSound(getConfig().getInt("msgSound")), (float) getConfig().getDouble("msgVolume"), (float) getConfig().getDouble("msgPitch"));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                player.sendMessage(convertedLang("testmessage"));
                return true;
            } else if (args[0].equalsIgnoreCase("broadcast")) {
                try {
					player.playSound(player.getLocation(), other.convertSound(getConfig().getInt("broadcastSound")), (float) getConfig().getDouble("broadcastVolume"), (float) getConfig().getDouble("broadcastPitch"));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                player.sendMessage(convertedLang("testbroadcast"));
                return true;
            } else if (args[0].equalsIgnoreCase("playerjoin")) {
                try {
					player.playSound(player.getLocation(), other.convertSound(getConfig().getInt("joinPlayerSound")), (float) getConfig().getDouble("joinPlayerVolume"), (float) getConfig().getDouble("joinPlayerPitch"));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                player.sendMessage(convertedLang("testplayerjoin"));
                return true;
            } else if (args[0].equalsIgnoreCase("adminjoin")) {
                try {
					player.playSound(player.getLocation(), other.convertSound(getConfig().getInt("joinAdminSound")), (float) getConfig().getDouble("joinAdminVolume"), (float) getConfig().getDouble("joinAdminPitch"));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                player.sendMessage(convertedLang("testadminjoin"));
                return true;
            } else if (args[0].equalsIgnoreCase("ownerjoin")) {
                try {
					player.playSound(player.getLocation(), other.convertSound(getConfig().getInt("joinOwnerSound")), (float) getConfig().getDouble("joinOwnerVolume"), (float) getConfig().getDouble("joinOwnerPitch"));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                player.sendMessage(convertedLang("testownerjoin"));
                return true;
            } else if (args[0].equalsIgnoreCase("hotbar")) {
                try {
					player.playSound(player.getLocation(), other.convertSound(getConfig().getInt("hotbarSound")), (float) getConfig().getDouble("hotbarVolume"), (float) getConfig().getDouble("hotbarPitch"));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                player.sendMessage(convertedLang("testhotbar"));
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
 
    // ======================
    // Auto-complete
    // ======================
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (cmd.getName().equalsIgnoreCase("mt")) {
            
            ArrayList<String> autocomplete = new ArrayList<String>();
            autocomplete.add("message");
            autocomplete.add("broadcast");
            autocomplete.add("playerjoin");
            autocomplete.add("adminjoin");
            autocomplete.add("ownerjoin");
            autocomplete.add("hotbar");
   
            if (args.length == 1) {
                ArrayList<String> toReturn = new ArrayList<String>();
                if (!args[0].equals("")) {
                    for (String arg : autocomplete) {
                        if (arg.startsWith(args[0].toLowerCase())) {
                            toReturn.add(arg);
                        }
                    }
                }
                return toReturn;
            }
            
        }
        return null;
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
                    StructureModifier < WrappedChatComponent > componets = packet.getChatComponents();

                    try {
                        Object chatData = parser.parse(componets.read(0).getJson());
                        final boolean[] result = new boolean[1];

                        if (chatData instanceof JSONObject) {
                            JSONObject chatJSON = (JSONObject) chatData;

                            if (chatJSON.get("extra") instanceof JSONArray) {
                                JSONArray chatJSONArray = (JSONArray) chatJSON.get("extra");

                                String chatLine = "";

                                for (int i = 0; i < chatJSONArray.size(); i++) {
                                    if (chatJSONArray.get(i) instanceof JSONObject) {
                                        JSONObject wordJSON = (JSONObject) chatJSONArray.get(i);
                                        if (wordJSON.get("text") instanceof String) {
                                            String word = (String) wordJSON.get("text");
                                            chatLine = chatLine + word;
                                        }
                                    } else if (chatJSONArray.get(i) instanceof String) {
                                        String word = (String) chatJSONArray.get(i);
                                        chatLine = chatLine + word;
                                    }
                                }
                                if (chatLine.contains(getConfig().getString("msgTrigger"))) {
                                    event.getPlayer().playSound(event.getPlayer().getLocation(), other.convertSound(getConfig().getInt("msgSound")), (float) getConfig().getDouble("msgVolume"), (float) getConfig().getDouble("msgPitch"));
                                    result[0] = true;
                                }
                            }
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
						// TODO Auto-generated catch block
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