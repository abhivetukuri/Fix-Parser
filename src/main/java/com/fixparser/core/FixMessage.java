package com.fixparser.core;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a FIX message with zero-copy field access using ByteBuffer views.
 * This class minimizes object allocation by reusing ByteBuffer slices.
 */
public class FixMessage {
    
    private final ByteBuffer originalBuffer;
    private final Map<Integer, FieldInfo> fields;
    private final int messageLength;
    private final int checksum;
    private final String messageType;
    
    /**
     * Internal class to store field information with ByteBuffer views
     */
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
    
    /**
     * Get a field by tag number with zero-copy access
     */
    public FieldInfo getField(int tag) {
        return fields.get(tag);
    }
    
    /**
     * Get field value as string with zero-copy access
     */
    public String getString(int tag) {
        FieldInfo field = fields.get(tag);
        return field != null ? field.getValueAsString() : null;
    }
    
    /**
     * Get field value as integer
     */
    public Integer getInt(int tag) {
        FieldInfo field = fields.get(tag);
        return field != null ? field.getValueAsInt() : null;
    }
    
    /**
     * Get field value as double
     */
    public Double getDouble(int tag) {
        FieldInfo field = fields.get(tag);
        return field != null ? field.getValueAsDouble() : null;
    }
    
    /**
     * Check if field exists
     */
    public boolean hasField(int tag) {
        return fields.containsKey(tag);
    }
    
    /**
     * Get all fields
     */
    public Map<Integer, FieldInfo> getAllFields() {
        return new HashMap<>(fields);
    }
    
    /**
     * Get the message type (tag 35)
     */
    public String getMessageType() {
        return messageType;
    }
    
    /**
     * Get the calculated checksum
     */
    public int getChecksum() {
        return checksum;
    }
    
    /**
     * Get the message length
     */
    public int getMessageLength() {
        return messageLength;
    }
    
    /**
     * Get the original ByteBuffer
     */
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