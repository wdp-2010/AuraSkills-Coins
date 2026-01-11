package dev.aurelium.auraskills.bukkit.skillcoins.listener;

import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.skillcoins.CurrencyType;
import dev.aurelium.auraskills.common.skillcoins.SkillCoinsEconomy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intercepts CMI money commands (/money and /cmi money) and ensures they
 * correctly update the SkillCoins economy so Vault/other plugins see the
 * changes immediately.
 */
public class CmiMoneyCommandListener implements Listener {

    private final AuraSkills plugin;
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("([0-9.,]+)([kmbtKMbt]?)");

    public CmiMoneyCommandListener(AuraSkills plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (handleMoneyCommand(message, event.getPlayer().getName())) {
            event.setCancelled(true); // Prevent CMI from doing its own (possibly inconsistent) handling
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerCommand(ServerCommandEvent event) {
        String message = "/" + event.getCommand(); // ServerCommand gives command without leading slash
        if (handleMoneyCommand(message, "CONSOLE")) {
            // Clear command so it's not processed by CMI (can't cancel ServerCommandEvent)
            event.setCommand("");
        }
    }

    private boolean handleMoneyCommand(String message, String senderName) {
        if (message == null) return false;
        String msg = message.trim();
        String lower = msg.toLowerCase(Locale.ROOT);

        String[] parts;
        if (lower.startsWith("/cmi ")) {
            // /cmi money set ...
            parts = msg.substring(5).split("\\s+");
            if (parts.length == 0) return false;
            if (!parts[0].equalsIgnoreCase("money")) return false;
            // shift to make subcommand first
            if (parts.length < 2) return false;
            String sub = parts[1];
            if (sub.equalsIgnoreCase("set") || sub.equalsIgnoreCase("give") || sub.equalsIgnoreCase("take")) {
                if (parts.length < 4) return false; // need sub, target, amount
                String targetName = parts[2];
                String amountStr = parts[3];
                return applyMoneyChange(senderName, targetName, sub, amountStr);
            }
        } else if (lower.startsWith("/money ") || lower.equalsIgnoreCase("/money")) {
            parts = msg.substring(1).split("\\s+"); // leading slash removed
            // parts[0] == money
            if (parts.length < 2) return false;
            String sub = parts[1];
            if (sub.equalsIgnoreCase("set") || sub.equalsIgnoreCase("give") || sub.equalsIgnoreCase("take")) {
                if (parts.length < 4) return false;
                String targetName = parts[2];
                String amountStr = parts[3];
                return applyMoneyChange(senderName, targetName, sub, amountStr);
            }
        }
        return false;
    }

    private boolean applyMoneyChange(String senderName, String targetName, String sub, String amountStr) {
        double amount = parseAmount(amountStr);
        if (amount < 0) {
            plugin.getLogger().warning("Failed to parse amount: " + amountStr + " from " + senderName);
            return false;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getUniqueId() == null) {
            plugin.getLogger().warning("Target player not found: " + targetName);
            return false;
        }

        SkillCoinsEconomy econ = plugin.getSkillCoinsEconomy();
        if (econ == null) {
            plugin.getLogger().warning("SkillCoins economy not available");
            return false;
        }

        CurrencyType type = CurrencyType.COINS;
        switch (sub.toLowerCase(Locale.ROOT)) {
            case "set":
                econ.setBalance(target.getUniqueId(), type, amount);
                return true;
            case "give":
                econ.addBalance(target.getUniqueId(), type, amount);
                return true;
            case "take":
                econ.subtractBalance(target.getUniqueId(), type, amount);
                return true;
            default:
                return false;
        }
    }

    private double parseAmount(String raw) {
        if (raw == null) return -1;
        String s = raw.replaceAll(",", "").trim().toLowerCase(Locale.ROOT);
        Matcher m = AMOUNT_PATTERN.matcher(s);
        if (!m.matches()) return -1;
        String num = m.group(1);
        String suffix = m.group(2);
        double base;
        try {
            base = Double.parseDouble(num);
        } catch (NumberFormatException e) {
            return -1;
        }
        switch (suffix) {
            case "k": return base * 1_000d;
            case "m": return base * 1_000_000d;
            case "b": return base * 1_000_000_000d;
            case "t": return base * 1_000_000_000_000d;
            default: return base;
        }
    }
}