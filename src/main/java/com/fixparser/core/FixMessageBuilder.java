package com.fixparser.core;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * High-performance FIX message builder using ByteBuffer operations.
 * Minimizes object allocation and provides efficient message construction.
 */
public class FixMessageBuilder {
    
    private static final byte FIELD_SEPARATOR = 0x01; // SOH character
    private static final byte EQUALS = 0x3D; // '=' character
    
    // Common FIX field tags
    private static final int TAG_BEGIN_STRING = 8;
    private static final int TAG_BODY_LENGTH = 9;
    private static final int TAG_MSG_TYPE = 35;
    private static final int TAG_CHECKSUM = 10;
    private static final int TAG_SENDER_COMP_ID = 49;
    private static final int TAG_TARGET_COMP_ID = 56;
    private static final int TAG_MSG_SEQ_NUM = 34;
    private static final int TAG_SENDING_TIME = 52;
    
    private final Map<Integer, String> fields;
    private final String beginString;
    private final String senderCompId;
    private final String targetCompId;
    private int msgSeqNum;
    private String messageType;
    
    // Reusable buffers for performance
    private final ByteBuffer tempBuffer;
    private final StringBuilder stringBuilder;
    
    public FixMessageBuilder(String beginString, String senderCompId, String targetCompId) {
        this.beginString = beginString;
        this.senderCompId = senderCompId;
        this.targetCompId = targetCompId;
        this.fields = new LinkedHashMap<>(); // Maintain order
        this.msgSeqNum = 1;
        this.tempBuffer = ByteBuffer.allocate(4096);
        this.stringBuilder = new StringBuilder(1024);
    }
    
    /**
     * Set the message type
     */
    public FixMessageBuilder setMessageType(String messageType) {
        this.messageType = messageType;
        return this;
    }
    
    /**
     * Set the message sequence number
     */
    public FixMessageBuilder setMsgSeqNum(int msgSeqNum) {
        this.msgSeqNum = msgSeqNum;
        return this;
    }
    
    /**
     * Add a field to the message
     */
    public FixMessageBuilder addField(int tag, String value) {
        if (value != null) {
            fields.put(tag, value);
        }
        return this;
    }
    
    /**
     * Add a field with integer value
     */
    public FixMessageBuilder addField(int tag, int value) {
        fields.put(tag, String.valueOf(value));
        return this;
    }
    
    /**
     * Add a field with double value
     */
    public FixMessageBuilder addField(int tag, double value) {
        fields.put(tag, String.valueOf(value));
        return this;
    }
    
    /**
     * Add a field with character value
     */
    public FixMessageBuilder addField(int tag, char value) {
        fields.put(tag, String.valueOf(value));
        return this;
    }
    
    /**
     * Add multiple fields at once
     */
    public FixMessageBuilder addFields(Map<Integer, String> additionalFields) {
        fields.putAll(additionalFields);
        return this;
    }
    
    /**
     * Build the FIX message as a ByteBuffer
     */
    public ByteBuffer build() {
        if (messageType == null) {
            throw new IllegalStateException("Message type must be set");
        }
        
        // Clear reusable buffers
        tempBuffer.clear();
        stringBuilder.setLength(0);
        
        // Build message body first (without header and trailer)
        buildMessageBody();
        
        // Calculate body length
        int bodyLength = stringBuilder.length();
        
        // Build complete message with header and trailer
        return buildCompleteMessage(bodyLength);
    }
    
