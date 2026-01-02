package dev.aurelium.auraskills.bukkit.skillcoins.menu;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.skillcoins.CurrencyType;
import dev.aurelium.auraskills.common.skillcoins.EconomyProvider;
import dev.aurelium.auraskills.common.user.User;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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

/**
 * Level Buy Menu - Uses the same track layout as level progression
 * Allows clicking levels to select range, with highlighting
 * +/- buttons at top with confirm button
 */
public class LevelBuyMenu {
    
    private final AuraSkills plugin;
    private final EconomyProvider economy;
    private final SharedNavbarManager navbarManager;
    
    // The track positions (same as level_progression.yml)
    private static final int[] TRACK = {9, 18, 27, 36, 37, 38, 29, 20, 11, 12, 13, 22, 31, 40, 41, 42, 33, 24, 15, 16, 17, 26, 35, 44};
    private static final int ITEMS_PER_PAGE = 24;
    private static final int TOKENS_PER_LEVEL = 10;
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0");
    
    // Title prefix to identify this menu
    private static final String TITLE_PREFIX = ChatColor.DARK_GRAY + "Buy ";
    
    // Session data per player
    private final Map<UUID, Skill> selectedSkill = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> selectedUpToLevel = new ConcurrentHashMap<>();  // Level they want to upgrade TO
    private final Map<UUID, Integer> currentPage = new ConcurrentHashMap<>();
    // Track whether the player opened this menu from the level progression "road" menu
    private final Map<UUID, Boolean> cameFromRoad = new ConcurrentHashMap<>();
    // Track whether the player opened this menu from the shop's skill selection screen
    private final Map<UUID, Boolean> cameFromSkillSelect = new ConcurrentHashMap<>();

    /**
     * Open the menu when initiated from the shop skill selection (so back returns there)
     */
    public void openFromSkillSelection(Player player, Skill skill) {
        // Reuse open logic but mark as opened from skill selection
        open(player, skill);
        if (player != null) {
            UUID uuid = player.getUniqueId();
            cameFromSkillSelect.put(uuid, true);
            // Ensure road flag is cleared when opened from skill selection
            cameFromRoad.put(uuid, false);
            MenuManager manager = MenuManager.getInstance(plugin);
            if (manager != null) {
                manager.setMenuOrigin(uuid, MenuManager.MenuOrigin.SKILL_SELECT);
            }

        }
    }
    
    public LevelBuyMenu(AuraSkills plugin, EconomyProvider economy) {
        if (plugin == null) throw new IllegalArgumentException("Plugin cannot be null");
        if (economy == null) throw new IllegalArgumentException("Economy cannot be null");
        this.plugin = plugin;
        this.economy = economy;
        this.navbarManager = new SharedNavbarManager(plugin, economy);
    }
    
    /**
     * Open the level buy menu for a specific skill
     */
    public void open(Player player, Skill skill) {
        if (player == null || skill == null) return;
        
        try {
            MenuManager manager = MenuManager.getInstance(plugin);
            if (manager != null) {
                manager.registerLevelBuyMenu(player, this);
            }
            
            UUID uuid = player.getUniqueId();
            selectedSkill.put(uuid, skill);
            currentPage.put(uuid, 0);
            
            User user = plugin.getUser(player);
            if (user == null) return;
            
            int currentLevel = user.getSkillLevel(skill);
            // Default selection is to buy 1 level
            selectedUpToLevel.put(uuid, currentLevel + 1);
            // Not opened from the level progression "road" by default
            cameFromRoad.put(uuid, false);
            // Not opened from skill-selection by default (clear previous state)
            cameFromSkillSelect.put(uuid, false);

            // Store a persistent default origin so back resolution is robust
            if (manager != null) {
                manager.setMenuOrigin(uuid, MenuManager.MenuOrigin.SKILL_SELECT);
            }

            updateInventory(player);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error opening level buy menu", e);
            player.sendMessage(ChatColor.of("#FF5555") + "✖ Error opening menu!");
        }
    }
    
