package com.envyworld;

import com.envyworld.config.ConfigIO;
import com.envyworld.config.RulesConfig;
import com.envyworld.items.CustomItems;
import com.envyworld.punish.PunishmentStore;
import com.envyworld.ranks.RankCatalog;
import com.envyworld.rules.RuleBook;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class EnvyWorld implements ModInitializer {
	public static final String MOD_ID = "envyworld";
	public static final Logger LOGGER = LoggerFactory.getLogger("EnvyWorld");

	private static RulesConfig config;
	private static PunishmentStore punishmentStore;
	private static MinecraftServer server;

	@Override
	public void onInitialize() {
		reload();
		RuleBook.load(LOGGER);
		RankCatalog.load(LOGGER);
		CustomItems.seedDefaults(LOGGER);
        // Public releases start with an empty catalog. Never restore deleted user presets.
        boolean changed=false;
        for(var binding:config.binds)if((binding.trigger==com.envyworld.bind.BindTrigger.PROJECTILE_HIT||binding.trigger==com.envyworld.bind.BindTrigger.ATTACK_BLOCK)&&binding.enabled){binding.enabled=false;changed=true;}
        if(changed)saveConfig();
		EnvyEvents.register();
		LOGGER.info("EnvyWorld rules loaded: {} bind(s), {} rule preset(s), {} rank(s)",
				config.binds.size(), RuleBook.all().size(), RankCatalog.all().size());
	}

	public static Path dataDir() {
		return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
	}

	/** Reloads config and punishments from disk. */
	public static void reload() {
		config = ConfigIO.load(dataDir().resolve("config.json"), RulesConfig.class, LOGGER);
        if (config.client == null) config.client = new com.envyworld.config.ClientSettings();
        if (config.client.watchedPlayers == null) config.client.watchedPlayers = new java.util.ArrayList<>();

		if (punishmentStore == null) {
			punishmentStore = new PunishmentStore(dataDir().resolve("punishments.json"), LOGGER);
		}
		punishmentStore.load();
	}

	public static RulesConfig config() {
		if (config == null) {
			reload();
		}
		return config;
	}

	public static void saveConfig() {
		ConfigIO.save(dataDir().resolve("config.json"), config(), LOGGER);
	}

	public static PunishmentStore punishments() {
		if (punishmentStore == null) {
			reload();
		}
		return punishmentStore;
	}

	public static MinecraftServer server() {
		return server;
	}

	public static void onServerStarted(MinecraftServer startedServer) {
		server = startedServer;
		punishmentStore.load();
	}

	public static void onServerStopped() {
		if (punishmentStore != null) {
			punishmentStore.save();
		}
		server = null;
	}
}
