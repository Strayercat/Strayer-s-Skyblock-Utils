# Changelog (since 5.0.2)

## New
- **Inventory Buttons**: the crafting grid, offhand slot and recipe book button are replaced by a fully customizable 3×4 grid of buttons
    - Default set with stashes, recipe book, bestiary, calendar, SkyCrypt, wiki, glowing players, HUD editor and SSU config
    - Every button gets a name, an icon and separate click, Shift + click and Ctrl + click actions (command, chat message, URL or SSU action)
    - Button editor with `/ssu buttons`: drag to move or swap buttons and rows, right click for more options, color codes and hex colors in names
    - Per-location button sets, with a Default set for everywhere else
    - Icon picker with Minecraft items, SkyBlock items, heads (yours, plain or any player's), SSU icons and emojis, with search and recents
    - New **Copy Hovered Item As Button Icon** keybind to grab any item's icon, even from a menu
    - Save, load, rename, delete, export and import button layouts
- **Wiki lookup**: hover or hold a SkyBlock item and press `U` to open its Hypixel SkyBlock wiki page
    - Reforges, stars, pet levels and stack counts are stripped from the search
    - Opens the home page with no item, or with `/ssu wiki`
    - Optional tooltip hint showing the key
- **SkyCrypt**: `/ssu skycrypt [player]` opens a SkyCrypt profile, yours by default
- **In-game browser**: the wiki and SkyCrypt open inside the game
    - Downloaded the first time it's used, with an option to preload it when joining SkyBlock
    - `Ctrl + O` to open in your normal browser, `Ctrl + Scroll` to zoom
    - Can be turned off to always use your normal browser
- **Slayer tracker HUD**: kills, last and average kill time per boss and tier, live boss timer, XP to next level and bosses left, `/ssu slayer reset`
- **Bestiary HUD**: your last killed mobs with their kill count and progress to the next milestone, loaded from your profile
- **Stash HUD**: how much is in your item and material stash, and hides the stash reminders from chat
- **Per-location HUD editor**: every HUD has its own position, size, scale, options and visibility per location, with an All Locations fallback
    - HUDs can be stuck together and follow each other
    - Right-click a HUD for its options menu, middle click to hide it
    - New background opacity slider per HUD
- **Obvious gemstones**: clearer textures for gemstone glass and panes (works with Continuity)
- **Custom block breaking animation**
- **SSU style chat**: rounded chat window and chat input with SSU's outline
- **Carry tracker**: `/ssu carry adjust <player> add|remove [amount]`, and carries aren't counted if the player died, left or the boss timer ran out
- Adjustable dungeon auto-rejoin delay
- Option to hide maxed mobs in the bestiary HUD
- New emojis

## Changes
- Config now lives in `config/SSU/`, the old config is moved there automatically (backup in `config/SSU/old/`)
- Switching color profiles applies right away, no restart needed
- The vanilla recipe book button is now one of the inventory buttons instead of a redirect
- Location detection now uses the Hypixel Mod API
- HUD Settings tab trimmed down: per-HUD options moved to the HUD editor
- **Copy Hovered System Message** is now bound to middle mouse by default
- Emoji names changed to match Discord's
- Removed Autofish: as controversial as that may be, I've decided it's best for the mod's long term health.

## Fixes
- Co-op scan no longer closes your chat
- Messages modified by other mods are no longer broken when sent to system chat
- Hovering an emoji no longer counts as hovering the chat window behind it
- Gemstone panes not updating correctly (still slightly wonky, to be worked on)
- System message copying
- Chat emote parsing
- Easter eggs not showing in some cases