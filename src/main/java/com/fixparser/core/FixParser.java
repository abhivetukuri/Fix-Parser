package com.fixparser.core;

import com.fixparser.dictionary.FixDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * High-performance FIX message parser using ByteBuffer operations and zero-copy techniques.
 * This parser minimizes garbage collection overhead by reusing ByteBuffer slices and
 * avoiding unnecessary object allocations.
 */
public class FixParser {
    
    private static final Logger logger = LoggerFactory.getLogger(FixParser.class);
    
    // FIX protocol constants
    private static final byte FIELD_SEPARATOR = 0x01; // SOH character
    private static final byte EQUALS = 0x3D; // '=' character
    private static final int MAX_MESSAGE_SIZE = 1024 * 1024; // 1MB max message size
    private static final int MIN_MESSAGE_SIZE = 20; // Minimum reasonable message size
    
    // Common FIX field tags
    private static final int TAG_BEGIN_STRING = 8;
    private static final int TAG_BODY_LENGTH = 9;
    private static final int TAG_MSG_TYPE = 35;
    private static final int TAG_CHECKSUM = 10;
    private static final int TAG_SENDER_COMP_ID = 49;
    private static final int TAG_TARGET_COMP_ID = 56;
    private static final int TAG_MSG_SEQ_NUM = 34;
    private static final int TAG_SENDING_TIME = 52;
    
    private final FixDictionary dictionary;
    private final boolean validateChecksum;
    private final boolean validateDictionary;
    
    // Reusable buffers to minimize allocation
    private final ByteBuffer tempBuffer;
    private final Map<Integer, FixMessage.FieldInfo> fieldCache;
    
    public FixParser() {
        this(new FixDictionary(), true, true);
    }
    
    public FixParser(FixDictionary dictionary, boolean validateChecksum, boolean validateDictionary) {
        this.dictionary = dictionary;
        this.validateChecksum = validateChecksum;
        this.validateDictionary = validateDictionary;
        this.tempBuffer = ByteBuffer.allocate(1024);
        this.fieldCache = new HashMap<>();
    }
    
    /**
     * Parse a FIX message from a ByteBuffer with zero-copy operations
     */
    public FixMessage parse(ByteBuffer buffer) throws FixParseException {
        if (buffer == null || buffer.remaining() < MIN_MESSAGE_SIZE) {
            throw new FixParseException("Invalid buffer or message too short");
        }
        
        // Reset field cache for reuse
        fieldCache.clear();
        
        // Mark the start position
        int startPosition = buffer.position();
        int originalLimit = buffer.limit();
        
        try {
            // Find message boundaries
            int messageEnd = findMessageEnd(buffer);
            if (messageEnd == -1) {
                throw new FixParseException("Could not find message end");
            }
            
            // Set limit to message end for parsing
            buffer.limit(messageEnd);
            
            // Parse the message
            FixMessage message = parseMessageInternal(buffer);
            
            // Validate checksum if enabled
            if (validateChecksum && !validateChecksum(buffer, startPosition, messageEnd, message.getChecksum())) {
                throw new FixParseException("Checksum validation failed");
            }
            
            // Validate against dictionary if enabled
            if (validateDictionary) {
                validateMessage(message);
            }
            
            // Reset buffer position to after the parsed message
            buffer.position(messageEnd);
            buffer.limit(originalLimit);
            
            return message;
            
        } catch (Exception e) {
            // Reset buffer state on error
            buffer.position(startPosition);
            buffer.limit(originalLimit);
            throw new FixParseException("Failed to parse FIX message", e);
        }
    }
    
    /**
     * Parse a FIX message from byte array
     */
    public FixMessage parse(byte[] data) throws FixParseException {
        return parse(ByteBuffer.wrap(data));
    }
    
    /**
     * Parse a FIX message from string
     */
    public FixMessage parse(String message) throws FixParseException {
        return parse(message.getBytes());
    }
    
    /**
     * Find the end of a FIX message (before checksum)
     */
    private int findMessageEnd(ByteBuffer buffer) {
        int position = buffer.position();
        int limit = buffer.limit();
        
        // Look for checksum field (tag 10)
        while (position < limit - 7) { // Need at least 7 bytes for "10=xxx|"
            if (buffer.get(position) == '1' && buffer.get(position + 1) == '0' && 
                buffer.get(position + 2) == '=') {
                // Found checksum field, find the end
                for (int i = position + 3; i < limit; i++) {
                    if (buffer.get(i) == FIELD_SEPARATOR) {
                        return i + 1; // Include the SOH character
                    }
                }
                return limit; // No SOH found, use limit
            }
            position++;
        }
        
        return -1; // Not found
    }
    
