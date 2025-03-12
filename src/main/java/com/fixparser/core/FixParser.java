package com.fixparser.core;

import com.fixparser.dictionary.FixDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;


public class FixParser {
    
    private static final Logger logger = LoggerFactory.getLogger(FixParser.class);
    
    private static final byte FIELD_SEPARATOR = 0x01; 
    private static final byte EQUALS = 0x3D; 
    private static final int MAX_MESSAGE_SIZE = 1024 * 1024; 
    private static final int MIN_MESSAGE_SIZE = 20; 
    
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
    

    public FixMessage parse(ByteBuffer buffer) throws FixParseException {
        if (buffer == null || buffer.remaining() < MIN_MESSAGE_SIZE) {
            throw new FixParseException("Invalid buffer or message too short");
        }
        
        fieldCache.clear();
        
        int startPosition = buffer.position();
        int originalLimit = buffer.limit();
        
        try {
            int messageEnd = findMessageEnd(buffer);
            if (messageEnd == -1) {
                throw new FixParseException("Could not find message end");
            }
            
            buffer.limit(messageEnd);
            
            FixMessage message = parseMessageInternal(buffer);
            
            if (validateChecksum && !validateChecksum(buffer, startPosition, messageEnd, message.getChecksum())) {
                throw new FixParseException("Checksum validation failed");
            }
            
            if (validateDictionary) {
                validateMessage(message);
            }

            buffer.position(messageEnd);
            buffer.limit(originalLimit);
            
            return message;
            
        } catch (Exception e) {
            buffer.position(startPosition);
            buffer.limit(originalLimit);
            throw new FixParseException("Failed to parse FIX message", e);
        }
    }
    

    public FixMessage parse(byte[] data) throws FixParseException {
        return parse(ByteBuffer.wrap(data));
    }
    

    public FixMessage parse(String message) throws FixParseException {
        return parse(message.getBytes());
    }
    

    private int findMessageEnd(ByteBuffer buffer) {
        int position = buffer.position();
        int limit = buffer.limit();
        

        while (position < limit - 7) { 
            if (buffer.get(position) == '1' && buffer.get(position + 1) == '0' && 
                buffer.get(position + 2) == '=') {

                for (int i = position + 3; i < limit; i++) {
                    if (buffer.get(i) == FIELD_SEPARATOR) {
                        return i + 1; 
                    }
                }
                return limit; 
            }
            position++;
        }
        
        return -1; 
    }
    

    private FixMessage parseMessageInternal(ByteBuffer buffer) throws FixParseException {
        int bodyLength = -1;
        String messageType = null;
        int checksum = -1;
        
        int position = buffer.position();
        int limit = buffer.limit();
        

        while (position < limit) {

            int fieldStart = position;
            int equalsPos = -1;
            

            while (position < limit && buffer.get(position) != EQUALS) {
                position++;
            }
            
            if (position >= limit) {
                break;
            }
            
            equalsPos = position;
                    position++; 
            

            int valueStart = position;
            while (position < limit && buffer.get(position) != FIELD_SEPARATOR) {
                position++;
            }
            
            int valueEnd = position;
            position++; 
            
            int tag = parseTag(buffer, fieldStart, equalsPos);
            
            FixMessage.FieldInfo fieldInfo = new FixMessage.FieldInfo(
                buffer, tag, valueStart, valueEnd - valueStart
            );
            
            fieldCache.put(tag, fieldInfo);
            
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
    
    private int parseTag(ByteBuffer buffer, int start, int end) throws FixParseException {
        tempBuffer.clear();
        tempBuffer.limit(end - start);
        
        buffer.position(start);
        buffer.limit(end);
        tempBuffer.put(buffer);
        
        buffer.limit(buffer.capacity());
        
        tempBuffer.flip();
        byte[] tagBytes = new byte[tempBuffer.remaining()];
        tempBuffer.get(tagBytes);
        
        try {
            return Integer.parseInt(new String(tagBytes));
        } catch (NumberFormatException e) {
            throw new FixParseException("Invalid tag format: " + new String(tagBytes));
        }
    }
    
    private boolean validateChecksum(ByteBuffer buffer, int start, int end, int expectedChecksum) {
        int calculatedChecksum = 0;
        
        for (int i = start; i < end; i++) {
            byte b = buffer.get(i);
            if (b != FIELD_SEPARATOR) {
                calculatedChecksum += b;
            }
        }
        
        calculatedChecksum %= 256;
        return calculatedChecksum == expectedChecksum;
    }
    
    private void validateMessage(FixMessage message) throws FixParseException {
        if (dictionary != null) {
            String msgType = message.getMessageType();
            if (!dictionary.isValidMessageType(msgType)) {
                throw new FixParseException("Invalid message type: " + msgType);
            }

            for (int requiredTag : dictionary.getRequiredFields(msgType)) {
                if (!message.hasField(requiredTag)) {
                    throw new FixParseException("Missing required field " + requiredTag + 
                                               " for message type " + msgType);
                }
            }
        }
    }
    

    public FixDictionary getDictionary() {
        return dictionary;
    }
    

    public boolean isChecksumValidationEnabled() {
        return validateChecksum;
    }
    

    public boolean isDictionaryValidationEnabled() {
        return validateDictionary;
    }
} 