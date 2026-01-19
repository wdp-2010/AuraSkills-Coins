package dev.aurelium.auraskills.bukkit.skillcoins.menu;

import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.skillcoins.shop.ShopItem;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class TierSelectionMenu {

    private final AuraSkills plugin;
    private final EconomyProvider economy;
    private final String menuTitle = ChatColor.of("#90EE90") + "Select Tier";
    private static final ConcurrentHashMap<UUID, EntityType> playerSelections = new ConcurrentHashMap<>();
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0");

    private final Map<ShopItem.SpawnerTier, Material> tierMaterials = new EnumMap<>(ShopItem.SpawnerTier.class);
    private final Map<ShopItem.SpawnerTier, String> tierColors = new EnumMap<>(ShopItem.SpawnerTier.class);

    public TierSelectionMenu(AuraSkills plugin, EconomyProvider economy) {
        this.plugin = plugin;
        this.economy = economy;

        tierMaterials.put(ShopItem.SpawnerTier.BASIC, Material.SKELETON_SKULL);
        tierMaterials.put(ShopItem.SpawnerTier.ENHANCED, Material.BLAZE_POWDER);
        tierMaterials.put(ShopItem.SpawnerTier.HYPER, Material.MAGMA_CREAM);
        tierMaterials.put(ShopItem.SpawnerTier.OMEGA, Material.NETHER_STAR);

        tierColors.put(ShopItem.SpawnerTier.BASIC, "#AAAAAA");
        tierColors.put(ShopItem.SpawnerTier.ENHANCED, "#55FF55");
        tierColors.put(ShopItem.SpawnerTier.HYPER, "#FFAA00");
        tierColors.put(ShopItem.SpawnerTier.OMEGA, "#FF5555");
    }

    public void open(Player player, EntityType entityType) {
        if (player == null || !player.isOnline()) return;

        playerSelections.put(player.getUniqueId(), entityType);

        try {
            Inventory inv = Bukkit.createInventory(null, 27, menuTitle);

            fillBorder(inv);
            addEntityDisplay(inv, entityType);
            addTierButtons(inv, entityType);
            addBackButton(inv);

            player.openInventory(inv);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error opening tier selection menu", e);
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

    private void addEntityDisplay(Inventory inv, EntityType entityType) {
        ItemStack display = new ItemStack(Material.SPAWNER);
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            String entityName = entityType.name().replace("_", " ");
            meta.setDisplayName(ChatColor.of("#00FFFF") + entityName + " Spawner");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#808080") + "Click a tier below to select");
            lore.add(ChatColor.of("#808080") + "the spawn rate you want:");
            lore.add("");
            lore.add(ChatColor.of("#AAAAAA") + " BASIC  │ 1.0x spawns │ 1x price");
            lore.add(ChatColor.of("#55FF55") + "ENHANCED │ 1.5x spawns │ 3x price");
            lore.add(ChatColor.of("#FFAA00") + "  HYPER  │ 2.0x spawns │ 6x price");
            lore.add(ChatColor.of("#FF5555") + "  OMEGA  │ 3.0x spawns │ 12x price");
            lore.add("");
            lore.add(ChatColor.of("#FFFF00") + "→ Select a tier to continue");

            meta.setLore(lore);
            display.setItemMeta(meta);
        }
        inv.setItem(4, display);
    }

    private void addTierButtons(Inventory inv, EntityType entityType) {
        ShopItem.SpawnerTier[] tiers = ShopItem.SpawnerTier.values();

        int slot = 9;
        for (ShopItem.SpawnerTier tier : tiers) {
            ItemStack tierItem = new ItemStack(tierMaterials.get(tier));
            ItemMeta meta = tierItem.getItemMeta();
            if (meta != null) {
                String tierName = tier.name();
                String color = tierColors.get(tier);

                meta.setDisplayName(ChatColor.of(color) + tierName + " Tier");

                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.of("#808080") + "Spawn Rate: " + ChatColor.of(color) +
                    String.format("%.1fx", tier.getSpawnRateMultiplier()));
                lore.add("");

                double price = getPriceFromConfig(entityType, tier);
                lore.add(ChatColor.of("#55FF55") + "Price: " + ChatColor.of("#FFFFFF") +
                    MONEY_FORMAT.format(price) + ChatColor.of("#FFFF00") + " Coins");

                lore.add("");
                lore.add(ChatColor.of("#808080") + "Click to confirm purchase");

                meta.setLore(lore);
                tierItem.setItemMeta(meta);
            }
            inv.setItem(slot, tierItem);
            slot++;
        }
    }

    private double getPriceFromConfig(EntityType entityType, ShopItem.SpawnerTier tier) {
        try {
            dev.aurelium.auraskills.bukkit.skillcoins.shop.ShopLoader loader = plugin.getShopLoader();
            if (loader != null) {
                ShopItem item = loader.getSpawnerItem(entityType, tier);
                if (item != null) {
                    return item.getBuyPrice();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get spawner price from config: " + e.getMessage());
        }

        return 100000;
    }

    private void addBackButton(Inventory inv) {
        ItemStack back = new ItemStack(Material.SPYGLASS);
        ItemMeta meta = back.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "← Back to Shop");
            back.setItemMeta(meta);
        }
        inv.setItem(26, back);
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
        EntityType entityType = playerSelections.get(player.getUniqueId());

        if (entityType == null) {
            player.closeInventory();
            return;
        }

        if (slot == 26) {
            playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            MenuManager manager = MenuManager.getInstance(plugin);
            if (manager != null) {
                manager.openMainMenu(player);
            }
            return;
        }

        if (slot >= 9 && slot <= 12) {
            ShopItem.SpawnerTier tier = ShopItem.SpawnerTier.values()[slot - 9];
            playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);

            openConfirmMenu(player, entityType, tier);
        }
    }

    private void openConfirmMenu(Player player, EntityType entityType, ShopItem.SpawnerTier tier) {
        SpawnerConfirmMenu confirmMenu = new SpawnerConfirmMenu(plugin, economy, entityType, tier);
        confirmMenu.open(player);
    }

    private void playSound(Player player, Sound sound, float volume, float pitch) {
        if (player == null || sound == null) return;
        try {
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception ignored) {}
    }

    public void handleClose(InventoryCloseEvent event) {
        if (event != null && event.getPlayer() instanceof Player) {
            playerSelections.remove(((Player) event.getPlayer()).getUniqueId());
        }
    }

    public boolean isMenuTitle(String title) {
        return title != null && title.equals(menuTitle);
    }
}
