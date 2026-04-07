package com.palmergames.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class JavaUtilTests {
    @Test
    void testSetUUIDVersion() {
        final UUID uuid = UUID.randomUUID();
        
        assertEquals(4, uuid.version());
        
        for (int i = 0; i < 16; i++) {
            assertEquals(i, JavaUtil.changeUUIDVersion(uuid, i).version());
        }
    }
}
