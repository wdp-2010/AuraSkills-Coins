package dev.aurelium.auraskills.bukkit.skillcoins.menu;

import dev.aurelium.auraskills.bukkit.AuraSkills;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MenuManager implements Listener {

    private static MenuManager instance;
    private final AuraSkills plugin;

    private final Map<UUID, ShopMainMenu> mainMenus = new ConcurrentHashMap<>();
    private final Map<UUID, ShopSectionMenu> sectionMenus = new ConcurrentHashMap<>();
    private final Map<UUID, TransactionMenu> transactionMenus = new ConcurrentHashMap<>();
    private final Map<UUID, TokenExchangeMenu> tokenMenus = new ConcurrentHashMap<>();
    private final Map<UUID, SkillLevelPurchaseMenu> skillMenus = new ConcurrentHashMap<>();
    private final Map<UUID, SellMenu> sellMenus = new ConcurrentHashMap<>();
    private final Map<UUID, LevelBuyMenu> levelBuyMenus = new ConcurrentHashMap<>();

    public enum MenuOrigin { SHOP_MAIN, SKILL_SELECT, SKILL_ROAD }
    private final Map<UUID, MenuOrigin> menuOrigins = new ConcurrentHashMap<>();

    private MenuManager(AuraSkills plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("AuraSkills plugin instance cannot be null");
        }
        this.plugin = plugin;
    }

    public static synchronized MenuManager getInstance(AuraSkills plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Cannot get MenuManager instance with null plugin");
        }

        if (instance == null) {
            instance = new MenuManager(plugin);
            plugin.getServer().getPluginManager().registerEvents(instance, plugin);
            plugin.getLogger().info("MenuManager initialized successfully");
        }
        return instance;
    }

    public void registerMainMenu(Player player, ShopMainMenu menu) {
        if (player == null || menu == null) {
            plugin.getLogger().warning("Attempted to register null main menu or player");
            return;
        }
        mainMenus.put(player.getUniqueId(), menu);
    }

    public void registerSectionMenu(Player player, ShopSectionMenu menu) {
        if (player == null || menu == null) {
            plugin.getLogger().warning("Attempted to register null section menu or player");
            return;
        }
        sectionMenus.put(player.getUniqueId(), menu);
    }

    public void registerTransactionMenu(Player player, TransactionMenu menu) {
        if (player == null || menu == null) {
            plugin.getLogger().warning("Attempted to register null transaction menu or player");
            return;
        }
        transactionMenus.put(player.getUniqueId(), menu);
    }

    public void registerTokenMenu(Player player, TokenExchangeMenu menu) {
        if (player == null || menu == null) {
            plugin.getLogger().warning("Attempted to register null token menu or player");
            return;
        }
        tokenMenus.put(player.getUniqueId(), menu);
    }

    public void registerSkillMenu(Player player, SkillLevelPurchaseMenu menu) {
        if (player == null || menu == null) {
            plugin.getLogger().warning("Attempted to register null skill menu or player");
            return;
        }
        skillMenus.put(player.getUniqueId(), menu);
    }

    public void registerSellMenu(Player player, SellMenu menu) {
        if (player == null || menu == null) {
            plugin.getLogger().warning("Attempted to register null sell menu or player");
            return;
        }
        sellMenus.put(player.getUniqueId(), menu);
    }

    public void registerLevelBuyMenu(Player player, LevelBuyMenu menu) {
        if (player == null || menu == null) {
            plugin.getLogger().warning("Attempted to register null level buy menu or player");
            return;
        }
        levelBuyMenus.put(player.getUniqueId(), menu);
    }

    public void unregisterLevelBuyMenu(Player player) {
        if (player == null) return;
        levelBuyMenus.remove(player.getUniqueId());
    }

    public void openMainMenu(Player player) {
        if (player == null) return;
        new ShopMainMenu(plugin, plugin.getSkillCoinsEconomy()).open(player);
    }

    public void setMenuOrigin(UUID playerId, MenuOrigin origin) {
        if (playerId == null || origin == null) return;
        menuOrigins.put(playerId, origin);
    }

    public java.util.Optional<MenuOrigin> getMenuOrigin(UUID playerId) {
        if (playerId == null) return java.util.Optional.empty();
        return java.util.Optional.ofNullable(menuOrigins.get(playerId));
    }

    public void clearMenuOrigin(UUID playerId) {
        if (playerId == null) return;
        menuOrigins.remove(playerId);
    }

    public void unregisterPlayer(UUID playerId) {
        if (playerId == null) return;

        mainMenus.remove(playerId);
        sectionMenus.remove(playerId);
        transactionMenus.remove(playerId);
        tokenMenus.remove(playerId);
        skillMenus.remove(playerId);
        sellMenus.remove(playerId);
        levelBuyMenus.remove(playerId);
        menuOrigins.remove(playerId);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        try {
            if (event == null || !(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            if (player == null) return;

            UUID playerId = player.getUniqueId();
            if (playerId == null) return;

            String title = null;
            try {
                title = event.getView().getTitle();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to get inventory title", e);
                return;
            }

            if (title == null) return;

            if (handleLevelBuyMenu(playerId, title, event)) return;
            if (handleTransactionMenu(playerId, title, event)) return;
            if (handleTokenMenu(playerId, title, event)) return;
            if (handleSkillMenu(playerId, title, event)) return;
            if (handleSectionMenu(playerId, title, event)) return;
            if (handleMainMenu(playerId, title, event)) return;
            if (handleSellMenu(playerId, title, event)) return;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected error in menu click handler", e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        try {
            if (event == null || !(event.getPlayer() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getPlayer();
            if (player == null) return;

            UUID playerId = player.getUniqueId();
            if (playerId == null) return;

            String title = null;
            try {
                title = event.getView().getTitle();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to get inventory title on close", e);
                unregisterPlayer(playerId);
                return;
            }

            if (title == null) {
                unregisterPlayer(playerId);
                return;
            }

            handleLevelBuyClose(playerId, title, event);
            handleTransactionClose(playerId, title, event);
            handleTokenClose(playerId, title, event);
            handleSkillClose(playerId, title, event);
            handleSectionClose(playerId, title, event);
            handleMainClose(playerId, title, event);
            handleSellClose(playerId, title, event);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected error in menu close handler", e);
        }
    }

    private boolean handleLevelBuyMenu(UUID playerId, String title, InventoryClickEvent event) {
        try {
            LevelBuyMenu menu = levelBuyMenus.get(playerId);
            if (menu != null && menu.isMenuTitle(title)) {
                menu.handleClick(event);
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error in level buy menu click", e);
            levelBuyMenus.remove(playerId);
        }
        return false;
    }

    private boolean handleTransactionMenu(UUID playerId, String title, InventoryClickEvent event) {
        try {
            TransactionMenu menu = transactionMenus.get(playerId);
            if (menu != null && menu.isMenuTitle(title)) {
                menu.handleClick(event);
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error in transaction menu click", e);
            transactionMenus.remove(playerId);
        }
        return false;
    }

    private boolean handleTokenMenu(UUID playerId, String title, InventoryClickEvent event) {
        try {
            TokenExchangeMenu menu = tokenMenus.get(playerId);
            if (menu != null && menu.isMenuTitle(title)) {
                menu.handleClick(event);
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error in token menu click", e);
            tokenMenus.remove(playerId);
        }
        return false;
    }

    private boolean handleSkillMenu(UUID playerId, String title, InventoryClickEvent event) {
        try {
            SkillLevelPurchaseMenu menu = skillMenus.get(playerId);
            if (menu != null && menu.isMenuTitle(title)) {
                menu.handleClick(event);
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error in skill menu click", e);
            skillMenus.remove(playerId);
        }
        return false;
    }

    private boolean handleSectionMenu(UUID playerId, String title, InventoryClickEvent event) {
        try {
            ShopSectionMenu menu = sectionMenus.get(playerId);
            if (menu != null && menu.isMenuTitle(title)) {
                menu.handleClick(event);
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error in section menu click", e);
            sectionMenus.remove(playerId);
        }
        return false;
    }

    private boolean handleMainMenu(UUID playerId, String title, InventoryClickEvent event) {
        try {
            ShopMainMenu menu = mainMenus.get(playerId);
            if (menu != null && menu.isMenuTitle(title)) {
                menu.handleClick(event);
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error in main menu click", e);
            mainMenus.remove(playerId);
        }
        return false;
    }

    private boolean handleSellMenu(UUID playerId, String title, InventoryClickEvent event) {
        try {
            SellMenu menu = sellMenus.get(playerId);
            if (menu != null && menu.isMenuTitle(title)) {
                menu.handleClick(event);
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error in sell menu click", e);
            sellMenus.remove(playerId);
        }
        return false;
    }

    private void handleLevelBuyClose(UUID playerId, String title, InventoryCloseEvent event) {
        try {
            LevelBuyMenu menu = levelBuyMenus.get(playerId);
            if (menu != null && menu.isMenuTitle(title)) {
                menu.handleClose(event);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error closing level buy menu", e);
            levelBuyMenus.remove(playerId);
        }
    }

    private void handleTransactionClose(UUID playerId, String title, InventoryCloseEvent event) {
        try {
            TransactionMenu menu = transactionMenus.get(playerId);
            if (menu != null && menu.isMenuTitle(title)) {
                menu.handleClose(event);
                transactionMenus.remove(playerId);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error closing transaction menu", e);
            transactionMenus.remove(playerId);
        }
    }

    private void handleTokenClose(UUID playerId, String title, InventoryCloseEvent event) {
        try {
            TokenExchangeMenu menu = tokenMenus.get(playerId);
            if (menu != null && menu.isMenuTitle(title)) {
                menu.handleClose(event);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error closing token menu", e);
            tokenMenus.remove(playerId);
        }
    }

    private void handleSkillClose(UUID playerId, String title, InventoryCloseEvent event) {
        try {
            SkillLevelPurchaseMenu menu = skillMenus.get(playerId);
            if (menu != null && menu.isMenuTitle(title)) {
                menu.handleClose(event);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error closing skill menu", e);
            skillMenus.remove(playerId);
        }
    }

    private void handleSectionClose(UUID playerId, String title, InventoryCloseEvent event) {
        try {
            ShopSectionMenu menu = sectionMenus.get(playerId);
            if (menu != null && menu.isMenuTitle(title)) {
                menu.handleClose(event);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error closing section menu", e);
            sectionMenus.remove(playerId);
        }
    }

    private void handleMainClose(UUID playerId, String title, InventoryCloseEvent event) {
        try {
            ShopMainMenu menu = mainMenus.get(playerId);
            if (menu != null && menu.isMenuTitle(title)) {
                menu.handleClose(event);
                mainMenus.remove(playerId);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error closing main menu", e);
            mainMenus.remove(playerId);
        }
    }

    private void handleSellClose(UUID playerId, String title, InventoryCloseEvent event) {
        try {
            SellMenu menu = sellMenus.get(playerId);
            if (menu != null && menu.isMenuTitle(title)) {
                menu.handleClose(event);
                sellMenus.remove(playerId);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error closing sell menu", e);
            sellMenus.remove(playerId);
        }
    }
}
