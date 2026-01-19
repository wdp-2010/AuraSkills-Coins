package dev.aurelium.auraskills.bukkit.skillcoins.menu;

import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.skillcoins.shop.ShopItem;
import dev.aurelium.auraskills.common.skillcoins.CurrencyType;
import dev.aurelium.auraskills.common.skillcoins.EconomyProvider;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class SpawnerConfirmMenu {

    private final AuraSkills plugin;
    private final EconomyProvider economy;
    private final EntityType entityType;
    private final ShopItem.SpawnerTier tier;
    private final String menuTitle = ChatColor.of("#FFD700") + "Confirm Purchase";
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0");
    private static final int MENU_SIZE = 45;
    private static final int[] NAVBAR_SLOTS = {36, 37, 38, 39, 40, 41, 42, 43, 44};

    public SpawnerConfirmMenu(AuraSkills plugin, EconomyProvider economy, EntityType entityType, ShopItem.SpawnerTier tier) {
        this.plugin = plugin;
        this.economy = economy;
        this.entityType = entityType;
        this.tier = tier;
    }

    public void open(Player player) {
        if (player == null || !player.isOnline()) return;

        try {
            Inventory inv = Bukkit.createInventory(null, MENU_SIZE, menuTitle);

            fillBorder(inv);
            addDenyButton(inv);
            addConfirmButton(inv);
            addExplanationPaper(inv);
            addSpawnerPreview(inv);
            addBackButton(inv);
            addNavbar(inv);

            player.openInventory(inv);

            MenuManager manager = MenuManager.getInstance(plugin);
            if (manager != null) {
                manager.registerTransactionMenu(player, null);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error opening confirm menu", e);
            player.sendMessage(ChatColor.RED + "An error occurred!");
        }
    }

    private void fillBorder(Inventory inv) {
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            border.setItemMeta(meta);
        }

        for (int i = 0; i < MENU_SIZE; i++) {
            boolean isNavbarArea = i >= 36 && i <= 44;
            if (!isNavbarArea && (i < 9 || i >= 27 || (i > 6 && i < 36))) {
                inv.setItem(i, border);
            }
        }
    }

    private void addDenyButton(Inventory inv) {
        ItemStack deny = new ItemStack(Material.RED_CONCRETE);
        ItemMeta meta = deny.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#FF5555") + "✖ DENY");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#808080") + "Click to cancel");
            lore.add(ChatColor.of("#808080") + "and go back");
            meta.setLore(lore);
            deny.setItemMeta(meta);
        }
        inv.setItem(2, deny);
    }

    private void addConfirmButton(Inventory inv) {
        ItemStack confirm = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta meta = confirm.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#55FF55") + "✔ CONFIRM");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#808080") + "Click to purchase");
            lore.add(ChatColor.of("#808080") + "this spawner");
            meta.setLore(lore);
            confirm.setItemMeta(meta);
        }
        inv.setItem(6, confirm);
    }

    private void addExplanationPaper(Inventory inv) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#FFFF00") + "Purchase Summary");

            double price = getPriceFromConfig();

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#FFFFFF") + "Entity: " + ChatColor.of("#00FFFF") +
                entityType.name().replace("_", " "));
            lore.add(ChatColor.of("#FFFFFF") + "Tier: " + ChatColor.of("#FFD700") + tier.name());
            lore.add("");

            lore.add(ChatColor.of("#55FF55") + "Price: " + ChatColor.of("#FFFFFF") +
                MONEY_FORMAT.format(price) + ChatColor.of("#FFFF00") + " Coins");
            lore.add("");

            if (tier != ShopItem.SpawnerTier.BASIC) {
                lore.add(ChatColor.of("#808080") + "Spawn Rate: " +
                    ChatColor.of("#55FF55") + String.format("%.1fx", tier.getSpawnRateMultiplier()));
                lore.add(ChatColor.of("#808080") + "(" + tier.getPriceMultiplier() + "x base price)");
            } else {
                lore.add(ChatColor.of("#808080") + "Spawn Rate: 1.0x (normal)");
            }
            lore.add("");
            lore.add(ChatColor.of("#808080") + "Click " + ChatColor.of("#55FF55") + "GREEN" +
                ChatColor.of("#808080") + " to confirm");
            lore.add(ChatColor.of("#808080") + "Click " + ChatColor.of("#FF5555") + "RED" +
                ChatColor.of("#808080") + " to cancel");

            meta.setLore(lore);
            paper.setItemMeta(meta);
        }
        inv.setItem(4, paper);
    }

    private void addSpawnerPreview(Inventory inv) {
        ItemStack spawner = new ItemStack(Material.SPAWNER);
        ItemMeta meta = spawner.getItemMeta();
        if (meta != null) {
            String tierPrefix = tier.getPrefix();
            String entityName = entityType.name().replace("_", " ");

            String tierColor = getTierColor(tier);
            meta.setDisplayName(ChatColor.of(tierColor) + tierPrefix + entityName + " Spawner");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#808080") + "This is what you will receive:");
            lore.add("");
            lore.add(ChatColor.of("#808080") + "Spawns: " + ChatColor.of("#FFFFFF") + entityName);
            lore.add(ChatColor.of("#808080") + "Rate: " + ChatColor.of(tierColor) +
                String.format("%.1fx", tier.getSpawnRateMultiplier()));

            meta.setLore(lore);
            spawner.setItemMeta(meta);
        }
        inv.setItem(13, spawner);
    }

    private String getTierColor(ShopItem.SpawnerTier tier) {
        switch (tier) {
            case BASIC: return "#AAAAAA";
            case ENHANCED: return "#55FF55";
            case HYPER: return "#FFAA00";
            case OMEGA: return "#FF5555";
            default: return "#FFFFFF";
        }
    }

    private void addBackButton(Inventory inv) {
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta meta = back.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#FFFF00") + "? Change Tier");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#808080") + "Click to select");
            lore.add(ChatColor.of("#808080") + "a different tier");
            meta.setLore(lore);
            back.setItemMeta(meta);
        }
        inv.setItem(26, back);
    }

    private void addNavbar(Inventory inv) {
        ItemStack back = new ItemStack(Material.SPYGLASS);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.RED + "← Back to Shop");
            back.setItemMeta(backMeta);
        }
        inv.setItem(36, back);

        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        if (pageMeta != null) {
            pageMeta.setDisplayName(ChatColor.of("#FFD700") + "Confirm Purchase");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#808080") + "Review your purchase");
            lore.add(ChatColor.of("#808080") + "before confirming");
            pageMeta.setLore(lore);
            pageInfo.setItemMeta(pageMeta);
        }
        inv.setItem(40, pageInfo);

        ItemStack home = new ItemStack(Material.BOOK);
        ItemMeta homeMeta = home.getItemMeta();
        if (homeMeta != null) {
            homeMeta.setDisplayName(ChatColor.of("#00FFFF") + "✦ Main Menu");
            home.setItemMeta(homeMeta);
        }
        inv.setItem(44, home);
    }

    private double getPriceFromConfig() {
        try {
            dev.aurelium.auraskills.bukkit.skillcoins.shop.ShopLoader loader = plugin.getShopLoader();
            if (loader != null) {
                ShopItem item = loader.getSpawnerItem(entityType, tier);
                if (item != null && item.canBuy()) {
                    return item.getBuyPrice();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get spawner price from config: " + e.getMessage());
        }
        return 100000;
    }

    public void handleClick(InventoryClickEvent event) {
        if (event == null || event.isCancelled()) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR ||
            clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            return;
        }

        int slot = event.getSlot();

        if (slot == 6) {
            performPurchase(player);
        } else if (slot == 2) {
            playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 0.9f);
            player.closeInventory();
            TierSelectionMenu tierMenu = new TierSelectionMenu(plugin, economy);
            tierMenu.open(player, entityType);
        } else if (slot == 26) {
            playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            player.closeInventory();
            TierSelectionMenu tierMenu = new TierSelectionMenu(plugin, economy);
            tierMenu.open(player, entityType);
        } else if (slot == 36) {
            playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            player.closeInventory();
            MenuManager manager = MenuManager.getInstance(plugin);
            if (manager != null) {
                manager.openMainMenu(player);
            }
        } else if (slot == 44) {
            playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            player.closeInventory();
            MenuManager manager = MenuManager.getInstance(plugin);
            if (manager != null) {
                manager.openMainMenu(player);
            }
        }
    }

    private void performPurchase(Player player) {
        UUID uuid = player.getUniqueId();

        double price = getPriceFromConfig();

        try {
            if (!economy.hasBalance(uuid, CurrencyType.COINS, price)) {
                player.sendMessage(ChatColor.RED + "✖ " + ChatColor.WHITE +
                    "You don't have enough Coins!");
                playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            if (!hasInventorySpace(player)) {
                player.sendMessage(ChatColor.RED + "✖ " + ChatColor.WHITE +
                    "Your inventory is full!");
                playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            economy.subtractBalance(uuid, CurrencyType.COINS, price);

            ShopItem spawnerItem = new ShopItem(
                Material.SPAWNER,
                price,
                price * 0.5,
                new java.util.HashMap<>(),
                ShopItem.ItemType.SPAWNER,
                null, 0, CurrencyType.COINS,
                entityType,
                tier,
                1
            );

            ItemStack spawner = spawnerItem.createItemStack(1);
            player.getInventory().addItem(spawner);

            String entityName = entityType.name().replace("_", " ");
            player.sendMessage(ChatColor.of("#55FF55") + "✔ Purchase Successful!");
            player.sendMessage(ChatColor.WHITE + "Bought " + ChatColor.of("#00FFFF") +
                tier.getPrefix() + entityName + " Spawner" + ChatColor.WHITE + " for " +
                ChatColor.of("#FFD700") + MONEY_FORMAT.format(price) + " Coins");

            playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            player.closeInventory();

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Purchase failed", e);
            player.sendMessage(ChatColor.RED + "✖ Transaction failed!");
            playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private boolean hasInventorySpace(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                return true;
            }
        }
        return false;
    }

    private void playSound(Player player, Sound sound, float volume, float pitch) {
        if (player == null || sound == null) return;
        try {
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception ignored) {}
    }

    public void handleClose(InventoryCloseEvent event) {
    }

    public boolean isMenuTitle(String title) {
        return title != null && title.equals(menuTitle);
    }
}
