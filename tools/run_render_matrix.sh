#!/usr/bin/env bash
# Renders the cat "render matrix" (skins x armor x held items x cosmetics x poses) to run-render/matrix/*.png.
# Opens a Minecraft window for about a minute, then the game quits by itself.
set -u
cd "$(dirname "$0")/.."
: "${JAVA_HOME:=$HOME/.gradle/jdks/eclipse_adoptium-17-amd64-windows/jdk-17.0.20.1+1}"; export JAVA_HOME
mkdir -p run-render/saves
if [ ! -d run-render/saves/render_world ]; then
  # Any world works (the cats are rendered offscreen); reuse the dedicated-server test world.
  cp -r run-server/world run-render/saves/render_world && rm -f run-render/saves/render_world/session.lock
fi
if [ ! -f run-render/options.txt ]; then
  printf 'pauseOnLostFocus:false\nonboardAccessibility:false\nrenderDistance:2\nsimulationDistance:5\njoinedFirstServer:true\ntutorialStep:none\n' > run-render/options.txt
fi
rm -rf run-render/matrix
LOG=run-render/render-output.log
./gradlew runRenderMatrix --console=plain > "$LOG" 2>&1
grep -E "\[RenderMatrix\]" "$LOG" | grep -v DEBUG
MISSING=$(grep -ciE "missing texture|Unable to load texture|FileNotFoundException.*png|Failed to load texture" "$LOG")
echo "texture warnings: $MISSING"
grep -iE "missing texture|Unable to load texture|FileNotFoundException.*png|Failed to load texture" "$LOG" | grep -v DEBUG | sort -u | head -20
N=$(ls run-render/matrix/*.png 2>/dev/null | wc -l)
echo "images: $N"
[ "$N" -gt 0 ] && grep -q "\[RenderMatrix\] wrote" "$LOG"
