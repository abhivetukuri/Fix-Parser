package com.fixparser.core;

import com.fixparser.dictionary.FixDictionary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FIX Parser Tests")
class FixParserTest {
    
    private FixParser parser;
    private FixDictionary dictionary;
    
    @BeforeEach
    void setUp() {
        dictionary = new FixDictionary();
        parser = new FixParser(dictionary, true, true);
    }
    
    @Test
    @DisplayName("Should parse valid heartbeat message")
    void shouldParseHeartbeatMessage() throws FixParseException {
        String fixMessage = "8=FIX.4.4\u00019=20\u000135=0\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000110=123\u0001";
        
        FixMessage message = parser.parse(fixMessage);
        
        assertNotNull(message);
        assertEquals("0", message.getMessageType());
        assertEquals(20, message.getMessageLength());
        assertEquals(123, message.getChecksum());
        assertEquals("CLIENT", message.getString(49));
        assertEquals("SERVER", message.getString(56));
        assertEquals(1, message.getInt(34));
    }
    
    @Test
    @DisplayName("Should parse valid logon message")
    void shouldParseLogonMessage() throws FixParseException {
        String fixMessage = "8=FIX.4.4\u00019=35\u000135=A\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000198=0\u0001108=30\u0001141=Y\u000110=234\u0001";
        
        FixMessage message = parser.parse(fixMessage);
        
        assertNotNull(message);
        assertEquals("A", message.getMessageType());
        assertEquals("0", message.getString(98)); 
        assertEquals("30", message.getString(108)); 
        assertEquals("Y", message.getString(141)); 
    }
    
    @Test
    @DisplayName("Should parse valid new order message")
    void shouldParseNewOrderMessage() throws FixParseException {
        String fixMessage = "8=FIX.4.4\u00019=45\u000135=D\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000111=ORDER001\u000121=1\u000155=AAPL\u000154=1\u000138=100.0\u000140=2\u000160=20231201-10:30:00.000\u000110=345\u0001";
        
        FixMessage message = parser.parse(fixMessage);
        
        assertNotNull(message);
        assertEquals("D", message.getMessageType());
        assertEquals("ORDER001", message.getString(11)); 
        assertEquals("1", message.getString(21)); 
        assertEquals("AAPL", message.getString(55)); 
        assertEquals("1", message.getString(54)); 
        assertEquals(100.0, message.getDouble(38)); 
        assertEquals("2", message.getString(40)); 
    }
    
    @Test
    @DisplayName("Should parse message from ByteBuffer")
    void shouldParseFromByteBuffer() throws FixParseException {
        String fixMessage = "8=FIX.4.4\u00019=20\u000135=0\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000110=123\u0001";
        ByteBuffer buffer = ByteBuffer.wrap(fixMessage.getBytes());
        
        FixMessage message = parser.parse(buffer);
        
        assertNotNull(message);
        assertEquals("0", message.getMessageType());
    }
    
    @Test
    @DisplayName("Should parse message from byte array")
    void shouldParseFromByteArray() throws FixParseException {
        String fixMessage = "8=FIX.4.4\u00019=20\u000135=0\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000110=123\u0001";
        byte[] data = fixMessage.getBytes();
        
        FixMessage message = parser.parse(data);
        
        assertNotNull(message);
        assertEquals("0", message.getMessageType());
    }
    
    @Test
    @DisplayName("Should validate checksum correctly")
    void shouldValidateChecksum() throws FixParseException {
        String fixMessage = "8=FIX.4.4\u00019=20\u000135=0\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000110=123\u0001";
        
        FixMessage message = parser.parse(fixMessage);
        
        assertNotNull(message);
    }
    
    @Test
    @DisplayName("Should throw exception for invalid checksum")
    void shouldThrowExceptionForInvalidChecksum() {
        String fixMessage = "8=FIX.4.4\u00019=20\u000135=0\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000110=999\u0001";
        
        assertThrows(FixParseException.class, () -> parser.parse(fixMessage));
    }
    
    @Test
    @DisplayName("Should throw exception for missing required fields")
    void shouldThrowExceptionForMissingRequiredFields() {
        String fixMessage = "8=FIX.4.4\u000135=0\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000110=123\u0001";
        
        assertThrows(FixParseException.class, () -> parser.parse(fixMessage));
    }
    
    @Test
    @DisplayName("Should throw exception for invalid message type")
    void shouldThrowExceptionForInvalidMessageType() {
        String fixMessage = "8=FIX.4.4\u00019=20\u000135=Z\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000110=123\u0001";
        
        assertThrows(FixParseException.class, () -> parser.parse(fixMessage));
    }
    
