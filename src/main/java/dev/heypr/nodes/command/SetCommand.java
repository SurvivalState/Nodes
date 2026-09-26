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
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SetCommand extends Subcommand {

    public SetCommand(NodeManager manager) {
        super(manager);
    }

    @Override
    public List<String> help() {
        return List.of("&f/nodes set &7[name] [seconds] &8- turn the looked-at block into a node");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("set")
                .executes(ctx -> run(ctx, null, manager.defaultRespawn()))
                .then(Commands.argument("id", StringArgumentType.word())
                        .executes(ctx -> run(ctx, StringArgumentType.getString(ctx, "id"), manager.defaultRespawn()))
                        .then(Commands.argument("respawn", FloatArgumentType.floatArg(0.05f))
                                .executes(ctx -> run(ctx, StringArgumentType.getString(ctx, "id"), FloatArgumentType.getFloat(ctx, "respawn")))));
    }

    private int run(CommandContext<CommandSourceStack> ctx, String requestedId, float respawn) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Message.prefixed("&cPlayers only."));
            return Command.SINGLE_SUCCESS;
        }

        Block target = targetBlock(player);
        if (target == null) {
            sender.sendMessage(Message.prefixed("&cLook at a block within &f" + manager.setRange() + " &cblocks."));
            return Command.SINGLE_SUCCESS;
        }

        String id = requestedId == null ? manager.nextId() : requestedId;
        sender.sendMessage(switch (manager.register(id, target.getLocation(), target.getType(), respawn)) {
            case OK -> Message.prefixed("&aCreated &f" + id + " &7(" + target.getType().name() + ", " + respawn + "s)");
            case INVALID_ID -> Message.prefixed("&cInvalid name. Use up to 32 letters, digits, _ or -.");
            case INVALID_MATERIAL -> Message.prefixed("&c" + target.getType().name() + " can't be a node.");
            case DUPLICATE_ID -> Message.prefixed("&cA node named &f" + id + " &calready exists.");
            case DUPLICATE_LOCATION -> Message.prefixed("&cThat block is already a node.");
        });
        return Command.SINGLE_SUCCESS;
    }
}
