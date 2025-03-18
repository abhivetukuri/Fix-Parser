package com.fixparser.core;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class FixMessageBuilder {
    
    private static final byte FIELD_SEPARATOR = 0x01; 
    private static final byte EQUALS = 0x3D; 
    
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
    
    private final ByteBuffer tempBuffer;
    private final StringBuilder stringBuilder;
    
    public FixMessageBuilder(String beginString, String senderCompId, String targetCompId) {
        this.beginString = beginString;
        this.senderCompId = senderCompId;
        this.targetCompId = targetCompId;
        this.fields = new LinkedHashMap<>(); 
        this.msgSeqNum = 1;
        this.tempBuffer = ByteBuffer.allocate(4096);
        this.stringBuilder = new StringBuilder(1024);
    }
    

    public FixMessageBuilder setMessageType(String messageType) {
        this.messageType = messageType;
        return this;
    }
    

    public FixMessageBuilder setMsgSeqNum(int msgSeqNum) {
        this.msgSeqNum = msgSeqNum;
        return this;
    }
    

    public FixMessageBuilder addField(int tag, String value) {
        if (value != null) {
            fields.put(tag, value);
        }
        return this;
    }
    

    public FixMessageBuilder addField(int tag, int value) {
        fields.put(tag, String.valueOf(value));
        return this;
    }
    

    public FixMessageBuilder addField(int tag, double value) {
        fields.put(tag, String.valueOf(value));
        return this;
    }
    

    public FixMessageBuilder addField(int tag, char value) {
        fields.put(tag, String.valueOf(value));
        return this;
    }
    

    public FixMessageBuilder addFields(Map<Integer, String> additionalFields) {
        fields.putAll(additionalFields);
        return this;
    }
    

    public ByteBuffer build() {
        if (messageType == null) {
            throw new IllegalStateException("Message type must be set");
        }
        
        tempBuffer.clear();
        stringBuilder.setLength(0);

        buildMessageBody();
        
        int bodyLength = stringBuilder.length();
        
        return buildCompleteMessage(bodyLength);
    }
    

    public String buildString() {
        ByteBuffer buffer = build();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    

    private void buildMessageBody() {
        stringBuilder.append("35=").append(messageType).append((char) FIELD_SEPARATOR);
        stringBuilder.append("49=").append(senderCompId).append((char) FIELD_SEPARATOR);
        stringBuilder.append("56=").append(targetCompId).append((char) FIELD_SEPARATOR);
        stringBuilder.append("34=").append(msgSeqNum).append((char) FIELD_SEPARATOR);
        stringBuilder.append("52=").append(getCurrentTimestamp()).append((char) FIELD_SEPARATOR);
        
        for (Map.Entry<Integer, String> entry : fields.entrySet()) {
            int tag = entry.getKey();
            String value = entry.getValue();
            
            if (isHeaderField(tag)) {
                continue;
            }
            
            stringBuilder.append(tag).append("=").append(value).append((char) FIELD_SEPARATOR);
        }
    }
    

    private ByteBuffer buildCompleteMessage(int bodyLength) {
        StringBuilder fullMessage = new StringBuilder();
        
        fullMessage.append("8=").append(beginString).append((char) FIELD_SEPARATOR);
        fullMessage.append("9=").append(bodyLength).append((char) FIELD_SEPARATOR);
        fullMessage.append(stringBuilder);
        
        int checksum = calculateChecksum(fullMessage.toString());
        fullMessage.append("10=").append(String.format("%03d", checksum)).append((char) FIELD_SEPARATOR);
        
        byte[] messageBytes = fullMessage.toString().getBytes(StandardCharsets.UTF_8);
        tempBuffer.clear();
        if (tempBuffer.capacity() < messageBytes.length) {
            return ByteBuffer.wrap(messageBytes);
        }
        
        tempBuffer.put(messageBytes);
        tempBuffer.flip();
        return tempBuffer;
    }
    

    private int calculateTotalSize(int bodyLength) {
        int size = 0;
        
        size += beginString.length() + 1; 
        size += String.valueOf(bodyLength).length() + 3; 
        size += messageType.length() + 3; 
        size += senderCompId.length() + 3; 
        size += targetCompId.length() + 3; 
        size += String.valueOf(msgSeqNum).length() + 3; 
        size += 20; 

        for (Map.Entry<Integer, String> entry : fields.entrySet()) {
            if (!isHeaderField(entry.getKey())) {
                size += String.valueOf(entry.getKey()).length() + entry.getValue().length() + 2; 
            }
        }
        
        size += 6; 
        
        return size;
    }
    

    private boolean isHeaderField(int tag) {
        return tag == TAG_BEGIN_STRING || tag == TAG_BODY_LENGTH || tag == TAG_MSG_TYPE ||
               tag == TAG_SENDER_COMP_ID || tag == TAG_TARGET_COMP_ID || 
               tag == TAG_MSG_SEQ_NUM || tag == TAG_SENDING_TIME || tag == TAG_CHECKSUM;
    }
    

    private int calculateChecksum(String message) {
        int checksum = 0;
        for (byte b : message.getBytes(StandardCharsets.UTF_8)) {
            checksum += (b & 0xFF);
        }
        return checksum % 256;
    }
    

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS"));
    }
    

    public static FixMessageBuilder newOrder(String symbol, char side, double quantity, char orderType) {
        return new FixMessageBuilder("FIX.4.4", "CLIENT", "SERVER")
                .setMessageType("D")
                .addField(55, symbol) 
                .addField(54, side) 
                .addField(38, quantity) 
                .addField(40, orderType) 
                .addField(21, '1') 
                .addField(60, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS"))); 
    }
    

    public static FixMessageBuilder heartbeat() {
        return new FixMessageBuilder("FIX.4.4", "CLIENT", "SERVER")
                .setMessageType("0");
    }
    

    public static FixMessageBuilder logon() {
        return new FixMessageBuilder("FIX.4.4", "CLIENT", "SERVER")
                .setMessageType("A")
                .addField(98, "0") 
                .addField(108, "30") 
                .addField(141, "Y"); 
    }
    

    public static FixMessageBuilder logout(String reason) {
        return new FixMessageBuilder("FIX.4.4", "CLIENT", "SERVER")
                .setMessageType("5")
                .addField(58, reason != null ? reason : "User requested");
    }
    

    public FixMessageBuilder clear() {
        fields.clear();
        messageType = null;
        return this;
    }
    

    public int getFieldCount() {
        return fields.size();
    }
    

    public boolean hasField(int tag) {
        return fields.containsKey(tag);
    }
    

    public String getField(int tag) {
        return fields.get(tag);
    }
} 