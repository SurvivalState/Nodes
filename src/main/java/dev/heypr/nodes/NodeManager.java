package dev.heypr.nodes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class NodeManager {

    public static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private final Nodes plugin = Nodes.get();
    private final File file = new File(plugin.getDataFolder(), "nodes.yml");
    private final NamespacedKey ownerKey = new NamespacedKey(plugin, "node-id");

    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final Map<String, Map<Long, Node>> byPosition = new HashMap<>();
    private final Map<String, Map<Long, List<Node>>> byChunk = new HashMap<>();
    private final Map<String, TextDisplay> displays = new HashMap<>();
    private final Map<String, BukkitTask> respawnTasks = new HashMap<>();
    private final Set<String> brokenIds = new LinkedHashSet<>();

    private BukkitTask displayTask;
    private BukkitTask autosaveTask;
    private boolean dirty;

    private Material brokenMaterial = Material.BEDROCK;
    private boolean protect = true;
    private boolean dropItems = true;
    private boolean restoreOnDisable = true;
    private boolean displayEnabled = true;
    private String displayTitle = "&e&lRESPAWNING";
    private int barLength = 20;
    private double displayYOffset = 1.5;
    private float textViewRange = 0.5f;
    private float hideVisualsBelow = 0.5f;

    public void load() {
        if (dirty) {
            save();
        }
        teardown();
        readConfig();

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        nodes.putAll(readFile());
        nodes.values().forEach(this::index);

        Bukkit.getWorlds().forEach(world -> purgeTagged(world.getEntities()));
        nodes.values().forEach(this::resume);
        nodes.values().forEach(this::ensureDisplay);
        startTasks();
    }

    public void shutdown() {
        if (restoreOnDisable) {
            for (Node node : nodes.values()) {
                if (node.isBroken()) {
                    applyMaterial(node);
                }
            }
        }
        save();
        teardown();
    }

    public void save() {
        write(snapshot(nodes.values()));
        dirty = false;
    }

    private void teardown() {
        cancel(displayTask);
        cancel(autosaveTask);
        displayTask = null;
        autosaveTask = null;

        respawnTasks.values().forEach(NodeManager::cancel);
        respawnTasks.clear();

        for (TextDisplay display : displays.values()) {
            if (!display.isDead()) {
                display.remove();
            }
        }
        displays.clear();

        brokenIds.clear();
        byChunk.clear();
        byPosition.clear();
        nodes.clear();
    }

    private void readConfig() {
        var config = plugin.getConfig();

        Material matched = Material.matchMaterial(config.getString("nodes.broken-material", "BEDROCK"));
        brokenMaterial = matched != null && matched.isBlock() ? matched : Material.BEDROCK;
        protect = config.getBoolean("nodes.protect", true);
        dropItems = config.getBoolean("nodes.drop-items", true);
        restoreOnDisable = config.getBoolean("nodes.restore-on-disable", true);
        hideVisualsBelow = (float) config.getDouble("nodes.hide-visuals-below-seconds", 0.5);

        displayEnabled = config.getBoolean("display.enabled", true);
        displayTitle = config.getString("display.title", "&e&lRESPAWNING");
        barLength = Math.max(config.getInt("display.bar-length", 20), 1);
        displayYOffset = config.getDouble("display.y-offset", 1.5);

        double viewBlocks = config.getDouble("display.view-range", 32.0);
        textViewRange = viewBlocks <= 0 ? 1f : (float) Math.max(viewBlocks / 64.0, 0.05);
    }

    private void startTasks() {
        if (displayEnabled) {
            long interval = Math.max(plugin.getConfig().getLong("display.update-ticks", 20L), 1L);
            displayTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickDisplays, interval, interval);
        }

        long autosaveTicks = Math.max(plugin.getConfig().getLong("nodes.autosave-seconds", 60L), 5L) * 20L;
        autosaveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (dirty) {
                save();
            }
        }, autosaveTicks, autosaveTicks);
    }

    public boolean isProtect() {
        return protect;
    }

    public boolean isDropItems() {
        return dropItems;
    }

    public int defaultRespawn() {
        return Math.max(plugin.getConfig().getInt("nodes.default-respawn-seconds", 60), 1);
    }

    public int setRange() {
        return Math.max(plugin.getConfig().getInt("nodes.set-range", 5), 1);
    }

    public RegisterResult register(String id, Location location, Material material, float respawnSeconds) {
        if (!ID_PATTERN.matcher(id).matches()) {
            return RegisterResult.INVALID_ID;
        }
        if (nodes.containsKey(id)) {
            return RegisterResult.DUPLICATE_ID;
        }
        if (material == null || !material.isBlock() || material.isAir()) {
            return RegisterResult.INVALID_MATERIAL;
        }
        if (at(location) != null) {
            return RegisterResult.DUPLICATE_LOCATION;
        }

        Node node = Node.at(id, location, material, respawnSeconds);
        nodes.put(id, node);
        index(node);
        sweepDisplays(node);
        save();
        return RegisterResult.OK;
    }

    public boolean remove(String id) {
        Node node = nodes.remove(id);
        if (node == null) {
            return false;
        }

        unindex(node);
        brokenIds.remove(id);
        cancel(respawnTasks.remove(id));
        removeDisplay(id);

        if (node.isBroken()) {
            applyMaterial(node);
        }

        save();
        return true;
    }

    public EditResult setMaterial(String id, Material material) {
        Node node = nodes.get(id);
        if (node == null) {
            return EditResult.NOT_FOUND;
        }
        if (material == null || !material.isBlock() || material.isAir()) {
            return EditResult.INVALID_MATERIAL;
        }

        node.setMaterial(material);
        dirty = true;
        if (!node.isBroken()) {
            applyMaterial(node);
        }
        save();
        return EditResult.OK;
    }

    public EditResult setRespawn(String id, float seconds) {
        Node node = nodes.get(id);
        if (node == null) {
            return EditResult.NOT_FOUND;
        }

        node.setRespawnSeconds(seconds);
        dirty = true;
        if (node.isBroken()) {
            schedule(node, node.remainingSeconds());
        }
        save();
        return EditResult.OK;
    }

    public String nextId() {
        for (int i = 1; i <= 10000; i++) {
            String id = "node" + i;
            if (!nodes.containsKey(id)) {
                return id;
            }
        }
        return "node" + System.currentTimeMillis();
    }

    public Node at(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        Map<Long, Node> world = byPosition.get(location.getWorld().getName());
        return world == null ? null : world.get(pack(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    }

    public Node get(String id) {
        return nodes.get(id);
    }

    public Map<String, Node> all() {
        return Collections.unmodifiableMap(nodes);
    }

    public boolean isNode(Location location) {
        return at(location) != null;
    }

    public boolean handleBreak(Location location) {
        Node node = at(location);
        if (node == null || node.isBroken()) {
            return false;
        }

        node.markBroken();
        brokenIds.add(node.getId());
        dirty = true;

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (nodes.get(node.getId()) != node || !node.isBroken()) {
                return;
            }
            applyBroken(node);
            ensureDisplay(node);
        });

        schedule(node, node.getRespawnSeconds());
        return true;
    }

    private void schedule(Node node, float seconds) {
        cancel(respawnTasks.remove(node.getId()));
        long ticks = Math.max(Math.round(seconds * 20f), 1L);
        respawnTasks.put(node.getId(), Bukkit.getScheduler().runTaskLater(plugin, () -> restore(node), ticks));
    }

    private void restore(Node node) {
        respawnTasks.remove(node.getId());
        if (nodes.get(node.getId()) != node) {
            return;
        }

        node.markRestored();
        brokenIds.remove(node.getId());
        dirty = true;
        applyMaterial(node);
        removeDisplay(node.getId());
    }

    public boolean forceRespawn(String id) {
        Node node = nodes.get(id);
        if (node == null) {
            return false;
        }
        cancel(respawnTasks.remove(id));
        restore(node);
        return true;
    }

    public int forceRespawnAll() {
        int count = 0;
        for (String id : new ArrayList<>(brokenIds)) {
            if (forceRespawn(id)) {
                count++;
            }
        }
        return count;
    }

    private void resume(Node node) {
        if (!node.isBroken()) {
            sync(node);
            return;
        }

        float remaining = node.remainingSeconds();
        if (remaining <= 0f) {
            node.markRestored();
            dirty = true;
            applyMaterial(node);
            return;
        }

        brokenIds.add(node.getId());
        applyBroken(node);
        schedule(node, remaining);
    }

    public void sync(Node node) {
        if (node.isBroken()) {
            applyBroken(node);
        } else {
            applyMaterial(node);
        }
    }

    private void applyMaterial(Node node) {
        setBlock(node, node.getMaterial());
    }

    private void applyBroken(Node node) {
        if (showsVisuals(node)) {
            setBlock(node, brokenMaterial);
        }
    }

    private boolean showsVisuals(Node node) {
        return node.getRespawnSeconds() >= hideVisualsBelow;
    }

    private void setBlock(Node node, Material material) {
        if (!node.isChunkLoaded()) {
            return;
        }
        Block block = node.getBlock();
        if (block != null && block.getType() != material) {
            block.setType(material, false);
        }
    }

    public void onChunkLoad(Chunk chunk) {
        for (Node node : inChunk(chunk)) {
            sync(node);
            sweepDisplays(node);
            ensureDisplay(node);
        }
    }

    public void onChunkUnload(Chunk chunk) {
        for (Node node : inChunk(chunk)) {
            removeDisplay(node.getId());
        }
    }

    private List<Node> inChunk(Chunk chunk) {
        Map<Long, List<Node>> world = byChunk.get(chunk.getWorld().getName());
        if (world == null) {
            return List.of();
        }
        List<Node> list = world.get(packChunk(chunk.getX(), chunk.getZ()));
        return list == null ? List.of() : list;
    }

    private void ensureDisplay(Node node) {
        if (!displayEnabled || !node.isBroken() || !node.isChunkLoaded() || !showsVisuals(node)) {
            removeDisplay(node.getId());
            return;
        }

        TextDisplay text = displays.get(node.getId());
        if (text == null || text.isDead() || !text.isValid()) {
            spawnDisplay(node);
        }
    }

    private void spawnDisplay(Node node) {
        World world = node.getWorld();
        if (world == null) {
            return;
        }

        removeDisplay(node.getId());

        Location loc = new Location(world, node.getX() + 0.5, node.getY() + displayYOffset, node.getZ() + 0.5);
        TextDisplay display = world.spawn(loc, TextDisplay.class, entity -> {
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setSeeThrough(false);
            entity.setPersistent(false);
            entity.setViewRange(textViewRange);
            entity.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, node.getId());
            entity.text(buildText(node));
        });
        displays.put(node.getId(), display);
    }

    private void sweepDisplays(Node node) {
        Location center = node.getCenter();
        World world = node.getWorld();
        if (center == null || world == null || !node.isChunkLoaded()) {
            return;
        }
        purgeTagged(world.getNearbyEntities(center, 1.5, Math.abs(displayYOffset) + 2.0, 1.5));
    }

    private void purgeTagged(Collection<Entity> entities) {
        for (Entity entity : entities) {
            if (!(entity instanceof Display) || displays.containsValue(entity)) {
                continue;
            }
            if (entity.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING)) {
                entity.remove();
            }
        }
    }

    private void removeDisplay(String id) {
        TextDisplay display = displays.remove(id);
        if (display != null && !display.isDead()) {
            display.remove();
        }
    }

    private void tickDisplays() {
        if (brokenIds.isEmpty()) {
            return;
        }

        for (String id : List.copyOf(brokenIds)) {
            Node node = nodes.get(id);
            if (node == null || !node.isChunkLoaded()) {
                continue;
            }
            ensureDisplay(node);
            TextDisplay display = displays.get(id);
            if (display != null && !display.isDead()) {
                display.text(buildText(node));
            }
        }
    }

    private Component buildText(Node node) {
        float progress = node.progress();
        return Message.of(displayTitle)
                .append(Component.newline())
                .append(buildBar(progress))
                .append(Component.newline())
                .append(Component.text((int) (progress * 100) + "%", NamedTextColor.GRAY))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(node.secondsLeft() + "s", NamedTextColor.WHITE));
    }

    private Component buildBar(float progress) {
        int filled = Math.round(progress * barLength);
        TextComponent.Builder builder = Component.text();
        TextColor empty = TextColor.color(0x44, 0x44, 0x44);

        for (int i = 0; i < barLength; i++) {
            TextColor color;
            if (i < filled) {
                float pos = barLength == 1 ? 1f : (float) i / (barLength - 1);
                color = TextColor.color((int) (255 * (1f - pos)), (int) (255 * pos), 0);
            } else {
                color = empty;
            }
            builder.append(Component.text("━", color));
        }
        return builder.build();
    }

    private Map<String, Node> readFile() {
        Map<String, Node> result = new LinkedHashMap<>();
        if (!file.exists()) {
            return result;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("nodes");
        if (root == null) {
            return result;
        }

        int defaultRespawn = defaultRespawn();
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null || !ID_PATTERN.matcher(id).matches()) {
                continue;
            }

            String world = section.getString("world");
            if (world == null || world.isBlank()) {
                continue;
            }

            Material material = Material.matchMaterial(section.getString("material", ""));
            if (material == null || !material.isBlock() || material.isAir()) {
                continue;
            }

            float respawn = Math.max((float) section.getDouble("respawn", defaultRespawn), 0.05f);
            Node node = new Node(id, world, section.getInt("x"), section.getInt("y"), section.getInt("z"), material, respawn);

            if (section.getBoolean("broken", false)) {
                long brokenAt = section.getLong("broken-at", 0L);
                if (brokenAt > 0L && brokenAt <= System.currentTimeMillis()) {
                    node.markBroken(brokenAt);
                } else {
                    node.markBroken();
                }
            }
            result.put(id, node);
        }
        return result;
    }

    private YamlConfiguration snapshot(Collection<Node> nodes) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Node node : nodes) {
            String path = "nodes." + node.getId();
            yaml.set(path + ".world", node.getWorldName());
            yaml.set(path + ".x", node.getX());
            yaml.set(path + ".y", node.getY());
            yaml.set(path + ".z", node.getZ());
            yaml.set(path + ".material", node.getMaterial().name());
            yaml.set(path + ".respawn", node.getRespawnSeconds());
            yaml.set(path + ".broken", node.isBroken());
            if (node.isBroken()) {
                yaml.set(path + ".broken-at", node.getBrokenAt());
            }
        }
        return yaml;
    }

    private void write(YamlConfiguration yaml) {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save nodes.yml: " + e.getMessage());
        }
    }

    private void index(Node node) {
        byPosition.computeIfAbsent(node.getWorldName(), k -> new HashMap<>())
                .put(pack(node.getX(), node.getY(), node.getZ()), node);
        byChunk.computeIfAbsent(node.getWorldName(), k -> new HashMap<>())
                .computeIfAbsent(packChunk(node.getX() >> 4, node.getZ() >> 4), k -> new ArrayList<>())
                .add(node);
    }

    private void unindex(Node node) {
        Map<Long, Node> positions = byPosition.get(node.getWorldName());
        if (positions != null) {
            positions.remove(pack(node.getX(), node.getY(), node.getZ()));
            if (positions.isEmpty()) {
                byPosition.remove(node.getWorldName());
            }
        }

        Map<Long, List<Node>> chunks = byChunk.get(node.getWorldName());
        if (chunks != null) {
            long key = packChunk(node.getX() >> 4, node.getZ() >> 4);
            List<Node> list = chunks.get(key);
            if (list != null) {
                list.remove(node);
                if (list.isEmpty()) {
                    chunks.remove(key);
                }
            }
            if (chunks.isEmpty()) {
                byChunk.remove(node.getWorldName());
            }
        }
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }

    private static long packChunk(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static void cancel(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    public enum RegisterResult {
        OK, INVALID_ID, INVALID_MATERIAL, DUPLICATE_ID, DUPLICATE_LOCATION
    }

    public enum EditResult {
        OK, NOT_FOUND, INVALID_MATERIAL
    }
}
