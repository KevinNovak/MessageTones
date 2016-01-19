package me.kevinnovak.messagetones;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
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
    
    // =========================
    // ProtocolLib
    // =========================
    private interface Processor {
        public Object process(Object value, Object parent);
    }
    // =========================
    // Enable
    // =========================
    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            Bukkit.getServer().getLogger().info("[MessageTones] ProtocolLib Detected!");
            if (getConfig().getBoolean("msgEnabled")) {
                startProtocolLib(); 
            }
        } else {
            Bukkit.getServer().getLogger().info("[MessageTones] ProtocolLib Not Detected!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saveDefaultConfig();
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
        Bukkit.getServer().getLogger().info("[MessageTones] Plugin Disabled!");
    }
    
    // =========================
    // ProtocolLib
    // =========================
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
                                            event.getPlayer().playSound(event.getPlayer().getLocation(), convertSound(getConfig().getInt("msgSound")), (float) getConfig().getDouble("msgVolume"), (float) getConfig().getDouble("msgPitch"));
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
                player.playSound(player.getLocation(), convertSound(getConfig().getInt("broadcastSound")), (float) getConfig().getDouble("broadcastVolume"), (float) getConfig().getDouble("broadcastPitch"));
            }
        }
    }

    // =========================
    // Convert String in Config
    // =========================
    String convertedLang(String toConvert) {
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString(toConvert));
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
            player.playSound(player.getLocation(), convertSound(getConfig().getInt("msgSound")), (float) getConfig().getDouble("msgVolume"), (float) getConfig().getDouble("msgPitch"));
        }

        return true;
    }

    // ======================
    // Play Sound
    // ======================
    Sound convertSound(int soundID) {
        Sound sound = Sound.NOTE_PLING;
        switch(soundID) {
        case 1: sound = Sound.AMBIENCE_CAVE;
                break;
        case 2: sound = Sound.AMBIENCE_RAIN;
                break;
        case 3: sound = Sound.AMBIENCE_THUNDER;
                break;
        case 4: sound = Sound.ANVIL_BREAK;
                break;
        case 5: sound = Sound.ANVIL_LAND;
                break;
        case 6: sound = Sound.ANVIL_USE;
                break;
        case 7: sound = Sound.ARROW_HIT;
                break;
        case 8: sound = Sound.BAT_DEATH;
                break;
        case 9: sound = Sound.BAT_HURT;
                break;
        case 10: sound = Sound.BAT_IDLE;
                break;
        case 11: sound = Sound.BAT_LOOP;
                break;
        case 12: sound = Sound.BAT_TAKEOFF;
                break;
        case 13: sound = Sound.BLAZE_BREATH;
                break;
        case 14: sound = Sound.BLAZE_DEATH;
                break;
        case 15: sound = Sound.BLAZE_HIT;
                break;
        case 16: sound = Sound.BURP;
                break;
        case 17: sound = Sound.CAT_HISS;
                break;
        case 18: sound = Sound.CAT_HIT;
                break;
        case 19: sound = Sound.CAT_MEOW;
                break;
        case 20: sound = Sound.CAT_PURR;
                break;
        case 21: sound = Sound.CAT_PURREOW;
                break;
        case 22: sound = Sound.CHEST_CLOSE;
                break;
        case 23: sound = Sound.CHEST_OPEN;
                break;
        case 24: sound = Sound.CHICKEN_EGG_POP;
                break;
        case 25: sound = Sound.CHICKEN_HURT;
                break;
        case 26: sound = Sound.CHICKEN_IDLE;
                break;
        case 27: sound = Sound.CHICKEN_WALK;
                break;
        case 28: sound = Sound.CLICK;
                break;
        case 29: sound = Sound.COW_HURT;
                break;
        case 30: sound = Sound.COW_IDLE;
                break;
        case 31: sound = Sound.COW_WALK;
                break;
        case 32: sound = Sound.CREEPER_DEATH;
                break;
        case 33: sound = Sound.CREEPER_HISS;
                break;
        case 34: sound = Sound.DIG_GRASS;
                break;
        case 35: sound = Sound.DIG_GRAVEL;
                break;
        case 36: sound = Sound.DIG_SAND;
                break;
        case 37: sound = Sound.DIG_SNOW;
                break;
        case 38: sound = Sound.DIG_STONE;
                break;
        case 39: sound = Sound.DIG_WOOD;
                break;
        case 40: sound = Sound.DIG_WOOL;
                break;
        case 41: sound = Sound.DONKEY_ANGRY;
                break;
        case 42: sound = Sound.DONKEY_DEATH;
                break;
        case 43: sound = Sound.DONKEY_HIT;
                break;
        case 44: sound = Sound.DONKEY_IDLE;
                break;
        case 45: sound = Sound.DOOR_CLOSE;
                break;
        case 46: sound = Sound.DOOR_OPEN;
                break;
        case 47: sound = Sound.DRINK;
                break;
        case 48: sound = Sound.EAT;
                break;
        case 49: sound = Sound.ENDERDRAGON_DEATH;
                break;
        case 50: sound = Sound.ENDERDRAGON_GROWL;
                break;
        case 51: sound = Sound.ENDERDRAGON_HIT;
                break;
        case 52: sound = Sound.ENDERDRAGON_WINGS;
                break;
        case 53: sound = Sound.ENDERMAN_DEATH;
                break;
        case 54: sound = Sound.ENDERMAN_HIT;
                break;
        case 55: sound = Sound.ENDERMAN_IDLE;
                break;
        case 56: sound = Sound.ENDERMAN_SCREAM;
                break;
        case 57: sound = Sound.ENDERMAN_STARE;
                break;
        case 58: sound = Sound.ENDERMAN_TELEPORT;
                break;
        case 59: sound = Sound.EXPLODE;
                break;
        case 60: sound = Sound.FALL_BIG;
                break;
        case 61: sound = Sound.FALL_SMALL;
                break;
        case 62: sound = Sound.FIRE;
                break;
        case 63: sound = Sound.FIRE_IGNITE;
                break;
        case 64: sound = Sound.FIREWORK_BLAST;
                break;
        case 65: sound = Sound.FIREWORK_BLAST2;
                break;
        case 66: sound = Sound.FIREWORK_LARGE_BLAST;
                break;
        case 67: sound = Sound.FIREWORK_LARGE_BLAST2;
                break;
        case 68: sound = Sound.FIREWORK_LAUNCH;
                break;
        case 69: sound = Sound.FIREWORK_TWINKLE;
                break;
        case 70: sound = Sound.FIREWORK_TWINKLE2;
                break;
        case 71: sound = Sound.FIZZ;
                break;
        case 72: sound = Sound.FUSE;
                break;
        case 73: sound = Sound.GHAST_CHARGE;
                break;
        case 74: sound = Sound.GHAST_DEATH;
                break;
        case 75: sound = Sound.GHAST_FIREBALL;
                break;
        case 76: sound = Sound.GHAST_MOAN;
                break;
        case 77: sound = Sound.GHAST_SCREAM;
                break;
        case 78: sound = Sound.GHAST_SCREAM2;
                break;
        case 79: sound = Sound.GLASS;
                break;
        case 80: sound = Sound.HORSE_ANGRY;
                break;
        case 81: sound = Sound.HORSE_ANGRY;
                break;
        case 82: sound = Sound.HORSE_BREATHE;
                break;
        case 83: sound = Sound.HORSE_DEATH;
                break;
        case 84: sound = Sound.HORSE_GALLOP;
                break;
        case 85: sound = Sound.HORSE_HIT;
                break;
        case 86: sound = Sound.HORSE_IDLE;
                break;
        case 87: sound = Sound.HORSE_JUMP;
                break;
        case 88: sound = Sound.HORSE_LAND;
                break;
        case 89: sound = Sound.HORSE_SADDLE;
                break;
        case 90: sound = Sound.HORSE_SKELETON_DEATH;
                break;
        case 91: sound = Sound.HORSE_SKELETON_HIT;
                break;
        case 92: sound = Sound.HORSE_SKELETON_IDLE;
                break;
        case 93: sound = Sound.HORSE_SOFT;
                break;
        case 94: sound = Sound.HORSE_WOOD;
                break;
        case 95: sound = Sound.HORSE_ZOMBIE_DEATH;
                break;
        case 96: sound = Sound.HORSE_ZOMBIE_HIT;
                break;
        case 97: sound = Sound.HORSE_ZOMBIE_IDLE;
                break;
        case 98: sound = Sound.HURT_FLESH;
                break;
        case 99: sound = Sound.IRONGOLEM_DEATH;
                break;
        case 100: sound = Sound.IRONGOLEM_HIT;
                break;
        case 101: sound = Sound.IRONGOLEM_THROW;
                break;
        case 102: sound = Sound.IRONGOLEM_WALK;
                break;
        case 103: sound = Sound.ITEM_BREAK;
                break;
        case 104: sound = Sound.ITEM_PICKUP;
                break;
        case 105: sound = Sound.LAVA;
                break;
        case 106: sound = Sound.LAVA_POP;
                break;
        case 107: sound = Sound.LEVEL_UP;
                break;
        case 108: sound = Sound.MAGMACUBE_JUMP;
                break;
        case 109: sound = Sound.MAGMACUBE_WALK;
                break;
        case 110: sound = Sound.MAGMACUBE_WALK2;
                break;
        case 111: sound = Sound.MINECART_BASE;
                break;
        case 112: sound = Sound.MINECART_INSIDE;
                break;
        case 113: sound = Sound.NOTE_BASS;
                break;
        case 114: sound = Sound.NOTE_BASS_DRUM;
                break;
        case 115: sound = Sound.NOTE_BASS_GUITAR;
                break;
        case 116: sound = Sound.NOTE_PIANO;
                break;
        case 117: sound = Sound.NOTE_PLING;
                break;
        case 118: sound = Sound.NOTE_SNARE_DRUM;
                break;
        case 119: sound = Sound.NOTE_STICKS;
                break;
        case 120: sound = Sound.ORB_PICKUP;
                break;
        case 121: sound = Sound.PIG_DEATH;
                break;
        case 122: sound = Sound.PIG_IDLE;
                break;
        case 123: sound = Sound.PIG_WALK;
                break;
        case 124: sound = Sound.PISTON_EXTEND;
                break;
        case 125: sound = Sound.PISTON_RETRACT;
                break;
        case 126: sound = Sound.PISTON_RETRACT;
                break;
        case 127: sound = Sound.PORTAL;
                break;
        case 128: sound = Sound.PORTAL_TRAVEL;
                break;
        case 129: sound = Sound.PORTAL_TRIGGER;
                break;
        case 130: sound = Sound.SHEEP_IDLE;
                break;
        case 131: sound = Sound.SHEEP_SHEAR;
                break;
        case 132: sound = Sound.SHEEP_WALK;
                break;
        case 133: sound = Sound.SHOOT_ARROW;
                break;
        case 134: sound = Sound.SILVERFISH_HIT;
                break;
        case 135: sound = Sound.SILVERFISH_IDLE;
                break;
        case 136: sound = Sound.SILVERFISH_KILL;
                break;
        case 137: sound = Sound.SILVERFISH_WALK;
                break;
        case 138: sound = Sound.SKELETON_DEATH;
                break;
        case 139: sound = Sound.SKELETON_HURT;
                break;
        case 140: sound = Sound.SKELETON_IDLE;
                break;
        case 141: sound = Sound.SKELETON_WALK;
                break;
        case 142: sound = Sound.SLIME_ATTACK;
                break;
        case 143: sound = Sound.SLIME_WALK;
                break;
        case 144: sound = Sound.SLIME_WALK2;
                break;
        case 145: sound = Sound.SPIDER_DEATH;
                break;
        case 146: sound = Sound.SPIDER_IDLE;
                break;
        case 147: sound = Sound.SPIDER_WALK;
                break;
        case 148: sound = Sound.SPLASH;
                break;
        case 149: sound = Sound.SPLASH2;
                break;
        case 150: sound = Sound.STEP_GRASS;
                break;
        case 151: sound = Sound.STEP_GRAVEL;
                break;
        case 152: sound = Sound.STEP_LADDER;
                break;
        case 153: sound = Sound.STEP_SAND;
                break;
        case 154: sound = Sound.STEP_SNOW;
                break;
        case 155: sound = Sound.STEP_STONE;
                break;
        case 156: sound = Sound.STEP_WOOD;
                break;
        case 157: sound = Sound.STEP_WOOL;
                break;
        case 158: sound = Sound.SUCCESSFUL_HIT;
                break;
        case 159: sound = Sound.SWIM;
                break;
        case 160: sound = Sound.VILLAGER_DEATH;
                break;
        case 161: sound = Sound.VILLAGER_HAGGLE;
                break;
        case 162: sound = Sound.VILLAGER_HIT;
                break;
        case 163: sound = Sound.VILLAGER_IDLE;
                break;
        case 164: sound = Sound.VILLAGER_NO;
                break;
        case 165: sound = Sound.VILLAGER_YES;
                break;
        case 166: sound = Sound.WATER;
                break;
        case 167: sound = Sound.WITHER_DEATH;
                break;
        case 168: sound = Sound.WITHER_HURT;
                break;
        case 169: sound = Sound.WITHER_IDLE;
                break;
        case 170: sound = Sound.WITHER_SHOOT;
                break;
        case 171: sound = Sound.WITHER_SPAWN;
                break;
        case 172: sound = Sound.WOLF_BARK;
                break;
        case 173: sound = Sound.WOLF_DEATH;
                break;
        case 174: sound = Sound.WOLF_GROWL;
                break;
        case 175: sound = Sound.WOLF_HOWL;
                break;
        case 176: sound = Sound.WOLF_HURT;
                break;
        case 177: sound = Sound.WOLF_PANT;
                break;
        case 178: sound = Sound.WOLF_SHAKE;
                break;
        case 179: sound = Sound.WOLF_WALK;
                break;
        case 180: sound = Sound.WOLF_WHINE;
                break;
        case 181: sound = Sound.WOOD_CLICK;
                break;
        case 182: sound = Sound.ZOMBIE_DEATH;
                break;
        case 183: sound = Sound.ZOMBIE_HURT;
                break;
        case 184: sound = Sound.ZOMBIE_IDLE;
                break;
        case 185: sound = Sound.ZOMBIE_INFECT;
                break;
        case 186: sound = Sound.ZOMBIE_METAL;
                break;
        case 187: sound = Sound.ZOMBIE_PIG_ANGRY;
                break;
        case 188: sound = Sound.ZOMBIE_PIG_DEATH;
                break;
        case 189: sound = Sound.ZOMBIE_PIG_HURT;
                break;
        case 190: sound = Sound.ZOMBIE_PIG_IDLE;
                break;
        case 191: sound = Sound.ZOMBIE_REMEDY;
                break;
        case 192: sound = Sound.ZOMBIE_UNFECT;
                break;
        case 193: sound = Sound.ZOMBIE_WALK;
                break;
        case 194: sound = Sound.ZOMBIE_WOOD;
                break;
        case 195: sound = Sound.ZOMBIE_WOODBREAK;
                break;
        default: sound = Sound.NOTE_PLING;
                break;
        }
        return sound;
    } 
}