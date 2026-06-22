package dev.cbkii.ts18intentbridge;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BridgeConfigTest {
    @Test public void normalizesValidPackagesAndRejectsInvalidInput() {
        assertEquals("com.tw.radio", BridgeConfig.normalizePackage(" com.tw.radio "));
        assertNull(BridgeConfig.normalizePackage("com.tw.radio;rm -rf"));
        assertNull(BridgeConfig.normalizePackage("radio"));
    }

    @Test public void splitsPackageListsWithDeduplication() {
        assertArrayEquals(
                new String[] {"com.mixplorer", "com.mixplorer.silver"},
                BridgeConfig.splitPackageList("com.mixplorer, com.mixplorer.silver com.mixplorer"));
    }

    @Test public void normalizesClassesIncludingRelativeClassNames() {
        assertEquals(".MainActivity", BridgeConfig.normalizeClass(" .MainActivity "));
        assertEquals("com.tw.music.MusicActivity", BridgeConfig.normalizeClass("com.tw.music.MusicActivity"));
        assertNull(BridgeConfig.normalizeClass("bad/class"));
    }
}
