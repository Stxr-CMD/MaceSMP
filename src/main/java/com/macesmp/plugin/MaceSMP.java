package com.macesmp.plugin;

import com.macesmp.plugin.commands.CoreShopCommand;
import com.macesmp.plugin.commands.CoresCommand;
import com.macesmp.plugin.commands.MaceSMPHelpCommand;
import com.macesmp.plugin.commands.ScoreboardCommand;
import com.macesmp.plugin.listeners.JoinQuitListener;
import com.macesmp.plugin.listeners.PlayerKillListener;
import com.macesmp.plugin.listeners.ShopClickListener;
import com.macesmp.plugin.listeners.SlowFallingBlocker;
import com.macesmp.plugin.managers.CoreManager;
import com.macesmp.plugin.managers.MessageManager;
import com.macesmp.plugin.managers.ScoreboardManager;
import com.macesmp.plugin.managers.ShopManager;
import com.macesmp.plugin.placeholder.MaceSMPExpansion;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * MaceSMP - Cores currency, core shop, PvP kill rewards and a live scoreboard.
 */
public final class MaceSMP extends JavaPlugin {

    private static MaceSMP instance;

    private CoreManager coreManager;
    private ShopManager shopManager;
    private MessageManager messageManager;
    private ScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config files if they don't already exist
        saveDefaultConfig();
        saveResource("shop.yml", false);

        this.messageManager = new MessageManager(this);
        this.coreManager = new CoreManager(this);
        this.shopManager = new ShopManager(this);
        this.scoreboardManager = new ScoreboardManager(this);

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerKillListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopClickListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new SlowFallingBlocker(this), this);

        // Commands
        getCommand("cores").setExecutor(new CoresCommand(this));
        getCommand("coreshop").setExecutor(new CoreShopCommand(this));
        getCommand("scoreboard").setExecutor(new ScoreboardCommand(this));
        getCommand("macesmp").setExecutor(new MaceSMPHelpCommand(this));

        // Hook into PlaceholderAPI if present, so any scoreboard/holograms plugin
        // can display %macesmp_cores% and %macesmp_kills% correctly.
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new MaceSMPExpansion(this).register();
            getLogger().info("Hooked into PlaceholderAPI - placeholders %macesmp_cores% and %macesmp_kills% are now available.");
        }

        // Start the built-in scoreboard updater (if enabled in config.yml)
        scoreboardManager.startUpdating();

        coreManager.startAutoSave();

        getLogger().info("MaceSMP has been enabled.");
    }

    @Override
    public void onDisable() {
        if (coreManager != null) {
            coreManager.stopAutoSave();
            coreManager.saveAll();
        }
        if (scoreboardManager != null) {
            scoreboardManager.stopUpdating();
        }
        getLogger().info("MaceSMP has been disabled.");
    }

    public static MaceSMP getInstance() {
        return instance;
    }

    public CoreManager getCoreManager() {
        return coreManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    /**
     * Reloads config.yml, shop.yml and re-applies scoreboard settings.
     */
    public void reloadAll() {
        reloadConfig();
        shopManager.reload();
        scoreboardManager.reload();
    }
}
