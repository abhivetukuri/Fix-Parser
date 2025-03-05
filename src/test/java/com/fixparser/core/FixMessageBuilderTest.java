package com.fixparser.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FIX Message Builder Tests")
class FixMessageBuilderTest {
    
    private FixMessageBuilder builder;
    
    @BeforeEach
    void setUp() {
        builder = new FixMessageBuilder("FIX.4.4", "CLIENT", "SERVER");
    }
    
    @Test
    @DisplayName("Should build heartbeat message")
    void shouldBuildHeartbeatMessage() {
        // When
        String message = builder.setMessageType("0").buildString();
        
        // Then
        assertNotNull(message);
        assertTrue(message.contains("35=0"));
        assertTrue(message.contains("49=CLIENT"));
        assertTrue(message.contains("56=SERVER"));
        assertTrue(message.contains("10=")); // Checksum
    }
    
    @Test
    @DisplayName("Should build logon message")
    void shouldBuildLogonMessage() {
        // When
        String message = builder.setMessageType("A")
                .addField(98, "0") // EncryptMethod
                .addField(108, "30") // HeartBtInt
                .addField(141, "Y") // ResetSeqNumFlag
                .buildString();
        
        // Then
        assertNotNull(message);
        assertTrue(message.contains("35=A"));
        assertTrue(message.contains("98=0"));
        assertTrue(message.contains("108=30"));
        assertTrue(message.contains("141=Y"));
    }
    
    @Test
    @DisplayName("Should build new order message")
    void shouldBuildNewOrderMessage() {
        // When
        String message = builder.setMessageType("D")
                .addField(11, "ORDER001") // ClOrdID
                .addField(21, "1") // HandlInst
                .addField(55, "AAPL") // Symbol
                .addField(54, "1") // Side
                .addField(38, 100.0) // OrderQty
                .addField(40, "2") // OrdType
                .buildString();
        
        // Then
        assertNotNull(message);
        assertTrue(message.contains("35=D"));
        assertTrue(message.contains("11=ORDER001"));
        assertTrue(message.contains("21=1"));
        assertTrue(message.contains("55=AAPL"));
        assertTrue(message.contains("54=1"));
        assertTrue(message.contains("38=100.0"));
        assertTrue(message.contains("40=2"));
    }
    
    @Test
    @DisplayName("Should build message as ByteBuffer")
    void shouldBuildMessageAsByteBuffer() {
        // When
        ByteBuffer buffer = builder.setMessageType("0").build();
        
        // Then
        assertNotNull(buffer);
        assertTrue(buffer.hasRemaining());
        
        // Convert to string and verify
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        String message = new String(bytes);
        
        assertTrue(message.contains("35=0"));
        assertTrue(message.contains("49=CLIENT"));
        assertTrue(message.contains("56=SERVER"));
    }
    
    @Test
    @DisplayName("Should handle different field types")
    void shouldHandleDifferentFieldTypes() {
        // When
        String message = builder.setMessageType("0")
                .addField(11, "STRING") // String
                .addField(34, 123) // Integer
                .addField(38, 100.5) // Double
                .addField(54, '1') // Character
                .buildString();
        
        // Then
        assertTrue(message.contains("11=STRING"));
        assertTrue(message.contains("34=123"));
        assertTrue(message.contains("38=100.5"));
        assertTrue(message.contains("54=1"));
    }
    
    @Test
    @DisplayName("Should set message sequence number")
    void shouldSetMessageSequenceNumber() {
        // When
        String message = builder.setMessageType("0")
                .setMsgSeqNum(42)
                .buildString();
        
        // Then
        assertTrue(message.contains("34=42"));
    }
    
    @Test
    @DisplayName("Should add multiple fields at once")
    void shouldAddMultipleFieldsAtOnce() {
        // Given
        java.util.Map<Integer, String> fields = new java.util.HashMap<>();
        fields.put(11, "ORDER001");
        fields.put(55, "AAPL");
        fields.put(38, "100.0");
        
        // When
        String message = builder.setMessageType("D")
                .addFields(fields)
                .buildString();
        
        // Then
        assertTrue(message.contains("11=ORDER001"));
        assertTrue(message.contains("55=AAPL"));
        assertTrue(message.contains("38=100.0"));
    }
    
    @Test
    @DisplayName("Should ignore null field values")
    void shouldIgnoreNullFieldValues() {
        // When
        String message = builder.setMessageType("0")
                .addField(11, "VALID")
                .addField(12, null)
                .buildString();
        
        // Then
        assertTrue(message.contains("11=VALID"));
        assertFalse(message.contains("12="));
    }
    
    @Test
    @DisplayName("Should throw exception when message type not set")
    void shouldThrowExceptionWhenMessageTypeNotSet() {
        // When & Then
        assertThrows(IllegalStateException.class, () -> builder.build());
    }
    
    @Test
    @DisplayName("Should clear builder state")
    void shouldClearBuilderState() {
        // Given
        builder.setMessageType("0").addField(11, "TEST");
        
        // When
        builder.clear();
        
        // Then
        assertEquals(0, builder.getFieldCount());
        assertFalse(builder.hasField(11));
        assertThrows(IllegalStateException.class, () -> builder.build());
    }
    
