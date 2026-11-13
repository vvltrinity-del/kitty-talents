"""
Writes the Kitty Talents player guide from the mod's own data (talents, abilities, modes, jobs, items, recipes...).

    python tools/gen_guide.py [artifact_out.html]

Output: docs/guide/kitty-talents-guide.html (a standalone page). With an argument, also writes the same page without
the <html>/<head>/<body> wrapper (for publishing as a claude.ai artifact). Re-run after changing talents or items.
"""
import html
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import guide_data as g  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs/guide/kitty-talents-guide.html"
D = g.all_data()
E = g.elements()


def esc(s):
    return html.escape(str(s), quote=True)


def kbd(k):
    return f"<kbd>{esc(k)}</kbd>"


def pips(n):
    return f'<span class="pips" aria-label="{n} ranks">' + "".join("<i></i>" for _ in range(n)) + "</span>"


def num(x):
    return f"{x:g}"


# ------------------------------------------------------------------------------------------------ item descriptions
ITEM_INFO = {
    "catnip": ("Food & treats", "Drops from grass, tall grass and ferns (15% per block). Tames a wild cat half the time; the base of most treats."),
    "catnip_treat": ("Food & treats", "Always tames a wild cat. Trains a cat from level 1 to 20."),
    "super_catnip_treat": ("Food & treats", "Trains a cat from level 21 to 40."),
    "master_catnip_treat": ("Food & treats", "Trains a cat from level 41 to 60."),
    "dire_treat": ("Food & treats", "Trains a cat from level 61 to 100, and awakens the five Dire talents."),
    "amnesia_treat": ("Care", "Wipes a cat's lifetime statistics."),
    "catnip_respec_elixir": ("Care", "Refunds every talent rank you paid for."),
    "healing_treat": ("Food & treats", "Heals 14 HP and restores 30 hunger."),
    "feline_wrap": ("Care", "Heals 10 HP and cures Poison and Wither."),
    "warm_milk": ("Care", "Revives a downed cat."),
    "catnip_cookie": ("Food & treats", "Cat food."),
    "salmon_sushi": ("Food & treats", "Cat food."),
    "tuna_bowl": ("Food & treats", "Cat food."),
    "premium_feast": ("Food & treats", "The most filling cat meal."),
    "deep_fried_chicken": ("Food & treats", "Food for players, cats and Food Bowls (nutrition 8, +27 cat hunger)."),
    "chicken_and_rice": ("Food & treats", "The best meal: Deep Fried Chicken + Steamed Rice Bowl. Nutrition 15, +50 cat hunger; the bowl comes back."),
    "base_pin": ("Tools", "Marks out bases (right-click two corners), gives cats guard posts (sneak-click a cat, then a block), and opens the base menu (right-click the air)."),
    "rice_grain": ("Food & treats", "From the Rice Mill."),
    "rice_bowl": ("Food & treats", "Cat food made from milled rice."),
    "catnip_bucket": ("Food & treats", "Milk a cow with it to get Frenzy Cat Milk."),
    "catnip_milk_bucket": ("Food & treats", "Feed to a cat to send it into a Super Feline Frenzy (with a cooldown)."),
    "coal_cat_treat": ("Production tiers", "Raises a Standard cat to the Coal tier."),
    "copper_cat_treat": ("Production tiers", "Raises a Coal cat to the Copper tier."),
    "iron_cat_treat": ("Production tiers", "Raises a Copper cat to the Iron tier."),
    "gold_cat_treat": ("Production tiers", "Raises an Iron cat to the Gold tier."),
    "lapis_cat_treat": ("Production tiers", "Raises a Gold cat to the Lapis tier."),
    "redstone_cat_treat": ("Production tiers", "Raises a Lapis cat to the Redstone tier."),
    "emerald_cat_treat": ("Production tiers", "Raises a Redstone cat to the Emerald tier."),
    "diamond_cat_treat": ("Production tiers", "Raises an Emerald cat to the Diamond tier."),
    "netherite_cat_treat": ("Production tiers", "Raises a Diamond cat to the Netherite tier."),
    "breeding_catnip": ("Breeding", "Feed two Talented Cats to breed a talented kitten."),
    "spirit_catnip": ("Breeding", "Feed a parent before breeding: the kitten is always a two-tailed Nekomata."),
    "variant_chart": ("Breeding", "Shows all 34 variants, their breeding recipes and production yields."),
    "gender_ribbon": ("Breeding", "Switches a cat between male and female."),
    "kitty_whistle": ("Tools", "Commands every cat of yours nearby. Sneak + right-click picks the command."),
    "producer_stick": ("Tools", "Calls cats to you from anywhere, even unloaded chunks. Sneak + right-click sends them to bed. Fireproof."),
    "kitty_tracker": ("Tools", "Lists your cats within 256 blocks and where they are."),
    "empty_locator_orb": ("Tools", "Right-click your cat to bind it."),
    "feline_locator_orb": ("Tools", "Shows the bound cat's coordinates and health from any distance."),
    "cat_carrier": ("Tools", "Right-click your cat to carry it; right-click the ground to let it out."),
    "cat_laser_pointer": ("Tools", "Opens the cat screen, like a stick."),
    "treat_bag": ("Tools", "Holds up to 576 treats. Right-click a cat to feed one."),
    "grooming_shears": ("Tools", "Removes accessories and grooms a cat."),
    "kitty_work_clipboard": ("Jobs & production", "Select a cat, then mark its work area, link chests, or put it in a Habitat."),
    "resource_catalyst": ("Jobs & production", "Habitat booster: +1 yield on every cycle. Crafted from Condensed Ore Essence, gold and redstone; never used up."),
    "catnip_catalyst": ("Jobs & production", "Makes a Habitat produce Catnip Leaves and treats. Crafted from catnip, bone meal and glowstone; never used up."),
    "ore_essence": ("Jobs & production", "Copper to netherite Habitat cats sometimes make one (5%, +5% per Mole Cat rank). Crafts the Resource Catalyst and a cheaper Netherite Cat Treat."),
    "cat_collar": ("Style", "Right-click your cat to put on a coloured collar."),
    "yarn_ball": ("Toys", "A toy to throw for Fetch cats."),
    "feather_toy": ("Toys", "A toy to throw for Fetch cats."),
    "toy_mouse": ("Toys", "A toy to throw for Fetch cats."),
    "cat_ball": ("Toys", "A toy to throw for Fetch cats."),
    "toy_fish": ("Toys", "A toy to throw for Fetch cats."),
    "frisbee": ("Toys", "A toy to throw for Fetch cats."),
    "talented_cat_spawn_egg": ("Other", "Creative mode only."),
}
for t in ("witch_hat", "top_hat", "bowtie", "hero_cape", "feline_goggles", "crown", "halo", "angel_wings", "cat_scarf", "phantom_mask"):
    ITEM_INFO[t] = ("Accessories", "A 3D accessory or cosmetic. Angel Wings beat when the cat flies." if t == "angel_wings" else "A 3D accessory or cosmetic.")
for i in range(1, 7):
    ITEM_INFO[f"upgrade_productivity_{i}"] = ("Jobs & production", "Makes a Kitty Habitat produce faster and more (up to 4 per Habitat).")

