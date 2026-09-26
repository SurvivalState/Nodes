package dev.heypr.nodes.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.heypr.nodes.Message;
import dev.heypr.nodes.NodeManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ResetCommand extends Subcommand {

    public ResetCommand(NodeManager manager) {
        super(manager);
    }

    @Override
    public List<String> help() {
        return List.of("&f/nodes reset <name|all> &8- end a respawn early");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("reset")
                .then(Commands.literal("all").executes(this::resetAll))
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests(nodeIds())
                        .executes(this::resetOne));
    }

    private int resetOne(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String id = StringArgumentType.getString(ctx, "id");
        sender.sendMessage(manager.forceRespawn(id)
                ? Message.prefixed("&aNode &f" + id + " &ais back.")
                : Message.prefixed("&cNo node named &f" + id + "&c."));
        return Command.SINGLE_SUCCESS;
    }

    private int resetAll(CommandContext<CommandSourceStack> ctx) {
        int count = manager.forceRespawnAll();
        ctx.getSource().getSender().sendMessage(Message.prefixed("&aRestored &f" + count + " &anode(s)."));
        return Command.SINGLE_SUCCESS;
    }
}
