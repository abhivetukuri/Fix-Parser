package com.fixparser.core;

/**
 * Exception thrown when FIX message parsing fails.
 * Provides detailed error information for debugging.
 */
public class FixParseException extends Exception {
    
    private final String message;
    private final int position;
    private final String field;
    
    public FixParseException(String message) {
        this(message, null, -1, null);
    }
    
    public FixParseException(String message, Throwable cause) {
        this(message, cause, -1, null);
    }
    
    public FixParseException(String message, int position) {
        this(message, null, position, null);
    }
    
    public FixParseException(String message, int position, String field) {
        this(message, null, position, field);
    }
    
    public FixParseException(String message, Throwable cause, int position, String field) {
        super(message, cause);
        this.message = message;
        this.position = position;
        this.field = field;
    }
    
    /**
     * Get the error message
     */
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("FIX Parse Error: ").append(message);
        
        if (position >= 0) {
            sb.append(" at position ").append(position);
        }
        
        if (field != null) {
            sb.append(" in field ").append(field);
        }
        
        return sb.toString();
    }
    
    /**
     * Get the position where the error occurred
     */
    public int getPosition() {
        return position;
    }
    
    /**
     * Get the field where the error occurred
     */
    public String getField() {
        return field;
    }
} 