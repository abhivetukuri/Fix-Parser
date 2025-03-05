package com.fixparser.example;

import com.fixparser.core.FixMessage;
import com.fixparser.core.FixMessageBuilder;
import com.fixparser.core.FixParser;
import com.fixparser.core.FixParseException;
import com.fixparser.dictionary.FixDictionary;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive example demonstrating the FIX parser functionality.
 * Shows zero-copy parsing, ByteBuffer operations, and performance optimizations.
 */
public class FixParserExample {
    
    public static void main(String[] args) {
        FixParserExample example = new FixParserExample();
        
        System.out.println("=== FIX Parser Example ===\n");
        
        // Basic parsing examples
        example.basicParsingExample();
        
        // Message building examples
        example.messageBuildingExample();
        
        // Performance examples
        example.performanceExample();
        
        // Zero-copy examples
        example.zeroCopyExample();
        
        // Validation examples
        example.validationExample();
        
        // Error handling examples
        example.errorHandlingExample();
    }
    
    /**
     * Demonstrate basic FIX message parsing
     */
    public void basicParsingExample() {
        System.out.println("1. Basic Parsing Examples:");
        System.out.println("---------------------------");
        
        FixParser parser = new FixParser();
        
        // Parse a heartbeat message
        String heartbeatMsg = "8=FIX.4.4\u00019=20\u000135=0\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000110=123\u0001";
        
        try {
            FixMessage message = parser.parse(heartbeatMsg);
            System.out.println("✓ Parsed heartbeat message:");
            System.out.println("  Message Type: " + message.getMessageType());
            System.out.println("  Sender: " + message.getString(49));
            System.out.println("  Target: " + message.getString(56));
            System.out.println("  Sequence: " + message.getInt(34));
            System.out.println("  Checksum: " + message.getChecksum());
        } catch (FixParseException e) {
            System.err.println("✗ Failed to parse heartbeat: " + e.getMessage());
        }
        
        // Parse a new order message
        String newOrderMsg = "8=FIX.4.4\u00019=45\u000135=D\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000111=ORDER001\u000121=1\u000155=AAPL\u000154=1\u000138=100.0\u000140=2\u000160=20231201-10:30:00.000\u000110=345\u0001";
        
        try {
            FixMessage message = parser.parse(newOrderMsg);
            System.out.println("\n✓ Parsed new order message:");
            System.out.println("  Message Type: " + message.getMessageType());
            System.out.println("  Order ID: " + message.getString(11));
            System.out.println("  Symbol: " + message.getString(55));
            System.out.println("  Side: " + message.getString(54));
            System.out.println("  Quantity: " + message.getDouble(38));
            System.out.println("  Order Type: " + message.getString(40));
        } catch (FixParseException e) {
            System.err.println("✗ Failed to parse new order: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrate FIX message building
     */
    public void messageBuildingExample() {
        System.out.println("2. Message Building Examples:");
        System.out.println("------------------------------");
        
        // Build a heartbeat message
        String heartbeat = FixMessageBuilder.heartbeat()
                .setMsgSeqNum(42)
                .buildString();
        System.out.println("✓ Built heartbeat message:");
        System.out.println("  " + heartbeat);
        
        // Build a logon message
        String logon = FixMessageBuilder.logon()
                .setMsgSeqNum(1)
                .buildString();
        System.out.println("\n✓ Built logon message:");
        System.out.println("  " + logon);
        
        // Build a new order message
        String newOrder = FixMessageBuilder.newOrder("AAPL", '1', 100.0, '2')
                .setMsgSeqNum(10)
                .addField(60, "20231201-10:30:00.000") // TransactTime
                .buildString();
        System.out.println("\n✓ Built new order message:");
        System.out.println("  " + newOrder);
        
        System.out.println();
    }
    
    /**
     * Demonstrate performance optimizations
     */
    public void performanceExample() {
        System.out.println("3. Performance Examples:");
        System.out.println("-------------------------");
        
        FixParser parser = new FixParser();
        
        // Performance test: Parse many messages
        int messageCount = 100_000;
        String testMessage = "8=FIX.4.4\u00019=45\u000135=D\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000111=ORDER001\u000121=1\u000155=AAPL\u000154=1\u000138=100.0\u000140=2\u000160=20231201-10:30:00.000\u000110=345\u0001";
        byte[] messageBytes = testMessage.getBytes();
        
        System.out.println("Running performance test with " + messageCount + " messages...");
        
        // Warm up
        for (int i = 0; i < 1000; i++) {
            try {
                parser.parse(messageBytes);
            } catch (FixParseException e) {
                // Ignore
            }
        }
        
        // Actual test
        Instant start = Instant.now();
        AtomicLong successCount = new AtomicLong(0);
        
        for (int i = 0; i < messageCount; i++) {
            try {
                FixMessage message = parser.parse(messageBytes);
                if (message != null) {
                    successCount.incrementAndGet();
                }
            } catch (FixParseException e) {
                // Ignore errors for performance test
            }
        }
        
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        
        System.out.println("✓ Performance Results:");
        System.out.println("  Messages parsed: " + successCount.get());
        System.out.println("  Time taken: " + duration.toMillis() + "ms");
        System.out.println("  Messages per second: " + (successCount.get() * 1000 / duration.toMillis()));
        System.out.println("  Average time per message: " + (duration.toNanos() / successCount.get()) + "ns");
        
        System.out.println();
    }
    
    /**
     * Demonstrate zero-copy operations
     */
    public void zeroCopyExample() {
        System.out.println("4. Zero-Copy Examples:");
        System.out.println("-----------------------");
        
        FixParser parser = new FixParser();
        String testMessage = "8=FIX.4.4\u00019=45\u000135=D\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000111=ORDER001\u000121=1\u000155=AAPL\u000154=1\u000138=100.0\u000140=2\u000160=20231201-10:30:00.000\u000110=345\u0001";
        
        try {
            FixMessage message = parser.parse(testMessage);
            
            System.out.println("✓ Zero-copy field access:");
            
            // Access field info with ByteBuffer views
            FixMessage.FieldInfo fieldInfo = message.getField(55); // Symbol
            if (fieldInfo != null) {
                System.out.println("  Symbol field (tag 55):");
                System.out.println("    Tag: " + fieldInfo.getTag());
                System.out.println("    Value: " + fieldInfo.getValueAsString());
                System.out.println("    Value start position: " + fieldInfo.getValueStart());
                System.out.println("    Value length: " + fieldInfo.getValueLength());
                System.out.println("    Has ByteBuffer view: " + (fieldInfo.getValueBuffer() != null));
            }
            
            // Multiple messages in single buffer
            System.out.println("\n✓ Multiple messages in buffer:");
            String message1 = "8=FIX.4.4\u00019=20\u000135=0\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000110=123\u0001";
            String message2 = "8=FIX.4.4\u00019=20\u000135=0\u000149=CLIENT\u000156=SERVER\u000134=2\u000152=20231201-10:30:01.000\u000110=124\u0001";
            String combined = message1 + message2;
            
            ByteBuffer buffer = ByteBuffer.wrap(combined.getBytes());
            FixMessage msg1 = parser.parse(buffer);
            FixMessage msg2 = parser.parse(buffer);
            
            System.out.println("  First message sequence: " + msg1.getInt(34));
            System.out.println("  Second message sequence: " + msg2.getInt(34));
            System.out.println("  Buffer position after parsing: " + buffer.position());
            System.out.println("  Buffer remaining: " + buffer.remaining());
            
        } catch (FixParseException e) {
            System.err.println("✗ Zero-copy example failed: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrate validation features
     */
    public void validationExample() {
        System.out.println("5. Validation Examples:");
        System.out.println("-----------------------");
        
        FixParser parser = new FixParser();
        FixDictionary dictionary = parser.getDictionary();
        
        System.out.println("✓ Dictionary validation:");
        System.out.println("  Valid message types: " + dictionary.getValidMessageTypes().size());
        System.out.println("  Field definitions: " + dictionary.getAllFieldDefinitions().size());
        
        // Test field validation
        System.out.println("\n✓ Field validation:");
        System.out.println("  Tag 8 (BeginString) validation: " + dictionary.validateFieldValue(8, "FIX.4.4"));
        System.out.println("  Tag 34 (MsgSeqNum) validation: " + dictionary.validateFieldValue(34, "123"));
        System.out.println("  Tag 52 (SendingTime) validation: " + dictionary.validateFieldValue(52, "20231201-10:30:00.000"));
        System.out.println("  Invalid timestamp validation: " + dictionary.validateFieldValue(52, "invalid"));
        
        System.out.println();
    }
    
    /**
     * Demonstrate error handling
     */
    public void errorHandlingExample() {
        System.out.println("6. Error Handling Examples:");
        System.out.println("----------------------------");
        
        FixParser parser = new FixParser();
        
        // Test various error conditions
        String[] invalidMessages = {
            "", // Empty message
            "8=FIX.4.4", // Incomplete message
            "8=FIX.4.4\u00019=20\u000135=Z\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000110=123\u0001", // Invalid message type
            "8=FIX.4.4\u00019=20\u000135=0\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000110=999\u0001" // Invalid checksum
        };
        
        String[] errorTypes = {
            "Empty message",
            "Incomplete message",
            "Invalid message type",
            "Invalid checksum"
        };
        
        for (int i = 0; i < invalidMessages.length; i++) {
            try {
                parser.parse(invalidMessages[i]);
                System.out.println("✗ Unexpected success for: " + errorTypes[i]);
            } catch (FixParseException e) {
                System.out.println("✓ Caught expected error for " + errorTypes[i] + ": " + e.getMessage());
            }
        }
        
        System.out.println();
    }
} 