    /**
     * Build the FIX message as a string
     */
    public String buildString() {
        ByteBuffer buffer = build();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    /**
     * Build the message body (without header and trailer)
     */
    private void buildMessageBody() {
        // Add message type first
        stringBuilder.append("35=").append(messageType).append((char) FIELD_SEPARATOR);
        
        // Add all other fields
        for (Map.Entry<Integer, String> entry : fields.entrySet()) {
            int tag = entry.getKey();
            String value = entry.getValue();
            
            // Skip header fields that will be added later
            if (isHeaderField(tag)) {
                continue;
            }
            
            stringBuilder.append(tag).append("=").append(value).append((char) FIELD_SEPARATOR);
        }
    }
    
    /**
     * Build complete message with header and trailer
     */
    private ByteBuffer buildCompleteMessage(int bodyLength) {
        // Calculate total message size
        int totalSize = calculateTotalSize(bodyLength);
        
        // Ensure buffer capacity
        if (tempBuffer.capacity() < totalSize) {
            tempBuffer.clear();
            // Note: In production, you might want to use a buffer pool
            ByteBuffer newBuffer = ByteBuffer.allocate(totalSize);
            tempBuffer.put(newBuffer);
            tempBuffer.clear();
        }
        
        // Build header
        tempBuffer.put((beginString + (char) FIELD_SEPARATOR).getBytes(StandardCharsets.UTF_8));
        tempBuffer.put(("9=" + bodyLength + (char) FIELD_SEPARATOR).getBytes(StandardCharsets.UTF_8));
        tempBuffer.put(("35=" + messageType + (char) FIELD_SEPARATOR).getBytes(StandardCharsets.UTF_8));
        tempBuffer.put(("49=" + senderCompId + (char) FIELD_SEPARATOR).getBytes(StandardCharsets.UTF_8));
        tempBuffer.put(("56=" + targetCompId + (char) FIELD_SEPARATOR).getBytes(StandardCharsets.UTF_8));
        tempBuffer.put(("34=" + msgSeqNum + (char) FIELD_SEPARATOR).getBytes(StandardCharsets.UTF_8));
        tempBuffer.put(("52=" + getCurrentTimestamp() + (char) FIELD_SEPARATOR).getBytes(StandardCharsets.UTF_8));
        
        // Add body fields
        for (Map.Entry<Integer, String> entry : fields.entrySet()) {
            int tag = entry.getKey();
            String value = entry.getValue();
            
            // Skip header fields
            if (isHeaderField(tag)) {
                continue;
            }
            
            tempBuffer.put((tag + "=" + value + (char) FIELD_SEPARATOR).getBytes(StandardCharsets.UTF_8));
        }
        
        // Calculate and add checksum
        int checksum = calculateChecksum(tempBuffer);
        tempBuffer.put(("10=" + String.format("%03d", checksum) + (char) FIELD_SEPARATOR).getBytes(StandardCharsets.UTF_8));
        
        tempBuffer.flip();
        return tempBuffer;
    }
    
    /**
     * Calculate total message size
     */
    private int calculateTotalSize(int bodyLength) {
        int size = 0;
        
        // Header fields
        size += beginString.length() + 1; // +1 for SOH
        size += String.valueOf(bodyLength).length() + 3; // "9=" + length + SOH
        size += messageType.length() + 3; // "35=" + type + SOH
        size += senderCompId.length() + 3; // "49=" + sender + SOH
        size += targetCompId.length() + 3; // "56=" + target + SOH
        size += String.valueOf(msgSeqNum).length() + 3; // "34=" + seq + SOH
        size += 20; // Approximate timestamp length "52=" + timestamp + SOH
        
        // Body fields
        for (Map.Entry<Integer, String> entry : fields.entrySet()) {
            if (!isHeaderField(entry.getKey())) {
                size += String.valueOf(entry.getKey()).length() + entry.getValue().length() + 2; // tag=value+SOH
            }
        }
        
        // Checksum
        size += 6; // "10=xxx" + SOH
        
        return size;
    }
    
    /**
     * Check if a field is a header field
     */
    private boolean isHeaderField(int tag) {
        return tag == TAG_BEGIN_STRING || tag == TAG_BODY_LENGTH || tag == TAG_MSG_TYPE ||
               tag == TAG_SENDER_COMP_ID || tag == TAG_TARGET_COMP_ID || 
               tag == TAG_MSG_SEQ_NUM || tag == TAG_SENDING_TIME || tag == TAG_CHECKSUM;
    }
    
    /**
     * Calculate checksum for the message
     */
    private int calculateChecksum(ByteBuffer buffer) {
        int checksum = 0;
        int position = buffer.position();
        
        buffer.rewind();
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            if (b != FIELD_SEPARATOR) {
                checksum += b;
            }
        }
        
        buffer.position(position);
        return checksum % 256;
    }
    
    /**
     * Get current timestamp in FIX format
     */
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS"));
    }
    
    /**
     * Create a new order message
     */
    public static FixMessageBuilder newOrder(String symbol, char side, double quantity, char orderType) {
        return new FixMessageBuilder("FIX.4.4", "CLIENT", "SERVER")
                .setMessageType("D")
                .addField(55, symbol) // Symbol
                .addField(54, side) // Side
                .addField(38, quantity) // OrderQty
                .addField(40, orderType) // OrdType
                .addField(21, '1') // HandlInst
                .addField(60, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS"))); // TransactTime
    }
    
    /**
     * Create a heartbeat message
     */
    public static FixMessageBuilder heartbeat() {
        return new FixMessageBuilder("FIX.4.4", "CLIENT", "SERVER")
                .setMessageType("0");
    }
    
    /**
     * Create a logon message
     */
    public static FixMessageBuilder logon() {
        return new FixMessageBuilder("FIX.4.4", "CLIENT", "SERVER")
                .setMessageType("A")
                .addField(98, "0") // EncryptMethod
                .addField(108, "30") // HeartBtInt
                .addField(141, "Y"); // ResetSeqNumFlag
    }
    
    /**
     * Create a logout message
     */
    public static FixMessageBuilder logout(String reason) {
        return new FixMessageBuilder("FIX.4.4", "CLIENT", "SERVER")
                .setMessageType("5")
                .addField(58, reason != null ? reason : "User requested");
    }
    
    /**
     * Clear all fields and reset builder
     */
    public FixMessageBuilder clear() {
        fields.clear();
        messageType = null;
        return this;
    }
    
    /**
     * Get current field count
     */
    public int getFieldCount() {
        return fields.size();
    }
    
    /**
     * Check if a field exists
     */
    public boolean hasField(int tag) {
        return fields.containsKey(tag);
    }
    
    /**
     * Get field value
     */
    public String getField(int tag) {
        return fields.get(tag);
    }
} 