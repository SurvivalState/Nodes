package dev.heypr.nodes.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.heypr.nodes.Node;
import dev.heypr.nodes.NodeManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class Subcommand {

    protected final NodeManager manager;

    protected Subcommand(NodeManager manager) {
        this.manager = manager;
    }

    public abstract LiteralArgumentBuilder<CommandSourceStack> build();

    public abstract List<String> help();

    protected SuggestionProvider<CommandSourceStack> nodeIds() {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            manager.all().keySet().stream()
                    .filter(id -> id.toLowerCase().startsWith(remaining))
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    protected Block targetBlock(Player player) {
        Block block = player.getTargetBlockExact(manager.setRange());
        return block == null || block.getType().isAir() ? null : block;
    }

    protected Node targetNode(Player player) {
        Block block = targetBlock(player);
        return block == null ? null : manager.at(block.getLocation());
    }
}
