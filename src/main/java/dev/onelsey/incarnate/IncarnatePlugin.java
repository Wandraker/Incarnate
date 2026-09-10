package dev.onelsey.incarnate;

import dev.onelsey.incarnate.ability.AbilityRegistry;
import dev.onelsey.incarnate.command.IncarnateCommand;
import dev.onelsey.incarnate.command.PossessCommand;
import dev.onelsey.incarnate.command.ReleaseCommand;
import dev.onelsey.incarnate.config.ConfigMigrator;
import dev.onelsey.incarnate.input.PrimaryInputDeduplicator;
import dev.onelsey.incarnate.input.SpectatorPrimaryInputBridge;
import dev.onelsey.incarnate.listener.SessionListener;
import dev.onelsey.incarnate.message.MessageService;
import dev.onelsey.incarnate.movement.ControllerRegistry;
import dev.onelsey.incarnate.possession.PossessionManager;
import dev.onelsey.incarnate.visibility.PossessionVisibilityManager;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class IncarnatePlugin extends JavaPlugin {
    private PossessionManager possessions;
    private MessageService messages;
    private SpectatorPrimaryInputBridge spectatorPrimaryInputBridge;

    @Override
    public void onEnable() {
        ConfigMigrator.prepare(this);
        messages = new MessageService(this);
        messages.initialize();

        Set<EntityType> excluded = loadExcludedTypes();
        ControllerRegistry controllers = new ControllerRegistry(getConfig());
        AbilityRegistry abilities = new AbilityRegistry(getConfig());
        PossessionVisibilityManager visibility = new PossessionVisibilityManager(this);
        possessions = new PossessionManager(this, controllers, abilities, visibility, messages, excluded);

        IncarnateCommand incarnateCommand = new IncarnateCommand(
            possessions,
            messages,
            getConfig().getDouble("admin.inspect-distance", 12.0)
        );
        Objects.requireNonNull(getCommand("incarnate")).setExecutor(incarnateCommand);
        Objects.requireNonNull(getCommand("incarnate")).setTabCompleter(incarnateCommand);
        Objects.requireNonNull(getCommand("possess")).setExecutor(new PossessCommand(
            possessions,
            messages,
            getConfig().getDouble("control.max-possession-distance", 8.0)
        ));
        Objects.requireNonNull(getCommand("release")).setExecutor(new ReleaseCommand(possessions, messages));

        PrimaryInputDeduplicator primaryInputDeduplicator = new PrimaryInputDeduplicator();
        SessionListener sessionListener = new SessionListener(this, possessions, primaryInputDeduplicator);
        getServer().getPluginManager().registerEvents(sessionListener, this);
        spectatorPrimaryInputBridge = new SpectatorPrimaryInputBridge(
            this,
            primaryInputDeduplicator,
            sessionListener::triggerPrimaryFromPacket
        );
        spectatorPrimaryInputBridge.start();

        possessions.recoverIndexedVessels();
        possessions.recoverAlreadyOnlinePlayers();
        visibility.recoverStaleTabEntries();
        getLogger().info("Incarnate " + getPluginMeta().getVersion() + " enabled for Minecraft 26.2+ (Paper/Purpur/Leaf/Folia).");
    }

    @Override
    public void onDisable() {
        if (spectatorPrimaryInputBridge != null) {
            spectatorPrimaryInputBridge.stop();
        }
        if (possessions != null) {
            possessions.shutdown();
        }
    }

    private Set<EntityType> loadExcludedTypes() {
        Set<EntityType> excluded = EnumSet.noneOf(EntityType.class);
        for (String raw : getConfig().getStringList("excluded-types")) {
            try {
                excluded.add(EntityType.valueOf(raw.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                getLogger().warning("Unknown excluded entity type in config.yml: " + raw);
            }
        }
        return excluded;
    }
}
