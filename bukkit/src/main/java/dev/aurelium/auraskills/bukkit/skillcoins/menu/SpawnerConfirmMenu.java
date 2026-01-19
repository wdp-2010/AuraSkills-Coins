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
    private final String menuTitle;
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0");

    public SpawnerConfirmMenu(AuraSkills plugin, EconomyProvider economy, EntityType entityType, ShopItem.SpawnerTier tier) {
        this.plugin = plugin;
        this.economy = economy;
        this.entityType = entityType;
        this.tier = tier;
        this.menuTitle = ChatColor.of("#FFD700") + "Confirm Purchase";
    }

    public void open(Player player) {
        if (player == null || !player.isOnline()) return;

        try {
            Inventory inv = Bukkit.createInventory(null, 27, menuTitle);

            fillBorder(inv);
            addConfirmButton(inv, true);
            addConfirmButton(inv, false);
            addExplanationPaper(inv);
            addSpawnerPreview(inv);
            addBackButton(inv);

            player.openInventory(inv);
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

        for (int i = 0; i < 27; i++) {
            if (i < 9 || i >= 18) {
                inv.setItem(i, border);
            }
        }
    }

    private void addConfirmButton(Inventory inv, boolean isConfirm) {
        int slot = isConfirm ? 24 : 18;

        Material material = isConfirm ? Material.GREEN_CONCRETE : Material.RED_CONCRETE;
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();

        if (meta != null) {
            if (isConfirm) {
                meta.setDisplayName(ChatColor.of("#55FF55") + "✔ CONFIRM");
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.of("#808080") + "Click to purchase this spawner");
                meta.setLore(lore);
            } else {
                meta.setDisplayName(ChatColor.of("#FF5555") + "✖ DENY");
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.of("#808080") + "Click to cancel and go back");
                meta.setLore(lore);
            }
            button.setItemMeta(meta);
        }
        inv.setItem(slot, button);
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
        inv.setItem(13, paper);
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
            lore.add(ChatColor.of("#808080") + "This is a preview of the");
            lore.add(ChatColor.of("#808080") + "spawner you will receive.");
            lore.add("");
            lore.add(ChatColor.of("#808080") + "Spawns: " + ChatColor.of("#FFFFFF") + entityName);
            lore.add(ChatColor.of("#808080") + "Rate: " + ChatColor.of(tierColor) +
                String.format("%.1fx", tier.getSpawnRateMultiplier()));

            meta.setLore(lore);
            spawner.setItemMeta(meta);
        }
        inv.setItem(4, spawner);
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
        ItemStack back = new ItemStack(Material.SPYGLASS);
        ItemMeta meta = back.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#FFFF00") + "? Change Tier");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#808080") + "Click to select a different tier");
            meta.setLore(lore);
            back.setItemMeta(meta);
        }
        inv.setItem(26, back);
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

        plugin.getLogger().severe("NO PRICE FOUND IN CONFIG for " + entityType + " " + tier +
            " - Check Spawners.yml! Returning 0 to prevent sale.");
        return 0;
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

        if (slot == 24) {
            performPurchase(player);
        } else if (slot == 18) {
            playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 0.9f);
            TierSelectionMenu tierMenu = new TierSelectionMenu(plugin, economy);
            tierMenu.open(player, entityType);
        } else if (slot == 26) {
            playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            TierSelectionMenu tierMenu = new TierSelectionMenu(plugin, economy);
            tierMenu.open(player, entityType);
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
