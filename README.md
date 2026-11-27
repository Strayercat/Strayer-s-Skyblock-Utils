# Strayer's Skyblock Utils (SSU)

A client-side Fabric mod with quality-of-life tweaks for Hypixel SkyBlock.

SSU started as a personal project: things I wanted in the game that other mods didn't do, or didn't do the way I liked. It's my way of contributing to the SkyBlock modding community.

**[Download on Modrinth](https://modrinth.com/mod/strayers-skyblock-utils)** · **[Report an issue](https://github.com/Strayercat/Strayer-s-Skyblock-Utils/issues)**

---

## Requirements
- [Fabric API](https://modrinth.com/mod/fabric-api) : Required
- [Cloth Config API](https://modrinth.com/mod/cloth-config) : Required
- [Mod Menu](https://modrinth.com/mod/modmenu) : Recommended

Everything can be configured with `/ssu config` or through Mod Menu. The config is split into **General**, **HUD Settings**, **Chat**, **Per Skill**, **Events** and **Voice Chat** tabs. HUD Settings only has the HUD editor, the button editor, the cat and an on/off toggle for each HUD, everything else about a HUD is set in the HUD editor.

SSU keeps its files in `config/SSU/` (config, chat filters, button layouts and caches). An old `config/SkyblockUtils.json` is moved there automatically, with a backup in `config/SSU/old/`.

---

## Features

<details>
<summary><b>HUD Editor</b></summary>

- Open it with `/ssu hud` or the **Edit HUDs** button in the config
- **Per-location layouts**: pick a location (Hub, Crimson Isle, Dwarven Mines, Catacombs…) from the dropdown at the bottom, it opens on your current one
    - Each location has its own position, size, scale, options and visibility for every HUD
    - HUDs you haven't changed in a location use the **All Locations** layout
- Drag a HUD to move it, it snaps to the screen edges and center (hold Shift to disable snapping)
- Scroll over a HUD to scale it (Shift for fine steps), text and images scale with it
- Drag a HUD's sides to change its width, text wraps to fit, double click for auto width
- **Stick HUDs together**: drag a HUD next to another one and it attaches to that side, aligned to its start, end or center
    - Stuck HUDs follow the one they're attached to when it moves or changes size
    - Corners that touch another HUD or a screen edge are square, every other corner is rounded
- **Right click a HUD** for its options menu:
    - Hide it in the current location
    - HUD-specific options (for example the cat, ping or coordinates in the SSU HUD), per location
    - Reset position, reset size, auto width, scale to 100%
    - Center it horizontally or vertically, unstick it
    - Square or rounded corners
    - Background opacity slider, and a reset for it
    - Use the All Locations layout, or make this location's layout the All Locations one
- **Middle click a HUD** to hide it in the current location
- **Right click empty space** to bring back the HUDs hidden in the current location
- HUDs turned off in the config don't show in the editor
- A short guide to every control is shown in the background of the editor
- HUDs can't go off screen
- Works with the SSU HUD, custom sidebar, slayer tracker, carry tracker, bestiary HUD, stash HUD, voice chat HUD and system messages HUD

</details>

<details>
<summary><b>Inventory Buttons</b></summary>

- Replaces the crafting grid, offhand slot and recipe book button in your inventory with a 3×4 grid of buttons in 3 named rows
- Comes with a default set: stashes, recipe book, bestiary, calendar, SkyCrypt, wiki, glowing players, HUD editor and SSU config
- Each button has a name, an icon and up to 3 actions: click, Shift + click and Ctrl + click, each with its own name in the tooltip
- Actions can be a command, a chat message, a URL or an SSU action (wiki, your SkyCrypt, HUD editor, SSU config, button editor, glowing players)
- **Button editor**: open it with `/ssu buttons`, the **Edit Buttons** button in the config or Shift + click on the default SSU Config button
    - Drag a button to move or swap it, drag a row name onto another to swap rows
    - Right click a button or row for more (clear, reset to default, copy…)
    - Names support color codes, `&#RRGGBB` hex colors and SSU's title gradient
- **Per-location sets**: edit the Default set, or give a location its own buttons
- **Icon picker** with Minecraft items, SkyBlock items (heads included), SSU icons and emojis, with search and recent icons
    - Use your own head, a plain head or any player's head
    - Hover any item, even in a menu, and press the **Copy Hovered Item As Button Icon** key, then paste it in the icon picker
- **Saved layouts**: save, load, rename and delete layouts, or export one as text to share and import someone else's
- Toggle it in the **HUD Settings** tab of the config

</details>

<details>
<summary><b>Wiki & SkyCrypt</b></summary>

- Hover a SkyBlock item (or hold it) and press `U` to open its page on the Hypixel SkyBlock wiki
    - Reforges, stars, pet levels and stack counts are stripped from the name so the search lands on the right item
    - With no item, it opens the wiki's home page
    - SkyBlock item tooltips show a hint with the key (can be turned off)
- `/ssu wiki` opens the wiki home page
- `/ssu skycrypt [player]` opens a player's SkyCrypt profile, or yours with no name
- **In-game browser**: pages open inside the game instead of your normal browser
    - The browser engine is downloaded the first time you use it
    - `Ctrl + O` opens the page in your normal browser, `Ctrl + Scroll` or `Ctrl + -/=/0` zooms, `Esc` closes it
    - Can be preloaded when you join SkyBlock so pages open instantly (~150 MB RAM)
    - Turn it off in the **General** tab to always use your normal browser

</details>

<details>
<summary><b>Voice Chat</b></summary>

- Talk with other SSU users in your party or co-op, no Discord needed
- Push-to-talk or voice activation
- RNNoise noise suppression, a speech gate and automatic gain so voices come through clean and at a steady volume
- Mute and deafen, with your status shown to others
- Voice HUD showing who's connected and who's talking
- Mic test to hear what others hear
- Off by default, enable it in the **Voice Chat** tab of the config

</details>

<details>
<summary><b>SSU Users</b></summary>

- A gem appears next to other SSU users in the tab list, chat and nametags
- SSU users get a custom nameplate framed in their gem color
- Gem colors:
    - **Prismatic**: mod developer
    - **Purple**: VIPs who supported the mod during development
    - **Red**: helpers who gave useful feedback, bug reports or ideas
- A few easter eggs for VIPs and special users, like paw print trails and custom capes
- Emoji picker in chat, emojis are sent as `:name:` and show up for other SSU users

</details>

<details>
<summary><b>Slayer</b></summary>

- **Slayer tracker HUD** showing your current slayer with its mob head
    - Kills this session, last kill time and average kill time, tracked separately for each boss and tier
    - Live timer while your boss is alive
    - Slayer XP towards the next level, and how many bosses until you level up (worked out from the XP you actually gain, so XP buffs are included)
    - Each line can be hidden per location in the HUD editor
    - `/ssu slayer reset` clears the session stats
- **Carry tracker** with `/ssu carry <player> <amount> <price>`
    - Counts a carry every time the player's slayer boss dies
    - Doesn't count it if the player died, left the server, or the boss timer ran out
    - Track several carries at once, each with its own progress bar and the coins owed so far on the carry HUD
    - Prices accept `26k`, `1.2m`, `1,2m`, `1,200,000` and more
    - `/ssu carry adjust <player> add|remove [amount]` fixes a carry's count by hand
    - A title and notification tell you when it's time to collect the payment, with the total owed
    - `/ssu carry stop [player]` cancels a carry and shows how much is owed so far

</details>

<details>
<summary><b>Dungeons</b></summary>

- Floor join commands from party chat (`!m7`, `!f3`…)
- Floor auto-rejoin with `/ssu autorejoin <floor>`, with an adjustable rejoin delay
- Downtime tracker with `!dt [reason]`, reminds you when the run ends
- Custom void lava texture in the F7 and M7 boss rooms (also removes the fire overlay)

</details>

<details>
<summary><b>Mining</b></summary>

- Corleone timer, reminds you 1 minute after killing him
- Umber and tungsten vein waypoints in the Glacite Tunnels
- Powder chest and loot chest rewards shown as a notification instead of chat spam
- Obvious gemstones: clearer gem textures for gemstone glass and panes (Opal on Crimson Isle, Crystal Hollows gems, Glacite Tunnels gems and every gem in mineshafts)

</details>

<details>
<summary><b>Foraging</b></summary>

- Tree gift notifications
- Phantom titles when a Phanflare, Phanpyre or Dreadwing spawns

</details>

<details>
<summary><b>Bestiary</b></summary>

- Bestiary HUD showing the last mobs you killed with their head, kill count and progress to the next milestone
- Hides itself after 2 minutes without a kill
- Progress is loaded from your SkyBlock profile and kept up to date as you kill mobs
- Maxed mobs and showing it in dungeons can be toggled per location in the HUD editor
- Kill counts can be slightly off: there's no way to make kill detection fully reliable, so SSU uses strict checks to keep miscounts as rare as possible, but they can still happen

</details>

<details>
<summary><b>Party</b></summary>

- Party commands for the leader: `!pt`, `!warp`, `!allinv`
- Party invites shown as clickable on-screen notifications, `/ssu partyinvites` lists recent ones
- Boop notifications that let you party whoever booped you
- Party members glow automatically

</details>

<details>
<summary><b>Chat</b></summary>

- SSU style chat: the chat window and chat input drawn with SSU's rounded corners and outline, other mods' chat changes are kept
- Toggleable chat filters (work in progress), including stash reminders and stash pickup messages
- Stat commands that answer in the same channel: `!tps`, `!ping`, `!fps`
- Silly percentage commands in party, guild and co-op chat (`!sus`, `!furry` and more)
- Fancy emotes (`o/` becomes `( ﾟ◡ﾟ)/` and more)
- Separate HUDs for server messages and player messages
    - Can be turned off in specific locations by hiding the system messages HUD there in the HUD editor
    - Optionally still open the server chat history in those locations
- Duplicate counter for system messages, repeated messages show `(x2)`, `(x3)`… with customizable text and color
- Chat peek and server chat history, with search
- Copy a system message by middle clicking it in the history, or with a keybind
- Separator width fix for non-default chat widths
- Send your coordinates (and location) to chat with a keybind

</details>

<details>
<summary><b>Glowing Players</b></summary>

- Make any player glow in the color of your choice
- Manage them with `/ssu glowingplayers` or the config GUI

</details>

<details>
<summary><b>HUD & Misc</b></summary>

- Contextual HUD with time, ping, TPS, FPS, coordinates, sidebar, dailies, party info and island fun facts, each toggleable per location
- Custom sidebar
- Stash HUD showing how much is in your item and material stash, and hides the stash reminders from chat
- Customizable mod colors and notification style, switching color profiles applies right away without a restart
- Notifications and screenshot previews always show on top of everything, including chat and menus
- Zoom
- NPC locator with `/ssu npcfinder <npc>`
- Puff farming helper that tells you when to kill
- Screenshot HUD with a preview, click to copy
- Daily task reminders
- Spooky Festival chest titles and loot notifications
- Auto Hoppity eggs
- Custom block breaking animation
- Option to remove the first person arm swing animation

</details>

---

## Commands

| Command                                           | Description                                                 |
|---------------------------------------------------|-------------------------------------------------------------|
| `/ssu config`                                     | Open the config screen                                      |
| `/ssu hud`                                        | Open the HUD editor                                         |
| `/ssu buttons`                                    | Open the inventory button editor                            |
| `/ssu wiki`                                       | Open the Hypixel SkyBlock wiki                              |
| `/ssu skycrypt [player]`                          | Open a player's SkyCrypt profile (yours by default)         |
| `/ssu carry <player> <amount> <price>`            | Start tracking a slayer carry                               |
| `/ssu carry adjust <player> add\|remove [amount]` | Add or remove completed carries by hand (default 1)         |
| `/ssu carry stop [player]`                        | Stop a carry (the player is required when carrying several) |
| `/ssu slayer reset`                               | Reset the slayer tracker's session stats                    |
| `/ssu autorejoin <floor\|off>`                    | Auto-rejoin a floor (`m1`–`m7`, `f1`–`f7`)                  |
| `/ssu glowingplayers add <player>`                | Make a player glow                                          |
| `/ssu glowingplayers remove <player>`             | Remove a glowing player                                     |
| `/ssu glowingplayers list`                        | List glowing players                                        |
| `/ssu glowingplayers clear`                       | Remove all glowing players                                  |
| `/ssu glowingplayers gui`                         | Open the glowing players GUI                                |
| `/ssu npcfinder <npc>`                            | Locate an NPC                                               |
| `/ssu partyinvites`                               | Show your last 10 party invites                             |
| `/ssu disableReminder <type>`                     | Disable a daily reminder                                    |

`/strayerskyblockutils` works as an alias for `/ssu`.

## Keybinds

All keybinds can be changed in **Options → Controls → Strayer's Skyblock Utils**.

| Keybind                          | Default        |
|----------------------------------|----------------|
| Show SSU HUD                     | `Z`            |
| Zoom                             | `C`            |
| Chat Peek                        | `Left Alt`     |
| Server Chat History              | `H`            |
| Copy Hovered System Message      | `Middle Mouse` |
| Open Wiki                        | `U`            |
| Copy Hovered Item As Button Icon | Unbound        |
| Send coordinates in chat         | `Home`         |
| Corleone Timer                   | `T`            |
| Puff Timer                       | `Y`            |
| Voice Chat Push To Talk          | `V`            |
| Voice Chat Mute                  | `M`            |
| Voice Chat Deafen                | Unbound        |

---

## Privacy

SSU connects to its own server to find other SSU users, to set up voice chat and for the bestiary HUD.

- **Login:** your account is verified through Mojang's session server, the same check a normal server join does. Your access token is never sent to the SSU server.
- **Finding SSU users:** only hashed player names from your tab list, party and guild are sent.
- **Voice chat:** Audio goes directly between players and is encrypted, it never passes through the SSU server.
- **Bestiary:** your SkyBlock profile ID is sent so the server can load your bestiary progress from the Hypixel API. The profile and the kills SSU counts are cached on the server so your progress carries over between sessions.
- **Item list:** the icon picker downloads SkyBlock's public item list straight from the Hypixel API (not through the SSU server) and caches it in `config/SSU/cache/`.

---

## Reporting Issues

This is the first mod I've developed and released, so bugs and crashes can happen :p

If you run into one, please [open an issue](https://github.com/Strayercat/Strayer-s-Skyblock-Utils/issues/new) and include:
- What happened and how to reproduce it
- Your mod version
- Screenshots if they help
- Your `logs/latest.log` file

---

## Credits

- [MCEF Modern](https://modrinth.com/mod/mcef-modern), the Chromium browser used for the in-game wiki and SkyCrypt
- [Hypixel Mod API](https://modrinth.com/mod/hypixel-mod-api), used for location detection
- [Concentus](https://github.com/lostromb/concentus), a pure Java Opus codec (BSD license)
- [rnnoise4j](https://github.com/henkelmax/rnnoise4j) by Max Henkel, Java bindings for [RNNoise](https://github.com/xiph/rnnoise) (BSD license)

## License

[CC0 1.0](LICENSE)