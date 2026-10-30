package my.shanunid.linespoofer.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.Map;

public class LogicAndSafetyTest {

    @Test
    public void testConfigManagerParsing() {
        String testConfig = "ro.product.model=Pixel 9 Pro\n" +
                "ro.product.brand=google\n" +
                "device.display.refresh_rate=144\n" +
                "device.battery.level=85\n" +
                "spoof.toggle.model=true\n" +
                "# Comment line\n" +
                "device.mac.mode=custom\n" +
                "device.mac.custom=00:11:22:33:44:55\n";

        Map<String, String> parsed = ConfigManager.parseConfigContent(testConfig);
        assertEquals("Pixel 9 Pro", parsed.get("ro.product.model"));
        assertEquals("google", parsed.get("ro.product.brand"));
        assertEquals("144", parsed.get("device.display.refresh_rate"));
        assertEquals("85", parsed.get("device.battery.level"));
        assertEquals("true", parsed.get("spoof.toggle.model"));
        assertEquals("00:11:22:33:44:55", parsed.get("device.mac.custom"));
    }

    @Test
    public void testObjectSerializationCompatibility() throws Exception {
        String testKey = "default_profile";
        String testVal = "ro.product.model=Galaxy S25 Ultra\nro.product.brand=samsung\n";

        byte[] serialized = RootScopeManager.serializeObject(testVal);
        assertNotNull(serialized);
        assertTrue(serialized.length > 4);

        // Check Java stream magic header: 0xAC, 0xED, 0x00, 0x05
        assertEquals((byte) 0xAC, serialized[0]);
        assertEquals((byte) 0xED, serialized[1]);
        assertEquals((byte) 0x00, serialized[2]);
        assertEquals((byte) 0x05, serialized[3]);

        // Verify that standard ObjectInputStream can deserialize it back
        ByteArrayInputStream bais = new ByteArrayInputStream(serialized);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Object deserialized = ois.readObject();
        assertEquals(testVal, deserialized);

        // Test Boolean serialization
        byte[] boolBytes = RootScopeManager.serializeObject(Boolean.TRUE);
        ByteArrayInputStream baisBool = new ByteArrayInputStream(boolBytes);
        ObjectInputStream oisBool = new ObjectInputStream(baisBool);
        assertEquals(Boolean.TRUE, oisBool.readObject());
    }

    @Test
    public void testUptimeDeterminism() {
        // Deterministic uptime per device profile
        long sec = ConfigManager.getSpoofedUptimeSeconds();
        assertTrue("Uptime seconds should be >= 0", sec >= 0);
        long ms = ConfigManager.getSpoofedUptimeMs();
        assertEquals(sec * 1000L, ms);
    }

    @Test
    public void testLocationAndFpsDefaults() {
        assertNotNull(ConfigManager.getSpoofedLatitude());
        assertNotNull(ConfigManager.getSpoofedLongitude());
        assertTrue(ConfigManager.getTargetRefreshRate() > 0);
    }
}

