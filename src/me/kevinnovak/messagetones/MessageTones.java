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
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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
    
	// Create the players.yml to hold user preferences
    public File playerFile = new File(getDataFolder()+"/players.yml");
    public FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerFile);
    
    // Create a sound converter
    ConvertSound soundConv = new ConvertSound();
    
    // Initialize version number to 0
    int version = 0;
    
    // =========================
    // Enable
    // =========================
    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        setVersion();
        
        // copy config file if not copied already
        if (getConfig().getBoolean("copied") != true) {
			copyFile();
        }
        
        checkProtocolLib();
        checkMetrics();
        checkSoundVersion();

        Bukkit.getServer().getLogger().info("[MessageTones] Plugin enabled!");
    }
    
	// ======================
    // Disable
    // ======================
    public void onDisable() {
        Bukkit.getServer().getLogger().info("[MessageTones] Plugin disabled!");
    }
    
    // =========================
    // Set Version
    // =========================
	private void setVersion() {
        String fullVersion = Bukkit.getServer().getVersion();
        Pattern versionPattern = Pattern.compile("\\d[.](\\d{1,2})[.]\\d{1,2}");
        Matcher versionMatcher = versionPattern.matcher(fullVersion);
        if (versionMatcher.find()) {
            try{
            	version = Integer.parseInt(versionMatcher.group(1));
            }catch(NumberFormatException e){
            	Bukkit.getServer().getLogger().warning("[MessageTones] Error parsing version as integer.");
            	e.printStackTrace();
            }
        }
	}
	
    // =========================
    // Copy File
    // =========================
	private void copyFile() {
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
			Bukkit.getServer().getLogger().warning("[MessageTones] FileNotFoundException - Copying config failed.");
			e.printStackTrace();
		} catch (IOException e) {
			Bukkit.getServer().getLogger().warning("[MessageTones] IOException - Copying config failed.");
			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e) {
			Bukkit.getServer().getLogger().warning("[MessageTones] IOException - Closing internal config failed.");
			e.printStackTrace();
		}
		try {
			out.close();
		} catch (IOException e) {
			Bukkit.getServer().getLogger().warning("[MessageTones] IOException - Closing external config failed.");
			e.printStackTrace();
		}
		reloadConfig();
	}
	
    // =========================
    // Check ProtocolLib
    // =========================
	private void checkProtocolLib() {
        if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            Bukkit.getServer().getLogger().info("[MessageTones] ProtocolLib detected!");
            if (getConfig().getBoolean("msgEnabled")) {
                startProtocolLib(); 
            }
        } else {
            Bukkit.getServer().getLogger().info("[MessageTones] ProtocolLib not detected!");
            Bukkit.getServer().getLogger().info("[MessageTones] Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
	}
	
    // =========================
    // Check Metrics
    // =========================
	private void checkMetrics() {
        if (getConfig().getBoolean("metrics")) {
            try {
                MetricsLite metrics = new MetricsLite(this);
                metrics.start();
                Bukkit.getServer().getLogger().info("[MessageTones] Metrics enabled!");
            } catch (IOException e) {
                Bukkit.getServer().getLogger().info("[MessageTones] Failed to start metrics.");
                e.printStackTrace();
            }
        } else {
            Bukkit.getServer().getLogger().info("[MessageTones] Metrics Disabled.");
        }
	}
    
    // =========================
    // Check Sound Version
    // =========================
    private void checkSoundVersion() {
        if (version > 7 && version < 100) {
        	Bukkit.getServer().getLogger().info("[MessageTones] Using Minecraft 1." + version + " sounds.");
        } else {
        	Bukkit.getServer().getLogger().warning("[MessageTones] Unknown Minecraft version.");
        }
	}
      
    // =========================
    // Broadcast
    // =========================
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) throws InterruptedException {
        if (getConfig().getBoolean("broadcastEnabled") && e.getPlayer().hasPermission("messagetones.broadcast")) {
            String[] args = e.getMessage().split(" ");
            if (args.length > 1) {
                String cmd = args[0];
                if (cmd.equals("/" + getConfig().getString("broadcastCommand"))) {
                    for(Player player : Bukkit.getOnlinePlayers()){
                    	if (shouldPlaySound(player, "broadcast")) {
                    		player.playSound(player.getLocation(), soundConv.convertSound(getConfig().getInt("broadcastSound")), (float) getConfig().getDouble("broadcastVolume"), (float) getConfig().getDouble("broadcastPitch"));
                    	}
                    }
                }
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
            	if (shouldPlaySound(player, "playerjoin")) {
            		player.playSound(player.getLocation(), soundConv.convertSound(getConfig().getInt("joinOwnerSound")), (float) getConfig().getDouble("joinOwnerVolume"), (float) getConfig().getDouble("joinOwnerPitch"));
            	}
            }
            return;
        } else if (event.getPlayer().hasPermission("messagetones.admin")) {
            if (!getConfig().getBoolean("joinAdminEnabled")) {
                return;
            }
            for(Player player : Bukkit.getOnlinePlayers()){
            	if (shouldPlaySound(player, "adminjoin")) {
            		player.playSound(player.getLocation(), soundConv.convertSound(getConfig().getInt("joinAdminSound")), (float) getConfig().getDouble("joinAdminVolume"), (float) getConfig().getDouble("joinAdminPitch"));
            	}
            }
            return;
        } else {
            if (!getConfig().getBoolean("joinPlayerEnabled")) {
                return;
            }
            for(Player player : Bukkit.getOnlinePlayers()){
            	if (shouldPlaySound(player, "playerjoin")) {
            		player.playSound(player.getLocation(), soundConv.convertSound(getConfig().getInt("joinPlayerSound")), (float) getConfig().getDouble("joinPlayerVolume"), (float) getConfig().getDouble("joinPlayerPitch"));
            	}
            }
            return;
        }
    }
    
    // =========================
    // Hotbar
    // =========================
    @EventHandler
    public void hotbarSwitch(PlayerItemHeldEvent event) throws InterruptedException {
    	if (getConfig().getBoolean("hotbarEnabled")) {
	        Player player = event.getPlayer();
	        if (shouldPlaySound(player, "hotbar")) {
	            player.playSound(player.getLocation(), soundConv.convertSound(getConfig().getInt("hotbarSound")), (float) getConfig().getDouble("hotbarVolume"), (float) getConfig().getDouble("hotbarPitch"));
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
        if (cmd.getName().equalsIgnoreCase("mt")) {
            if (args.length == 0) {
                printHelp(player);
            } else {
            	if (args[0].equalsIgnoreCase("status")) {
            		printStatus(player);
            		return true;
            	}
            	else if (args[0].equalsIgnoreCase("message")) {
                    processSound(args, player, "message");
                    return true;
                } else if (args[0].equalsIgnoreCase("broadcast")) {
                    processSound(args, player, "broadcast");
                    return true;
                } else if (args[0].equalsIgnoreCase("ownerjoin")) {
                    processSound(args, player, "ownerjoin");
                    return true;
                } else if (args[0].equalsIgnoreCase("adminjoin")) {
                    processSound(args, player, "adminjoin");
                    return true;
                } else if (args[0].equalsIgnoreCase("playerjoin")) {
                    processSound(args, player, "playerjoin");
                    return true;
                } else if (args[0].equalsIgnoreCase("hotbar")) {
                    processSound(args, player, "hotbar");
                    return true;
                } else {
                	printHelp(player);
                	return true;
                }
            }
        }
        return true;
    }
 
    private void printStatus(Player player) {
    	if (getConfig().getBoolean("msgEnabled")) {
    		if (shouldPlaySound(player, "message")) {
    			player.sendMessage("Message: On");
    		} else {
    			player.sendMessage("Message: Off");
    		}
    	}
    	if (getConfig().getBoolean("broadcastEnabled")) {
    		if (shouldPlaySound(player, "hotbar")) {
    			player.sendMessage("Broadcast: On");
    		} else {
    			player.sendMessage("Broadcast: Off");
    		}
    	}
    	if (getConfig().getBoolean("joinPlayerEnabled")) {
    		if (shouldPlaySound(player, "playerjoin")) {
    			player.sendMessage("Player Join: On");
    		} else {
    			player.sendMessage("Player Join: Off");
    		}
    	}
    	if (getConfig().getBoolean("joinAdminEnabled")) {
    		if (shouldPlaySound(player, "adminjoin")) {
    			player.sendMessage("Admin Join: On");
    		} else {
    			player.sendMessage("Admin Join: Off");
    		}
    	}
    	if (getConfig().getBoolean("joinOwnerEnabled")) {
    		if (shouldPlaySound(player, "ownerjoin")) {
    			player.sendMessage("Owner Join: On");
    		} else {
    			player.sendMessage("Owner Join: Off");
    		}
    	}
    	if (getConfig().getBoolean("hotbarEnabled")) {
    		if (shouldPlaySound(player, "hotbar")) {
    			player.sendMessage("Hotbar: On");
    		} else {
    			player.sendMessage("Hotbar: Off");
    		}
    	}
	}

	// ======================
    // Auto-complete
    // ======================
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (cmd.getName().equalsIgnoreCase("mt")) {
            
            ArrayList<String> autocomplete = new ArrayList<String>();
            autocomplete.add("status");
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
    
    boolean shouldPlaySound(Player player, String sound) {
    	if (sound == "message") {
        	if(getConfig().getBoolean("msgEnabled")) {
        		if (playerData.isSet(player.getName() + "." + "PrivateMessage")) {
        			return playerData.getBoolean(player.getName() + "." + "PrivateMessage");
        		} else {
        			return getConfig().getBoolean("msgDeafultOn");
        		}
        	}
    	} else if (sound == "broadcast") {
        	if(getConfig().getBoolean("broadcastEnabled")) {
        		if (playerData.isSet(player.getName() + "." + "Broadcast")) {
        			return playerData.getBoolean(player.getName() + "." + "Broadcast");
        		} else {
        			return getConfig().getBoolean("broadcastDefaultOn");
        		}
        	}   		
    	} else if (sound == "ownerjoin") {
        	if(getConfig().getBoolean("joinOwnerEnabled")) {
        		if (playerData.isSet(player.getName() + "." + "OwnerJoin")) {
        			return playerData.getBoolean(player.getName() + "." + "OwnerJoin");
        		} else {
        			return getConfig().getBoolean("joinOwnerDeafultOn");
        		}
        	}    		
    	} else if (sound == "adminjoin") {
        	if(getConfig().getBoolean("msgEnabled")) {
        		if (playerData.isSet(player.getName() + "." + "AdminJoin")) {
        			return playerData.getBoolean(player.getName() + "." + "AdminJoin");
        		} else {
        			return getConfig().getBoolean("joinAdminDeafultOn");
        		}
        	}    		
    	} else if (sound == "playerjoin") {
        	if(getConfig().getBoolean("msgEnabled")) {
        		if (playerData.isSet(player.getName() + "." + "PlayerJoin")) {
        			return playerData.getBoolean(player.getName() + "." + "PlayerJoin");
        		} else {
        			return getConfig().getBoolean("joinPlayerDeafultOn");
        		}
        	}    		
    	} else if (sound == "hotbar") {
        	if(getConfig().getBoolean("msgEnabled")) {
        		if (playerData.isSet(player.getName() + "." + "Hotbar")) {
        			return playerData.getBoolean(player.getName() + "." + "Hotbar");
        		} else {
        			return getConfig().getBoolean("hotbarDeafultOn");
        		}
        	}    		
    	}
    	return false;
    }
    
    void printHelp(Player player) {
        List<String> list = getConfig().getStringList("help");
        List<String> convertedList = convertedLang(list);
        player.sendMessage(convertedList.toArray(new String[convertedList.size()]));
    }
    
    void processSound(String[] args, Player player, String sound) {
    	Sound soundID = null;
    	float soundVolume = (float) 0.0;
    	float soundPitch = (float) 0.0;
    	String soundMessage = null;
    	String soundOnMessage = null;
    	String soundOffMessage = null;
    	String soundData = null;
    	
    	if (sound.equalsIgnoreCase("message")) {
    		try {
				soundID = soundConv.convertSound(getConfig().getInt("msgSound"));
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			}
        	soundVolume = (float) getConfig().getDouble("msgVolume");
        	soundPitch = (float) getConfig().getDouble("msgPitch");
        	soundMessage = convertedLang("testmessage");
        	soundOnMessage = "Changed Private Message to On";
        	soundOffMessage = "Changed Private Message to Off";
        	soundData = "PrivateMessage";
    	} else if (sound.equalsIgnoreCase("broadcast")) {
    		try {
				soundID = soundConv.convertSound(getConfig().getInt("broadcastSound"));
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			}
        	soundVolume = (float) getConfig().getDouble("broadcastVolume");
        	soundPitch = (float) getConfig().getDouble("broadcastPitch");
        	soundMessage = convertedLang("testbroadcast");
        	soundOnMessage = "Changed Broadcast to On";
        	soundOffMessage = "Changed Broadcast to Off";
        	soundData = "Broadcast";
    	} else if (sound.equalsIgnoreCase("playerjoin")) {
    		try {
				soundID = soundConv.convertSound(getConfig().getInt("joinPlayerSound"));
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			}
        	soundVolume = (float) getConfig().getDouble("joinPlayerVolume");
        	soundPitch = (float) getConfig().getDouble("joinPlayerPitch");
        	soundMessage = convertedLang("testplayerjoin");
        	soundOnMessage = "Changed Player Join to On";
        	soundOffMessage = "Changed Player Join to Off";
        	soundData = "PlayerJoin";
    	} else if (sound.equalsIgnoreCase("adminjoin")) {
    		try {
				soundID = soundConv.convertSound(getConfig().getInt("joinAdminSound"));
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			}
        	soundVolume = (float) getConfig().getDouble("joinAdminVolume");
        	soundPitch = (float) getConfig().getDouble("joinAdminPitch");
        	soundMessage = convertedLang("testadminjoin");
        	soundOnMessage = "Changed Admin Join to On";
        	soundOffMessage = "Changed Admin Join to Off";
        	soundData = "AdminJoin";
    	} else if (sound.equalsIgnoreCase("ownerjoin")) {
    		try {
				soundID = soundConv.convertSound(getConfig().getInt("joinOwnerSound"));
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			}
        	soundVolume = (float) getConfig().getDouble("joinOwnerVolume");
        	soundPitch = (float) getConfig().getDouble("joinOwnerPitch");
        	soundMessage = convertedLang("testownerjoin");
        	soundOnMessage = "Changed Owner Join to On";
        	soundOffMessage = "Changed Owner Join to Off";
        	soundData = "OwnerJoin";
    	} else if (sound.equalsIgnoreCase("hotbar")) {
    		try {
				soundID = soundConv.convertSound(getConfig().getInt("hotbarSound"));
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			}
        	soundVolume = (float) getConfig().getDouble("hotbarVolume");
        	soundPitch = (float) getConfig().getDouble("hotbarPitch");
        	soundMessage = convertedLang("testhotbar");
        	soundOnMessage = "Changed Hotbar to On";
        	soundOffMessage = "Changed Hotbar to Off";
        	soundData = "Hotbar";
    	} else {
    		printHelp(player);
    		return;
    	}
    	
    	if (args.length == 1) {
            player.playSound(player.getLocation(), soundID, soundVolume, soundPitch);
            player.sendMessage(soundMessage);
    	} else {
    		if (args[1].equalsIgnoreCase("on")) {
    			playerData.set(player.getName() + "." + soundData, true);
    			player.sendMessage(soundOnMessage);
                try {
                	playerData.save(playerFile);
                } catch (IOException e1) {
                    
                    e1.printStackTrace();
                }
    		} else if (args[1].equalsIgnoreCase("off")) {
    			playerData.set(player.getName() + "." + soundData, false);
    			player.sendMessage(soundOffMessage);
                try {
                	playerData.save(playerFile);
                } catch (IOException e1) {
                    
                    e1.printStackTrace();
                }
    		} else {
    			printHelp(player);
    		}
    	}
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
                                	if (shouldPlaySound(event.getPlayer(), "message")) {
	                                    event.getPlayer().playSound(event.getPlayer().getLocation(), soundConv.convertSound(getConfig().getInt("msgSound")), (float) getConfig().getDouble("msgVolume"), (float) getConfig().getDouble("msgPitch"));
	                                    result[0] = true;
                                	}
                                }
                            }
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
						
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