BLOCK_INFO = {
    "cat_food_bowl": "Holds 5 stacks of food. Hungry or hurt cats walk over and eat. Shows how full it is.",
    "cat_bed": "Cats sleep here at night. A bed is a cat's home for Return Home.",
    "cat_scratcher": "Cats use it when idle.",
    "cat_bath": "A wooden tub with a rubber duck.",
    "cat_rice_mill": "Mills rice grain.",
    "kitty_habitat": "House a cat here and it produces its tier's resource over time.",
    "cat_tree": "Two blocks tall. Guard and Wander cats perch on top and mark nearby monsters with Glowing.",
    "feeding_station": "Feeds nearby working cats automatically.",
    "kitten_incubator": "A wicker basket with a lamp for kittens.",
    "kitty_work_station": "A worker cat's base: drops its haul into the station's buffer.",
    "treat_oven": "Bakes treats.",
}


def recipe_text(rid):
    rs = D["recipes"].get(rid)
    if not rs:
        return '<span class="muted">Found or made in the world</span>'
    parts = []
    for r in rs[:2]:
        ing = ", ".join(f"{c}× {esc(n)}" if c > 1 else esc(n) for n, c in r["ingredients"].items())
        out = f" → {r['count']}" if r["count"] > 1 else ""
        parts.append(f'<span class="how">{esc(r["how"])}</span> {ing}{out}')
    return "<br>".join(parts)


# ------------------------------------------------------------------------------------------------ tables
def talents_table():
    rows = []
    order = ["Combat", "Defense", "Mobility", "Utility", "Support", "Dire"]
    for t in sorted(D["talents"], key=lambda t: (order.index(t["category"]) if t["category"] in order else 9, t["name"])):
        notes = []
        if t["dire"]:
            notes.append('<span class="tag dire">Needs Dire Treat</span>')
        if t["standard"]:
            notes.append('<span class="tag free">5 free ranks</span>')
        if t["needs"]:
            notes.append(f'<span class="tag mod">Needs {esc(t["needs"].upper() if t["needs"] == "tacz" else t["needs"])}</span>')
        mastery = f'<div class="mastery"><b>Mastery</b> {esc(t["mastery"])}</div>' if t["mastery"] else ""
        search = f'{t["name"]} {t["category"]} {t["desc"]} {t["mastery"]}'.lower()
        rows.append(f'<tr data-cat="{esc(t["category"])}" data-search="{esc(search)}"><td><b>{esc(t["name"])}</b><div class="sub">{esc(t["category"])}</div></td>'
                    f'<td>{pips(t["max"])}</td><td>{esc(t["desc"])}{mastery}</td><td>{"".join(notes)}</td></tr>')
    chips = "".join(f'<button type="button" class="chip" data-filter="{c}" aria-pressed="false">{c}</button>' for c in order)
    return (f'<div class="chips" role="group" aria-label="Filter talents by category"><button type="button" class="chip" data-filter="" aria-pressed="true">All 51</button>{chips}</div>'
            f'<div class="tablewrap"><table class="ref" id="talent-table"><thead><tr><th>Talent</th><th>Ranks</th><th>What it does</th><th></th></tr></thead><tbody>{"".join(rows)}</tbody></table></div>')


def abilities_table():
    rows = []
    for a in D["abilities"]:
        search = f'{a["name"]} {a["desc"]} {a["element"]} {a["kind"]}'.lower()
        rows.append(f'<tr data-search="{esc(search)}"><td><b>{esc(a["name"])}</b><div class="sub">{esc(a["kind"])}</div></td><td><span class="el el-{esc(a["element"].lower())}">{esc(a["element"])}</span></td>'
                    f'<td class="n">{a["energy"]}</td><td class="n">{num(a["cooldown"])} s</td><td>{esc(a["desc"])}</td></tr>')
    return f'<div class="tablewrap"><table class="ref"><thead><tr><th>Ability</th><th>Element</th><th class="n">Energy</th><th class="n">Cooldown</th><th>Effect</th></tr></thead><tbody>{"".join(rows)}</tbody></table></div>'


POLICY = {"NONE": "Never", "SELF_DEFENSE": "Only when attacked", "DEFEND_OWNER": "Anything threatening you or it",
          "AREA_DEFENSE": "Monsters near its post", "HUNT": "Prey, pests and nearby monsters", "SEEK_HOSTILES": "Every monster in a wide radius"}


def modes_table():
    rows = []
    for m in D["modes"]:
        where = "Follows you" if m["follows"] else (f"Holds its spot ({m['anchor']} blocks)" if m["anchor"] >= 0 else "Free")
        rows.append(f'<tr><td><b>{esc(m["name"])}</b></td><td>{esc(m["what"])}</td><td>{where}</td><td>{POLICY.get(m["targets"], m["targets"])}</td>'
                    f'<td>{"Yes" if m["retreats"] else "<b>No</b>"}</td></tr>')
    return f'<div class="tablewrap"><table class="ref"><thead><tr><th>Mode</th><th>Behaviour</th><th>Position</th><th>Attacks</th><th>Retreats when hurt</th></tr></thead><tbody>{"".join(rows)}</tbody></table></div>'


def jobs_table():
    rows = []
    for j in D["jobs"]:
        need = [f"level {j['level']}"] if j["level"] > 1 else []
        if j["talent"]:
            need.insert(0, f"{j['talent']} rank {j['rank']}")
        if j["tacz"]:
            need.append("TacZ installed")
        rows.append(f'<tr><td><b>{esc(j["name"])}</b></td><td>{esc(j["desc"])}</td><td>{esc(", ".join(need) or "Nothing")}</td></tr>')
    return f'<div class="tablewrap"><table class="ref"><thead><tr><th>Job</th><th>Work</th><th>Requires</th></tr></thead><tbody>{"".join(rows)}</tbody></table></div>'


def roles_table():
    src = (g.SRC / "combat/CatRole.java").read_text(encoding="utf-8")
    rows = []
    for name, hp, dmg, spd, en, crit, res, what in re.findall(
            r'^\s+[A-Z]+\("([^"]+)", ([\d.]+)f, ([\d.]+)f, ([\d.]+)f, ([\d.]+)f, (-?[\d.]+)f, (-?[\d.]+)f, "([^"]+)"', src, re.M):
        def pct(v, base=1.0):
            d = round((float(v) - base) * 100)
            return "·" if d == 0 else (f"+{d}%" if d > 0 else f"−{-d}%")
        rows.append(f'<tr><td><b>{esc(name)}</b></td><td class="n">{pct(hp)}</td><td class="n">{pct(dmg)}</td><td class="n">{pct(spd)}</td>'
                    f'<td class="n">{pct(en)}</td><td class="n">{pct(crit, 0)}</td><td class="n">{pct(res, 0)}</td><td>{esc(what)}</td></tr>')
    return (f'<div class="tablewrap"><table class="ref"><thead><tr><th>Role</th><th class="n">Health</th><th class="n">Damage</th><th class="n">Speed</th>'
            f'<th class="n">Energy regen</th><th class="n">Crit</th><th class="n">Resist</th><th>Style</th></tr></thead><tbody>{"".join(rows)}</tbody></table></div>')


