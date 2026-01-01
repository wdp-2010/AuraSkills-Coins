package dev.aurelium.auraskills.bukkit.skillcoins;

import dev.aurelium.auraskills.api.event.skill.SkillLevelUpEvent;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.skillcoins.CurrencyType;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listener that rewards players with Skill Tokens AND SkillCoins when they level up skills
 * 
 * ECONOMY BALANCE V2:
 * - Coins are awarded ONLY every 5 levels (makes each reward feel meaningful)
 * - Tokens are awarded ONLY every 10 levels (premium currency, 1 token = 500 coins worth)
 * - Scaling rewards encourage continued progression without being overwhelming
 * 
 * Expected total per skill (100 levels):
 * - Coins: 2,670 coins (from 20 payouts at levels 5,10,15...100)
 * - Tokens: 32 tokens (from 10 payouts at levels 10,20,30...100)
 * 
 * With 11 skills, a maxed player earns: ~29,370 coins, ~352 tokens
 * This ensures long-term goals while keeping early game accessible
 */
public class TokenRewardListener implements Listener {

    private final AuraSkills plugin;

    public TokenRewardListener(AuraSkills plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSkillLevelUp(SkillLevelUpEvent event) {
        Player player = event.getPlayer();
        int level = event.getLevel();
        
        if (plugin.getSkillCoinsEconomy() == null) {
            return;
        }

        // Calculate rewards - only given at milestone levels
        int tokenReward = calculateTokenReward(level);
        int coinsReward = calculateCoinsReward(level);
        
        // Award tokens only at milestone levels (every 10)
        if (tokenReward > 0) {
            plugin.getSkillCoinsEconomy().addBalance(player.getUniqueId(), CurrencyType.TOKENS, tokenReward);
        }
        
        // Award coins only at milestone levels (every 5)
        if (coinsReward > 0) {
            plugin.getSkillCoinsEconomy().addBalance(player.getUniqueId(), CurrencyType.COINS, coinsReward);
        }
        
        // Record transient reward so the default AuraSkills level-up message can include it
        dev.aurelium.auraskills.common.skillcoins.TokenCoinRewardCache.put(player.getUniqueId(), event.getSkill().toString(), level, coinsReward, tokenReward);
    }

    /**
     * Calculate token reward based on level
     * ONLY given every 10 levels (premium currency)
     * 
     * Token rewards by level:
     * - Level 10: 1 token
     * - Level 20: 1 token
     * - Level 30: 2 tokens
     * - Level 40: 2 tokens
     * - Level 50: 3 tokens (milestone bonus)
     * - Level 60: 3 tokens
     * - Level 70: 4 tokens
     * - Level 80: 4 tokens
     * - Level 90: 5 tokens
     * - Level 100: 7 tokens (max level bonus)
     * Total per skill: 32 tokens
     */
    private int calculateTokenReward(int level) {
        // Only award tokens at multiples of 10
        if (level % 10 != 0) {
            return 0;
        }
        
        if (level == 100) {
            return 7; // Max level bonus
        } else if (level >= 90) {
            return 5;
        } else if (level >= 70) {
            return 4;
        } else if (level >= 50) {
            return 3;
        } else if (level >= 30) {
            return 2;
        } else {
            return 1; // Levels 10, 20
        }
    }
    
    /**
     * Calculate SkillCoins reward based on level
     * ONLY given every 5 levels
     * 
     * Coin rewards by level tier:
     * - Levels 5-10: 15, 20 coins (35 total)
     * - Levels 15-25: 30, 35, 40 coins (105 total)
     * - Levels 30-50: 50, 60, 70, 80, 90 coins (350 total)
     * - Levels 55-75: 110, 130, 150, 170, 190 coins (750 total)
     * - Levels 80-90: 220, 250, 280 coins (750 total)
     * - Levels 95-100: 320, 360 coins (680 total)
     * Total per skill: 2,670 coins
     */
    private int calculateCoinsReward(int level) {
        // Only give coins on multiples of 5
        if (level % 5 != 0) {
            return 0;
        }
        
        // Scaled rewards that feel meaningful but not overwhelming
        if (level <= 10) {
            // Early game: 15, 20, (total 35 for first 10 levels)
            return 10 + (level / 5) * 5; // 15 at 5, 20 at 10
        } else if (level <= 25) {
            // Establishing: 30, 35, 40 (total 105 for 15-25)
            return 25 + ((level - 10) / 5) * 5; // 30, 35, 40
        } else if (level <= 50) {
            // Mid game: 50, 60, 70, 80, 90 (total 350 for 30-50)
            return 40 + ((level - 25) / 5) * 10; // 50, 60, 70, 80, 90
        } else if (level <= 75) {
            // Late game: 110, 130, 150, 170, 190 (total 750 for 55-75)
            return 90 + ((level - 50) / 5) * 20; // 110, 130, 150, 170, 190
        } else if (level <= 90) {
            // End game: 220, 250, 280 (total 750 for 80-90)
            return 190 + ((level - 75) / 5) * 30; // 220, 250, 280
        } else {
            // Master tier: 320, 360 (total 680 for 95-100)
            return 280 + ((level - 90) / 5) * 40; // 320 at 95, 360 at 100
        }
    }
    
    /**
     * Get the SkillCoins reward for a specific level
     * Returns 0 for non-milestone levels
     */
    public static int getCoinsRewardForLevel(int level) {
        if (level % 5 != 0) {
            return 0;
        }
        
        if (level <= 10) {
            return 10 + (level / 5) * 5;
        } else if (level <= 25) {
            return 25 + ((level - 10) / 5) * 5;
        } else if (level <= 50) {
            return 40 + ((level - 25) / 5) * 10;
        } else if (level <= 75) {
            return 90 + ((level - 50) / 5) * 20;
        } else if (level <= 90) {
            return 190 + ((level - 75) / 5) * 30;
        } else {
            return 280 + ((level - 90) / 5) * 40;
        }
    }
    
    /**
     * Get the Token reward for a specific level
     * Returns 0 for non-milestone levels
     */
    public static int getTokenRewardForLevel(int level) {
        if (level % 10 != 0) {
            return 0;
        }
        
        if (level == 100) {
            return 7;
        } else if (level >= 90) {
            return 5;
        } else if (level >= 70) {
            return 4;
        } else if (level >= 50) {
            return 3;
        } else if (level >= 30) {
            return 2;
        } else {
            return 1;
        }
    }
}
