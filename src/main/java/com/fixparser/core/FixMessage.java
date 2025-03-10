package com.fixparser.core;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class FixMessage {
    
    private final ByteBuffer originalBuffer;
    private final Map<Integer, FieldInfo> fields;
    private final int messageLength;
    private final int checksum;
    private final String messageType;
    
    
    public static class FieldInfo {
        private final ByteBuffer valueBuffer;
        private final int tag;
        private final int valueStart;
        private final int valueLength;
        
        public FieldInfo(ByteBuffer valueBuffer, int tag, int valueStart, int valueLength) {
            this.valueBuffer = valueBuffer;
            this.tag = tag;
            this.valueStart = valueStart;
            this.valueLength = valueLength;
        }
        
        public ByteBuffer getValueBuffer() {
            return valueBuffer;
        }
        
        public int getTag() {
            return tag;
        }
        
        public int getValueStart() {
            return valueStart;
        }
        
        public int getValueLength() {
            return valueLength;
        }
        
        public String getValueAsString() {
            byte[] bytes = new byte[valueLength];
            valueBuffer.position(valueStart);
            valueBuffer.get(bytes);
            return new String(bytes);
        }
        
        public int getValueAsInt() {
            return Integer.parseInt(getValueAsString());
        }
        
        public double getValueAsDouble() {
            return Double.parseDouble(getValueAsString());
        }
    }
    
    public FixMessage(ByteBuffer buffer, Map<Integer, FieldInfo> fields, 
                     int messageLength, int checksum, String messageType) {
        this.originalBuffer = buffer;
        this.fields = fields;
        this.messageLength = messageLength;
        this.checksum = checksum;
        this.messageType = messageType;
    }
    
    
    public FieldInfo getField(int tag) {
        return fields.get(tag);
    }
    
    
    public String getString(int tag) {
        FieldInfo field = fields.get(tag);
        return field != null ? field.getValueAsString() : null;
    }
    
    
    public Integer getInt(int tag) {
        FieldInfo field = fields.get(tag);
        return field != null ? field.getValueAsInt() : null;
    }
    
    
    public Double getDouble(int tag) {
        FieldInfo field = fields.get(tag);
        return field != null ? field.getValueAsDouble() : null;
    }
    
    
    public boolean hasField(int tag) {
        return fields.containsKey(tag);
    }
    
    
    public Map<Integer, FieldInfo> getAllFields() {
        return new HashMap<>(fields);
    }
    
    
    public String getMessageType() {
        return messageType;
    }
    
    
    public int getChecksum() {
        return checksum;
    }
    
    
    public int getMessageLength() {
        return messageLength;
    }
    
            
    public ByteBuffer getOriginalBuffer() {
        return originalBuffer;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FixMessage{type=").append(messageType)
          .append(", length=").append(messageLength)
          .append(", checksum=").append(checksum)
          .append(", fields=[");
        
        fields.values().stream()
              .sorted((a, b) -> Integer.compare(a.getTag(), b.getTag()))
              .forEach(field -> 
                  sb.append(field.getTag()).append("=")
                    .append(field.getValueAsString()).append(" ")
              );
        
        sb.append("]}");
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FixMessage that = (FixMessage) obj;
        return messageLength == that.messageLength &&
               checksum == that.checksum &&
               Objects.equals(messageType, that.messageType) &&
               Objects.equals(fields, that.fields);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(fields, messageLength, checksum, messageType);
    }
} 