def element_grid():
    names = E["names"]
    head = "".join(f'<th scope="col"><span class="el el-{n.lower()}">{n}</span></th>' for n in names)
    rows = []
    for a in names:
        cells = []
        for d in names:
            m = E["table"][a][d]
            cls = "up" if m > 1 else ("down" if m < 1 else "")
            label = "×2" if m > 1 else ("×½" if m < 1 else "")
            cells.append(f'<td class="mx {cls}">{label}</td>')
        rows.append(f'<tr><th scope="row"><span class="el el-{a.lower()}">{a}</span></th>{"".join(cells)}</tr>')
    return (f'<div class="tablewrap"><table class="matrix"><thead><tr><th>Attacker ↓ / Target →</th>{head}</tr></thead>'
            f'<tbody>{"".join(rows)}</tbody></table></div>')


def statuses_table():
    rows = "".join(f'<tr><td><b>{esc(s["name"])}</b></td><td>{"Buff" if s["good"] else "Debuff"}</td><td>{esc(s["what"])}</td></tr>' for s in D["statuses"])
    return f'<div class="tablewrap"><table class="ref"><thead><tr><th>Status</th><th>Kind</th><th>Effect</th></tr></thead><tbody>{rows}</tbody></table></div>'


def items_table():
    rows = []
    groups = {}
    for iid, name in D["items"].items():
        grp, what = ITEM_INFO.get(iid, ("Other", ""))
        groups.setdefault(grp, []).append((iid, name, what))
    blocks = [(bid, name, BLOCK_INFO.get(bid, "")) for bid, name in D["blocks"].items()]
    order = ["Blocks", "Food & treats", "Care", "Tools", "Toys", "Accessories", "Style", "Breeding", "Production tiers", "Jobs & production", "Other"]
    groups["Blocks"] = blocks
    for grp in order:
        if grp not in groups:
            continue
        rows.append(f'<tr class="group"><th colspan="3">{esc(grp)}</th></tr>')
        for iid, name, what in groups[grp]:
            search = f"{name} {what} {grp}".lower()
            rows.append(f'<tr data-search="{esc(search)}"><td><b>{esc(name)}</b></td><td>{esc(what)}</td><td class="recipe">{recipe_text(iid)}</td></tr>')
    return f'<div class="tablewrap"><table class="ref items"><thead><tr><th>Item</th><th>Use</th><th>How to get it</th></tr></thead><tbody>{"".join(rows)}</tbody></table></div>'


def simple_rows(rows, head):
    th = "".join(f"<th>{h}</th>" for h in head)
    body = "".join("<tr>" + "".join(f"<td>{c}</td>" for c in r) + "</tr>" for r in rows)
    return f'<div class="tablewrap"><table class="ref"><thead><tr>{th}</tr></thead><tbody>{body}</tbody></table></div>'


# ------------------------------------------------------------------------------------------------ page
SECTIONS = [
    ("start", "Getting started"), ("screen", "The cat screen"), ("care", "Care"), ("living", "Living cats"),
    ("progress", "Levels and talents"), ("talents", "Talent list"), ("modes", "Behaviour modes"), ("combat", "Combat"),
    ("abilities", "Abilities"), ("twine", "Twine"), ("whistle", "Whistle and tools"), ("jobs", "Jobs and production"),
    ("breeding", "Breeding"), ("gear", "Gear and style"), ("together", "Playing together"), ("items", "Items and blocks"),
    ("servers", "Servers and modpacks"), ("faq", "Questions"),
]

nav = "".join(f'<a href="#{sid}">{esc(t)}</a>' for sid, t in SECTIONS)
keys = {name: k for name, k in D["keys"]}
growth = " → ".join(f"<b>{esc(s['name'])}</b> ({round(s['scale'] * 100)}%)" for s in D["growth"])
traits_rows = [[f"<b>{esc(t['name'])}</b>", esc(t["bonus"]), esc(t["quirk"])] for t in D["traits"]]
bond_rows = [[str(b["level"]), f"<b>{esc(b['name'])}</b>", esc(b["what"])] for b in D["bond"]]
tier_rows = [[f"<b>{esc(t['name'])}</b>", esc(t["output"])] for t in D["tiers"]]
promo_rows = [[f"<b>{esc(r['name'])}</b>", f"<b>{esc(g.lang('promotion.kittytalents.' + r['name'].lower(), ''))}</b>",
               esc(g.lang('promotion.kittytalents.' + r['name'].lower() + '.desc', ''))]
              for r in D["roles"] if g.lang('promotion.kittytalents.' + r['name'].lower())]
whistle_src = (g.SRC / "entity/WhistleCommand.java").read_text(encoding="utf-8")
whistle = re.findall(r'^\s+[A-Z_]+\("([^"]+)", "([^"]+)"\)', whistle_src, re.M)
whistle_list = "".join(f"<li><b>{esc(n)}</b> {esc(d)}</li>" for n, d in whistle)
counts = [("51", "talents"), ("22", "abilities"), ("12", "modes"), ("9", "jobs"), ("34", "variants"), ("10", "production tiers")]
count_html = "".join(f'<div><b>{n}</b><span>{l}</span></div>' for n, l in counts)

