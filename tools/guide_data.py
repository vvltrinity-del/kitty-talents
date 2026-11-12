"""Reads the mod's own sources and lang file and returns the facts the player guide shows (tools/gen_guide.py)."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src/main/java/com/kittytalents"
RES = ROOT / "src/main/resources"
LANG = json.loads((RES / "assets/kittytalents/lang/en_us.json").read_text(encoding="utf-8"))


def read(rel):
    return (SRC / rel).read_text(encoding="utf-8")


def lang(key, default=None):
    return LANG.get(key, default)


def title(s):
    return " ".join(w.capitalize() for w in s.replace("_", " ").split())


def talents():
    out = []
    pat = re.compile(r'register\(new Talent\("([a-z_]+)", (\d+), \(\) -> new ItemStack\(Items\.([A-Z_]+)\), (true|false), (true|false), TalentCategory\.([A-Z]+)(?:, "([a-z]+)")?\)\)')
    for m in pat.finditer(read("talent/Talents.java")):
        tid, mx, icon, dire, std, cat, mod = m.groups()
        out.append({"id": tid, "name": lang(f"talent.kittytalents.{tid}", title(tid)), "max": int(mx),
                    "desc": lang(f"talent.kittytalents.{tid}.desc", ""), "mastery": lang(f"talent.kittytalents.{tid}.mastery", ""),
                    "category": cat.title(), "dire": dire == "true", "standard": std == "true", "needs": mod})
    return out


def abilities():
    out = []
    for f in sorted((SRC / "ability").glob("*.java")):
        s = f.read_text(encoding="utf-8")
        m = re.search(r'super\("([a-z_]+)",\s*"([^"]+)",\s*"([^"]*)",\s*(\d+),\s*(\d+),\s*FelineElement\.([A-Z]+),\s*Category\.([A-Z]+)\)', s, re.S)
        if m:
            aid, name, desc, cost, cd, el, cat = m.groups()
            out.append({"id": aid, "name": name, "desc": desc, "energy": int(cost), "cooldown": int(cd) / 20.0,
                        "element": el.title(), "kind": cat.title()})
    order = [l for l in re.findall(r'register\(new ([A-Za-z]+)\(\)\)', read("ability/CatAbilities.java"))]
    by_class = {}
    for f in (SRC / "ability").glob("*.java"):
        s = f.read_text(encoding="utf-8")
        m = re.search(r'super\("([a-z_]+)"', s)
        if m:
            by_class[f.stem] = m.group(1)
    rank = {by_class.get(c): i for i, c in enumerate(order)}
    out.sort(key=lambda a: rank.get(a["id"], 99))
    return out


def modes():
    s = read("entity/CatMode.java")
    out = []
    for doc, name, key, follows, anchor, policy, retreat in re.findall(
            r'/\*\* (.*?) \*/\s*public static final CatMode ([A-Z_]+) = builtin\("[A-Z_]+", "([a-z_.]+)", (true|false), (-?\d+), TargetPolicy\.([A-Z_]+), (true|false)\);', s):
        out.append({"id": name, "name": lang(key, title(name)), "what": doc.strip(), "follows": follows == "true",
                    "anchor": int(anchor), "targets": policy, "retreats": retreat == "true"})
    return out


def jobs():
    s = read("work/CatJob.java")
    out = []
    for name, jid, disp, desc, talent, rank, level, tacz in re.findall(
            r'public static final CatJob ([A-Z]+) = register\(new CatJob\("[A-Z]+", true, "([a-z]+)", "([^"]+)", "([^"]+)", (Talents\.[A-Z_]+|null), (\d+), (\d+), .*?, (true|false), ', s):
        if jid == "none":
            continue
        tname = None
        if talent != "null":
            tid = talent.split(".")[1].lower()
            tname = lang(f"talent.kittytalents.{tid}", title(tid))
        out.append({"id": jid, "name": disp, "desc": desc, "talent": tname, "rank": int(rank), "level": int(level), "tacz": tacz == "true"})
    return out


def enum_rows(rel, pattern):
    return re.findall(pattern, read(rel))


def traits():
    return [{"name": n, "bonus": b, "quirk": q} for _, n, b, q in
            enum_rows("core/living/CatTrait.java", r'([A-Z]+)\("[a-z]+", "([^"]+)", "([^"]+)", "([^"]+)", 0x[0-9A-F]+\)') if n != "None"]


def growth():
    return [{"name": n, "scale": float(sc)} for _, n, sc, _ in
            enum_rows("core/living/GrowthStage.java", r'([A-Z]+)\("[a-z]+", "([^"]+)", ([\d.]+), ([\d.]+)\)')]


def bond_perks():
    return [{"level": int(l), "name": n, "what": d} for _, l, n, d in
            enum_rows("core/living/CatBond.java", r'([A-Z_]+)\((\d+), "([^"]+)", "([^"]+)"\)')]


def roles():
    return [{"name": n, "what": d} for _, n, d in enum_rows("combat/CatRole.java", r'^\s+([A-Z]+)\("([^"]+)",[^"]*"([^"]+)"', ) ] if False else \
        [{"name": n, "what": d} for n, d in re.findall(r'^\s+[A-Z]+\("([^"]+)",[^"\n]*"([^"]+)"', read("combat/CatRole.java"), re.M)]


def elements():
    """Element names and the damage matchups: {attacker: {defender: multiplier}} (1.0 when not listed)."""
    s = read("combat/FelineElement.java")
    names = re.findall(r"^    ([A-Z]+)[,;]", s, re.M)
    table = {a: {d: 1.0 for d in names} for a in names}
    for att, body in re.findall(r"if \(this == ([A-Z]+)\) \{(.*?)\n        \}", s, re.S):
        for targets, mult in re.findall(r"if \(((?:target == [A-Z]+(?: \|\| )?)+)\) return ([\d.]+)f;", body):
            for t in re.findall(r"target == ([A-Z]+)", targets):
                table[att][t] = float(mult)
    return {"names": [x.title() for x in names], "table": {a.title(): {d.title(): m for d, m in row.items()} for a, row in table.items()}}


def statuses():
    return [{"name": n, "good": g == "true", "what": d} for n, g, d in
            re.findall(r'^\s+[A-Z]+\("([^"]+)", (true|false), \d+, \d+, "([^"]+)"', read("combat/FelineStatusType.java"), re.M)]


def tiers():
    return [{"name": n, "output": o.replace("_", " ").title()} for n, o in
            re.findall(r'^\s+[A-Z]+\("[a-z]+", "([^"]+)", 0x[0-9A-F]+, ChatFormatting\.[A-Z_]+, \(\) -> new ItemStack\(Items\.([A-Z_]+)', read("entity/ProductionTier.java"), re.M)]


def items():
    names = {k.split(".")[2]: v for k, v in LANG.items() if k.startswith("item.kittytalents.") and k.count(".") == 2}
    blocks = {k.split(".")[2]: v for k, v in LANG.items() if k.startswith("block.kittytalents.") and k.count(".") == 2}
    return names, blocks


def name_of(item_id):
    ns, _, path = item_id.partition(":")
    if ns == "kittytalents":
        n, b = items()
        return n.get(path) or b.get(path) or title(path)
    return title(path)


def recipes():
    out = {}
    for f in sorted((RES / "data/kittytalents/recipes").glob("*.json")):
        r = json.loads(f.read_text(encoding="utf-8"))
        res = r.get("result")
        if isinstance(res, dict):
            rid, count = res.get("item"), res.get("count", 1)
        elif isinstance(res, str):
            rid, count = res, 1
        else:
            continue
        if not rid or not rid.startswith("kittytalents:"):
            continue
        ing = {}
        t = r.get("type", "")
        if "shaped" in t and "shapeless" not in t:
            key = r.get("key", {})
            for row in r.get("pattern", []):
                for ch in row:
                    if ch != " " and ch in key:
                        v = key[ch]
                        v = v[0] if isinstance(v, list) else v
                        nm = name_of(v["item"]) if "item" in v else "any " + title(v.get("tag", "?").split(":")[-1].split("/")[-1])
                        ing[nm] = ing.get(nm, 0) + 1
            how = "Crafting table"
        elif "shapeless" in t:
            for v in r.get("ingredients", []):
                v = v[0] if isinstance(v, list) else v
                nm = name_of(v["item"]) if "item" in v else "any " + title(v.get("tag", "?").split(":")[-1].split("/")[-1])
                ing[nm] = ing.get(nm, 0) + 1
            how = "Crafting table (any shape)"
        elif t in ("minecraft:smelting", "minecraft:smoking", "minecraft:campfire_cooking"):
            v = r["ingredient"]
            ing[name_of(v["item"]) if "item" in v else title(v.get("tag", "?"))] = 1
            how = {"minecraft:smelting": "Furnace", "minecraft:smoking": "Smoker", "minecraft:campfire_cooking": "Campfire"}[t]
        else:
            how = title(t.split(":")[-1])
            for v in (r.get("ingredients") or []):
                v = v[0] if isinstance(v, list) else v
                if isinstance(v, dict) and "item" in v:
                    ing[name_of(v["item"])] = ing.get(name_of(v["item"]), 0) + 1
        path = rid.split(":")[1]
        out.setdefault(path, []).append({"how": how, "count": count, "ingredients": ing})
    return out


def keys():
    s = read("client/twine/TwineInput.java")
    return [(lang(f"key.kittytalents.{n}", title(n)), k.replace("LEFT_", "Left ").title()) for n, k in re.findall(r'key\("([a-z_0-9]+)", GLFW\.GLFW_KEY_([A-Z_0-9]+)\)', s)]


def all_data():
    n, b = items()
    return {"talents": talents(), "abilities": abilities(), "modes": modes(), "jobs": jobs(), "traits": traits(),
            "growth": growth(), "bond": bond_perks(), "roles": roles(), "elements": elements(), "statuses": statuses(),
            "tiers": tiers(), "items": n, "blocks": b, "recipes": recipes(), "keys": keys()}


if __name__ == "__main__":
    d = all_data()
    for k, v in d.items():
        print(k, len(v), (v[:2] if isinstance(v, list) else list(v.items())[:2]))
