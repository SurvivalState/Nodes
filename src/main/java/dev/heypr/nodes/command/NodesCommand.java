package dev.heypr.nodes.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.heypr.nodes.Message;
import dev.heypr.nodes.NodeManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.command.CommandSender;

import java.util.List;

public class NodesCommand {

    public static void register(Commands commands, NodeManager manager) {
        List<Subcommand> subcommands = List.of(
                new SetCommand(manager),
                new RemoveCommand(manager),
                new EditCommand(manager),
                new ListCommand(manager),
                new TeleportCommand(manager),
                new ResetCommand(manager),
                new ReloadCommand(manager)
        );

        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("nodes")
                .requires(source -> source.getSender().hasPermission("nodes.admin"))
                .executes(ctx -> {
                    help(ctx.getSource().getSender(), subcommands);
                    return Command.SINGLE_SUCCESS;
                });

        for (Subcommand subcommand : subcommands) {
            root.then(subcommand.build());
        }

        commands.register(root.build(), "Manage respawning resource nodes.", List.of("node"));
    }

    private static void help(CommandSender sender, List<Subcommand> subcommands) {
        TextComponent.Builder builder = Message.of("&bHelp Menu").toBuilder();
        for (Subcommand subcommand : subcommands) {
            for (String line : subcommand.help()) {
                builder.appendNewline().append(Message.of("  " + line));
            }
        }
        sender.sendMessage(builder.build());
    }
}
