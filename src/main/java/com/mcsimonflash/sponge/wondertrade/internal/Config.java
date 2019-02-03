package com.mcsimonflash.sponge.wondertrade.internal;

import com.mcsimonflash.sponge.teslalibs.configuration.ConfigHolder;
import com.mcsimonflash.sponge.wondertrade.WonderTrade;
import com.mcsimonflash.sponge.wondertrade.data.TradeEntry;
import com.pixelmonmod.pixelmon.config.PixelmonEntityList;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.Gender;
import com.pixelmonmod.pixelmon.enums.EnumGrowth;
import com.pixelmonmod.pixelmon.enums.EnumNature;
import com.pixelmonmod.pixelmon.enums.EnumPokemon;
import com.pixelmonmod.pixelmon.enums.items.EnumPokeballs;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

public class Config {
	
	private static final Path DIRECTORY = WonderTrade.getDirectory(), STORAGE = DIRECTORY.resolve("storage");
	public static boolean allowEggs, broadcastTrades, regenOnRestart, regenOverwritePlayers;
	public static int poolSize, minLvl, maxLvl, shinyRate, legendRate, announceInt;
	public static long defCooldown;
	private static ConfigHolder config, cooldowns, trades;
	
	public static void load() {
		try {
			config = getLoader(DIRECTORY, "wondertrade.conf", true);
			cooldowns = getLoader(STORAGE, "cooldowns.conf", false);
			trades = getLoader(STORAGE, "trades.conf", false);
			allowEggs = config.getNode("allow-eggs").getBoolean(true);
			broadcastTrades = config.getNode("broadcast-trades").getBoolean(true);
			regenOnRestart = config.getNode("regen-on-restart").getBoolean(false);
			regenOverwritePlayers = config.getNode("regen-overwrite-players").getBoolean(false);
			poolSize = config.getNode("pool-size").getInt(100);
			defCooldown = config.getNode("default-cooldown").getLong(600000);
			minLvl = config.getNode("min-level").getInt(5);
			maxLvl = config.getNode("max-level").getInt(95);
			shinyRate = config.getNode("shiny-rate").getInt(1365);
			legendRate = config.getNode("legendary-rate").getInt(8192);
			announceInt = config.getNode("announcement-interval").getInt(600000);
			boolean startup = Manager.trades == null;
			Manager.trades = new TradeEntry[poolSize];
			List<? extends ConfigurationNode> trades = Config.trades.getNode("trades").getChildrenList();
			for (int i = 0; i < poolSize && i < trades.size(); i++) {
				Manager.trades[i] = deserializeTrade(trades.get(i));
			}
			Manager.fillPool(startup && regenOnRestart, regenOverwritePlayers);
		} catch (IOException | IllegalArgumentException e) {
			WonderTrade.getLogger().error("Error loading config: " + e.getMessage());
		}
	}
	
	private static ConfigHolder getLoader(Path dir, String name, boolean asset) throws IOException {
		try {
			Path path = dir.resolve(name);
			if (Files.notExists(path)) {
				Files.createDirectories(dir);
				if (asset) {
					WonderTrade.getContainer().getAsset(name).get().copyToFile(path);
				} else {
					Files.createFile(path);
				}
			}
			return ConfigHolder.of(HoconConfigurationLoader.builder().setPath(path).build());
		} catch (IOException e) {
			WonderTrade.getLogger().error("Unable to load config file " + name + ".");
			throw e;
		}
	}
	
	public static boolean saveTrade(int index) {
		serializeTrade(Manager.trades[index], trades.getNode("trades", index));
		return trades.save();
	}
	
	public static boolean saveAll() {
		for (int i = 0; i < poolSize; i++) {
			serializeTrade(Manager.trades[i], trades.getNode("trades", i));
		}
		return trades.save();
	}
	
	public static long getCooldown(UUID uuid) {
		return cooldowns.getNode(uuid.toString()).getLong(0);
	}
	
	public static boolean resetCooldown(UUID uuid) {
		cooldowns.getNode(uuid.toString()).setValue(System.currentTimeMillis());
		return cooldowns.save();
	}
	
	private static TradeEntry deserializeTrade(ConfigurationNode node) {
		EnumPokemon type = EnumPokemon.getFromName(node.getNode("name").getString("")).orElseThrow(() -> new IllegalStateException("Malformed storage - no pokemon named " + node.getNode("name").getString()));
		UUID owner = UUID.fromString(node.getNode("owner").getString(Utils.ZERO_UUID.toString()));
		LocalDateTime date = LocalDateTime.ofEpochSecond(node.getNode("time").getLong(0), 0, ZoneOffset.UTC);
		EntityPixelmon pokemon = (EntityPixelmon) PixelmonEntityList.createEntityByName(type.name, Utils.getWorld());
		pokemon.getLvl().setLevel(node.getNode("level").getInt(5));
		pokemon.setGender(Gender.values()[node.getNode("gender").getInt(0)]);
		pokemon.setGrowth(EnumGrowth.getGrowthFromIndex(node.getNode("growth").getInt(3)));
		pokemon.setNature(EnumNature.getNatureFromIndex(node.getNode("nature").getInt(4)));
		pokemon.setAbility(node.getNode("ability").getString(""));
		pokemon.setIsShiny(node.getNode("shiny").getBoolean(false));
		pokemon.setForm(node.getNode("form").getInt(0));
		pokemon.caughtBall = EnumPokeballs.values()[node.getNode("ball").getInt(0)];
		return new TradeEntry(pokemon, owner, date);
	}
	
	private static void serializeTrade(TradeEntry entry, ConfigurationNode node) {
		node.getNode("owner").setValue(entry.getOwner().toString());
		node.getNode("time").setValue(entry.getDate().toEpochSecond(ZoneOffset.UTC));
		node.getNode("name").setValue(entry.getPokemon().getSpecies().name);
		node.getNode("level").setValue(entry.getPokemon().getLvl().getLevel());
		node.getNode("gender").setValue(entry.getPokemon().getGender().ordinal());
		node.getNode("growth").setValue(entry.getPokemon().getGrowth().index);
		node.getNode("nature").setValue(entry.getPokemon().getNature().index);
		node.getNode("ability").setValue(entry.getPokemon().getAbility().getName());
		node.getNode("shiny").setValue(entry.getPokemon().getIsShiny());
		node.getNode("form").setValue(entry.getPokemon().getForm());
		node.getNode("ball").setValue(entry.getPokemon().caughtBall != null ? entry.getPokemon().caughtBall.ordinal() : 0);
	}
	
}