CSS = """
:root{
  /* Layout: sticky contents rail on the left, one reading column; reference tables run the full column width. */
  --bg:#f3f5f1; --surface:#ffffff; --sunk:#e8ece6; --ink:#1d2722; --muted:#5a6b61; --line:#d3dbd4;
  --catnip:#2d8a57; --catnip-ink:#ffffff; --xp:#b97c0c; --dire:#8b2fc9; --warn:#b4471a;
  --up:#d8f0e0; --up-ink:#14532d; --down:#f8dfd6; --down-ink:#7c2d12;
  --display:"Pixelify Sans", "Trebuchet MS", system-ui, sans-serif;
  --body:"Nunito", "Segoe UI", system-ui, sans-serif;
  --mono:"JetBrains Mono", ui-monospace, "Cascadia Mono", Consolas, monospace;
}
@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){
  --bg:#111713; --surface:#18201b; --sunk:#0d120f; --ink:#e3ebe5; --muted:#98aa9f; --line:#2a3530;
  --catnip:#5ccb8b; --catnip-ink:#0b1a11; --xp:#f0b444; --dire:#c38bf0; --warn:#f08a5d;
  --up:#173826; --up-ink:#a7f3c4; --down:#3d1f16; --down-ink:#fdba9c; color-scheme:dark}}
:root[data-theme="dark"]{
  --bg:#111713; --surface:#18201b; --sunk:#0d120f; --ink:#e3ebe5; --muted:#98aa9f; --line:#2a3530;
  --catnip:#5ccb8b; --catnip-ink:#0b1a11; --xp:#f0b444; --dire:#c38bf0; --warn:#f08a5d;
  --up:#173826; --up-ink:#a7f3c4; --down:#3d1f16; --down-ink:#fdba9c; color-scheme:dark}
*{box-sizing:border-box}
body{background:var(--bg);color:var(--ink);font-family:var(--body);font-size:16px;line-height:1.6;margin:0}
a{color:var(--catnip)}
a:focus-visible,button:focus-visible,input:focus-visible{outline:3px solid var(--xp);outline-offset:2px;border-radius:4px}
.top{background:var(--surface);border-bottom:1px solid var(--line);padding-block:28px 22px;
  padding-left:max(16px,env(safe-area-inset-left,0px));padding-right:max(16px,env(safe-area-inset-right,0px))}
.top-in{max-width:1180px;margin:0 auto;display:grid;gap:18px}
.eyebrow{font-family:var(--mono);font-size:12px;letter-spacing:.08em;text-transform:uppercase;color:var(--muted)}
h1{font-family:var(--display);font-weight:600;font-size:clamp(34px,6vw,56px);line-height:1.02;margin:0;letter-spacing:.01em;text-wrap:balance}
h1 em{font-style:normal;color:var(--catnip)}
.lede{max-width:62ch;margin:0;color:var(--muted);font-size:17px}
.counts{display:flex;flex-wrap:wrap;gap:10px 28px}
.counts div{display:flex;align-items:baseline;gap:6px}
.counts b{font-family:var(--mono);font-weight:600;font-size:24px;color:var(--xp);font-variant-numeric:tabular-nums}
.counts span{color:var(--muted);font-size:14px}
.search{display:flex;gap:10px;align-items:center;flex-wrap:wrap}
.search input{flex:1 1 260px;min-width:0;max-width:460px;font:inherit;padding:10px 14px;border:1px solid var(--line);border-radius:10px;background:var(--bg);color:var(--ink)}
.search output{font-size:13px;color:var(--muted)}
.wrap{max-width:1180px;margin:0 auto;display:grid;grid-template-columns:210px minmax(0,1fr);gap:40px;padding-block:28px 80px;
  padding-left:max(16px,env(safe-area-inset-left,0px));padding-right:max(16px,env(safe-area-inset-right,0px))}
nav.toc{position:sticky;top:calc(16px + env(safe-area-inset-top,0px));align-self:start;display:flex;flex-direction:column;gap:2px;font-size:14px}
nav.toc a{color:var(--muted);text-decoration:none;padding:5px 10px;border-radius:8px}
nav.toc a:hover{background:var(--sunk);color:var(--ink)}
nav.toc a.on{color:var(--catnip);background:var(--sunk);font-weight:700}
details.toc-m{display:none}
main{min-width:0}
section{padding-top:8px;margin-bottom:46px;scroll-margin-top:16px}
h2{font-family:var(--display);font-weight:600;font-size:30px;margin:0 0 6px;text-wrap:balance}
h3{font-size:18px;margin:26px 0 8px}
p,li{max-width:70ch}
.muted,.sub{color:var(--muted)}
.sub{font-size:13px}
kbd{font-family:var(--mono);font-size:13px;padding:2px 7px;border:1px solid var(--line);border-bottom-width:3px;border-radius:6px;background:var(--surface);white-space:nowrap}
.steps{counter-reset:s;list-style:none;padding:0;display:grid;gap:12px}
.steps li{counter-increment:s;display:grid;grid-template-columns:34px minmax(0,1fr);gap:12px;align-items:start}
.steps li::before{content:counter(s);font-family:var(--mono);font-weight:600;font-size:16px;width:34px;height:34px;display:grid;place-items:center;border-radius:9px;background:var(--catnip);color:var(--catnip-ink)}
.note{border-left:4px solid var(--xp);background:var(--surface);padding:12px 16px;border-radius:0 10px 10px 0;max-width:70ch}
.tablewrap{overflow-x:auto;border:1px solid var(--line);border-radius:12px;background:var(--surface);margin:12px 0}
table{border-collapse:collapse;width:100%;font-size:14.5px}
th,td{text-align:left;vertical-align:top;padding:9px 12px;border-bottom:1px solid var(--line)}
thead th{font-size:12px;letter-spacing:.06em;text-transform:uppercase;color:var(--muted);background:var(--sunk);white-space:nowrap}
tbody tr:last-child td{border-bottom:0}
td.n,th.n{text-align:right;font-variant-numeric:tabular-nums;font-family:var(--mono);font-size:13.5px;white-space:nowrap}
tr.group th{background:var(--sunk);font-family:var(--display);font-size:16px;font-weight:500;text-transform:none;letter-spacing:0;color:var(--ink)}
.ref td:first-child{min-width:150px}
.items td.recipe{font-size:13.5px;min-width:220px}
.how{font-family:var(--mono);font-size:11.5px;color:var(--muted);text-transform:uppercase;letter-spacing:.04em;display:block}
.mastery{margin-top:6px;font-size:13.5px;color:var(--muted)}
.mastery b{color:var(--xp);font-weight:700;margin-right:4px}
.pips{display:inline-flex;gap:3px;flex-wrap:wrap;max-width:70px}
.pips i{width:7px;height:7px;border-radius:2px;background:var(--catnip)}
.tag{display:inline-block;font-size:12px;padding:1px 8px;border-radius:999px;border:1px solid var(--line);margin:0 4px 4px 0;white-space:nowrap}
.tag.dire{color:var(--dire);border-color:currentColor}
.tag.free{color:var(--catnip);border-color:currentColor}
.tag.mod{color:var(--xp);border-color:currentColor}
.chips{display:flex;flex-wrap:wrap;gap:8px;margin-top:12px}
.chip{font:inherit;font-size:14px;padding:5px 12px;border-radius:999px;border:1px solid var(--line);background:var(--surface);color:var(--ink);cursor:pointer}
.chip[aria-pressed="true"]{background:var(--catnip);color:var(--catnip-ink);border-color:var(--catnip)}
.el{font-size:12.5px;font-weight:700;white-space:nowrap}
.el-fire{color:#d9480f}.el-water{color:#1c7ed6}.el-frost{color:#0b9bb5}.el-poison{color:#5c940d}.el-electric{color:#c99400}
.el-shadow{color:#7048e8}.el-arcane{color:#ae3ec9}.el-nature{color:#2b8a3e}.el-physical{color:var(--muted)}
table.matrix{width:auto;font-size:13px}
table.matrix th,table.matrix td{padding:6px 9px;text-align:center}
table.matrix tbody th{text-align:left;background:var(--sunk)}
td.mx{font-family:var(--mono);min-width:44px}
td.mx.up{background:var(--up);color:var(--up-ink);font-weight:700}
td.mx.down{background:var(--down);color:var(--down-ink)}
.cols{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:14px;margin:12px 0}
.card{background:var(--surface);border:1px solid var(--line);border-radius:12px;padding:14px 16px;min-width:0}
.card h4{margin:0 0 6px;font-size:15px}
.card p{margin:0;font-size:14.5px;color:var(--muted)}
ul.whistle{columns:2 300px;column-gap:28px;padding-left:18px}
ul.whistle li{break-inside:avoid;margin-bottom:6px;font-size:14.5px}
code{font-family:var(--mono);font-size:13.5px;background:var(--sunk);padding:1px 6px;border-radius:5px}
.hidden-row{display:none}
footer{color:var(--muted);font-size:13px;border-top:1px solid var(--line);padding-top:18px}
@media (max-width:960px){
  .wrap{grid-template-columns:minmax(0,1fr);gap:0}
  nav.toc{display:none}
  details.toc-m{display:block;margin-bottom:22px;background:var(--surface);border:1px solid var(--line);border-radius:12px;padding:10px 14px}
  details.toc-m summary{cursor:pointer;font-weight:700}
  details.toc-m div{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:4px 12px;margin-top:10px}
  details.toc-m a{font-size:14px;text-decoration:none}
}
@media (prefers-reduced-motion:no-preference){html{scroll-behavior:smooth}}
"""

