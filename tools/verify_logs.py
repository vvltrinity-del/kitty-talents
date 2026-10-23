#!/usr/bin/env python3
"""
tools/verify_logs.py - Automated log verification for Kitty Talents 3.0.

Scans run/logs/latest.log (or custom path) for missing textures, missing sounds,
model loading errors, or classloading crashes related to kittytalents.

Exits with:
  0 - Zero matching warnings/errors detected in the log file
  1 - File missing OR one or more warnings/errors detected (prints line numbers and messages)
"""

import sys
import os
import re

PATTERNS = [
    re.compile(r"(?i)(missing\s+texture|unable\s+to\s+resolve\s+texture|textureatlas.*unable).*kittytalents"),
    re.compile(r"(?i)kittytalents.*(missing\s+texture|missing\s+sound|filenotfoundexception)"),
    re.compile(r"(?i)missing\s+sound\s+for\s+event:\s+kittytalents:"),
    re.compile(r"(?i)unable\s+to\s+load\s+model:\s+'kittytalents:"),
    re.compile(r"(?i)kittytalents:textures/.*filenotfound"),
    re.compile(r"java\.lang\.NoClassDefFoundError(?::\s+.*kittytalents.*)?"),
    re.compile(r"java\.lang\.ClassNotFoundException(?::\s+.*kittytalents.*)?"),
]

def scan_log(log_path):
    print(f"Scanning log file: {log_path}")
    if not os.path.isfile(log_path):
        print(f"ERROR: Target log file '{log_path}' does not exist or is not a file.")
        return None

    matches = []
    with open(log_path, "r", encoding="utf-8", errors="ignore") as f:
        for lineno, line in enumerate(f, start=1):
            line_str = line.strip()
            for pattern in PATTERNS:
                if pattern.search(line_str):
                    matches.append((lineno, line_str))
                    break

    return matches

def main():
    if len(sys.argv) > 1:
        log_path = sys.argv[1]
    else:
        # Default relative to repository root
        script_dir = os.path.dirname(os.path.abspath(__file__))
        repo_root = os.path.dirname(script_dir)
        log_path = os.path.join(repo_root, "run", "logs", "latest.log")

    matches = scan_log(log_path)

    if matches is None:
        sys.exit(1)

    if matches:
        print(f"\nFAILED: Found {len(matches)} asset/classloading issue(s) in {log_path}:")
        for lineno, line in matches:
            print(f"  Line {lineno}: {line}")
        sys.exit(1)
    else:
        print("\nPASSED: Zero Missing Texture, Missing Sound, or Classloading warnings detected.")
        sys.exit(0)

if __name__ == "__main__":
    main()
