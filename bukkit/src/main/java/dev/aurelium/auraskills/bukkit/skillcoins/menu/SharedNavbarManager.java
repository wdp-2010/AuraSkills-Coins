package dev.aurelium.auraskills.bukkit.skillcoins.menu;

import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.skillcoins.CurrencyType;
import dev.aurelium.auraskills.common.skillcoins.EconomyProvider;
import dev.aurelium.auraskills.common.message.MessageKey;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.auraskills.common.util.text.Replacer;
import dev.aurelium.auraskills.common.util.text.TextUtil;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Level; 

/**
 * Shared navbar manager for consistent navigation across all menus
 */
public class SharedNavbarManager {
    
    private final AuraSkills plugin;
    private final EconomyProvider economy;
    private final FileConfiguration navbarConfig;
    
    // Exception menus that only show back button
    private static final List<String> SKILL_SELECTION_MENUS = Arrays.asList(
        "skills", "skill_select", "skill_abilities", "skill_sources", "skill_stats", "stat_info"
    );
    
    public SharedNavbarManager(AuraSkills plugin, EconomyProvider economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.navbarConfig = loadNavbarConfig();
    }
    
    private FileConfiguration loadNavbarConfig() {
        try {
            File file = new File(plugin.getDataFolder(), "menus/shared_navbar.yml");
            if (!file.exists()) {
                plugin.saveResource("menus/shared_navbar.yml", false);
            }
            return YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load navbar config", e);
            return null;
        }
    }
    
    /**
     * Add consistent navbar to any inventory
     */
    public void addNavbar(Inventory inv, String menuName, int page, int maxPage, Player player) {
        if (navbarConfig == null) {
            addDefaultNavbar(inv, page, maxPage, player);
            return;
        }
        
        // Check if this is a skill selection menu (exceptions)
        if (isSkillSelectionMenu(menuName)) {
            addSkillSelectionNavbar(inv, menuName);
            return;
        }
        
        // Add standard navbar
        addStandardNavbar(inv, page, maxPage, player);
    }
    
    private boolean isSkillSelectionMenu(String menuName) {
        return SKILL_SELECTION_MENUS.contains(menuName);
    }
    