JS = """
(function(){
  var input=document.getElementById('q'), out=document.getElementById('q-count');
  var chips=[].slice.call(document.querySelectorAll('.chip')), cat='';
  function apply(){
    var q=(input.value||'').trim().toLowerCase(), shown=0, total=0;
    document.querySelectorAll('tr[data-search]').forEach(function(tr){
      total++;
      var okq=!q||tr.getAttribute('data-search').indexOf(q)>=0;
      var okc=!cat||!tr.hasAttribute('data-cat')||tr.getAttribute('data-cat')===cat;
      var show=okq&&okc; tr.classList.toggle('hidden-row',!show); if(show&&okq)shown++;
    });
    out.textContent=q?(shown+' matching rows in the talent, ability and item tables'):'';
  }
  input.addEventListener('input',apply);
  chips.forEach(function(c){c.addEventListener('click',function(){
    cat=c.getAttribute('data-filter'); chips.forEach(function(x){x.setAttribute('aria-pressed',String(x===c))}); apply();
  })});
  var links=[].slice.call(document.querySelectorAll('nav.toc a'));
  if('IntersectionObserver' in window){
    var io=new IntersectionObserver(function(es){es.forEach(function(e){if(e.isIntersecting){
      links.forEach(function(a){a.classList.toggle('on',a.getAttribute('href')==='#'+e.target.id)})}})},{rootMargin:'-20% 0px -70% 0px'});
    document.querySelectorAll('main section').forEach(function(s){io.observe(s)});
  }
})();
"""