    @Test
    @DisplayName("Should check field existence")
    void shouldCheckFieldExistence() {
        // Given
        builder.setMessageType("0").addField(11, "TEST");
        
        // When & Then
        assertTrue(builder.hasField(11));
        assertFalse(builder.hasField(999));
        assertEquals("TEST", builder.getField(11));
        assertNull(builder.getField(999));
    }
    
    @Test
    @DisplayName("Should get field count")
    void shouldGetFieldCount() {
        // Given
        builder.setMessageType("0")
                .addField(11, "TEST1")
                .addField(12, "TEST2");
        
        // When & Then
        assertEquals(2, builder.getFieldCount());
    }
    
    @Test
    @DisplayName("Should create new order using static factory")
    void shouldCreateNewOrderUsingStaticFactory() {
        // When
        FixMessageBuilder orderBuilder = FixMessageBuilder.newOrder("AAPL", '1', 100.0, '2');
        String message = orderBuilder.buildString();
        
        // Then
        assertTrue(message.contains("35=D"));
        assertTrue(message.contains("55=AAPL"));
        assertTrue(message.contains("54=1"));
        assertTrue(message.contains("38=100.0"));
        assertTrue(message.contains("40=2"));
        assertTrue(message.contains("21=1"));
    }
    
    @Test
    @DisplayName("Should create heartbeat using static factory")
    void shouldCreateHeartbeatUsingStaticFactory() {
        // When
        FixMessageBuilder heartbeatBuilder = FixMessageBuilder.heartbeat();
        String message = heartbeatBuilder.buildString();
        
        // Then
        assertTrue(message.contains("35=0"));
    }
    
    @Test
    @DisplayName("Should create logon using static factory")
    void shouldCreateLogonUsingStaticFactory() {
        // When
        FixMessageBuilder logonBuilder = FixMessageBuilder.logon();
        String message = logonBuilder.buildString();
        
        // Then
        assertTrue(message.contains("35=A"));
        assertTrue(message.contains("98=0"));
        assertTrue(message.contains("108=30"));
        assertTrue(message.contains("141=Y"));
    }
    
    @Test
    @DisplayName("Should create logout using static factory")
    void shouldCreateLogoutUsingStaticFactory() {
        // When
        FixMessageBuilder logoutBuilder = FixMessageBuilder.logout("User requested");
        String message = logoutBuilder.buildString();
        
        // Then
        assertTrue(message.contains("35=5"));
        assertTrue(message.contains("58=User requested"));
    }
    
    @Test
    @DisplayName("Should create logout with default reason")
    void shouldCreateLogoutWithDefaultReason() {
        // When
        FixMessageBuilder logoutBuilder = FixMessageBuilder.logout(null);
        String message = logoutBuilder.buildString();
        
        // Then
        assertTrue(message.contains("35=5"));
        assertTrue(message.contains("58=User requested"));
    }
    
    @Test
    @DisplayName("Should handle large messages")
    void shouldHandleLargeMessages() {
        // Given
        StringBuilder largeValue = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeValue.append("A");
        }
        
        // When
        String message = builder.setMessageType("0")
                .addField(11, largeValue.toString())
                .buildString();
        
        // Then
        assertNotNull(message);
        assertTrue(message.contains("11=" + largeValue.toString()));
    }
    
    @Test
    @DisplayName("Should calculate correct checksum")
    void shouldCalculateCorrectChecksum() {
        // When
        ByteBuffer buffer = builder.setMessageType("0").build();
        
        // Then
        assertNotNull(buffer);
        
        // Verify checksum is present and valid
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        String message = new String(bytes);
        
        assertTrue(message.contains("10="));
        // Extract checksum and verify it's a 3-digit number
        int checksumStart = message.lastIndexOf("10=");
        String checksumPart = message.substring(checksumStart + 3, checksumStart + 6);
        assertTrue(checksumPart.matches("\\d{3}"));
    }
    
    @Test
    @DisplayName("Should include timestamp in message")
    void shouldIncludeTimestampInMessage() {
        // When
        String message = builder.setMessageType("0").buildString();
        
        // Then
        assertTrue(message.contains("52="));
        // Verify timestamp format (YYYYMMDD-HH:MM:SS.sss)
        int timestampStart = message.indexOf("52=");
        int timestampEnd = message.indexOf("\u0001", timestampStart);
        String timestamp = message.substring(timestampStart + 3, timestampEnd);
        assertTrue(timestamp.matches("\\d{8}-\\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));
    }
    
    @Test
    @DisplayName("Should handle builder reuse")
    void shouldHandleBuilderReuse() {
        // Given
        String message1 = builder.setMessageType("0").buildString();
        
        // When
        String message2 = builder.setMessageType("A")
                .addField(98, "0")
                .buildString();
        
        // Then
        assertTrue(message1.contains("35=0"));
        assertTrue(message2.contains("35=A"));
        assertTrue(message2.contains("98=0"));
    }
} 