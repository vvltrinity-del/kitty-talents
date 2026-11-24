# Kitty Talents 5.1.1

> **Minecraft Forge 1.20.1** · Bug fixes and a Habitat energy bar. Update the server and every client together: the Habitat screen now syncs one more value.

## 🐛 Fixes
- **Morning gifts can't be farmed any more.** Cats gave gifts every time you got out of bed, so sleeping and clicking "Leave Bed" partway through the night gave gifts without limit. Gifts now come only when the night actually passes, once a morning. (Reported by a player.)
- **Support cats come through portals with you.** Followers changing dimension with their owner were picked by a fixed list of modes, so Support cats (and follow modes from add-ons) were left behind, while Guard and Aggressive cats got pulled off their posts. Now every mode that follows you comes along, and only those.
- **The whistle's Return Home goes to the right dimension.** It teleported the cat to its bed's coordinates in whatever dimension the cat was in, so a cat in the Nether with a bed in the Overworld could land in lava. It now crosses to the bed's dimension. Cats also no longer try to walk to a bed in another dimension.
- **Co-owner items follow the trust roles.** Twelve items checked only "is this a co-owner", so a Rider could use them all:
  - **Handler and up:** the whistle, laser pointer and base pin.
  - **Full co-owners and up:** collars, the gender ribbon, grooming shears, the work clipboard, the cat carrier, breeding catnip, spirit catnip and tier-upgrade treats.
  - **Owner only:** the Respec Elixir and the Amnesia Treat.
  - Food, healing, toys and trackers still work for every co-owner.
- **Happy Eater's meat rule works.** The cat food tag also holds every meat, so every cat ate beef and pork from bowls and Habitats. Now beef, pork, mutton and rabbit need Happy Eater. Every cat eats fish and chicken, by hand too.
- **Habitat recipes give all their outputs.** Only a recipe's first output was ever made, so the bonus outputs and chances JEI shows never dropped (for example the catnip recipe's 50% Catnip Treat). Each extra output now rolls by its chance every cycle, through the Smelter Upgrade too. A Nekomata's double cycle rolls them twice.
- **The catnip Habitat recipe runs at its own speed.** It wrote its time as `ticks`, which nothing read, so it used the 160-tick default instead of its 200. Habitat recipes accept `ticks` or `duration` now.

## ✨ New
- **Energy bar in the Habitat screen**, under the outputs. Hover it for the stored FE (up to 20,000). The bar lights up while the Habitat has the 40 FE per tick it needs for double speed.

## 🔧 Under the hood
- `util/CatTeleport`: moving a cat to a point in any dimension, used by portal followers and Return Home.
- New permission actions `CARE` (Full co-owners) and `RESET_STATS` (owner only).
- 8 new GameTests (`KittyFiveOneOneGameTests`); 218 in all.

*This mod was created with AI.*
