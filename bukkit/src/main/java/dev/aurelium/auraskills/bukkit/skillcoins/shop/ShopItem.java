package dev.aurelium.auraskills.bukkit.skillcoins.shop;

import dev.aurelium.auraskills.common.skillcoins.CurrencyType;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class ShopItem {

    public enum ItemType {
        REGULAR,
        SKILL_LEVEL,
        TOKEN_EXCHANGE,
        SPAWNER
    }

    public enum SpawnerTier {
        BASIC(1.0, ""),
        ENHANCED(1.5, "✦ "),
        HYPER(2.0, "✦✦ "),
        OMEGA(3.0, "✦✦✦ ");

        private final double spawnRateMultiplier;
        private final String prefix;

        SpawnerTier(double spawnRateMultiplier, String prefix) {
            this.spawnRateMultiplier = spawnRateMultiplier;
            this.prefix = prefix;
        }

        public double getSpawnRateMultiplier() {
            return spawnRateMultiplier;
        }

        public String getPrefix() {
            return prefix;
        }

        public int getPriceMultiplier() {
            switch (this) {
                case BASIC: return 1;
                case ENHANCED: return 3;
                case HYPER: return 6;
                case OMEGA: return 12;
                default: return 1;
            }
        }
    }

    private final Material material;
    private final double buyPrice;
    private final double sellPrice;
    private final Map<Enchantment, Integer> enchantments;
    private final ItemType type;
    private final String skillName;
    private final int tokenAmount;
    private final CurrencyType currency;
    private final EntityType spawnerType;
    private final SpawnerTier spawnerTier;
    private final int packSize;

    public ShopItem(Material material, double buyPrice, double sellPrice) {
        this(material, buyPrice, sellPrice, new HashMap<>(), ItemType.REGULAR, null, 0, CurrencyType.COINS,
             null, null, 1);
    }

    public ShopItem(Material material, double buyPrice, double sellPrice, Map<Enchantment, Integer> enchantments) {
        this(material, buyPrice, sellPrice, enchantments, ItemType.REGULAR, null, 0, CurrencyType.COINS,
             null, null, 1);
    }

    public ShopItem(Material material, double buyPrice, double sellPrice, Map<Enchantment, Integer> enchantments,
                    ItemType type, String skillName, int tokenAmount, CurrencyType currency) {
        this(material, buyPrice, sellPrice, enchantments, type, skillName, tokenAmount, currency,
             null, null, 1);
    }

    public ShopItem(Material material, double buyPrice, double sellPrice, Map<Enchantment, Integer> enchantments,
                    ItemType type, String skillName, int tokenAmount, CurrencyType currency,
                    EntityType spawnerType, SpawnerTier spawnerTier, int packSize) {
        this.material = material;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.enchantments = new HashMap<>(enchantments);
        this.type = type;
        this.skillName = skillName;
        this.tokenAmount = tokenAmount;
        this.currency = currency;
        this.spawnerType = spawnerType;
        this.spawnerTier = spawnerTier;
        this.packSize = packSize;
    }

    public Material getMaterial() {
        return material;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return new HashMap<>(enchantments);
    }

    public boolean hasEnchantments() {
        return !enchantments.isEmpty();
    }

    public ItemType getType() {
        return type;
    }

    public String getSkillName() {
        return skillName;
    }

    public int getTokenAmount() {
        return tokenAmount;
    }

    public CurrencyType getCurrency() {
        return currency;
    }

    public boolean canBuy() {
        return buyPrice >= 0;
    }

    public boolean canSell() {
        return sellPrice >= 0;
    }

    public boolean isSpawner() {
        return type == ItemType.SPAWNER;
    }

    public EntityType getSpawnerType() {
        return spawnerType;
    }

    public SpawnerTier getSpawnerTier() {
        return spawnerTier;
    }

    public int getPackSize() {
        return packSize;
    }

    public ItemStack createItemStack(int amount) {
        if (isSpawner()) {
            return createSpawnerItemStack(amount);
        }

        ItemStack item = new ItemStack(material, amount);

        if (!enchantments.isEmpty()) {
            if (material == Material.ENCHANTED_BOOK) {
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                if (meta != null) {
                    for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                        meta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
                    }
                    item.setItemMeta(meta);
                }
            } else {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                        meta.addEnchant(entry.getKey(), entry.getValue(), true);
                    }
                    item.setItemMeta(meta);
                }
            }
        }

        return item;
    }

    private ItemStack createSpawnerItemStack(int amount) {
        ItemStack item = new ItemStack(Material.SPAWNER, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String displayName;

            if (spawnerType != null) {
                String entityName = spawnerType.name().replace("_", " ");
                String tierPrefix = spawnerTier != null ? spawnerTier.getPrefix() : "";
                displayName = "§a" + tierPrefix + entityName + " Spawner";
            } else {
                displayName = "§aMonster Spawner";
            }

            meta.setDisplayName(displayName);

            java.util.List<String> lore = new java.util.ArrayList<>();

            if (spawnerType != null && spawnerTier != null) {
                String entityName = spawnerType.name().replace("_", " ");
                lore.add("");
                lore.add("§7Entity: §f" + entityName);
                lore.add("§7Tier: §f" + spawnerTier.name());

                if (spawnerTier != SpawnerTier.BASIC) {
                    lore.add("§7Spawn Rate: §f" + String.format("%.1fx", spawnerTier.getSpawnRateMultiplier()));
                }
            }

            lore.add("");

            if (canBuy()) {
                double price = buyPrice;
                String currencySymbol = currency == CurrencyType.TOKENS ? "Tokens" : "Coins";
                lore.add("§aBuy: §f" + String.format("%,.0f", price) + " " + currencySymbol);
            } else {
                lore.add("§cCannot buy");
            }

            lore.add("");

            if (canSell()) {
                lore.add("§6Sell: §f" + String.format("%,.0f", sellPrice) + " Coins");
            } else {
                lore.add("§cCannot sell");
            }

            lore.add("");
            lore.add("§7Left-click to purchase");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != material) {
            return false;
        }

        if (hasEnchantments()) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return false;
            }

            if (material == Material.ENCHANTED_BOOK) {
                if (!(meta instanceof EnchantmentStorageMeta)) {
                    return false;
                }
                EnchantmentStorageMeta storageMeta = (EnchantmentStorageMeta) meta;
                Map<Enchantment, Integer> storedEnchants = storageMeta.getStoredEnchants();
                if (storedEnchants.size() != enchantments.size()) {
                    return false;
                }
                for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                    Integer itemLevel = storedEnchants.get(entry.getKey());
                    if (itemLevel == null || !itemLevel.equals(entry.getValue())) {
                        return false;
                    }
                }
            } else {
                if (!meta.hasEnchants()) {
                    return false;
                }
                Map<Enchantment, Integer> itemEnchants = meta.getEnchants();
                if (itemEnchants.size() != enchantments.size()) {
                    return false;
                }
                for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                    Integer itemLevel = itemEnchants.get(entry.getKey());
                    if (itemLevel == null || !itemLevel.equals(entry.getValue())) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
