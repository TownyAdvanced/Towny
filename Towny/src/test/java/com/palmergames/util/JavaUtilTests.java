package com.palmergames.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class JavaUtilTests {
    @Test
    void testSetUUIDVersion() {
        final UUID uuid = UUID.randomUUID();
		final UUID startUUID = new UUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
        
        assertEquals(4, uuid.version());
        
        for (int i = 0; i < 16; i++) {
            assertEquals(i, JavaUtil.changeUUIDVersion(uuid, i).version());
        }
		
		JavaUtil.changeUUIDVersion(uuid, 4);
		assertEquals(startUUID, uuid);
    }
}