    @Test
    @DisplayName("Should access fields with zero-copy operations")
    void shouldAccessFieldsWithZeroCopy() throws FixParseException {
            String fixMessage = "8=FIX.4.4\u00019=20\u000135=0\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000110=123\u0001";
        
        FixMessage message = parser.parse(fixMessage);
        FixMessage.FieldInfo fieldInfo = message.getField(49);
        
        assertNotNull(fieldInfo);
        assertEquals(49, fieldInfo.getTag());
        assertEquals("CLIENT", fieldInfo.getValueAsString());
        assertNotNull(fieldInfo.getValueBuffer());
    }
    
    @Test
    @DisplayName("Should handle multiple messages in buffer")
    void shouldHandleMultipleMessagesInBuffer() throws FixParseException {
        String message1 = "8=FIX.4.4\u00019=20\u000135=0\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000110=123\u0001";
        String message2 = "8=FIX.4.4\u00019=20\u000135=0\u000149=CLIENT\u000156=SERVER\u000134=2\u000152=20231201-10:30:01.000\u000110=124\u0001";
        String combined = message1 + message2;
        ByteBuffer buffer = ByteBuffer.wrap(combined.getBytes());
        
        FixMessage firstMessage = parser.parse(buffer);
        FixMessage secondMessage = parser.parse(buffer);
        
        assertNotNull(firstMessage);
        assertEquals(1, firstMessage.getInt(34));
        
        assertNotNull(secondMessage);
        assertEquals(2, secondMessage.getInt(34));
    }
    
    @Test
    @DisplayName("Should handle parser without validation")
    void shouldHandleParserWithoutValidation() throws FixParseException {
        FixParser noValidationParser = new FixParser(dictionary, false, false);
        String fixMessage = "8=FIX.4.4\u00019=20\u000135=Z\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000110=123\u0001";
        
        FixMessage message = noValidationParser.parse(fixMessage);
        
        assertNotNull(message);
        assertEquals("Z", message.getMessageType());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"0", "A", "D", "F", "G", "H", "8", "9", "V", "W", "X", "Y"})
    @DisplayName("Should parse valid message types")
    void shouldParseValidMessageTypes(String messageType) throws FixParseException {
        String fixMessage = String.format("8=FIX.4.4\u00019=20\u000135=%s\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000110=123\u0001", messageType);
        
        FixMessage message = parser.parse(fixMessage);
        
        assertNotNull(message);
        assertEquals(messageType, message.getMessageType());
    }
    
    @Test
    @DisplayName("Should handle empty or null input")
    void shouldHandleEmptyOrNullInput() {
        String emptyMessage = "";
        String shortMessage = "8=FIX.4.4";

        assertThrows(FixParseException.class, () -> parser.parse(emptyMessage));
        assertThrows(FixParseException.class, () -> parser.parse(shortMessage));
        assertThrows(FixParseException.class, () -> parser.parse((String) null));
    }
    
    @Test
    @DisplayName("Should preserve buffer state on error")
    void shouldPreserveBufferStateOnError() {
        String invalidMessage = "8=FIX.4.4\u00019=20\u000135=0\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000110=999\u0001";
        ByteBuffer buffer = ByteBuffer.wrap(invalidMessage.getBytes());
        int originalPosition = buffer.position();
        
        try {
            parser.parse(buffer);
        } catch (FixParseException e) {
        }
        
        assertEquals(originalPosition, buffer.position());
    }
    
    @Test
    @DisplayName("Should handle field access methods")
    void shouldHandleFieldAccessMethods() throws FixParseException {
        String fixMessage = "8=FIX.4.4\u00019=20\u000135=0\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000110=123\u0001";
        
        FixMessage message = parser.parse(fixMessage);
        
        assertTrue(message.hasField(49));
        assertFalse(message.hasField(999));
        assertEquals("CLIENT", message.getString(49));
        assertEquals(1, message.getInt(34));
        assertNull(message.getString(999));
        assertNull(message.getInt(999));
    }
    
    @Test
    @DisplayName("Should handle all field types")
    void shouldHandleAllFieldTypes() throws FixParseException {
            String fixMessage = "8=FIX.4.4\u00019=30\u000135=0\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000111=ORDER001\u000138=100.5\u000154=1\u000110=123\u0001";
        
        FixMessage message = parser.parse(fixMessage);
        
        assertEquals("ORDER001", message.getString(11));
        assertEquals(100.5, message.getDouble(38));
        assertEquals(1, message.getInt(54));
    }
} 