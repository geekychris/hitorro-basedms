package com.hitorro.basedms.transformer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The JSON renditions, FDF files and JVS documents basedms produces from JSON are what 3.1.2 produced
 * on Jackson 2. {@code jackson2-format-3.1.2.txt} is {@link BasedmsFormatGolden#compute} run on the
 * published hitorro-basedms 3.1.2 jar.
 */
class Jackson2FormatCompatTest {

    @Test
    void outputMatches312(@TempDir Path dir) throws Exception {
        Map<String, String> golden = new LinkedHashMap<>();
        try (InputStream in = getClass().getResourceAsStream("jackson2-format-3.1.2.txt")) {
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (line.isEmpty()) continue;
                int eq = line.indexOf('=');
                golden.put(line.substring(0, eq), line.substring(eq + 1));
            }
        }
        Map<String, String> got = BasedmsFormatGolden.compute(dir);
        for (Map.Entry<String, String> e : golden.entrySet()) {
            assertEquals(e.getValue(), got.get(e.getKey()), e.getKey());
        }
        assertEquals(golden.keySet(), got.keySet());
    }
}