    private void addSkillSelectionNavbar(Inventory inv, String menuName) {
        // Only show back button for skill selection menus
        String backText = menuName.equals("skills") ? "Close" : "Back";
        
        ItemStack back = new ItemStack(Material.SPYGLASS);
        ItemMeta meta = back.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + backText);
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Return to main menu");
            meta.setLore(lore);
            back.setItemMeta(meta);
        }
        inv.setItem(53, back);
    }
    
    private void addStandardNavbar(Inventory inv, int page, int maxPage, Player player) {
        // Balance display (slot 45)
        addBalanceDisplay(inv, player);
        
        // Glass panes (slots 46, 47, 51, 52)
        addGlassPanes(inv);
        
        // Previous page button (slot 48)
        addPreviousPageButton(inv, page);
        
        // Page info (slot 49)
        addPageInfo(inv, page, maxPage);
        
        // Next page button (slot 50)
        addNextPageButton(inv, page, maxPage);
        
        // Back button (slot 53)
        addBackButton(inv);
    }
    
    private void addDefaultNavbar(Inventory inv, int page, int maxPage, Player player) {
        // Fallback to default navbar if config fails
        addBalanceDisplay(inv, player);
        addGlassPanes(inv);
        
        if (page > 0) {
            addPreviousPageButton(inv, page);
        }
        
        addPageInfo(inv, page, maxPage);
        
        if (page < maxPage) {
            addNextPageButton(inv, page, maxPage);
        }
        
        addBackButton(inv);
    }
    
    private void addBalanceDisplay(Inventory inv, Player player) {
        if (navbarConfig == null) return;
        
        User user = plugin.getUser(player);
        if (user == null) return;
        
        // Get balance configuration
        if (!navbarConfig.contains("navbar.balance")) {
            return;
        }
        
        double moneyBalance = economy.getBalance(player.getUniqueId(), CurrencyType.COINS);
        double tokenBalance = economy.getBalance(player.getUniqueId(), CurrencyType.TOKENS);
        String formattedMoney = String.format("%,.0f", moneyBalance);
        String formattedTokens = String.format("%,.0f", tokenBalance);
        
        // Create replacer for data placeholders
        Replacer replacer = new Replacer()
            .map("{balance}", () -> formattedMoney)
            .map("{coins}", () -> formattedMoney)
            .map("{tokens}", () -> formattedTokens);
        
        // Get navbar balance config
        String materialName = navbarConfig.getString("navbar.balance.material", "GOLD_NUGGET");
        Material material = Material.valueOf(materialName.toUpperCase());
        
        ItemStack balanceItem = new ItemStack(material);
        ItemMeta meta = balanceItem.getItemMeta();
        if (meta != null) {
            // Apply placeholders to display name
            String displayName = navbarConfig.getString("navbar.balance.display_name", "{{balance}}");
            displayName = applyMenuPlaceholders(displayName, player, "navbar", replacer);
            meta.setDisplayName(displayName);
            
            // Apply placeholders to lore
            List<String> lore = new ArrayList<>();
            List<String> configLore = navbarConfig.getStringList("navbar.balance.lore");
            for (String line : configLore) {
                line = applyMenuPlaceholders(line, player, "navbar", replacer);
                lore.add(line);
            }
            meta.setLore(lore);
            balanceItem.setItemMeta(meta);
        }
        
        // Get slot from config
        int slot = navbarConfig.getInt("navbar.balance.slot", 45);
        inv.setItem(slot, balanceItem);
    }
    
    private String applyMenuPlaceholders(String text, Player player, String menuName, Replacer replacer) {
        // First replace data placeholders in the original text
        text = TextUtil.replace(text, replacer);
        
        // Then replace menu message placeholders
        String[] placeholders = dev.aurelium.slate.util.TextUtil.substringsBetween(text, "{{", "}}");
        if (placeholders != null) {
            for (String placeholder : placeholders) {
                MessageKey messageKey = MessageKey.of("menus.navbar." + placeholder);
                MessageKey commonKey = MessageKey.of("menus.common." + placeholder);
                
                // Get locale - use player's locale if available, otherwise use default
                java.util.Locale locale;
                if (player != null) {
                    locale = plugin.getUser(player).getLocale();
                } else {
                    locale = plugin.getMessageProvider().getDefaultLanguage();
                }
                
                String message = plugin.getMessageProvider().getRaw(messageKey, locale);
                if (message.equals(messageKey.getPath())) { // Key not found, try common
                    message = plugin.getMessageProvider().getRaw(commonKey, locale);
                }
                
                if (!message.equals(commonKey.getPath())) { // Found a valid message
                    // Replace the {{placeholder}} with the message
                    text = text.replace("{{" + placeholder + "}}", message);
                }
            }
        }
        
        // Finally, apply the replacer again to handle any data placeholders that were in the messages
        text = TextUtil.replace(text, replacer);
        
        // Replace color placeholders and translate color codes
        text = text.replace("<black>", "&0")
            .replace("<dark_blue>", "&1")
            .replace("<dark_green>", "&2")
            .replace("<dark_aqua>", "&3")
            .replace("<dark_red>", "&4")
            .replace("<dark_purple>", "&5")
            .replace("<gold>", "&6")
            .replace("<yellow>", "&e")
            .replace("<dark_yellow>", "&6")
            .replace("<green>", "&a")
            .replace("<aqua>", "&b")
            .replace("<blue>", "&9")
            .replace("<light_purple>", "&d")
            .replace("<purple>", "&5")
            .replace("<white>", "&f")
            .replace("<gray>", "&7")
            .replace("<dark_gray>", "&8")
            // Formatting codes
            .replace("<bold>", "&l")
            .replace("<italic>", "&o")
            .replace("<underline>", "&n")
            .replace("<strikethrough>", "&m")
            .replace("<obfuscated>", "&k")
            .replace("<reset>", "&r");
        
        // Replace hex color codes <#RRGGBB>
        Pattern hexPattern = Pattern.compile("<#([0-9a-fA-F]{6})>");
        Matcher matcher = hexPattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(sb, ChatColor.of("#" + hex).toString());
        }
        matcher.appendTail(sb);
        text = sb.toString();
        
        text = ChatColor.translateAlternateColorCodes('&', text);
        
        return text;
    }
    
    private void addGlassPanes(Inventory inv) {
        if (navbarConfig == null) return;

        // New unified format: glass_fill with slots array
        if (navbarConfig.contains("navbar.glass_fill")) {
            String materialName = navbarConfig.getString("navbar.glass_fill.material", "BLACK_STAINED_GLASS_PANE");
            Material material;
            try {
                material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException ex) {
                AuraSkills.getPlugin().getLogger().log(
                        Level.WARNING,
                        "Invalid material '" + materialName + "' for navbar glass_fill. Using BLACK_STAINED_GLASS_PANE instead.",
                        ex
                );
                material = Material.BLACK_STAINED_GLASS_PANE;
            }
            String displayName = navbarConfig.getString("navbar.glass_fill.display_name", " ");

            ItemStack glass = new ItemStack(material);
            ItemMeta meta = glass.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(displayName);
                glass.setItemMeta(meta);
            }

            List<Integer> slots = navbarConfig.getIntegerList("navbar.glass_fill.slots");
            for (int slot : slots) {
                if (slot >= 45 && slot <= 53) {
                    inv.setItem(slot, glass);
                }
            }
            return;
        }

        // Old list format: navbar.glass: [...]
        if (navbarConfig.contains("navbar.glass")) {
            List<Map<?, ?>> glassList = navbarConfig.getMapList("navbar.glass");
            for (Map<?, ?> entry : glassList) {
                Object materialObj = entry.get("material");
                String materialName = materialObj != null ? String.valueOf(materialObj) : "BLACK_STAINED_GLASS_PANE";
                Material material = Material.valueOf(materialName.toUpperCase());

                ItemStack glass = new ItemStack(material);
                ItemMeta meta = glass.getItemMeta();
                if (meta != null) {
                    Object displayObj = entry.get("display_name");
                    String displayName = displayObj != null ? String.valueOf(displayObj) : " ";
                    meta.setDisplayName(displayName);
                    glass.setItemMeta(meta);
                }

                int slot;
                Object slotObj = entry.get("slot");
                if (slotObj instanceof Number) {
                    slot = ((Number) slotObj).intValue();
                } else if (slotObj != null) {
                    try {
                        slot = Integer.parseInt(String.valueOf(slotObj));
                    } catch (NumberFormatException e) {
                        slot = 46;
                    }
                } else {
                    slot = 46;
                }
                inv.setItem(slot, glass);
            }
            return;
        }

        // Backwards compatibility: check old separate glass keys
        String[] glassKeys = {"glass_left_1", "glass_left_2", "glass_right_1", "glass_right_2"};
        for (String key : glassKeys) {
            if (navbarConfig.contains("navbar." + key)) {
                String materialName = navbarConfig.getString("navbar." + key + ".material", "BLACK_STAINED_GLASS_PANE");
                Material material = Material.valueOf(materialName.toUpperCase());

                ItemStack glass = new ItemStack(material);
                ItemMeta meta = glass.getItemMeta();
                if (meta != null) {
                    String displayName = navbarConfig.getString("navbar." + key + ".display_name", " ");
                    meta.setDisplayName(displayName);
                    glass.setItemMeta(meta);
                }

                int slot = navbarConfig.getInt("navbar." + key + ".slot", 46); // Default slot
                inv.setItem(slot, glass);
            }
        }
    }
    
    private void addPreviousPageButton(Inventory inv, int page) {
        if (navbarConfig == null || !navbarConfig.contains("navbar.previous_page")) return;

        ItemStack prev;

        if (page == 0) {
            // If no previous page, create a disabled previous button
            prev = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = prev.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GRAY + "No Previous Page");
                prev.setItemMeta(meta);
            }

        } else {
            // Only add previous page button if there is a previous page
            Replacer replacer = new Replacer()
                .map("{page}", () -> String.valueOf(page));
            
            String materialName = navbarConfig.getString("navbar.previous_page.material", "ARROW");
            Material material = Material.valueOf(materialName.toUpperCase());
            
            prev = new ItemStack(material);
            ItemMeta meta = prev.getItemMeta();
            if (meta != null) {
                String displayName = navbarConfig.getString("navbar.previous_page.display_name", "{{previous_page}}");
                displayName = applyMenuPlaceholders(displayName, null, "navbar", replacer);
                meta.setDisplayName(displayName);
                
                List<String> lore = new ArrayList<>();
                List<String> configLore = navbarConfig.getStringList("navbar.previous_page.lore");
                for (String line : configLore) {
                    line = applyMenuPlaceholders(line, null, "navbar", replacer);
                    lore.add(line);
                }
                meta.setLore(lore);
                prev.setItemMeta(meta);
            }

        }

        
        int slot = navbarConfig.getInt("navbar.previous_page.slot", 48);
        inv.setItem(slot, prev);
    }
    
    private void addPageInfo(Inventory inv, int page, int maxPage) {
        if (navbarConfig == null || !navbarConfig.contains("navbar.page_info")) return;
        
        Replacer replacer = new Replacer()
            .map("{page}", () -> String.valueOf(page + 1))
            .map("{total_pages}", () -> String.valueOf(maxPage + 1));
        
        String materialName = navbarConfig.getString("navbar.page_info.material", "PAPER");
        Material material = Material.valueOf(materialName.toUpperCase());
        
        ItemStack pageItem = new ItemStack(material);
        ItemMeta meta = pageItem.getItemMeta();
        if (meta != null) {
            String displayName = navbarConfig.getString("navbar.page_info.display_name", "{{page}}");
            displayName = applyMenuPlaceholders(displayName, null, "navbar", replacer);
            meta.setDisplayName(displayName);
            
            List<String> lore = new ArrayList<>();
            List<String> configLore = navbarConfig.getStringList("navbar.page_info.lore");
            for (String line : configLore) {
                line = applyMenuPlaceholders(line, null, "navbar", replacer);
                lore.add(line);
            }
            meta.setLore(lore);
            pageItem.setItemMeta(meta);
        }
        
        int slot = navbarConfig.getInt("navbar.page_info.slot", 49);
        inv.setItem(slot, pageItem);
    }
    
    private void addNextPageButton(Inventory inv, int page, int maxPage) {
        if (navbarConfig == null || !navbarConfig.contains("navbar.next_page")) return;

        ItemStack next;

        if (page < maxPage) {
            // Only add next page button if there is a next page
            Replacer replacer = new Replacer()
                .map("{page}", () -> String.valueOf(page + 2));
            
            String materialName = navbarConfig.getString("navbar.next_page.material", "ARROW");
            Material material = Material.valueOf(materialName.toUpperCase());
            
            next = new ItemStack(material);
            ItemMeta meta = next.getItemMeta();
            if (meta != null) {
                String displayName = navbarConfig.getString("navbar.next_page.display_name", "{{next_page}}");
                displayName = applyMenuPlaceholders(displayName, null, "navbar", replacer);
                meta.setDisplayName(displayName);
                
                List<String> lore = new ArrayList<>();
                List<String> configLore = navbarConfig.getStringList("navbar.next_page.lore");
                for (String line : configLore) {
                    line = applyMenuPlaceholders(line, null, "navbar", replacer);
                    lore.add(line);
                }
                meta.setLore(lore);
                next.setItemMeta(meta);
            }

        } else {
            // If no next page, create a disabled next button
            next = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = next.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GRAY + "No Next Page");
                next.setItemMeta(meta);
            }
        }
        
        int slot = navbarConfig.getInt("navbar.next_page.slot", 50);
        inv.setItem(slot, next);
    }
    
    private void addBackButton(Inventory inv) {
        addBackButton(inv, true); // Default: show back button (has previous menu)
    }

    /**
     * Add back or close button based on whether there's a previous menu
     * @param inv The inventory to add the button to
     * @param hasPreviousMenu True to show back button, false to show close button
     */
    private void addBackButton(Inventory inv, boolean hasPreviousMenu) {
        if (navbarConfig == null) return;
        
        String configKey = hasPreviousMenu ? "navbar.back" : "navbar.close";
        String fallbackKey = "navbar.back_close"; // Backwards compatibility
        
        // Try new format first
        if (!navbarConfig.contains(configKey)) {
            // Fall back to old back_close format
            if (!navbarConfig.contains(fallbackKey)) return;
            configKey = fallbackKey;
        }
        
        Replacer replacer = new Replacer()
            .map("{menu_name}", () -> "Main Menu"); // Default fallback
        
        Material defaultMaterial = hasPreviousMenu ? Material.SPYGLASS : Material.BARRIER;
        String materialName = navbarConfig.getString(configKey + ".material");
        Material material;
        if (materialName == null || materialName.trim().isEmpty()) {
            material = defaultMaterial;
        } else {
            try {
                material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException ex) {
                AuraSkills.getPlugin().getLogger().log(
                        Level.WARNING,
                        "Invalid material '" + materialName + "' for navbar key '" + configKey + "'. " +
                        "Using default material '" + defaultMaterial + "' instead.",
                        ex
                );
                material = defaultMaterial;
            }
        }
        
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            String displayName = navbarConfig.getString(configKey + ".display_name", hasPreviousMenu ? "§c§l← Back" : "§c§l✗ Close");
            displayName = applyMenuPlaceholders(displayName, null, "navbar", replacer);
            meta.setDisplayName(displayName);
            
            List<String> lore = new ArrayList<>();
            List<String> configLore = navbarConfig.getStringList(configKey + ".lore");
            for (String line : configLore) {
                line = applyMenuPlaceholders(line, null, "navbar", replacer);
                lore.add(line);
            }
            meta.setLore(lore);
            button.setItemMeta(meta);
        }
        
        int slot = navbarConfig.getInt(configKey + ".slot", 53);
        inv.setItem(slot, button);
    }

    /**
     * Add close button (for menus without a previous menu)
     */
    public void addCloseButton(Inventory inv) {
        addBackButton(inv, false);
    }
}