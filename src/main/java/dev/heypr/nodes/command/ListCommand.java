package dev.heypr.nodes.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.heypr.nodes.Message;
import dev.heypr.nodes.Node;
import dev.heypr.nodes.NodeManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public class ListCommand extends Subcommand {

    public ListCommand(NodeManager manager) {
        super(manager);
    }

    @Override
    public List<String> help() {
        return List.of("&f/nodes list &8- list every node, click one to teleport");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("list").executes(this::list);
    }

    private int list(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        Map<String, Node> all = manager.all();

        if (all.isEmpty()) {
            sender.sendMessage(Message.prefixed("&7No nodes yet. Look at a block and run &f/nodes set&7."));
            return Command.SINGLE_SUCCESS;
        }

        sender.sendMessage(Message.of("&bNodes &7(" + all.size() + ") &8- click to teleport"));
        for (Node node : all.values()) {
            sender.sendMessage(line(node));
        }
        return Command.SINGLE_SUCCESS;
    }

    private Component line(Node node) {
        String status = node.getWorld() == null ? "&8world unloaded"
                : node.isBroken() ? "&crespawning " + node.secondsLeft() + "s"
                : "&aready";

        Component hover = Message.of("&f" + node.getMaterial().name()
                + "\n&7" + node.getWorldName() + " &f" + node.getX() + " " + node.getY() + " " + node.getZ()
                + "\n&7respawn: &f" + node.getRespawnSeconds() + "s"
                + "\n&8click to teleport");

        return Message.of("  &8- &f" + node.getId() + " &7(" + status + "&7)")
                .hoverEvent(HoverEvent.showText(hover))
                .clickEvent(ClickEvent.runCommand("/nodes tp " + node.getId()));
    }
}