    /**
     * Open from a specific locked level click in /skill menu
     */
    public void openFromLevelClick(Player player, Skill skill, int clickedLevel) {
        if (player == null || skill == null) return;
        
        try {
            MenuManager manager = MenuManager.getInstance(plugin);
            if (manager != null) {
                manager.registerLevelBuyMenu(player, this);
            }
            
            UUID uuid = player.getUniqueId();
            selectedSkill.put(uuid, skill);
            
            User user = plugin.getUser(player);
            if (user == null) return;
            
            int currentLevel = user.getSkillLevel(skill);
            
            // Calculate which page this level is on
            int levelIndex = clickedLevel - 1; // 0-indexed
            int page = levelIndex / ITEMS_PER_PAGE;
            currentPage.put(uuid, page);
            
            // Set selection to the clicked level (or current+1 if below current)
            selectedUpToLevel.put(uuid, Math.max(currentLevel + 1, clickedLevel));
            // Opened from the level progression "road"
            cameFromRoad.put(uuid, true);
            // Ensure skill selection flag is cleared when opening from road
            cameFromSkillSelect.put(uuid, false);

            if (manager != null) {
                manager.setMenuOrigin(uuid, MenuManager.MenuOrigin.SKILL_ROAD);
            }

            updateInventory(player);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error opening level buy menu from click", e);
            player.sendMessage(ChatColor.of("#FF5555") + "✖ Error opening menu!");
        }
    }
    
    public boolean isMenuTitle(String title) {
        if (title == null) return false;
        return title.startsWith(TITLE_PREFIX);
    }
    
    private String getMenuTitle(Skill skill, UUID uuid) {
        return TITLE_PREFIX + ChatColor.DARK_GRAY + skill.getDisplayName(Locale.ENGLISH) + " Levels - Page " +
                (currentPage.getOrDefault(uuid, 0) + 1);
    }
    
