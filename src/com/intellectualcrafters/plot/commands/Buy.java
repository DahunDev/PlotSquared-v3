package com.intellectualcrafters.plot.commands;

import com.google.common.base.Optional;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.flag.Flags;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RunnableVal2;
import com.intellectualcrafters.plot.object.RunnableVal3;
import com.intellectualcrafters.plot.util.EconHandler;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.UUIDHandler;
import com.plotsquared.general.commands.Command;
import com.plotsquared.general.commands.CommandDeclaration;

import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;

@CommandDeclaration(
        command = "buy",
        description = "Buy the plot you are standing on",
        usage = "/plot buy",
        permission = "plots.buy",
        category = CommandCategory.CLAIMING,
        requiredType = RequiredType.NONE)
public class Buy extends Command {

    public Buy() {
        super(MainCommand.getInstance(), true);
    }
 
    @Override
    public void execute(final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, final RunnableVal2<Command, CommandResult> whenDone) {
        check(EconHandler.getEconHandler(), C.ECON_DISABLED);
        final Plot plot;
        if (args.length != 0) {
            checkTrue(args.length == 1, C.COMMAND_SYNTAX, getUsage());
            plot = check(MainUtil.getPlotFromString(player, args[0], true), null);
        } else {
            plot = check(player.getCurrentPlot(), C.NOT_IN_PLOT);
        }
        checkTrue(plot.hasOwner(), C.PLOT_UNOWNED);
        checkTrue(!plot.isOwner(player.getUUID()), C.CANNOT_BUY_OWN);
        Set<Plot> plots = plot.getConnectedPlots();
        checkTrue(player.getPlotCount() + plots.size() <= player.getAllowedPlots(), C.CANT_CLAIM_MORE_PLOTS);
        
        

		if (plot != null) {

			com.intellectualcrafters.plot.object.Location locs = plot.getCenter();

			World w = Bukkit.getWorld(plot.getWorldName());
			double radius = w.getWorldBorder().getSize();
			org.bukkit.Location center = w.getWorldBorder().getCenter();


			if (locs.getX() <= (center.getX() - radius + 1000) || locs.getX() >= (center.getX() + radius - 1000)
					|| locs.getZ() <= (center.getZ() - radius + 1000) || locs.getZ() >= (center.getZ() + radius - 1000)) {

				player.sendMessage("§b§l[ §f§lKite SV §b§l] §c세계 경계 밖입니다. 좌표: x:" + locs.getX() + " z:" + locs.getZ());

				return;

			}

		}
        
        
        
        
        
        Optional<Double> flag = plot.getFlag(Flags.PRICE);
        if (!flag.isPresent()) {
            throw new CommandException(C.NOT_FOR_SALE);
        }
        final double price = flag.get();
        checkTrue(player.getMoney() >= price, C.CANNOT_AFFORD_PLOT, "" + price); 
   
        player.withdraw(price);
        confirm.run(this, new Runnable() {
            @Override // Success
            public void run() {
                C.REMOVED_BALANCE.send(player, price);
                EconHandler.getEconHandler().depositMoney(UUIDHandler.getUUIDWrapper().getOfflinePlayer(plot.owner), price);
                PlotPlayer owner = UUIDHandler.getPlayer(plot.owner);
                if (owner != null) {
                    C.PLOT_SOLD.send(owner, plot.getId(), player.getName(), price);
                    System.out.println("주인이 " + owner.getName() + "님인 플롯 " + plot.toString() + "이 판매됬습니다.");
                }
                plot.removeFlag(Flags.PRICE);
                plot.setOwner(player.getUUID());
                plot.setSign(player.getName());
                
                /*
                
                for (UUID uuid : plot.getTrusted()) {
                    plot.removeTrusted(uuid);
                }
                for (UUID uuid : plot.getMembers()) {
                    plot.removeMember(uuid);
                }
                for (UUID uuid : plot.getDenied()) {
                    plot.removeDenied(uuid);
                }
                
                */
                
                
                C.CLAIMED.send(player);
                System.out.println(player.getName() + "님이 판매중인 " + plot.toString() + "을 " + price +"원에 구매함" );
                whenDone.run(Buy.this, CommandResult.SUCCESS);
            }
        }, new Runnable() {
            @Override // Failure
            public void run() {
                player.deposit(price);
              MainUtil.sendMessage(player, C.ADDED_BALANCE, price + "");

                whenDone.run(Buy.this, CommandResult.FAILURE);
            }
        });
    }
}
