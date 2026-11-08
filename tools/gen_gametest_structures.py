"""Generate GameTest structure templates into src/main/resources/data/kittytalents/structures/.

Usage (from repo root):  python tools/gen_gametest_structures.py

Writes gzip-compressed binary NBT structure files (Minecraft 1.20.1, DataVersion 3465).
"""
import gzip
import os
import struct

DATA_VERSION = 3465  # Minecraft 1.20.1
ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
OUT_DIR = os.path.join(ROOT, "src", "main", "resources", "data", "kittytalents", "structures")

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10


def _name(n):
    b = n.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def _payload(tag_type, value):
    if tag_type == TAG_INT:
        return struct.pack(">i", value)
    if tag_type == TAG_STRING:
        return _name(value)
    if tag_type == TAG_LIST:
        elem_type, items = value
        out = struct.pack(">bi", elem_type if items else TAG_END, len(items))
        return out + b"".join(_payload(elem_type, i) for i in items)
    if tag_type == TAG_COMPOUND:
        out = b""
        for key, (t, v) in value.items():
            out += struct.pack(">b", t) + _name(key) + _payload(t, v)
        return out + struct.pack(">b", TAG_END)
    raise ValueError(tag_type)


def write_structure(name, size, blocks, palette):
    """blocks: list of ((x, y, z), palette_index)."""
    root = {
        "DataVersion": (TAG_INT, DATA_VERSION),
        "size": (TAG_LIST, (TAG_INT, list(size))),
        "palette": (TAG_LIST, (TAG_COMPOUND, [{"Name": (TAG_STRING, p)} for p in palette])),
        "blocks": (TAG_LIST, (TAG_COMPOUND, [
            {"pos": (TAG_LIST, (TAG_INT, list(pos))), "state": (TAG_INT, idx)} for pos, idx in blocks
        ])),
        "entities": (TAG_LIST, (TAG_COMPOUND, [])),
    }
    data = struct.pack(">b", TAG_COMPOUND) + _name("") + _payload(TAG_COMPOUND, root)
    os.makedirs(OUT_DIR, exist_ok=True)
    path = os.path.join(OUT_DIR, name + ".nbt")
    with gzip.open(path, "wb") as f:
        f.write(data)
    print("wrote", path)


def main():
    # "empty": 8x8x8 arena with a stone floor at y=0 and air above (air is implicit/omitted).
    size = (8, 8, 8)
    floor = [((x, 0, z), 0) for x in range(size[0]) for z in range(size[2])]
    write_structure("empty", size, floor, ["minecraft:stone"])


if __name__ == "__main__":
    main()
