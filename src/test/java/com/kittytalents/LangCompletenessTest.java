package com.kittytalents;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fails the build when a translation key used by the code is missing from en_us.json, so players never see raw keys
 * like "talent.kittytalents.green_paw". Scans source text (no Minecraft bootstrap needed).
 */
class LangCompletenessTest {

    private static final Path SRC = Path.of("src/main/java/com/kittytalents");
    private static final Path LANG = Path.of("src/main/resources/assets/kittytalents/lang/en_us.json");

    private static final Pattern LITERAL_KEY = Pattern.compile(
            "\"((?:mode|message|gui|item|block|entity|talent|ability|key|tooltip|container|combat)\\.kittytalents\\.[a-z0-9_.]+)\"");
    private static final Pattern TALENT_ID = Pattern.compile("new Talent\\(\"([a-z0-9_]+)\"");
    private static final Pattern ITEM_ID = Pattern.compile("ITEMS\\.register\\(\"([a-z0-9_]+)\"");
    private static final Pattern BLOCK_ID = Pattern.compile("BLOCKS\\.register\\(\"([a-z0-9_]+)\"");

    @Test
    void everyUsedKeyHasAnEnglishTranslation() throws IOException {
        JsonObject lang = JsonParser.parseString(Files.readString(LANG, StandardCharsets.UTF_8)).getAsJsonObject();
        Set<String> required = new TreeSet<>();

        try (Stream<Path> files = Files.walk(SRC)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                collect(LITERAL_KEY, Files.readString(f, StandardCharsets.UTF_8), "", "", required);
            }
        }
        String talents = Files.readString(SRC.resolve("talent/Talents.java"), StandardCharsets.UTF_8);
        collect(TALENT_ID, talents, "talent.kittytalents.", "", required);
        collect(TALENT_ID, talents, "talent.kittytalents.", ".desc", required);
        collect(TALENT_ID, talents, "talent.kittytalents.", ".mastery", required);

        Set<String> blocks = new TreeSet<>();
        collect(BLOCK_ID, Files.readString(SRC.resolve("registry/ModBlocks.java"), StandardCharsets.UTF_8), "", "", blocks);
        blocks.forEach(b -> required.add("block.kittytalents." + b));
        Set<String> items = new TreeSet<>();
        collect(ITEM_ID, Files.readString(SRC.resolve("registry/ModItems.java"), StandardCharsets.UTF_8), "", "", items);
        items.stream().filter(i -> !blocks.contains(i)).forEach(i -> required.add("item.kittytalents." + i));

        Set<String> missing = new TreeSet<>();
        for (String key : required) if (!lang.has(key)) missing.add(key);
        assertTrue(missing.isEmpty(), "Missing en_us translations (" + missing.size() + "): " + missing);
    }

    private static void collect(Pattern p, String text, String prefix, String suffix, Set<String> out) {
        Matcher m = p.matcher(text);
        while (m.find()) out.add(prefix + m.group(1) + suffix);
    }
}
