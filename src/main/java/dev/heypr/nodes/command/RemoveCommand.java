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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class RemoveCommand extends Subcommand {

    public RemoveCommand(NodeManager manager) {
        super(manager);
    }

    @Override
    public List<String> help() {
        return List.of("&f/nodes remove &7[name] &8- delete the looked-at node, or one by name");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("remove")
                .executes(this::removeLookedAt)
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests(nodeIds())
                        .executes(this::removeById));
    }

    private int removeLookedAt(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Message.prefixed("&cUsage: &f/nodes remove <name>"));
            return Command.SINGLE_SUCCESS;
        }

        Node node = targetNode(player);
        if (node == null) {
            sender.sendMessage(Message.prefixed("&cLook at a node, or use &f/nodes remove <name>&c."));
            return Command.SINGLE_SUCCESS;
        }

        manager.remove(node.getId());
        sender.sendMessage(Message.prefixed("&aRemoved node &f" + node.getId() + "&a."));
        return Command.SINGLE_SUCCESS;
    }

    private int removeById(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String id = StringArgumentType.getString(ctx, "id");
        sender.sendMessage(manager.remove(id)
                ? Message.prefixed("&aRemoved node &f" + id + "&a.")
                : Message.prefixed("&cNo node named &f" + id + "&c."));
        return Command.SINGLE_SUCCESS;
    }
}