def body():
    k = lambda name: kbd(keys.get(name, "?"))  # noqa: E731
    return f"""
<header class="top"><div class="top-in">
  <div class="eyebrow">Version 4.0 · Minecraft 1.20.1 · Forge 47</div>
  <h1>Kitty Talents <em>Field Guide</em></h1>
  <p class="lede">Everything about raising, training and fighting beside a Talented Cat: care, talents, combat, Twine, jobs, breeding and every item, taken straight from the mod's own data.</p>
  <div class="counts">{count_html}</div>
  <div class="search"><label for="q" class="eyebrow">Search the tables</label><input id="q" type="search" placeholder="Try “pounce”, “frost” or “collar”" autocomplete="off"><output id="q-count" for="q"></output></div>
</div></header>

<div class="wrap">
<nav class="toc" aria-label="Contents">{nav}</nav>
<main>
<details class="toc-m"><summary>Contents</summary><div>{nav}</div></details>

<section id="start"><h2>Getting started</h2>
<ol class="steps">
<li><div><b>Find a Talented Cat.</b> They spawn in jungles and villages. Break grass, tall grass or ferns for <b>Catnip Leaves</b> (15% per block).</div></li>
<li><div><b>Tame it.</b> A <b>Catnip Treat</b> always works. Catnip Leaf works half the time, raw fish one time in three.
A new cat starts with enough skill points to max out any one regular talent.</div></li>
<li><div><b>Open its screen.</b> Right-click your cat with a <b>Stick</b> or the <b>Laser Pointer</b>.</div></li>
<li><div><b>Pick a mode and a talent.</b> Follow is a good start. Spend your first points on the Talents tab.</div></li>
<li><div><b>Feed it and give it a bed.</b> Cats get hungry. A Food Bowl and a Cat Bed near home look after that.</div></li>
</ol>
<div class="cols">
  <div class="card"><h4>Empty hand</h4><p>Right-click: sit or stand. Sneak + right-click: pet (builds bond).</p></div>
  <div class="card"><h4>Stick or Laser Pointer</h4><p>Opens the cat screen. It never makes you ride the cat.</p></div>
  <div class="card"><h4>Food in hand</h4><p>Feeds the cat; heals it and fills hunger. Kittens grow faster.</p></div>
</div>
</section>

<section id="screen"><h2>The cat screen</h2>
<p>Seven tabs. Switch with the mouse, {kbd("Ctrl")} + {kbd("Tab")}, or the number keys {kbd("1")} to {kbd("7")}.</p>
{simple_rows([
    ["<b>Home</b>", "Portrait with level, health, XP, Energy and the bond bar. Personality, mode and job, Sit, Kitty Pack, Whistle, TWINE. Rename and co-owners."],
    ["<b>Talents</b>", "Every talent with rank pips, its description and mastery perk, and the TRAIN button. Respec."],
    ["<b>Gear</b>", "Weapon, four armor pieces, accessory, tool and three cosmetic slots, with your inventory below. Drag items in and out, or shift-click."],
    ["<b>Style</b>", "Collar colour, skin, and the breeding codex."],
    ["<b>Stats</b>", "Lifetime records: kills, damage, distance, food, blocks mined and more."],
    ["<b>Combat</b>", "Role, element, the four Twine ability slots and saved loadouts."],
    ["<b>Twine</b>", "Start Twining, the controls, and what the cat's body can do."],
], ["Tab", "What's on it"])}
<p class="muted">Co-owners see the same screen without the owner-only controls.</p>
</section>

<section id="care"><h2>Care</h2>
<div class="cols">
  <div class="card"><h4>Hunger</h4><p>Cats get hungry over time, slower while resting. Feed fish, cat food or treats by hand, or fill a <b>Food Bowl</b> (5 stacks) and they eat on their own.</p></div>
  <div class="card"><h4>Sleep and play</h4><p>Cats sleep on a <b>Cat Bed</b> at night, scratch the <b>Scratching Post</b> and perch on a <b>Cat Tree</b>.</p></div>
  <div class="card"><h4>Downed, not dead</h4><p>At 0 HP a cat is <b>downed</b>. Revive it with a Golden Apple, Milk or <b>Warm Milk</b>. <b>Nine Lives</b> can bring it back on its own, with a recharge. For real stakes, <code>/gamerule kittyTalentsCatDeath true</code> makes a downed cat die if it isn't revived within 60 seconds (change the time with <code>/gamerule kittyTalentsBleedOutSeconds</code>).</p></div>
  <div class="card"><h4>Healing</h4><p><b>Healing Herbal Treat</b> heals 14 HP. <b>Feline Recovery Wrap</b> heals 10 HP and cures Poison and Wither.</p></div>
</div>
</section>

<section id="living"><h2>Living cats</h2>
<h3>Growing up</h3>
<p>Kittens pass through four stages, growing in size: {growth}. Young cats can't be ridden. Feeding a kitten makes it grow faster, and treats more so.</p>
<h3>Personality</h3>
<p>Every cat has one trait with a bonus and a quirk you can see in how it behaves. Kittens usually inherit a parent's trait. The trait shows on the Home tab.</p>
{simple_rows(traits_rows, ["Trait", "Bonus", "Quirk"])}
<h3>Bond</h3>
<p>Bond grows from <b>time spent near you</b>, <b>petting</b>, <b>feeding</b>, <b>playing</b> and <b>kills made while you're close</b>. It has 10 levels, shown as the bar on the Home tab. Perks unlock along the way:</p>
{simple_rows(bond_rows, ["Level", "Perk", "Effect"])}
<h3>Captain</h3>
<p>At bond level 10 a cat can become your <b>Captain</b> (the ★ on its Home tab; one per owner, and it wears a crown). In Guard or Wander mode it makes monsters within 32 blocks glow. Perched on a Cat Tree, or leading a patrol, it <b>rallies</b>: your cats within 16 blocks deal +10% damage and working Habitats there run 5% faster.</p>
</section>

<section id="progress"><h2>Levels and talents</h2>
<div class="cols">
  <div class="card"><h4>Experience</h4><p>Cats earn XP from kills (theirs, including ability kills). Level <i>n</i> needs <code>20 + 10 × n</code> XP. Each level gives <b>1 skill point</b>. The cap is level 100 (server config).</p></div>
  <div class="card"><h4>Treats train levels</h4><p><b>Catnip Treat</b> 1–20 · <b>Super</b> 21–40 · <b>Master</b> 41–60 · <b>Dire</b> 61–100.</p></div>
  <div class="card"><h4>Rank cost</h4><p>5-rank talents: ranks 1–2 cost 1 point, 3–5 cost 2 (8 total). 8-rank talents: 1, 1, 2, 2, 2, 3, 3, 3 (17 total). Dire ranks cost 3, 6, 9, 12, 15.</p></div>
  <div class="card"><h4>Mastery and respec</h4><p>A talent at max rank unlocks its <b>mastery perk</b>. The <b>Catnip Respec Elixir</b> refunds every rank you paid for.</p></div>
</div>
<div class="note"><b>Dire talents</b> (Sky Feline, Mole Cat, Frenzy Focus, Alchemy Kitty, Catnip Catastrophe) unlock after feeding the cat one <b>Dire Treat</b>.</div>
</section>

<section id="talents"><h2>Talent list</h2>
<p>All 51 talents. Filter by category, or use the search box at the top.</p>
{talents_table()}
</section>

<section id="modes"><h2>Behaviour modes</h2>
<p>Set the mode on the Home tab or with the whistle. Every mode decides where the cat stays and what it may attack.</p>
{modes_table()}
</section>

<section id="combat"><h2>Combat</h2>
<div class="cols">
  <div class="card"><h4>Stalk and pounce</h4><p>A cat creeps up crouched on an enemy that hasn't noticed it, then pounces as its opening move. Pounce Training adds reach and power.</p></div>
  <div class="card"><h4>Dodge and retreat</h4><p>Cats dodge arrows and creeper blasts, and fall back to you when badly hurt (not in Aggressive mode).</p></div>
  <div class="card"><h4>Claw combo</h4><p>Hits chain into a 4-step combo: normal, +15%, +30% with Bleed, then a +60% finisher with knockback. Hits 0.4 to 0.9 s apart are <b>perfect</b>: +25% damage and 10 Energy back.</p></div>
  <div class="card"><h4>Energy</h4><p>Abilities, dodges and pounces cost Energy. Max Energy is 100, +2 per level (up to +100), plus talents and role. It refills 2/s in a fight, 4/s out of combat and 6/s while resting.</p></div>
</div>
<h3>Roles</h3>
<p>Pick a role on the Combat tab. It changes the cat's stats and how it fights.</p>
{roles_table()}
<h3>Promotions</h3>
<p>A cat at the maximum level has mastered its role. Click <b>★ Promote</b> on the Combat tab to promote it one tier: it keeps the role's stats and gains a signature perk. The promotion belongs to that role; switch back to it to use it again.</p>
{simple_rows(promo_rows, ["Role", "Promotion", "Perk"])}
<h3>Elements</h3>
<p>A cat's element makes its claws stronger or weaker against some targets, and adds that element's status on a hit.</p>
{element_grid()}
<h3>Status effects</h3>
{statuses_table()}
<div class="note"><b>Neko Night.</b> Some nights the moon glows with feline spirit: monsters hit 30% harder and shrug off 20% of a cat&#39;s claw damage, cats get 10 Energy back per hit, and new Talented Cats can spawn at a higher production tier.</div>
</section>

<section id="abilities"><h2>Abilities</h2>
<p>A cat carries <b>three skills and one ultimate</b>, set on the Combat tab. In a fight the cat casts them on its own when the moment is right: heals when hurt, area attacks against a crowd, ultimates against several enemies or a big one. While Twined you cast them yourself.</p>
{abilities_table()}
<p class="muted">Energy and cooldowns are the defaults; a server can change them with a datapack.</p>
</section>

<section id="twine"><h2>Twine</h2>
<p><b>Twine</b> puts you inside your cat. Your body sleeps safely where you left it while you move, fight and cast as the cat. Press TWINE on the Home tab, or look at the cat and press {k("Twine with Cat")}.</p>
{simple_rows([
    [kbd("W A S D"), "Move", kbd("Space"), "Jump; with Feline Agility 3, leap off walls"],
    [kbd("Left click"), "Claw (twice a second)", kbd("Right click"), "Pounce leap; with a gun, aim"],
    [k("Ability Slot 1") + " " + k("Ability Slot 2") + " " + k("Ability Slot 3"), "Cast skill 1, 2, 3", k("Ultimate Ability"), "Ultimate; with a gun, reload (Shift + R casts)"],
    [k("Dodge (with movement key for direction)"), "Dodge the way you're moving (back if standing still)", k("Team Synergy Attack"), "Combo attack with your nearest other cat"],
    [k("Untwine from Cat"), "Return to your body", "", ""],
], ["Key", "Action", "Key", "Action"])}
<div class="cols">
  <div class="card"><h4>Perfect dodge</h4><p>Dodging with an enemy close by refunds the Energy and slows and weakens the enemy.</p></div>
  <div class="card"><h4>Team Synergy</h4><p>Your talents pick the combo: <b>Inferno Hunt</b> (a fire cat), <b>Frozen Tide</b> (water and snow cats), <b>Twin Soul Assault</b> (shadow or Nekomata), otherwise <b>Pack Pounce Surge</b>. 25 Energy, 5 s recharge. It never hits your friends.</p></div>
  <div class="card"><h4>Flight</h4><p>With <b>Sky Feline</b>, hold jump to climb and glide.</p></div>
  <div class="card"><h4>Always safe</h4><p>Your body comes back if the cat dies, you take heavy damage (below 25% health), log out, change dimension, or the game crashes.</p></div>
</div>
</section>

<section id="whistle"><h2>Whistle and tools</h2>
<p>The <b>Kitty Whistle</b> commands every cat of yours nearby at once. Right-click blows it; sneak + right-click picks the command; or open the whistle wheel from the Home tab. The wheel has a <b>Mine only / Mine + co-owned</b> switch, and right-clicking a command pins it to a favourites row.</p>
<ul class="whistle">{whistle_list}</ul>
<h3>Other tools</h3>
<div class="cols">
  <div class="card"><h4>Producer Stick</h4><p>Lists your cats in every dimension and calls them to you, even from unloaded chunks. Sneak + right-click sends them to bed. Fireproof.</p></div>
  <div class="card"><h4>Tracker and Locator Orb</h4><p>The Tracker scans 256 blocks. A Locator Orb bound to one cat shows where it is and how it's doing from anywhere.</p></div>
  <div class="card"><h4>Pet Carrier</h4><p>Pick your cat up and carry it; right-click the ground to let it out.</p></div>
  <div class="card"><h4>Treat Bag</h4><p>Stores 576 treats. Right-click a cat to feed one.</p></div>
</div>
</section>

<section id="jobs"><h2>Jobs and production</h2>
<ol class="steps">
<li><div>Place a <b>Kitty Work Station</b> near the work area. It holds what the cat gathers (the Collector drops its haul there).</div></li>
<li><div>Right-click your cat with the <b>Work Clipboard</b> to select it, then use the clipboard on blocks to set the work area's corners (and link chests for Courier and Sorter).</div></li>
<li><div>Pick the job on the Home tab. Picking a job clocks the cat in and puts it in Work mode. Clock it out to stop.</div></li>
</ol>
{jobs_table()}
<h3>Production tiers and the Habitat</h3>
<p>Every cat has a production tier. A tier treat raises it one step (Coal Cat Treat turns a Standard cat into a Coal cat, and so on). Higher tiers are tougher and hit harder, and two cats of the same tier breed a kitten of the next tier. A cat housed in a <b>Kitty Habitat</b> (use the clipboard on the Habitat) produces its tier's resource over time; <b>Productivity Upgrades</b> (up to 4 per Habitat) speed it up.</p>
{simple_rows(tier_rows, ["Tier", "Habitat output"])}
<h3>Personality and bond in the Habitat</h3>
<p>A housed cat's trait changes how it works, and its neighbours matter, so plan the layout of your colony. The Habitat screen shows the total speed next to the cycle count; hover it for the list.</p>
<ul><li><b>Shy:</b> +20% with no other working Habitat within 4 blocks, -10% with 3 or more.</li><li><b>Brave (foreman):</b> other working Habitats within 6 blocks get +5% (doesn't stack).</li><li><b>Lazy:</b> 10% slower, but never gets hungry while working.</li><li><b>Curious:</b> each cycle, a 10% chance of a bonus item from the tier below.</li><li><b>Playful:</b> +15% with another Playful Habitat within 4 blocks, -10% with no working Habitat near.</li><li><b>Greedy:</b> +25% output, +50% hunger.</li><li><b>Bond:</b> +1% per bond level while you're within 32 blocks.</li></ul>
<p>A housed <b>Nekomata</b> wards its Habitat with ghostly flames: hostile mobs within 6 blocks are pushed away. Out in the world, a Nekomata keeps up to 3 blue wisps circling it and sends them at its target (5 damage each; one regrows every 10 s).</p>
</section>

<section id="breeding"><h2>Breeding</h2>
<div class="cols">
  <div class="card"><h4>Kittens</h4><p>Feed <b>Breeding Catnip</b> to two Talented Cats. The kitten is tamed, inherits a trait and a talent, and grows up over time.</p></div>
  <div class="card"><h4>34 variants</h4><p>The <b>Feline Variant Codex</b> lists every variant, its breeding recipe and its production yield.</p></div>
  <div class="card"><h4>Nekomata</h4><p>Feed <b>Spirit Catnip</b> to a parent before breeding for a guaranteed two-tailed Nekomata. Nekomata strike twice and get extra drops.</p></div>
</div>
</section>

<section id="gear"><h2>Gear and style</h2>
<p>The Gear tab has a <b>weapon</b> slot (held in the mouth; a TacZ gun works too), <b>helmet, chest, legs and boots</b>, an <b>accessory</b>, a <b>tool</b> for work, and <b>three cosmetic</b> slots. Hover an empty slot to see what fits, and a red tooltip explains why an item doesn't (wrong slot, missing talent rank, or not your cat). Any cat can wear leather to iron armor and hold basic swords. Diamond gear needs <b>Feline Armor</b> or <b>Cat Armory</b> rank 1, netherite rank 3, and guns Cat Armory 1; each rank also adds real power.</p>
<p>On the Style tab: collar colour (dye works too), and <b>17 skins</b>: {esc(", ".join(g.title(p.stem) for p in sorted((g.RES / "data/kittytalents/kittytalents/skins").glob("*.json"))))}. Servers can add more with a skin pack.</p>
</section>

<section id="together"><h2>Playing together</h2>
<h3>Bases, guard posts and patrols</h3>
<p>Craft a <b>Base Pin</b> (red dye over two iron nuggets). Right-click two corners to mark a base; right-click the air to rename, resize, merge or delete your bases. Sneak + right-click a cat, then sneak + right-click a block: that block is its <b>guard post</b> (on a Cat Tree it perches there for good). Guard cats in a base without a post <b>patrol</b> its border in teams of up to 3.</p>
<p>Add another online player as a <b>co-owner</b> from the Home tab, then click their name to set a role.</p>
{simple_rows([
    ["<b>Rider</b>", "Ride the cat."],
    ["<b>Handler</b>", "Also command it (modes, whistle, jobs) and Twine with it."],
    ["<b>Full</b>", "Also change its gear and style."],
], ["Role", "Can"])}
<p>Only the owner can train, respec, rename or manage co-owners. A co-owner can leave on their own. Cats that aren't yours show their owner's name on the HUD, and other players see a label over a Twined cat and over the spot where its player's body rests.</p>
<p>Up to <b>4 cats</b> can follow you at once (Follow, Tactical and Support); a fifth sits down instead. Change it with <code>/gamerule kittyTalentsFollowerLimit</code> (0 = no limit). The <b>HUD</b> shows small cards for your nearby cats that are following you, hurt or downed. Change it in <code>config/kittytalents-client.toml</code> (<code>showFriendCats</code>, <code>showTwineMarkers</code>, range).</p>
</section>

<section id="items"><h2>Items and blocks</h2>
<p>Every item and block with its recipe. Use the search box at the top to find one.</p>
{items_table()}
</section>

<section id="servers"><h2>Servers and modpacks</h2>
{simple_rows([
    ["<code>/kittytalents tuning export</code>", "Writes every tunable number (talents, abilities, traits, growth, bond) with its default."],
    ["<code>/kittytalents tuning changed</code>", "Lists the numbers a datapack changed."],
    ["<code>/kittyrpg setrole | setelement | setability</code>", "Set a cat's role, element or ability slot."],
    ["<code>/gamerule kittyTalentsCatDeath | kittyTalentsBleedOutSeconds | kittyTalentsFollowerLimit</code>", "Real stakes (off by default), bleed-out time (60 s), followers per player (4)."],
    ["<code>/kittytalents audit</code>", "Runs the mod's self-check."],
    ["<code>/kittytalents uninstall</code>", "Before removing the mod: turns every Talented Cat into a vanilla tamed cat (same owner, breed, collar and name) and drops its gear. Its Kitty Talents data stays hidden on the cat."],
    ["<code>/kittytalents reinstall</code>", "Undoes it: those cats turn back into the same Talented Cats, with their level, talents and bond."],
], ["Command (operators)", "What it does"])}
<div class="cols">
  <div class="card"><h4>Balance by datapack</h4><p>Put JSON in <code>data/&lt;pack&gt;/kittytalents/tuning/</code>. Changes apply on <code>/reload</code>, and tooltips show the new numbers.</p></div>
  <div class="card"><h4>Skin packs</h4><p>A skin is a JSON file in <code>data/&lt;pack&gt;/kittytalents/skins/</code> plus a texture in a resource pack.</p></div>
  <div class="card"><h4>Offline owners</h4><p>Cats don't get hungry while their owner is offline, including cats housed in Habitats, which keep producing.</p></div>
  <div class="card"><h4>Server config</h4><p><code>kittytalents-common.toml</code>: max level, friendly fire, immortal cats, worker block breaking, worker chunk loading, Rancher culling, cat guns.</p></div>
  <div class="card"><h4>Compatibility</h4><p>TacZ (guns, Gunsmith job), Apotheosis, Curios, Jade, JEI (Habitat output and breeding pages), Farmer's Delight (rice, slices and meals for cats; a cooking-pot Chicken and Rice). The KittyTalentsCompat addon adds Lootr, PlayerRevive and Sophisticated Backpacks.</p></div>
</div>
</section>

<section id="faq"><h2>Questions</h2>
<h3>My cat won't follow me.</h3><p>It's probably sitting (right-click with an empty hand) or in a mode that holds its spot. Switch it to Follow.</p>
<h3>I can't Twine.</h3><p>You can't Twine a downed cat, a cat someone else is Twined with, or while it's carrying a rider. You need to be close, and a co-owner needs the Handler role.</p>
<h3>My cat isn't using its abilities.</h3><p>It casts only in a fight, only in modes that fight, and only when it has the Energy. It saves each one for the right moment: heals when hurt, area attacks against a crowd. Hiss and Healing Purr from the default loadout often go first.</p>
<h3>Things act strangely in multiplayer (cats untwine, the bond bar is missing).</h3><p>The server and every player must run the same build of the mod.</p>
<h3>I lost a cat.</h3><p>Use the Producer Stick to call it, or a Locator Orb or Tracker to find it.</p>
</section>

<footer>Kitty Talents 4.0 for Minecraft 1.20.1 (Forge). Tables generated from the mod's own data by <code>tools/gen_guide.py</code>.</footer>
</main>
</div>
"""


