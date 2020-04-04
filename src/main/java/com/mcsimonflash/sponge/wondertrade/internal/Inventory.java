package com.mcsimonflash.sponge.wondertrade.internal;

import com.google.common.collect.Lists;
import com.mcsimonflash.sponge.teslalibs.inventory.*;
import com.mcsimonflash.sponge.wondertrade.WonderTrade;
import com.mcsimonflash.sponge.wondertrade.data.TradeEntry;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.storage.PCStorage;
import com.pixelmonmod.pixelmon.client.gui.GuiResources;
import com.pixelmonmod.pixelmon.config.PixelmonItems;
import com.pixelmonmod.pixelmon.entities.pixelmon.EnumSpecialTexture;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;
import com.pixelmonmod.pixelmon.storage.PlayerPartyStorage;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.InventoryArchetype;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.user.UserStorageService;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Inventory {
	
	private static final Element RED = Element.of(ItemStack.builder()
			.itemType(ItemTypes.STAINED_GLASS_PANE)
			.add(Keys.DYE_COLOR, DyeColors.RED)
			.build());
	private static final Element BLACK = Element.of(ItemStack.builder()
			.itemType(ItemTypes.STAINED_GLASS_PANE)
			.add(Keys.DYE_COLOR, DyeColors.BLACK)
			.build());
	private static final Element WHITE = Element.of(ItemStack.builder()
			.itemType(ItemTypes.STAINED_GLASS_PANE)
			.add(Keys.DYE_COLOR, DyeColors.RED)
			.build());
	private static final Element CLOSE = Element.of(createItem(ItemTypes.BARRIER, "&cClose", "&4Close the menu"), inTask(a -> a.getPlayer().closeInventory()));
	private static final Layout MAIN = Layout.builder()
			.set(WHITE, 18, 19, 20, 21, 22, 23, 24, 25, 26)
			.set(RED, 0, 1, 2, 3, 4, 5, 6, 7, 8)
			.set(BLACK, 9, 17)
			.build();
	private static final Element MENU = Element.of(createItem((ItemType) PixelmonItems.tradeMonitor, "&bMenu", "&3Return to the main menu"), inTask(a -> createMainMenu(a.getPlayer()).open(a.getPlayer())));
	private static final Layout PAGE = Layout.builder()
			.set(BLACK, 0, 2, 4, 6, 8, 18, 26, 36, 38, 40, 42, 44)
			.set(RED, 1, 3, 5, 7, 9, 17, 27, 35, 37, 39, 41, 43, 45, 53)
			.set(MENU, 46)
			.set(Page.FIRST, 47)
			.set(Page.PREVIOUS, 48)
			.set(Page.CURRENT, 49)
			.set(Page.NEXT, 50)
			.set(Page.LAST, 51)
			.set(CLOSE, 52)
			.build();
	private static final Layout PC = Layout.builder()
			.set(BLACK, 0, 6, 8, 18, 24, 26, 36, 42, 44)
			.set(RED, 9, 15, 17, 27, 33, 35, 45, 51, 53)
			.set(Page.FIRST, 7)
			.set(Page.PREVIOUS, 16)
			.set(Page.CURRENT, 25)
			.set(Page.NEXT, 34)
			.set(Page.LAST, 43)
			.set(MENU, 52)
			.build();
	
	private static Consumer<Action.Click> inTask(Consumer<Action.Click> consumer) {
		return a -> Task.builder().execute(t -> consumer.accept(a)).submit(WonderTrade.getContainer());
	}
	
	private static View createView(InventoryArchetype archetype, String name, Layout layout) {
		return View.builder()
				.archetype(archetype)
				.property(InventoryTitle.of(Utils.toText(name)))
				.build(WonderTrade.getContainer())
				.define(layout);
	}
	
	public static View createMainMenu(Player player) {
		Pokemon[] party = Pixelmon.storageManager.getParty(player.getUniqueId()).getAll();
		Element[] elements = new Element[7];
		for (int i = 0; i < 6; i++) {
			int slot = i;
			elements[i] = createPokemonElement(player, party[i], "Slot " + (i + 1), inTask(a -> createTradeMenu(player, slot).open(player)));
		}
		elements[6] = Element.of(createItem(Sponge.getRegistry().getType(ItemType.class, "pixelmon:pc").get(), "&bPC (Box " + (Pixelmon.storageManager.getPCForPlayer(player.getUniqueId()).getLastBox() + 1) + ")", "&3Click to view your PC"), inTask(a -> createPCMenu(player, -1).open(player)));
		return createView(InventoryArchetypes.CHEST, "&6WonderTrade", Layout.builder()
				.from(MAIN)
				.page(Arrays.asList(elements))
				.build());
	}
	
	public static View createPCMenu(Player player, int boxNum) {
		PCStorage pc = Pixelmon.storageManager.getPCForPlayer(player.getUniqueId());
		int box = boxNum != -1 ? boxNum : pc.getLastBox();
		Element[] elements = new Element[30];
		for (int i = 0; i < 30; i++) {
			int pos = i;
			elements[i] = createPokemonElement(player, pc.get(box, pos), "Position " + (pos + 1), inTask(a -> createTradeMenu(player, box, pos).open(player)));
		}
		return createView(InventoryArchetypes.DOUBLE_CHEST, "&cPoke&3Clash &6WT &8PC", Layout.builder()
				.from(PC)
				.replace(Page.FIRST, createPageElement("&bFirst Box ", box, 0))
				.replace(Page.LAST, createPageElement("&bLast Box ", box, pc.getBoxCount() - 1))
				.replace(Page.NEXT, createPageElement("&bNext Box ", box, box == pc.getBoxCount() - 1 ? box : box + 1))
				.replace(Page.PREVIOUS, createPageElement("&bPrevious Box", box, box == 0 ? box : box - 1))
				.replace(Page.CURRENT, createPageElement("&bCurrent Box", box, box))
				.page(Arrays.asList(elements))
				.build());
	}
	
	private static Element createPokemonElement(Player player, Pokemon pokemon, String name, Consumer<Action.Click> action) {
		if (pokemon != null) {
			if (Config.allowEggs || !pokemon.isEgg()) {
				return Element.of(createPokemonItem("&b" + name, pokemon), action);
			} else {
				ItemStack item = createPokemonItem("&b" + name, pokemon);
				item.offer(Keys.ITEM_LORE, Lists.newArrayList(WonderTrade.getMessage(player.getLocale(), "wondertrade.trade.no-eggs").toText()));
				return Element.of(item);
			}
		} else {
			return Element.of(createItem(ItemTypes.BARRIER, "&cEmpty", "&4No Pokemon in " + name.toLowerCase()));
		}
	}
	
	private static Element createPageElement(String name, int page, int target) {
		ItemStack item = createItem(page == target ? ItemTypes.MAP : ItemTypes.PAPER, name + " (" + (target + 1) + ")", "");
		return page == target ? Element.of(item) : Element.of(item, inTask(a -> createPCMenu(a.getPlayer(), target).open(a.getPlayer())));
	}
	
	public static View createTradeMenu(Player player, int slot) {
		PlayerPartyStorage party = Pixelmon.storageManager.getParty(player.getUniqueId());
		return createTradeMenu(player, party.get(slot), "&bSlot " + (slot + 1), a -> Utils.trade(player, slot));
	}
	
	public static View createTradeMenu(Player player, int box, int pos) {
		PCStorage pc = Pixelmon.storageManager.getPCForPlayer(player.getUniqueId());
		pc.setLastBox(box);
		return createTradeMenu(player, pc.get(box, pos), "Box " + (box + 1) + ", Position " + (pos + 1), a -> Utils.trade(player, box, pos));
	}
	
	public static View createTradeMenu(Player player, Pokemon pokemon, String name, Consumer<Action.Click> action) {
		AtomicReference<Task> task = new AtomicReference<>(null);
		View view = View.builder()
				.archetype(InventoryArchetypes.CHEST)
				.property(InventoryTitle.of(Utils.toText("&cPoke&3Clash &6WonderTrade ")))
				.onClose(a -> { if (task.get() != null) task.get().cancel(); })
				.build(WonderTrade.getContainer());
		Element confirm;
		if (Config.defCooldown > 0 && !player.hasPermission("wondertrade.trade.no-defCooldown")) {
			long time = Utils.getCooldown(player) - (System.currentTimeMillis() - Config.getCooldown(player.getUniqueId()));
			Consumer<Action.Click> act = inTask(a -> {
				if (Config.resetCooldown(player.getUniqueId())) {
					action.accept(a);
					player.closeInventory();
				} else {
					player.sendMessage(WonderTrade.getMessage(player, "wondertrade.trade.reset-cooldown.failure"));
				}
			});
			if (time > 0) {
				confirm = Element.of(createItem(Sponge.getRegistry().getType(ItemType.class, "pixelmon:hourglass_silver").get(), "&cCooldown", "&4You must wait " + (time / 1000) + " seconds."));
				AtomicLong remaining = new AtomicLong(time / 1000);
				task.set(Task.builder()
						.execute(t -> view.setElement(10, remaining.get() <= 0 ? Element.of(createItem(ItemTypes.SLIME_BALL, "&aConfirm", "&2WonderTrade your " + Utils.getShortDesc(pokemon)), act) :
								Element.of(createItem(Sponge.getRegistry().getType(ItemType.class, "pixelmon:hourglass_silver").get(), "&cCooldown", "&4You must wait " + remaining.getAndDecrement() + " seconds."))))
						.interval(1, TimeUnit.SECONDS)
						.submit(WonderTrade.getContainer()));
			} else {
				confirm = Element.of(createItem(ItemTypes.SLIME_BALL, "&aConfirm", "&2WonderTrade your " + Utils.getShortDesc(pokemon)), act);
			}
		} else {
			confirm = Element.of(createItem(ItemTypes.SLIME_BALL, "&aConfirm", "&2WonderTrade your " + Utils.getShortDesc(pokemon)), inTask(a -> {
				action.accept(a);
				createMainMenu(player).open(player);
			}));
		}
		return view.define(Layout.builder()
				.from(MAIN)
				.set(RED, 12, 14)
				.set(BLACK, 11, 15)
				.set(Element.of(createPokemonItem(name, pokemon)), 13)
				.set(confirm, 10)
				.set(Element.of(createItem(ItemTypes.MAGMA_CREAM, "&cCancel", "&4Cancel this trade"), inTask(a -> createMainMenu(player).open(player))), 16)
				.build());
	}
	
	public static Page createPoolMenu(boolean take) {
		Page page = Page.builder()
				.archetype(InventoryArchetypes.DOUBLE_CHEST)
				.property(InventoryTitle.of(Utils.toText("&cPoke&3Clash &6WT &8Pool")))
				.layout(PAGE)
				.build(WonderTrade.getContainer());
		Element[] pool = new Element[Config.poolSize];
		for (int i = 0; i < Config.poolSize; i++) {
			int index = i;
			pool[i] = take ? Element.of(createPokemonItem("&bPosition " + (i + 1), Manager.trades[index]), inTask(a -> {
				Utils.take(a.getPlayer(), index);
				createPoolMenu(take).open(a.getPlayer(), index / 21);
			})) : Element.of(createPokemonItem("&bPosition " + (i + 1), Manager.trades[index]));
		}
		return page.define(Arrays.asList(pool));
	}
	
	private static ItemStack createItem(ItemType type, String name, String lore) {
		return ItemStack.builder()
				.itemType(type)
				.add(Keys.DISPLAY_NAME, Utils.toText(name))
				.add(Keys.ITEM_LORE, lore.isEmpty() ? Lists.newArrayList() : Lists.newArrayList(Utils.toText(lore)))
				.build();
	}
	
	private static ItemStack createPokemonItem(String name, TradeEntry entry) {
		String owner = entry.getOwner().equals(Utils.ZERO_UUID) ? "Server" : Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(entry.getOwner()).map(User::getName).orElse(entry.getOwner().toString());
		return ItemStack.builder().fromContainer(createItem((ItemType) PixelmonItems.itemPixelmonSprite, name, Utils.getDesc(entry.getPokemon()) + "\nOwner: " + owner).toContainer()
				.set(DataQuery.of("UnsafeData", "SpriteName"), getSpriteName(entry.getPokemon())))
				.build();
	}
	
	private static ItemStack createPokemonItem(String name, Pokemon pokemon) {
		return ItemStack.builder().fromContainer(createItem((ItemType) PixelmonItems.itemPixelmonSprite, name, Utils.getDesc(pokemon)).toContainer()
				.set(DataQuery.of("UnsafeData", "SpriteName"), getSpriteName(pokemon)))
				.build();
	}
	
	private static String getSpriteName(Pokemon pokemon) {
		if (pokemon.isEgg()) {
			EnumSpecies species = pokemon.getSpecies();
			int cycles = pokemon.getEggCycles();
			return "pixelmon:sprites/eggs/"
					+ (species == EnumSpecies.Togepi ? "togepi" : species == EnumSpecies.Manaphy ? "manaphy" : "egg")
					+ (cycles > 10 ? "1" : cycles > 5 ? "2" : "3");
		} else {
			return "pixelmon:" + GuiResources.getSpritePath(pokemon.getSpecies(), pokemon.getForm(), pokemon.getGender(), pokemon.getSpecialTexture() != EnumSpecialTexture.None, pokemon.isShiny());
		}
	}
}