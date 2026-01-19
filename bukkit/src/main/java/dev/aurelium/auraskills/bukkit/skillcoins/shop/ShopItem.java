package dev.aurelium.auraskills.bukkit.skillcoins.shop;

import dev.aurelium.auraskills.common.skillcoins.CurrencyType;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
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
    private final boolean isSpawnerPack;
    private final int packSize;
    private final boolean isMysteryBox;
    private final double mysteryRarity;

    public ShopItem(Material material, double buyPrice, double sellPrice) {
        this(material, buyPrice, sellPrice, new HashMap<>(), ItemType.REGULAR, null, 0, CurrencyType.COINS,
             null, null, false, 1, false, 0);
    }

    public ShopItem(Material material, double buyPrice, double sellPrice, Map<Enchantment, Integer> enchantments) {
        this(material, buyPrice, sellPrice, enchantments, ItemType.REGULAR, null, 0, CurrencyType.COINS,
             null, null, false, 1, false, 0);
    }

    public ShopItem(Material material, double buyPrice, double sellPrice, Map<Enchantment, Integer> enchantments,
                    ItemType type, String skillName, int tokenAmount, CurrencyType currency) {
        this(material, buyPrice, sellPrice, enchantments, type, skillName, tokenAmount, currency,
             null, null, false, 1, false, 0);
    }

    public ShopItem(Material material, double buyPrice, double sellPrice, Map<Enchantment, Integer> enchantments,
                    ItemType type, String skillName, int tokenAmount, CurrencyType currency,
                    EntityType spawnerType, SpawnerTier spawnerTier, boolean isSpawnerPack, int packSize,
                    boolean isMysteryBox, double mysteryRarity) {
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
        this.isSpawnerPack = isSpawnerPack;
        this.packSize = packSize;
        this.isMysteryBox = isMysteryBox;
        this.mysteryRarity = mysteryRarity;
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
        return sellPrice >= 0 && type == ItemType.REGULAR;
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

    public boolean isSpawnerPack() {
        return isSpawnerPack;
    }

    public int getPackSize() {
        return packSize;
    }

    public boolean isMysteryBox() {
        return isMysteryBox;
    }

    public double getMysteryRarity() {
        return mysteryRarity;
    }

    public double getEffectiveBuyPrice() {
        if (isSpawnerPack && packSize > 1) {
            return buyPrice;
        }
        return buyPrice;
    }

    public int getEffectiveAmount() {
        if (isSpawnerPack) {
            return packSize;
        }
        return 1;
    }

    public ItemStack createItemStack(int amount) {
        if (isSpawner()) {
            return createSpawnerItemStack(amount);
        }

        ItemStack item = new ItemStack(material, amount);

        if (!enchantments.isEmpty()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    private ItemStack createSpawnerItemStack(int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String displayName;

            if (isMysteryBox) {
                displayName = "§d§l? Mystery Spawner Box ?";
            } else if (spawnerType != null) {
                String entityName = spawnerType.name().replace("_", " ");
                String tierPrefix = spawnerTier != null ? spawnerTier.getPrefix() : "";
                displayName = "§a" + tierPrefix + entityName + " Spawner";

                if (isSpawnerPack) {
                    displayName = "§e" + tierPrefix + entityName + " Spawner Pack (§6x" + packSize + "§e)";
                }
            } else {
                displayName = "§aMonster Spawner";
            }

            meta.setDisplayName(displayName);

            java.util.List<String> lore = new java.util.ArrayList<>();

            if (isMysteryBox) {
                lore.add("");
                lore.add("§7Contains a random spawner!");
                lore.add("§7Rarity: " + getRarityDisplay());
            } else if (spawnerType != null && spawnerTier != null) {
                lore.add("");
                lore.add("§7Entity: §f" + spawnerType.name().replace("_", " "));
                lore.add("§7Tier: §f" + spawnerTier.name());

                if (spawnerTier != SpawnerTier.BASIC) {
                    lore.add("§7Spawn Rate: §f" + String.format("%.1fx", spawnerTier.getSpawnRateMultiplier()));
                }

                if (isSpawnerPack) {
                    lore.add("§7Amount: §f" + packSize + " spawners");
                }
            }

            lore.add("");

            if (canBuy()) {
                double price = getEffectiveBuyPrice();
                String currencySymbol = currency == CurrencyType.TOKENS ? "Tokens" : "Coins";
                lore.add("§aBuy: §f" + String.format("%,.0f", price) + " " + currencySymbol);

                if (isSpawnerPack && packSize > 1) {
                    double perUnit = price / packSize;
                    lore.add("§7  (§f" + String.format("%,.0f", perUnit) + "§7 each)");
                }
            } else {
                lore.add("§cCannot buy");
            }

            lore.add("");

            if (canSell()) {
                lore.add("§6Sell: §f" + String.format("%,.0f", sellPrice) + " Coins");
            } else {
                lore.add("§cCannot sell");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private String getRarityDisplay() {
        if (mysteryRarity >= 0.8) return "§4§lLEGENDARY §7(>80%)";
        if (mysteryRarity >= 0.5) return "§6§lEPIC §7(50-80%)";
        if (mysteryRarity >= 0.2) return "§5§lRARE §7(20-50%)";
        return "§9§lCOMMON §7(<20%)";
    }

    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != material) {
            return false;
        }

        if (hasEnchantments()) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasEnchants()) {
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

        return true;
    }
}