HEAD = """<title>Kitty Talents Field Guide</title>
<meta name="claude:viewport-fit-sides" content="cover">
<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Pixelify+Sans:wght@500;600&family=Nunito:ital,wght@0,400;0,700;1,400&family=JetBrains+Mono:wght@400;600&display=swap">
"""


GS_PAGES = [
    ("01-home", "Home", True, ["start", "screen", "care"]),
    ("02-living-cats", "Living Cats", False, ["living"]),
    ("03-talents", "Levels and Talents", False, ["progress", "talents"]),
    ("04-combat", "Combat", False, ["modes", "combat"]),
    ("05-abilities", "Abilities", False, ["abilities"]),
    ("06-twine", "Twine", False, ["twine"]),
    ("07-whistle-and-tools", "Whistle and Tools", False, ["whistle"]),
    ("08-jobs", "Jobs and Production", False, ["jobs"]),
    ("09-breeding-and-gear", "Breeding and Gear", False, ["breeding", "gear"]),
    ("10-multiplayer", "Playing Together", False, ["together"]),
    ("11-items", "Items and Blocks", False, ["items"]),
    ("12-servers-and-faq", "Servers and FAQ", False, ["servers", "faq"]),
]

HOME_SEARCH = ('<div class="search"><label for="q" class="eyebrow">Search the tables</label><input id="q" type="search" '
               'placeholder="Try “pounce”, “frost” or “collar”" autocomplete="off"><output id="q-count" for="q"></output></div>')


