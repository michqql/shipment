package me.michqql.shipmentplugin.commands;

import me.michqql.shipmentplugin.ShipmentPlugin;
import me.michqql.shipmentplugin.gui.guis.admin.MainOverviewGUI;
import me.michqql.shipmentplugin.preset.PresetHandler;
import me.michqql.shipmentplugin.schematic.SchematicHandler;
import me.michqql.shipmentplugin.shipment.ShipmentManager;
import me.michqql.shipmentplugin.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class ShipmentCommand implements CommandExecutor {

    private final ShipmentPlugin plugin;
    private final SchematicHandler schematicHandler;
    private final ShipmentManager shipmentManager;
    private final PresetHandler presetHandler;

    private final MessageUtil msg;

    public ShipmentCommand(ShipmentPlugin plugin, MessageUtil messageUtil, SchematicHandler schematicHandler,
                           ShipmentManager shipmentManager, PresetHandler presetHandler) {

        this.plugin = plugin;
        this.schematicHandler = schematicHandler;
        this.shipmentManager = shipmentManager;
        this.presetHandler = presetHandler;

        this.msg = messageUtil;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean isPlayer = sender instanceof Player;
        if(!sender.hasPermission("shipment.admin")) {
            msg.sendList(sender, "no-permission", new HashMap<String, String>(){{
                put("permission", "shipment.admin");
            }});
            return true;
        }

        if(args.length == 0) {
            if(!isPlayer) {
                Bukkit.getLogger().warning("[Shipment] You must be a player to open the admin GUI");
            } else {
                new MainOverviewGUI(plugin, (Player) sender, msg, shipmentManager, presetHandler).openGUI();
            }
            return true;
        }

        String subCommand = args[0];

        if(subCommand.equalsIgnoreCase("help")) {
            msg.sendList(sender, "admin-help-command");
        }
        else if(subCommand.equalsIgnoreCase("reload")) {
            long now = System.currentTimeMillis();
            plugin.reload();

            long timeTaken = System.currentTimeMillis() - now;
            msg.sendList(sender, "reload-command", new HashMap<String, String>(){{
                put("ms", String.valueOf(timeTaken));
            }});
        }
        else if(subCommand.equalsIgnoreCase("save")) {
            long now = System.currentTimeMillis();
            plugin.saveData();

            long timeTaken = System.currentTimeMillis() - now;
            msg.sendList(sender, "save-command", new HashMap<String, String>(){{
                put("ms", String.valueOf(timeTaken));
            }});
        }
        else if(subCommand.equalsIgnoreCase("bounds")) {
            if(!isPlayer) {
                Bukkit.getLogger().warning("[Shipment] You must be a player to view bounds");
            } else {
                boolean enabled = schematicHandler.toggleViewingSchematicBoundaries((Player) sender);

                msg.sendList(sender, "bounds-command", new HashMap<String, String>() {{
                    put("enabled", enabled ? "enabled" : "disabled");
                }});
            }
        }
        else if(subCommand.equalsIgnoreCase("force")) {
            // shipment force start
            if(args.length < 2) {
                msg.sendList(sender, "missing-argument", new HashMap<String, String>(){{
                    put("usage", "shipment force <start|stop>");
                }});
                return true;
            }

            String arg1 = args[1];
            if(!(arg1.equalsIgnoreCase("start") || arg1.equalsIgnoreCase("stop"))) {
                msg.sendList(sender, "missing-argument", new HashMap<String, String>(){{
                    put("usage", "shipment force <start|stop>");
                }});
                return true;
            }

            boolean start = arg1.equalsIgnoreCase("start");
            shipmentManager.forceShipment(start);
            msg.sendList(sender, "force-command", new HashMap<String, String>(){{
                put("started", start ? "started" : "ended");
            }});
        }

        return true;
    }
}
