# Exponential Level Purchase Pricing System

## Overview
Implemented an exponential pricing system for skill level purchases that is balanced with the shop economy.

## Pricing Formula
- **Base Price**: 500 tokens (500 coins)
- **Growth Rate**: 1.08 (8% increase per level)
- **Formula**: `Cost = 500 × (1.08 ^ (level - 1))`

## Pricing Examples

### Early Levels (1-10)
| Level | Cost (Tokens) | Cumulative Total |
|-------|---------------|------------------|
| 1     | 500          | 500              |
| 2     | 540          | 1,040            |
| 3     | 583          | 1,623            |
| 4     | 630          | 2,253            |
| 5     | 680          | 2,933            |
| 6     | 734          | 3,667            |
| 7     | 793          | 4,460            |
| 8     | 856          | 5,316            |
| 9     | 925          | 6,241            |
| 10    | 999          | 7,240            |

### Mid Levels (20-30)
| Level | Cost (Tokens) | 
|-------|---------------|
| 20    | 2,330         |
| 25    | 3,426         |
| 30    | 5,034         |

### Late Levels (40-50)
| Level | Cost (Tokens) |
|-------|---------------|
| 40    | 10,970        |
| 45    | 16,124        |
| 50    | 23,699        |

## Balance Philosophy

### Economy Integration
- **1 Token = 100 Coins** in the shop system
- Players earn **10 coins per skill level** (~0.1 tokens)
- Starting price of **500 tokens** = **50,000 coins earned** through gameplay

### Rarity Tiers (from Balance system)
- **500 tokens** = Legendary tier item (skill levels are premium)
- Matches the economy where end-game items cost 500-2000 coins
- Skill levels are 100x more expensive, making them a long-term investment

### Progression Balance
- **Early game** (Levels 1-10): Affordable for new players
  - Total cost: ~7,240 tokens
  - Average ~724 tokens per level
  
- **Mid game** (Levels 11-30): Steady progression
  - Total cost: ~50,000 tokens (for all 30 levels)
  - Average ~1,667 tokens per level
  
- **Late game** (Levels 31-50): Prestige investment
  - Total cost: ~200,000 tokens (for all 50 levels)
  - Average ~4,000 tokens per level

### Growth Rate Justification
- **8% per level** provides smooth exponential growth
- Not too steep: Allows gradual progression
- Not too shallow: Maintains value and challenge
- Comparable to skill trees in other RPG systems

## Display Features

### Skill Selection Menu
- Shows **exponential pricing** notice
- Displays **next level cost** for each skill
- Shows example costs at levels 1, 10, and 50

### Level Buy Menu
- **Individual level prices** shown on each level icon
- **Total cost** displayed in purchase confirmation
- **Average cost per level** shown for multi-level purchases
- Real-time balance updates showing tokens after purchase

### Price Transparency
- All prices clearly displayed in tokens
- Selected levels show their individual costs
- Confirm button shows:
  - Total cost
  - Average per level
  - Balance after purchase

## Implementation Details

### Files Modified
1. `LevelBuyMenu.java`
   - Added exponential pricing constants
   - Implemented `calculateLevelCost(int level)` method
   - Implemented `calculateTotalCost(int start, int end)` method
   - Updated all price displays to show exponential costs

2. `SkillLevelPurchaseMenu.java`
   - Added same pricing constants for consistency
   - Updated skill selection display
   - Updated balance info display
   - Added pricing calculation methods

### Key Methods
```java
// Calculate cost of a single level
private double calculateLevelCost(int level) {
    return BASE_PRICE * Math.pow(GROWTH_RATE, level - 1);
}

// Calculate total cost for multiple levels
private double calculateTotalCost(int startLevel, int endLevel) {
    double total = 0.0;
    for (int i = startLevel + 1; i <= endLevel; i++) {
        total += calculateLevelCost(i);
    }
    return total;
}
```

## Testing Recommendations

### Test Cases
1. **Level 1 Purchase**: Should cost exactly 500 tokens
2. **Multiple Levels**: Total should be sum of individual costs
3. **High Levels**: Verify prices scale appropriately
4. **Display**: Confirm all prices show correctly in lore
5. **Balance Check**: Verify can't purchase without sufficient tokens

### Balance Testing
- Monitor player progression rates
- Track average levels purchased per session
- Verify economy remains balanced
- Check if prices feel fair to players

## Future Adjustments

### If Prices Too High
- Reduce `BASE_PRICE` to 400 or 450
- Reduce `GROWTH_RATE` to 1.07 (7%)

### If Prices Too Low
- Increase `BASE_PRICE` to 600 or 750
- Increase `GROWTH_RATE` to 1.09 (9%)

### Alternative Models
- Could implement different rates for different skills
- Could add bulk purchase discounts
- Could add level milestones with bonus rewards

## Summary
This exponential pricing system provides:
✅ Balanced progression from early to late game
✅ Clear price transparency in all menus
✅ Integration with existing token economy
✅ Scalable system for 50+ levels
✅ Premium feel matching legendary item tier
