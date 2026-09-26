package dev.heypr.nodes;

import dev.heypr.nodes.command.NodesCommand;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

public class Nodes extends JavaPlugin {

    private static Nodes instance;
    private NodeManager nodes;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        nodes = new NodeManager();
        nodes.load();
        getServer().getPluginManager().registerEvents(new NodeListener(nodes), this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> NodesCommand.register(event.registrar(), nodes));
    }

    @Override
    public void onDisable() {
        if (nodes != null) {
            nodes.shutdown();
        }
    }

    public static Nodes get() {
        return instance;
    }

    public NodeManager nodes() {
        return nodes;
    }
}
