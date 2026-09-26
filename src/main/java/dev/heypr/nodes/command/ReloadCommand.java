package dev.heypr.nodes.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.heypr.nodes.Message;
import dev.heypr.nodes.NodeManager;
import dev.heypr.nodes.Nodes;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.List;

public class ReloadCommand extends Subcommand {

    public ReloadCommand(NodeManager manager) {
        super(manager);
    }

    @Override
    public List<String> help() {
        return List.of("&f/nodes reload &8- reload the config");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("nodes.reload"))
                .executes(this::reload);
    }

    private int reload(CommandContext<CommandSourceStack> ctx) {
        Nodes plugin = Nodes.get();
        manager.save();
        plugin.reloadConfig();
        manager.load();
        ctx.getSource().getSender().sendMessage(Message.prefixed("&aReloaded &f" + manager.all().size() + " &anode(s)."));
        return Command.SINGLE_SUCCESS;
    }
}
