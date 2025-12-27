# AuraSkills (AuraSkills-Coins fork)

**AI Authorship Notice:** **This fork was produced by an AI due to a time shortage.** Review the code, integration (SkillCoins), and licensing carefully before reuse.

---

## 🚀 Overview

AuraSkills is an RPG-style skills and stats plugin for Minecraft servers, providing skills, abilities, menus, loot, and a flexible API for integrations. This repository contains the AuraSkills code modified to include SkillCoins integration (see the `skillcoins` and `bukkit` modules).

## ✨ Features (summary)

- Skill progression and XP systems
- Player stats and attribute modifiers
- Abilities with mana and activation mechanics
- Inventory-style skill/ability menus
- Loot tables and configurable rewards
- Extensible API for integrations and addons
- Included `SkillCoins` economy integration module

## ⚙️ Requirements

- Java 17+ (verify Gradle `toolchain` or `gradle.properties` for exact target)
- Paper/Spigot 1.20+ / 1.21+ (verify `api-version` in `plugin.yml` for the desired build)
- Gradle (wrapper included)

## 🛠 Build & Run

Build using the provided Gradle wrapper:

```bash
./gradlew clean build
```

Install the produced JAR(s) into your server's `plugins/` folder.

## 🧩 SkillCoins Integration

This fork contains the `skillcoins` implementation under `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/skillcoins` (or similar). It adds:
- Token/coin currency types
- Shop menu hooks and transaction menus
- Token exchange UI

Refer to `SKILLCOINS_README.md` in this repository for specific setup and default shop configs.

## ✅ Important Notes

- This copy was processed by an AI assistant to speed development. Verify all functionality and license compatibility (AuraSkills upstream license and any changes made here) before distributing.
- Default resource files (e.g., `SkillCoinsShop/*`) are bundled in `bukkit/src/main/resources/` and will be auto-generated into server `plugins/` folder on first run.

## 🛠 Contributing & Support

See `CONTRIBUTING.md` and the project `wiki/` for guidelines. If this fork is for internal use, update docs and configs to reflect your deployment choices.

## 📄 License

This project retains the upstream license; verify `LICENSE.md` in this repository for details.

---

If you'd like, I can also standardize the `resourcepack/README.md` so the style matches these READMEs.✅
