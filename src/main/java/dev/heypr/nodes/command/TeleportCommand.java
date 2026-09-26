package dev.heypr.nodes.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.heypr.nodes.Message;
import dev.heypr.nodes.Node;
import dev.heypr.nodes.NodeManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class TeleportCommand extends Subcommand {

    public TeleportCommand(NodeManager manager) {
        super(manager);
    }

    @Override
    public List<String> help() {
        return List.of("&f/nodes tp <name> &8- teleport to a node");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("tp")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests(nodeIds())
                        .executes(this::teleport));
    }

    private int teleport(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Message.prefixed("&cPlayers only."));
            return Command.SINGLE_SUCCESS;
        }

        String id = StringArgumentType.getString(ctx, "id");
        Node node = manager.get(id);
        if (node == null) {
            sender.sendMessage(Message.prefixed("&cNo node named &f" + id + "&c."));
            return Command.SINGLE_SUCCESS;
        }

        Location center = node.getCenter();
        if (center == null) {
            sender.sendMessage(Message.prefixed("&cWorld &f" + node.getWorldName() + " &cisn't loaded."));
            return Command.SINGLE_SUCCESS;
        }

        Location destination = center.clone().add(0, 1, 0);
        destination.setDirection(player.getLocation().getDirection());
        player.teleportAsync(destination);
        sender.sendMessage(Message.prefixed("&aTeleported to &f" + id + "&a."));
        return Command.SINGLE_SUCCESS;
    }
}