    /**
     * Internal parsing method using zero-copy ByteBuffer operations
     */
    private FixMessage parseMessageInternal(ByteBuffer buffer) throws FixParseException {
        int bodyLength = -1;
        String messageType = null;
        int checksum = -1;
        
        int position = buffer.position();
        int limit = buffer.limit();
        
        // First pass: find required fields and calculate body length
        while (position < limit) {
            // Find field separator
            int fieldStart = position;
            int equalsPos = -1;
            
            // Find equals sign
            while (position < limit && buffer.get(position) != EQUALS) {
                position++;
            }
            
            if (position >= limit) {
                break;
            }
            
            equalsPos = position;
            position++; // Skip equals
            
            // Find field separator
            int valueStart = position;
            while (position < limit && buffer.get(position) != FIELD_SEPARATOR) {
                position++;
            }
            
            int valueEnd = position;
            position++; // Skip separator
            
            // Parse tag
            int tag = parseTag(buffer, fieldStart, equalsPos);
            
            // Create field info with zero-copy ByteBuffer view
            FixMessage.FieldInfo fieldInfo = new FixMessage.FieldInfo(
                buffer, tag, valueStart, valueEnd - valueStart
            );
            
            fieldCache.put(tag, fieldInfo);
            
            // Extract required fields
            switch (tag) {
                case TAG_BODY_LENGTH:
                    bodyLength = fieldInfo.getValueAsInt();
                    break;
                case TAG_MSG_TYPE:
                    messageType = fieldInfo.getValueAsString();
                    break;
                case TAG_CHECKSUM:
                    checksum = fieldInfo.getValueAsInt();
                    break;
            }
        }
        
        // Validate required fields
        if (bodyLength == -1) {
            throw new FixParseException("Missing BodyLength field");
        }
        if (messageType == null) {
            throw new FixParseException("Missing MsgType field");
        }
        if (checksum == -1) {
            throw new FixParseException("Missing CheckSum field");
        }
        
        return new FixMessage(buffer, new HashMap<>(fieldCache), bodyLength, checksum, messageType);
    }
    
    /**
     * Parse tag number from ByteBuffer slice
     */
    private int parseTag(ByteBuffer buffer, int start, int end) throws FixParseException {
        // Reuse temp buffer for tag parsing
        tempBuffer.clear();
        tempBuffer.limit(end - start);
        
        // Copy tag bytes to temp buffer
        buffer.position(start);
        buffer.limit(end);
        tempBuffer.put(buffer);
        
        // Reset original buffer
        buffer.limit(buffer.capacity());
        
        // Parse tag as string
        tempBuffer.flip();
        byte[] tagBytes = new byte[tempBuffer.remaining()];
        tempBuffer.get(tagBytes);
        
        try {
            return Integer.parseInt(new String(tagBytes));
        } catch (NumberFormatException e) {
            throw new FixParseException("Invalid tag format: " + new String(tagBytes));
        }
    }
    
    /**
     * Validate checksum of the message
     */
    private boolean validateChecksum(ByteBuffer buffer, int start, int end, int expectedChecksum) {
        int calculatedChecksum = 0;
        
        // Calculate checksum for all bytes except the checksum field itself
        for (int i = start; i < end; i++) {
            byte b = buffer.get(i);
            if (b != FIELD_SEPARATOR) {
                calculatedChecksum += b;
            }
        }
        
        calculatedChecksum %= 256;
        return calculatedChecksum == expectedChecksum;
    }
    
    /**
     * Validate message against FIX dictionary
     */
    private void validateMessage(FixMessage message) throws FixParseException {
        if (dictionary != null) {
            String msgType = message.getMessageType();
            if (!dictionary.isValidMessageType(msgType)) {
                throw new FixParseException("Invalid message type: " + msgType);
            }
            
            // Validate required fields for this message type
            for (int requiredTag : dictionary.getRequiredFields(msgType)) {
                if (!message.hasField(requiredTag)) {
                    throw new FixParseException("Missing required field " + requiredTag + 
                                               " for message type " + msgType);
                }
            }
        }
    }
    
    /**
     * Get the underlying dictionary
     */
    public FixDictionary getDictionary() {
        return dictionary;
    }
    
    /**
     * Check if checksum validation is enabled
     */
    public boolean isChecksumValidationEnabled() {
        return validateChecksum;
    }
    
    /**
     * Check if dictionary validation is enabled
     */
    public boolean isDictionaryValidationEnabled() {
        return validateDictionary;
    }
} 