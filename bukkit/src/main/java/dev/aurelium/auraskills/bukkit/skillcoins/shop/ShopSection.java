package dev.aurelium.auraskills.bukkit.skillcoins.shop;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a section of the shop (Combat, Resources, etc.)
 * 
 * Enhanced with EconomyShopGUI-style configuration options:
 * - title: Custom menu title for this section
 * - hidden: Whether to hide this section from the main menu
 * - subSection: Whether this is a sub-section
 * - displayItem: Whether to display the section item
 * - fillItem: Material to fill empty slots
 * - navBarMode: Navbar mode (INHERIT, CUSTOM, DISABLED)
 */
public class ShopSection {
    
    private final String id;
    private final String displayName;
    private final Material icon;
    private final int slot;
    private final List<ShopItem> items;
    
    // EconomyShopGUI-style options
    private final String title;
    private final boolean hidden;
    private final boolean subSection;
    private final boolean displayItem;
    private final Material fillItem;
    private final String navBarMode;
    
    public ShopSection(String id, String displayName, Material icon, int slot) {
        this(id, displayName, icon, slot, "", false, false, false, Material.AIR, "INHERIT");
    }
    
    public ShopSection(String id, String displayName, Material icon, int slot, 
                      String title, boolean hidden, boolean subSection, boolean displayItem, 
                      Material fillItem, String navBarMode) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.slot = slot;
        this.items = new ArrayList<>();
        
        // EconomyShopGUI options
        this.title = title;
        this.hidden = hidden;
        this.subSection = subSection;
        this.displayItem = displayItem;
        this.fillItem = fillItem;
        this.navBarMode = navBarMode;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public Material getIcon() {
        return icon;
    }
    
    public int getSlot() {
        return slot;
    }
    
    public List<ShopItem> getItems() {
        return new ArrayList<>(items);
    }
    
    public void addItem(ShopItem item) {
        items.add(item);
    }
    
    public int getItemCount() {
        return items.size();
    }
    
    // EconomyShopGUI-style getters
    
    public String getTitle() {
        return title;
    }
    
    public boolean isHidden() {
        return hidden;
    }
    
    public boolean isSubSection() {
        return subSection;
    }
    
    public boolean isDisplayItem() {
        return displayItem;
    }
    
    public Material getFillItem() {
        return fillItem;
    }
    
    public String getNavBarMode() {
        return navBarMode;
    }
    
    /**
     * Get the display color based on section type
     */
    public String getDisplayColor() {
        String lower = (displayName != null ? displayName.toLowerCase() : id.toLowerCase());
        
        if (lower.contains("combat")) return "#FF5555";
        if (lower.contains("enchant")) return "#FF55FF";
        if (lower.contains("resource")) return "#55FF55";
        if (lower.contains("tool")) return "#5555FF";
        if (lower.contains("food")) return "#FFFF00";
        if (lower.contains("block")) return "#FFD700";
        if (lower.contains("farm")) return "#55FF55";
        if (lower.contains("potion")) return "#FF55FF";
        if (lower.contains("redstone")) return "#FF5555";
        if (lower.contains("skill") || lower.contains("level")) return "#FFD700";
        if (lower.contains("token") || lower.contains("exchange")) return "#00FFFF";
        if (lower.contains("misc")) return "#808080";
        if (lower.contains("decoration")) return "#FF69B4";
        if (lower.contains("dye")) return "#DA70D6";
        if (lower.contains("music")) return "#9370DB";
        if (lower.contains("ore")) return "#CD853F";
        if (lower.contains("spawn")) return "#90EE90";
        if (lower.contains("workstation")) return "#DEB887";
        
        return "#00FFFF"; // Default cyan
    }
    
    /**
     * Get an icon character for the section
     */
    public String getIconChar() {
        String lower = (displayName != null ? displayName.toLowerCase() : id.toLowerCase());
        
        if (lower.contains("combat")) return "⚔ ";
        if (lower.contains("enchant")) return "✦ ";
        if (lower.contains("resource")) return "❖ ";
        if (lower.contains("tool")) return "⚒ ";
        if (lower.contains("food")) return "🍖 ";
        if (lower.contains("block")) return "⬛ ";
        if (lower.contains("farm")) return "🌾 ";
        if (lower.contains("potion")) return "⚗ ";
        if (lower.contains("redstone")) return "🔴 ";
        if (lower.contains("skill") || lower.contains("level")) return "★ ";
        if (lower.contains("token") || lower.contains("exchange")) return "🎟 ";
        if (lower.contains("misc")) return "⋯ ";
        if (lower.contains("decoration")) return "✿ ";
        if (lower.contains("dye")) return "🎨 ";
        if (lower.contains("music")) return "♫ ";
        if (lower.contains("ore")) return "⛏ ";
        if (lower.contains("spawn")) return "🥚 ";
        if (lower.contains("workstation")) return "🔨 ";
        
        return "● "; // Default bullet
    }
}