def google_sites(out_dir):
    """Writes one embed-code snippet per Google Sites page plus SETUP.txt."""
    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    full = body()
    header = re.search(r'<header class="top">.*?</header>', full, re.S).group(0).replace(HOME_SEARCH, "")
    secs = {m.group(1): m.group(0) for m in re.finditer(r'<section id="([a-z]+)">.*?</section>', full, re.S)}
    # Light only: a Google Site has one look, and a dark embed on a light page looks broken.
    css = re.sub(r'@media \(prefers-color-scheme: dark\)\{.*?\}\}\n:root\[data-theme="dark"\]\{.*?\}\n', "", CSS, flags=re.S)
    assert "prefers-color-scheme: dark" not in css
    css += ("body{padding-block:8px 24px;padding-left:max(16px,env(safe-area-inset-left,0px));padding-right:max(16px,env(safe-area-inset-right,0px))}"
            ".top{border:1px solid var(--line);border-radius:14px;margin-bottom:24px}\n")
    search = ('<div class="search" style="margin-bottom:12px"><label for="q" class="eyebrow">Search</label>'
              '<input id="q" type="search" placeholder="Search this page" autocomplete="off"><output id="q-count" for="q"></output></div>')
    js = JS.replace("var input=document.getElementById('q'), out=document.getElementById('q-count');",
                    "var input=document.getElementById('q'), out=document.getElementById('q-count'); if(!input) return;")
    head = HEAD.replace('<meta name="claude:viewport-fit-sides" content="cover">\n', "")
    steps = []
    for slug, name, with_header, ids in GS_PAGES:
        parts = [secs[i] for i in ids]
        has_rows = any("data-search=" in p for p in parts)
        page = (head.replace("<title>Kitty Talents Field Guide</title>", f"<title>{name}</title>")
                + f"<style>{css}</style>\n" + (header if with_header else "") + (search if has_rows else "")
                + "<main>" + "".join(parts) + "</main>" + (f"<script>{js}</script>" if has_rows else ""))
        (out / f"{slug}.html").write_text(page, encoding="utf-8")
        steps.append(f'  {slug}.html  ->  page "{name}"  ({len(page) // 1024} KB)')
    (out / "SETUP.txt").write_text(SETUP.replace("{PAGES}", "\n".join(steps)), encoding="utf-8")
    print(f"wrote {len(GS_PAGES)} Google Sites pages to {out}")


SETUP = """KITTY TALENTS GUIDE ON GOOGLE SITES
====================================

Each .html file in this folder is the content of one page. Google Sites shows it through an "Embed code" block.

1. Go to https://sites.google.com and start a Blank site. Name it "Kitty Talents".
2. Make the pages (right panel > Pages > +), in this order, with these names:

{PAGES}

   The first file goes on the Home page that Google Sites already created.

3. On each page:
   a. Delete the default text box (you can keep the page title in the header).
   b. Insert > Embed > "Embed code".
   c. Open the matching .html file in Notepad, select all (Ctrl+A), copy, and paste it into the box. Click Next, then Insert.
   d. Drag the embed's right edge to the full page width, and drag its bottom edge down until the inner scrollbar disappears.
      Long pages (Talents, Items) may hit Google's height limit; they then scroll inside the box, which works fine.

4. Theme: pick a light theme (the guide is designed for a light page). "Simple" matches well.
5. Click Publish, choose a web address (for example sites.google.com/view/kitty-talents), and share it.

To update later: run `python tools/gen_guide.py --google-sites docs/guide/google-sites`, then on each changed page click
the embed, the pencil (Edit), select all, and paste the new file over the old code.
"""


def main():
    if len(sys.argv) > 2 and sys.argv[1] == "--google-sites":
        google_sites(sys.argv[2])
        return
    content = HEAD + f"<style>{CSS}</style>\n" + body() + f"<script>{JS}</script>\n"
    standalone = ('<!doctype html>\n<html lang="en">\n<head>\n<meta charset="utf-8">\n'
                  '<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">\n'
                  + HEAD + f"<style>{CSS}</style>\n</head>\n<body>\n" + body() + f"<script>{JS}</script>\n</body>\n</html>\n")
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(standalone, encoding="utf-8")
    print(f"wrote {OUT} ({len(standalone) // 1024} KB)")
    if len(sys.argv) > 1:
        Path(sys.argv[1]).write_text(content, encoding="utf-8")
        print(f"wrote {sys.argv[1]}")


if __name__ == "__main__":
    main()
