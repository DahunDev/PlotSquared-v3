package com.intellectualcrafters.plot.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;

import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.config.Settings;
import com.intellectualcrafters.plot.database.DBFunc;
import com.intellectualcrafters.plot.object.Expression;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RunnableVal;
import com.intellectualcrafters.plot.util.ByteArrayUtilities;
import com.intellectualcrafters.plot.util.EconHandler;
import com.intellectualcrafters.plot.util.Permissions;
import com.intellectualcrafters.plot.util.TaskManager;
import com.plotsquared.general.commands.CommandDeclaration;

@CommandDeclaration(command = "claim",
        aliases = "c",
        description = "Claim the current plot you're standing on",
        category = CommandCategory.CLAIMING,
        requiredType = RequiredType.NONE,
        permission = "plots.claim", usage = "/plot claim")
public class Claim extends SubCommand {

    @Override
    public boolean onCommand(final PlotPlayer player, String[] args) {
        String schematic = "";
        if (args.length >= 1) {
            schematic = args[0];
        }
        Location loc = player.getLocation();
        final Plot plot = loc.getPlotAbs();
        if (plot == null) {
            return sendMessage(player, C.NOT_IN_PLOT);
        }
        

		if (plot != null) {

			com.intellectualcrafters.plot.object.Location locs = plot.getCenter();

			World w = Bukkit.getWorld(plot.getWorldName());
			double radius = w.getWorldBorder().getSize();
			org.bukkit.Location center = w.getWorldBorder().getCenter();


			if (locs.getX() <= (center.getX() - radius + 1000) || locs.getX() >= (center.getX() + radius - 1000)
					|| locs.getZ() <= (center.getZ() - radius + 1000) || locs.getZ() >= (center.getZ() + radius - 1000)) {

				player.sendMessage("§b§l[ §f§lKite SV §b§l] §c세계 경계 밖입니다. 좌표: x:" + locs.getX() + " z:" + locs.getZ());

				return false;

			}

		}
        
        
        
        int currentPlots = Settings.Limit.GLOBAL ? player.getPlotCount() : player.getPlotCount(loc.getWorld());
        int grants = 0;
        if (currentPlots >= player.getAllowedPlots()) {
            if (player.hasPersistentMeta("grantedPlots")) {
                grants = ByteArrayUtilities.bytesToInteger(player.getPersistentMeta("grantedPlots"));
                if (grants <= 0) {
                    player.removePersistentMeta("grantedPlots");
                    return sendMessage(player, C.CANT_CLAIM_MORE_PLOTS);
                }
            } else {
                return sendMessage(player, C.CANT_CLAIM_MORE_PLOTS);
            }
        }
        if (!plot.canClaim(player)) {
            return sendMessage(player, C.PLOT_IS_CLAIMED);
        }
        final PlotArea area = plot.getArea();
        if (!schematic.isEmpty()) {
            if (area.SCHEMATIC_CLAIM_SPECIFY) {
                if (!area.SCHEMATICS.contains(schematic.toLowerCase())) {
                    return sendMessage(player, C.SCHEMATIC_INVALID, "non-existent: " + schematic);
                }
                if (!Permissions.hasPermission(player, C.PERMISSION_CLAIM_SCHEMATIC.f(schematic)) && !Permissions.hasPermission(player, C.PERMISSION_ADMIN_COMMAND_SCHEMATIC)) {
                    return sendMessage(player, C.NO_SCHEMATIC_PERMISSION, schematic);
                }
            }
        }
        int border = area.getBorder();
        if (border != Integer.MAX_VALUE && plot.getDistanceFromOrigin() > border) {
            return !sendMessage(player, C.BORDER);
        }
        if ((EconHandler.getEconHandler() != null) && area.USE_ECONOMY) {
            Expression<Double> costExr = area.PRICES.get("claim");
            double cost = costExr.evaluate((double) currentPlots);
            if (cost > 0d) {
                if (EconHandler.getEconHandler().getMoney(player) < cost) {
                    return sendMessage(player, C.CANNOT_AFFORD_PLOT, "" + cost);
                }
                EconHandler.getEconHandler().withdrawMoney(player, cost);
                sendMessage(player, C.REMOVED_BALANCE, cost + "");
            }
        }
        if (grants > 0) {
            if (grants == 1) {
                player.removePersistentMeta("grantedPlots");
            } else {
                player.setPersistentMeta("grantedPlots", ByteArrayUtilities.integerToBytes(grants - 1));
            }
            sendMessage(player, C.REMOVED_GRANTED_PLOT, "1", "" + (grants - 1));
        }
        if (plot.canClaim(player)) {
            plot.owner = player.getUUID();
            final String finalSchematic = schematic;
            DBFunc.createPlotSafe(plot, new Runnable() {
                @Override
                public void run() {
                    TaskManager.IMP.sync(new RunnableVal<Object>() {
                        @Override
                        public void run(Object value) {
                            plot.claim(player, Settings.Claim.TELEPORT_ON_CLAIM, finalSchematic, false);
                            if (area.AUTO_MERGE) {
                                plot.autoMerge(-1, Integer.MAX_VALUE, player.getUUID(), true);
                            }
                        }
                    });
                }
            }, new Runnable() {
                @Override
                public void run() {
                    sendMessage(player, C.PLOT_NOT_CLAIMED);
                    if ((EconHandler.getEconHandler() != null) && area.USE_ECONOMY) {
                        Expression<Double> costExr = area.PRICES.get("claim");
                        double cost = costExr.evaluate((double) currentPlots);
                        if (cost > 0d) {
                            EconHandler.getEconHandler().depositMoney(player, cost);
                            sendMessage(player, C.ADDED_BALANCE, cost + "");
                        }
                    }
                    
                }
            });
            return true;
        } else {
            sendMessage(player, C.PLOT_NOT_CLAIMED);
            if ((EconHandler.getEconHandler() != null) && area.USE_ECONOMY) {
                Expression<Double> costExr = area.PRICES.get("claim");
                double cost = costExr.evaluate((double) currentPlots);
                if (cost > 0d) {
                    EconHandler.getEconHandler().depositMoney(player, cost);
                    sendMessage(player, C.ADDED_BALANCE, cost + "");
                }
            }
            
        }
        return false;
    }
}
