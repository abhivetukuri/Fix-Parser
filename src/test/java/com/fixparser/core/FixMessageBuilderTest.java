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
        String message = builder.setMessageType("0").buildString();
        
        assertNotNull(message);
        assertTrue(message.contains("35=0"));
        assertTrue(message.contains("49=CLIENT"));
        assertTrue(message.contains("56=SERVER"));
        assertTrue(message.contains("10=")); // Checksum
    }
    
    @Test
    @DisplayName("Should build logon message")
    void shouldBuildLogonMessage() {
        String message = builder.setMessageType("A")
                .addField(98, "0") 
                .addField(108, "30") 
                .addField(141, "Y") 
                .buildString();
        
        assertNotNull(message);
        assertTrue(message.contains("35=A"));
        assertTrue(message.contains("98=0"));
        assertTrue(message.contains("108=30"));
        assertTrue(message.contains("141=Y"));
    }
    
    @Test
    @DisplayName("Should build new order message")
    void shouldBuildNewOrderMessage() {
        String message = builder.setMessageType("D")
                .addField(11, "ORDER001") 
                .addField(21, "1") 
                .addField(55, "AAPL") 
                .addField(54, "1") 
                .addField(38, 100.0) 
                .addField(40, "2") 
                .buildString();
        
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
        ByteBuffer buffer = builder.setMessageType("0").build();
        
        assertNotNull(buffer);
        assertTrue(buffer.hasRemaining());
        
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
            String message = builder.setMessageType("0")
                .addField(11, "STRING") 
                .addField(34, 123) 
                .addField(38, 100.5) 
                .addField(54, '1') 
                .buildString();
        
        assertTrue(message.contains("11=STRING"));
        assertTrue(message.contains("34=123"));
        assertTrue(message.contains("38=100.5"));
        assertTrue(message.contains("54=1"));
    }
    
    @Test
    @DisplayName("Should set message sequence number")
    void shouldSetMessageSequenceNumber() {
        String message = builder.setMessageType("0")
                .setMsgSeqNum(42)
                .buildString();
        
        assertTrue(message.contains("34=42"));
    }
    
    @Test
    @DisplayName("Should add multiple fields at once")
    void shouldAddMultipleFieldsAtOnce() {
        java.util.Map<Integer, String> fields = new java.util.HashMap<>();
        fields.put(11, "ORDER001");
        fields.put(55, "AAPL");
        fields.put(38, "100.0");
        
        String message = builder.setMessageType("D")
                .addFields(fields)
                .buildString();
        
        assertTrue(message.contains("11=ORDER001"));
        assertTrue(message.contains("55=AAPL"));
        assertTrue(message.contains("38=100.0"));
    }
    
    @Test
    @DisplayName("Should ignore null field values")
    void shouldIgnoreNullFieldValues() {
        String message = builder.setMessageType("0")
                .addField(11, "VALID")
                .addField(12, null)
                .buildString();
        
        assertTrue(message.contains("11=VALID"));
        assertFalse(message.contains("12="));
    }
    
    @Test
    @DisplayName("Should throw exception when message type not set")
    void shouldThrowExceptionWhenMessageTypeNotSet() {
        assertThrows(IllegalStateException.class, () -> builder.build());
    }
    
    @Test
    @DisplayName("Should clear builder state")
    void shouldClearBuilderState() {
        builder.setMessageType("0").addField(11, "TEST");
        
        builder.clear();

        assertEquals(0, builder.getFieldCount());
        assertFalse(builder.hasField(11));
        assertThrows(IllegalStateException.class, () -> builder.build());
    }
    
    @Test
    @DisplayName("Should check field existence")
    void shouldCheckFieldExistence() {
        builder.setMessageType("0").addField(11, "TEST");
        
        assertTrue(builder.hasField(11));
        assertFalse(builder.hasField(999));
        assertEquals("TEST", builder.getField(11));
        assertNull(builder.getField(999));
    }
    
    @Test
    @DisplayName("Should get field count")
    void shouldGetFieldCount() {
        builder.setMessageType("0")
                .addField(11, "TEST1")
                .addField(12, "TEST2");
        
        assertEquals(2, builder.getFieldCount());
    }
    
    @Test
    @DisplayName("Should create new order using static factory")
    void shouldCreateNewOrderUsingStaticFactory() {
        FixMessageBuilder orderBuilder = FixMessageBuilder.newOrder("AAPL", '1', 100.0, '2');
        String message = orderBuilder.buildString();
        
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
        FixMessageBuilder heartbeatBuilder = FixMessageBuilder.heartbeat();
        String message = heartbeatBuilder.buildString();
        
        assertTrue(message.contains("35=0"));
    }
    
    @Test
    @DisplayName("Should create logon using static factory")
    void shouldCreateLogonUsingStaticFactory() {
        FixMessageBuilder logonBuilder = FixMessageBuilder.logon();
        String message = logonBuilder.buildString();
        
        assertTrue(message.contains("35=A"));
        assertTrue(message.contains("98=0"));
        assertTrue(message.contains("108=30"));
        assertTrue(message.contains("141=Y"));
    }
    
    @Test
    @DisplayName("Should create logout using static factory")
    void shouldCreateLogoutUsingStaticFactory() {
        FixMessageBuilder logoutBuilder = FixMessageBuilder.logout("User requested");
        String message = logoutBuilder.buildString();

        assertTrue(message.contains("35=5"));
        assertTrue(message.contains("58=User requested"));
    }
    
    @Test
    @DisplayName("Should create logout with default reason")
    void shouldCreateLogoutWithDefaultReason() {
        FixMessageBuilder logoutBuilder = FixMessageBuilder.logout(null);
        String message = logoutBuilder.buildString();
        
        assertTrue(message.contains("35=5"));
        assertTrue(message.contains("58=User requested"));
    }
    
    @Test
    @DisplayName("Should handle large messages")
    void shouldHandleLargeMessages() {
        StringBuilder largeValue = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeValue.append("A");
        }
        
        String message = builder.setMessageType("0")
                .addField(11, largeValue.toString())
                .buildString();
        
        assertNotNull(message);
        assertTrue(message.contains("11=" + largeValue.toString()));
    }
    
    @Test
    @DisplayName("Should calculate correct checksum")
    void shouldCalculateCorrectChecksum() {
        ByteBuffer buffer = builder.setMessageType("0").build();
        
        assertNotNull(buffer);
        
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        String message = new String(bytes);
        
        assertTrue(message.contains("10="));
        int checksumStart = message.lastIndexOf("10=");
        String checksumPart = message.substring(checksumStart + 3, checksumStart + 6);
        assertTrue(checksumPart.matches("\\d{3}"));
    }
    
    @Test
    @DisplayName("Should include timestamp in message")
    void shouldIncludeTimestampInMessage() {
        String message = builder.setMessageType("0").buildString();
        
        assertTrue(message.contains("52="));
        int timestampStart = message.indexOf("52=");
        int timestampEnd = message.indexOf("\u0001", timestampStart);
        String timestamp = message.substring(timestampStart + 3, timestampEnd);
        assertTrue(timestamp.matches("\\d{8}-\\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));
    }
    
    @Test
    @DisplayName("Should handle builder reuse")
    void shouldHandleBuilderReuse() {
            String message1 = builder.setMessageType("0").buildString();
        
        String message2 = builder.setMessageType("A")
                .addField(98, "0")
                .buildString();
        
        assertTrue(message1.contains("35=0"));
        assertTrue(message2.contains("35=A"));
        assertTrue(message2.contains("98=0"));
    }
} 