    private void updateInventory(Player player) {
        if (player == null || !player.isOnline()) return;
        
        try {
            UUID uuid = player.getUniqueId();
            Skill skill = selectedSkill.get(uuid);
            if (skill == null) return;
            
            User user = plugin.getUser(player);
            if (user == null) return;
            
            int page = currentPage.getOrDefault(uuid, 0);
            int currentLevel = user.getSkillLevel(skill);
            int maxLevel = skill.getMaxLevel();
            int selectedLevel = selectedUpToLevel.getOrDefault(uuid, currentLevel + 1);
            
            // Clamp selection
            selectedLevel = Math.max(currentLevel + 1, Math.min(maxLevel, selectedLevel));
            selectedUpToLevel.put(uuid, selectedLevel);
            
            String title = getMenuTitle(skill, uuid);
            Inventory inv = null;
            boolean isNewInventory = false;
            
            try {
                Inventory currentInv = player.getOpenInventory().getTopInventory();
                if (currentInv != null && currentInv.getSize() == 54) {
                    String openTitle = player.getOpenInventory().getTitle();
                    // Reuse if the title prefix matches (allows page changes without recreating inventory)
                    if (openTitle != null && openTitle.startsWith(TITLE_PREFIX)) {
                        inv = currentInv;
                    }
                }
            } catch (Exception e) {
                // Will create new inventory
            }
            
            if (inv == null) {
                inv = Bukkit.createInventory(null, 54, title);
                isNewInventory = true;
            } else {
                // Reuse the existing inventory; titles are immutable in Bukkit so we cannot update it here
                // This is safe because we're clearing and refilling the contents anyway
            }
            
            inv.clear();
            
            
            // Add top controls (replacing rank/sources/abilities)
            addTopControls(inv, player, skill, currentLevel, maxLevel, selectedLevel);
            
            // Add level track items
            addLevelTrack(inv, skill, currentLevel, maxLevel, selectedLevel, page);
            
            // Add universal navbar
            int maxPage = (maxLevel - 1) / ITEMS_PER_PAGE;
            navbarManager.addNavbar(inv, "level_buy", page, maxPage, player);
            
            // Override the back button visually to reflect where it will return to
            try {
                MenuManager manager = MenuManager.getInstance(plugin);
                MenuManager.MenuOrigin currentOrigin = MenuManager.MenuOrigin.SKILL_SELECT;
                if (manager != null) {
                    currentOrigin = manager.getMenuOrigin(uuid).orElse(MenuManager.MenuOrigin.SKILL_SELECT);
                }
                ItemStack backItem = new ItemStack(Material.SPYGLASS);
                ItemMeta backMeta = backItem.getItemMeta();
                if (backMeta != null) {
                    String displayName;
                    List<String> lore = new ArrayList<>();
                        if (currentOrigin == MenuManager.MenuOrigin.SKILL_SELECT) {
                        displayName = "§c§l← Back";
                    } else if (currentOrigin == MenuManager.MenuOrigin.SKILL_ROAD) {
                        displayName = "§c§l← Back§7 to road";
                        lore.add(" ");
                        lore.add("§7Return to Skill Road");
                    } else {
                        displayName = "§c§l← Back";
                    }
                    backMeta.setDisplayName(displayName);
                    backMeta.setLore(lore);
                    backItem.setItemMeta(backMeta);
                }
                inv.setItem(53, backItem);
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to override back button display", e);
            }
            
            if (isNewInventory) {
                player.openInventory(inv);
            } else {
                player.updateInventory();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating level buy inventory", e);
        }
    }
    
    private void addTopControls(Inventory inv, Player player, Skill skill, int currentLevel, int maxLevel, int selectedLevel) {
        UUID uuid = player.getUniqueId();
        double tokenBalance = economy.getBalance(uuid, CurrencyType.TOKENS);
        
        int levelsToBuy = selectedLevel - currentLevel;
        int totalCost = levelsToBuy * TOKENS_PER_LEVEL;
        boolean canAfford = tokenBalance >= totalCost;
        
        // Slot 0: Skill Info
        ItemStack skillItem = new ItemStack(getSkillIcon(skill));
        ItemMeta skillMeta = skillItem.getItemMeta();
        if (skillMeta != null) {
            skillMeta.setDisplayName(ChatColor.of("#00FFFF") + "✦ " + skill.getDisplayName(Locale.ENGLISH));
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Current Level: " + ChatColor.WHITE + currentLevel);
            lore.add(ChatColor.GRAY + "Max Level: " + ChatColor.WHITE + maxLevel);
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click levels below to select!");
            skillMeta.setLore(lore);
            skillItem.setItemMeta(skillMeta);
        }
        inv.setItem(0, skillItem);
        
        // Slot 2: Minus button (-1) - cleaner single-step control
        createControlButton(inv, 2, Material.ORANGE_TERRACOTTA,
                ChatColor.RED + "▼ -1 Level",
                selectedLevel > currentLevel + 1,
                Arrays.asList("", ChatColor.GRAY + "Remove 1 from selection"));
        
        // Slot 4: Confirm/Purchase Button
        if (levelsToBuy > 0 && canAfford && currentLevel < maxLevel) {
            ItemStack confirm = new ItemStack(Material.EMERALD_BLOCK);
            ItemMeta confirmMeta = confirm.getItemMeta();
            if (confirmMeta != null) {
                confirmMeta.setDisplayName(ChatColor.of("#55FF55") + "✔ CONFIRM PURCHASE");
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.GRAY + "Levels: " + ChatColor.WHITE + currentLevel + " → " + 
                        ChatColor.GREEN + selectedLevel);
                lore.add(ChatColor.GRAY + "Buying: " + ChatColor.YELLOW + levelsToBuy + " level" + (levelsToBuy > 1 ? "s" : ""));
                lore.add("");
                lore.add(ChatColor.GOLD + "Cost: " + ChatColor.WHITE + MONEY_FORMAT.format(totalCost) + " Tokens");
                lore.add(ChatColor.GRAY + "Balance after: " + ChatColor.WHITE + 
                        MONEY_FORMAT.format(tokenBalance - totalCost));
                lore.add("");
                lore.add(ChatColor.GREEN + "▸ Click to purchase!");
                confirmMeta.setLore(lore);
                confirm.setItemMeta(confirmMeta);
            }
            inv.setItem(4, confirm);
        } else if (levelsToBuy > 0 && !canAfford) {
            ItemStack cantAfford = new ItemStack(Material.REDSTONE_BLOCK);
            ItemMeta cantMeta = cantAfford.getItemMeta();
            if (cantMeta != null) {
                cantMeta.setDisplayName(ChatColor.of("#FF5555") + "✖ INSUFFICIENT TOKENS");
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.GRAY + "Cost: " + ChatColor.RED + MONEY_FORMAT.format(totalCost) + " Tokens");
                lore.add(ChatColor.GRAY + "You have: " + ChatColor.WHITE + MONEY_FORMAT.format(tokenBalance));
                lore.add(ChatColor.GRAY + "Need: " + ChatColor.RED + MONEY_FORMAT.format(totalCost - tokenBalance) + " more");
                cantMeta.setLore(lore);
                cantAfford.setItemMeta(cantMeta);
            }
            inv.setItem(4, cantAfford);
        } else if (currentLevel >= maxLevel) {
            ItemStack maxed = new ItemStack(Material.NETHER_STAR);
            ItemMeta maxedMeta = maxed.getItemMeta();
            if (maxedMeta != null) {
                maxedMeta.setDisplayName(ChatColor.of("#FFD700") + "★ MAX LEVEL REACHED");
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.GRAY + "This skill is at maximum level!");
                maxedMeta.setLore(lore);
                maxed.setItemMeta(maxedMeta);
            }
            inv.setItem(4, maxed);
        }
        
        // Slot 6: Plus button (+1)
        createControlButton(inv, 6, Material.LIME_TERRACOTTA,
                ChatColor.of("#55FF55") + "▲ +1 Level",
                selectedLevel < maxLevel,
                Arrays.asList("", ChatColor.GRAY + "Add 1 to selection"));
    }
    
    private void createControlButton(Inventory inv, int slot, Material material, String name, boolean enabled, List<String> lore) {
        ItemStack button = new ItemStack(enabled ? material : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(enabled ? name : ChatColor.GRAY + "[Limit Reached]");
            if (enabled) {
                meta.setLore(lore);
            }
            button.setItemMeta(meta);
        }
        inv.setItem(slot, button);
    }
    
    private void addLevelTrack(Inventory inv, Skill skill, int currentLevel, int maxLevel, int selectedLevel, int page) {
        int startLevel = (page * ITEMS_PER_PAGE) + 1;
        
        for (int i = 0; i < TRACK.length; i++) {
            int level = startLevel + i;
            if (level > maxLevel) break;
            
            int slot = TRACK[i];
            Material material;
            String displayName;
            List<String> lore = new ArrayList<>();
            
            if (level <= currentLevel) {
                // Already unlocked - green glass
                material = Material.LIME_STAINED_GLASS_PANE;
                displayName = ChatColor.GREEN + "Level " + level + " ✔";
                lore.add("");
                lore.add(ChatColor.GREEN + "Already Unlocked!");
            } else if (level <= selectedLevel) {
                // Selected for purchase - cyan/light blue glass (highlighted)
                material = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
                displayName = ChatColor.of("#00FFFF") + "Level " + level + " ★";
                lore.add("");
                lore.add(ChatColor.AQUA + "SELECTED FOR PURCHASE");
                lore.add("");
                lore.add(ChatColor.YELLOW + "▸ Click to deselect this level");
            } else {
                // Not selected, can be purchased - red/orange glass
                material = Material.RED_STAINED_GLASS_PANE;
                displayName = ChatColor.RED + "Level " + level;
                lore.add("");
                lore.add(ChatColor.GRAY + "Locked");
                lore.add("");
                lore.add(ChatColor.GOLD + "Cost: " + ChatColor.WHITE + TOKENS_PER_LEVEL + " Tokens");
                lore.add("");
                lore.add(ChatColor.YELLOW + "▸ Click to select up to this level");
            }
            
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(displayName);
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
        }
    }
    
    // addNavigation removed — navigation handled by shared navbar managers now
    
    
    /**
     * Handle click events in the level buy menu
     */
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();

        Skill skill = selectedSkill.get(uuid);
        if (skill == null) return;

        User user = plugin.getUser(player);
        if (user == null) return;

        int slot = event.getRawSlot();
        int currentLevel = user.getSkillLevel(skill);
        int maxLevel = skill.getMaxLevel();
        int selectedLevel = selectedUpToLevel.getOrDefault(uuid, currentLevel + 1);
        int page = currentPage.getOrDefault(uuid, 0);

        Runnable updateTask = null;

        // Check for control buttons
        switch (slot) {
            case 2: // -1 level
                if (selectedLevel > currentLevel + 1) {
                    selectedUpToLevel.put(uuid, selectedLevel - 1);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    updateTask = () -> updateInventory(player);
                }
                break;
            case 4: // Confirm purchase
                if (currentLevel < maxLevel) {
                    processPurchase(player, skill, currentLevel, selectedLevel);
                }
                break;
            case 6: // +1 level
                if (selectedLevel < maxLevel) {
                    selectedUpToLevel.put(uuid, selectedLevel + 1);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    updateTask = () -> updateInventory(player);
                }
                break;
            // removed ±10 controls for cleaner single-step selection
            case 45: // Balance display (navbar) - no action
                break;
            case 46: // Glass pane (navbar) - no action
                break;
            case 47: // Glass pane (navbar) - no action
                break;
            case 48: // Previous page
                if (page > 0) {
                    currentPage.put(uuid, page - 1);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    updateTask = () -> updateInventory(player);
                }
                break;
            case 49: // Page info (navbar) - no action
                break;
            case 50: // Next page
                int maxPage = (maxLevel - 1) / ITEMS_PER_PAGE;
                if (page < maxPage) {
                    currentPage.put(uuid, page + 1);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    updateTask = () -> updateInventory(player);
                }
                break;
            case 51: // Glass pane (navbar) - no action
                break;
            case 52: // Glass pane (navbar) - no action
                break;
            case 53: // Back (navbar)
                // Consult persistent MenuManager origin
                MenuManager manager = MenuManager.getInstance(plugin);
                MenuManager.MenuOrigin origin = MenuManager.MenuOrigin.SKILL_SELECT; // Default fallback
                if (manager != null) {
                    origin = manager.getMenuOrigin(uuid).orElse(MenuManager.MenuOrigin.SKILL_SELECT);
                }

                // Perform cleanup now because we won't rely on the close event to clear session
                cleanupSession(player);

                // Use the resolved source inside the scheduled task
                final MenuManager.MenuOrigin finalOrigin = origin;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        switch (finalOrigin) {
                            case SKILL_SELECT -> {
                                SkillLevelPurchaseMenu skillMenu = new SkillLevelPurchaseMenu(plugin, economy);
                                skillMenu.open(player);
                            }
                            case SKILL_ROAD -> {
                                new dev.aurelium.auraskills.bukkit.menus.util.LevelProgressionOpener(plugin).open(player, skill);
                            }
                                case SHOP_MAIN -> {
                                    SkillLevelPurchaseMenu skillMenu = new SkillLevelPurchaseMenu(plugin, economy);
                                    skillMenu.open(player);
                                }
                        }
                    } finally {
                        // Clear the persistent origin after navigation attempt
                        if (manager != null) {
                            manager.clearMenuOrigin(uuid);
                        }
                    }
                });
                break;
        }

        // Check if clicked on a level in the track
        int trackIndex = -1;
        for (int i = 0; i < TRACK.length; i++) {
            if (TRACK[i] == slot) {
                trackIndex = i;
                break;
            }
        }

        if (trackIndex != -1) {
            int clickedLevel = (page * ITEMS_PER_PAGE) + trackIndex + 1;

            if (clickedLevel <= currentLevel) {
                // Can't select already unlocked levels
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }

            if (clickedLevel > maxLevel) return;

            // Toggle selection
            if (clickedLevel <= selectedLevel) {
                // Clicking on a selected level - deselect down to the clicked level minus 1
                // But at minimum keep 1 level selected
                int newSelection = clickedLevel;
                if (newSelection < currentLevel) {
                    newSelection = currentLevel;
                }
                selectedUpToLevel.put(uuid, newSelection);
            } else {
                // Clicking on an unselected level - select up to that level
                selectedUpToLevel.put(uuid, clickedLevel);
            }

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            updateTask = () -> updateInventory(player);
        }

        // If an update is needed, schedule it after the event
        if (updateTask != null) {
            Bukkit.getScheduler().runTask(plugin, updateTask);
        }
    }
    
    private void processPurchase(Player player, Skill skill, int currentLevel, int selectedLevel) {
        UUID uuid = player.getUniqueId();
        
        int levelsToBuy = selectedLevel - currentLevel;
        int totalCost = levelsToBuy * TOKENS_PER_LEVEL;
        
        double balance = economy.getBalance(uuid, CurrencyType.TOKENS);
        
        if (balance < totalCost) {
            player.sendMessage(ChatColor.RED + "✖ Insufficient tokens!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Deduct tokens
        economy.subtractBalance(uuid, CurrencyType.TOKENS, totalCost);
        
        // Add levels
        User user = plugin.getUser(player);
        if (user == null) return;
        
        user.setSkillLevel(skill, selectedLevel);
        
        // Success feedback
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.0f);
        
        player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        player.sendMessage(ChatColor.GREEN + "  ✔ " + ChatColor.WHITE + ChatColor.BOLD + "PURCHASE COMPLETE!");
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "  " + skill.getDisplayName(Locale.ENGLISH) + ": " + 
                ChatColor.WHITE + currentLevel + ChatColor.GRAY + " → " + ChatColor.GREEN + selectedLevel);
        player.sendMessage(ChatColor.GRAY + "  Spent: " + ChatColor.YELLOW + MONEY_FORMAT.format(totalCost) + " Tokens");
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Reset selection and update
        selectedUpToLevel.put(uuid, selectedLevel + 1);
        updateInventory(player);
    }
    
    private void cleanupSession(Player player) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();



        selectedSkill.remove(uuid);
        selectedUpToLevel.remove(uuid);
        currentPage.remove(uuid);
        cameFromRoad.remove(uuid);
        cameFromSkillSelect.remove(uuid);

        MenuManager manager = MenuManager.getInstance(plugin);
        if (manager != null) {
            manager.unregisterLevelBuyMenu(player);
        }
    }

    public void handleClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        
        // Delay cleanup to allow page navigation to complete
        // If player opens another menu immediately (page change), this prevents premature unregistration
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Only cleanup if player doesn't have this menu open anymore
            if (player.isOnline() && player.getOpenInventory() != null) {
                String currentTitle = player.getOpenInventory().getTitle();
                if (currentTitle == null || !currentTitle.startsWith(TITLE_PREFIX)) {
                    // Player has different menu or no menu - safe to cleanup
                    cleanupSession(player);
                }
            } else {
                // Player offline or no inventory - cleanup
                cleanupSession(player);
            }
        }, 1L); // 1 tick delay
    }
    
    private Material getSkillIcon(Skill skill) {
        String skillName = skill.getId().getKey().toLowerCase();
        
        return switch (skillName) {
            case "farming" -> Material.IRON_HOE;
            case "foraging" -> Material.IRON_AXE;
            case "mining" -> Material.IRON_PICKAXE;
            case "fishing" -> Material.FISHING_ROD;
            case "excavation" -> Material.IRON_SHOVEL;
            case "archery" -> Material.BOW;
            case "defense" -> Material.CHAINMAIL_CHESTPLATE;
            case "fighting" -> Material.IRON_SWORD;
            case "endurance" -> Material.GOLDEN_APPLE;
            case "agility" -> Material.FEATHER;
            case "alchemy" -> Material.POTION;
            case "enchanting" -> Material.ENCHANTING_TABLE;
            case "sorcery" -> Material.BLAZE_ROD;
            case "healing" -> Material.SPLASH_POTION;
            case "forging" -> Material.ANVIL;
            default -> Material.EXPERIENCE_BOTTLE;
        };
    }
}
