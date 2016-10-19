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
    ColorConverter colorConv = new ColorConverter(getConfig());
    
    ArrayList<CustomSound> soundList = new ArrayList<CustomSound>();
    CustomSound message = null;
    CustomSound broadcast = null;
    CustomSound playerJoin = null;
    CustomSound adminJoin = null;
    CustomSound ownerJoin = null;
    CustomSound hotbar = null;
    
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
        try {
			initSounds();
		} catch (InterruptedException e) {
			Bukkit.getServer().getLogger().warning("[MessageTones] Failed to initialize sounds.");
			e.printStackTrace();
			Bukkit.getServer().getLogger().info("[MessageTones] Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
		}

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
    // Initialize Sounds
    // =========================
	private void initSounds() throws InterruptedException {
		message = new CustomSound(colorConv.convertConfig("nameMessage"), "message", soundConv.convertSound(getConfig().getInt("msgSound")), (float) getConfig().getDouble("msgPitch"), (float) getConfig().getDouble("msgVolume"), getConfig().getBoolean("msgEnabled"), getConfig().getBoolean("msgDefaultOn"), "PrivateMessage", colorConv.convertConfig("testmessage"), colorConv.convertConfig("statusMessage"), colorConv.convertConfig("toggleMessage"), colorConv.convertConfig("helpMessage"), colorConv.convertConfig("infoTestSound"), colorConv.convertConfig("infoToggleSound"));
		broadcast = new CustomSound(colorConv.convertConfig("nameBroadcast"), "broadcast", soundConv.convertSound(getConfig().getInt("broadcastSound")), (float) getConfig().getDouble("broadcastPitch"), (float) getConfig().getDouble("broadcastVolume"), getConfig().getBoolean("broadcastEnabled"), getConfig().getBoolean("broadcastDefaultOn"), "Broadcast", colorConv.convertConfig("testmessage"), colorConv.convertConfig("statusMessage"), colorConv.convertConfig("toggleMessage"), colorConv.convertConfig("helpMessage"), colorConv.convertConfig("infoTestSound"), colorConv.convertConfig("infoToggleSound"));
		playerJoin = new CustomSound(colorConv.convertConfig("namePlayerJoin"), "playerjoin", soundConv.convertSound(getConfig().getInt("joinPlayerSound")), (float) getConfig().getDouble("joinPlayerPitch"), (float) getConfig().getDouble("joinPlayerVolume"), getConfig().getBoolean("joinPlayerEnabled"), getConfig().getBoolean("playerJoinDefaultOn"), "PlayerJoin", colorConv.convertConfig("testmessage"), colorConv.convertConfig("statusMessage"), colorConv.convertConfig("toggleMessage"), colorConv.convertConfig("helpMessage"), colorConv.convertConfig("infoTestSound"), colorConv.convertConfig("infoToggleSound"));
		adminJoin = new CustomSound(colorConv.convertConfig("nameAdminJoin"), "adminjoin", soundConv.convertSound(getConfig().getInt("joinAdminSound")), (float) getConfig().getDouble("joinAdminPitch"), (float) getConfig().getDouble("joinAdminVolume"), getConfig().getBoolean("joinAdminEnabled"), getConfig().getBoolean("adminJoinDefaultOn"), "AdminJoin", colorConv.convertConfig("testmessage"), colorConv.convertConfig("statusMessage"), colorConv.convertConfig("toggleMessage"), colorConv.convertConfig("helpMessage"), colorConv.convertConfig("infoTestSound"), colorConv.convertConfig("infoToggleSound"));
		ownerJoin = new CustomSound(colorConv.convertConfig("nameOwnerJoin"), "ownerjoin", soundConv.convertSound(getConfig().getInt("joinOwnerSound")), (float) getConfig().getDouble("joinOwnerPitch"), (float) getConfig().getDouble("joinOwnerVolume"), getConfig().getBoolean("joinOwnerEnabled"), getConfig().getBoolean("ownerJoinDefaultOn"), "OwnerJoin", colorConv.convertConfig("testmessage"), colorConv.convertConfig("statusMessage"), colorConv.convertConfig("toggleMessage"), colorConv.convertConfig("helpMessage"), colorConv.convertConfig("infoTestSound"), colorConv.convertConfig("infoToggleSound"));
		hotbar = new CustomSound(colorConv.convertConfig("nameHotbar"), "hotbar", soundConv.convertSound(getConfig().getInt("hotbarSound")), (float) getConfig().getDouble("hotbarPitch"), (float) getConfig().getDouble("hotbarVolume"), getConfig().getBoolean("hotbarEnabled"), getConfig().getBoolean("hotbarDefaultOn"), "Hotbar", colorConv.convertConfig("testmessage"), colorConv.convertConfig("statusMessage"), colorConv.convertConfig("toggleMessage"), colorConv.convertConfig("helpMessage"), colorConv.convertConfig("infoTestSound"), colorConv.convertConfig("infoToggleSound"));
		soundList.add(message);
		soundList.add(broadcast);
		soundList.add(playerJoin);
		soundList.add(adminJoin);
		soundList.add(ownerJoin);
		soundList.add(hotbar);
	}
      
    // =========================
    // Broadcast
    // =========================
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) throws InterruptedException {
        if (broadcast.isEnabled() && e.getPlayer().hasPermission("messagetones.broadcast")) {
            String[] args = e.getMessage().split(" ");
            if (args.length > 1) {
                String cmd = args[0];
                if (cmd.equals("/" + getConfig().getString("broadcastCommand"))) {
                    for (Player player: Bukkit.getOnlinePlayers()) {
                        if (shouldPlaySound(player, broadcast)) {
                            broadcast.playSound(player);
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
        if (ownerJoin.isEnabled()) {
            if (event.getPlayer().hasPermission("messagetones.owner")) {
                for (Player player: Bukkit.getOnlinePlayers()) {
                    if (shouldPlaySound(player, ownerJoin)) {
                    	new java.util.Timer().schedule( 
                    	        new java.util.TimerTask() {
                    	            @Override
                    	            public void run() {
                                        ownerJoin.playSound(player);
                    	            }
                    	        }, 
                    	        1000 
                    	);
                    }
                }
                return;
            }
        }
        if (adminJoin.isEnabled()) {
            if (event.getPlayer().hasPermission("messagetones.admin")) {
                for (Player player: Bukkit.getOnlinePlayers()) {
                    if (shouldPlaySound(player, adminJoin)) {
                    	new java.util.Timer().schedule( 
                    	        new java.util.TimerTask() {
                    	            @Override
                    	            public void run() {
                    	            	adminJoin.playSound(player);
                    	            }
                    	        }, 
                    	        1000 
                    	);
                    }
                }
                return;
            }
        }
        if (playerJoin.isEnabled()) {
            for (Player player: Bukkit.getOnlinePlayers()) {
                if (shouldPlaySound(player, playerJoin)) {
                	new java.util.Timer().schedule( 
                	        new java.util.TimerTask() {
                	            @Override
                	            public void run() {
                                    playerJoin.playSound(player);
                	            }
                	        }, 
                	        1000 
                	);
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
    	if (hotbar.isEnabled()) {
	        Player player = event.getPlayer();
	        if (shouldPlaySound(player, hotbar)) {
	            hotbar.playSound(player);
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
            sender.sendMessage(colorConv.convertConfig("noconsole"));
            return true;
        }
        Player player = (Player) sender;
        // ======================
        // Player
        // ======================
        if (cmd.getName().equalsIgnoreCase("mt")) {
            if (args.length == 0) {
                printHelp(player);
            } else if (args.length == 1) {
            	boolean playedSound = false;
            	for (CustomSound sound : soundList) {
            		if (sound.isEnabled()) {
                		if (args[0].equalsIgnoreCase(sound.getCommandName())) {
                			sound.playSound(player);
                			playedSound = true;
                			sound.printTestMessage(player);
                		}
            		}
            	}
            	if (playedSound != true) {
            		if (args[0].equalsIgnoreCase("status")) {
            			printStatus(player);
            		} else {
                		printHelp(player);
            		}
            	}
            } else if (args.length == 2) {
            	boolean toggledSound = false;
            	for (CustomSound sound : soundList) {
            		if (sound.isEnabled()) {
                		if (args[0].equalsIgnoreCase(sound.getCommandName())) {
                    		if (args[1].equalsIgnoreCase("on")) {
                    			if (player.hasPermission(sound.getTogglePerm())) {
                        			playerData.set(player.getName() + "." + sound.getDataName(), true);
                        			sound.printToggleMessage(player, colorConv.convert(getConfig().getString("statusOn")));
                                    try {
                                    	playerData.save(playerFile);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                    			} else {
                    				player.sendMessage(colorConv.convertConfig("notpermitted"));
                    			}
                    			toggledSound = true;
                    		} else if (args[1].equalsIgnoreCase("off")) {
                    			if (player.hasPermission(sound.getTogglePerm())) {
                        			playerData.set(player.getName() + "." + sound.getDataName(), false);
                        			sound.printToggleMessage(player, colorConv.convert(getConfig().getString("statusOff")));
                                    try {
                                    	playerData.save(playerFile);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                    			} else {
                    				player.sendMessage(colorConv.convertConfig("notpermitted"));
                    			}
                    			toggledSound = true;
                    		}
                		}
            		}
            	}
            	if (toggledSound != true) {
            		printHelp(player);
            	}
            } else {
            	printHelp(player);
            }
        }
        return true;
    }
 
    // =========================
    // Helper Functions
    // =========================
    private void printStatus(Player player) {
    	player.sendMessage(colorConv.convertConfig("statusHeader"));
    	for (CustomSound sound : soundList) {
    		if (sound.isEnabled()) {
    			if (shouldPlaySound(player, sound)) {
        			sound.printStatusMessage(player, colorConv.convert(getConfig().getString("statusOn")));
    			} else {
        			sound.printStatusMessage(player, colorConv.convert(getConfig().getString("statusOff")));
    			}
    		}
    	}
    	player.sendMessage(colorConv.convertConfig("statusFooter"));
	}
    
    boolean shouldPlaySound(Player player, CustomSound sound) {
		if (sound.isEnabled()) {
			if (playerData.isSet(player.getName() + "." + sound.getDataName())) {
				return playerData.getBoolean(player.getName() + "." + sound.getDataName());
			} else {
				return sound.isDefaultOn();
			}
		}
		return false;
    }
    
    void printHelp(Player player) {
    	player.sendMessage(colorConv.convertConfig("helpHeader"));
    	player.sendMessage(colorConv.convertConfig("helpMessage").replace("{COMMAND}", "/mt status").replace("{INFO}", colorConv.convertConfig("infoStatus")));
    	for (CustomSound sound : soundList) {
    		if (sound.isEnabled()) {
    			sound.printHelpTestMessage(player);
        		if (player.hasPermission(sound.getTogglePerm())) {
        			sound.printHelpToggleMessage(player);
        		}
    		}
    	}
    	player.sendMessage(colorConv.convertConfig("helpFooter"));
    }

	// ======================
    // Auto-complete
    // ======================
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (cmd.getName().equalsIgnoreCase("mt")) {
            
            ArrayList<String> autocomplete = new ArrayList<String>();
            autocomplete.add("status");
            for (CustomSound sound : soundList) {
            	if (sound.isEnabled()) {
            		autocomplete.add(sound.getCommandName());
            	}
            }
   
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
                                	if (shouldPlaySound(event.getPlayer(), message)) {
	                                    message.playSound(event.getPlayer());
	                                    result[0] = true;
                                	}
                                }
                            }
                        }
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