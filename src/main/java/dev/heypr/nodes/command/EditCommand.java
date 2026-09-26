package dev.heypr.nodes.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.heypr.nodes.Message;
import dev.heypr.nodes.NodeManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class EditCommand extends Subcommand {

    public EditCommand(NodeManager manager) {
        super(manager);
    }

    @Override
    public List<String> help() {
        return List.of(
                "&f/nodes edit <name> material &8- retype a node to the looked-at block",
                "&f/nodes edit <name> respawn <seconds> &8- change a node's respawn time"
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("edit")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests(nodeIds())
                        .then(Commands.literal("material").executes(this::editMaterial))
                        .then(Commands.literal("respawn")
                                .then(Commands.argument("seconds", FloatArgumentType.floatArg(0.05f))
                                        .executes(this::editRespawn))));
    }

    private int editMaterial(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Message.prefixed("&cPlayers only."));
            return Command.SINGLE_SUCCESS;
        }

        String id = StringArgumentType.getString(ctx, "id");
        Block target = targetBlock(player);
        if (target == null) {
            sender.sendMessage(Message.prefixed("&cLook at a block within &f" + manager.setRange() + " &cblocks."));
            return Command.SINGLE_SUCCESS;
        }

        Material material = target.getType();
        sender.sendMessage(switch (manager.setMaterial(id, material)) {
            case OK -> Message.prefixed("&aNode &f" + id + " &anow uses &f" + material.name() + "&a.");
            case NOT_FOUND -> Message.prefixed("&cNo node named &f" + id + "&c.");
            case INVALID_MATERIAL -> Message.prefixed("&c" + material.name() + " can't be a node.");
        });
        return Command.SINGLE_SUCCESS;
    }

    private int editRespawn(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String id = StringArgumentType.getString(ctx, "id");
        float seconds = FloatArgumentType.getFloat(ctx, "seconds");
        if (manager.setRespawn(id, seconds) == NodeManager.EditResult.OK) {
            sender.sendMessage(Message.prefixed("&aNode &f" + id + " &anow respawns in &f" + seconds + "s&a."));
        } else {
            sender.sendMessage(Message.prefixed("&cNo node named &f" + id + "&c."));
        }
        return Command.SINGLE_SUCCESS;
    }
}
