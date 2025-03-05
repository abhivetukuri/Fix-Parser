package com.fixparser.dictionary;

import java.util.*;

/**
 * FIX 4.4 Data Dictionary implementation for message validation.
 * Provides validation of message types and required fields according to FIX 4.4 specification.
 */
public class FixDictionary {
    
    // FIX 4.4 message types
    private static final Set<String> VALID_MESSAGE_TYPES = new HashSet<>(Arrays.asList(
        // Session layer messages
        "0", "1", "2", "3", "4", "5", "A", "D", "0", "1", "2", "3", "4", "5", "A", "D",
        
        // Application messages
        "6", "7", "8", "9", "B", "C", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N",
        "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
        
        // Market data messages
        "W", "X", "Y", "Z", "V", "W", "X", "Y", "Z",
        
        // Trade messages
        "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "P", "Q", "R", "S", "T", "U"
    ));
    
    // Required fields for common message types (FIX 4.4)
    private static final Map<String, Set<Integer>> REQUIRED_FIELDS = new HashMap<>();
    
    static {
        // Logon message (A)
        REQUIRED_FIELDS.put("A", new HashSet<>(Arrays.asList(8, 9, 35, 49, 56, 34, 52, 10)));
        
        // Logout message (5)
        REQUIRED_FIELDS.put("5", new HashSet<>(Arrays.asList(8, 9, 35, 49, 56, 34, 52, 10)));
        
        // Heartbeat message (0)
        REQUIRED_FIELDS.put("0", new HashSet<>(Arrays.asList(8, 9, 35, 49, 56, 34, 52, 10)));
        
        // Test Request message (1)
        REQUIRED_FIELDS.put("1", new HashSet<>(Arrays.asList(8, 9, 35, 49, 56, 34, 52, 112, 10)));
        
        // Resend Request message (2)
        REQUIRED_FIELDS.put("2", new HashSet<>(Arrays.asList(8, 9, 35, 49, 56, 34, 52, 7, 16, 10)));
        
        // Reject message (3)
        REQUIRED_FIELDS.put("3", new HashSet<>(Arrays.asList(8, 9, 35, 49, 56, 34, 52, 45, 58, 10)));
        
        // Sequence Reset message (4)
        REQUIRED_FIELDS.put("4", new HashSet<>(Arrays.asList(8, 9, 35, 49, 56, 34, 52, 36, 10)));
        
        // New Order Single message (D)
        REQUIRED_FIELDS.put("D", new HashSet<>(Arrays.asList(8, 9, 35, 49, 56, 34, 52, 11, 21, 55, 54, 60, 10)));
        
        // Order Cancel Request message (F)
        REQUIRED_FIELDS.put("F", new HashSet<>(Arrays.asList(8, 9, 35, 49, 56, 34, 52, 11, 21, 41, 55, 54, 60, 10)));
        
        // Order Cancel Replace Request message (G)
        REQUIRED_FIELDS.put("G", new HashSet<>(Arrays.asList(8, 9, 35, 49, 56, 34, 52, 11, 21, 41, 55, 54, 60, 10)));
        
        // Order Status Request message (H)
        REQUIRED_FIELDS.put("H", new HashSet<>(Arrays.asList(8, 9, 35, 49, 56, 34, 52, 11, 21, 55, 54, 60, 10)));
        
        // Execution Report message (8)
        REQUIRED_FIELDS.put("8", new HashSet<>(Arrays.asList(8, 9, 35, 49, 56, 34, 52, 6, 11, 14, 17, 20, 31, 32, 37, 38, 39, 40, 54, 55, 60, 10)));
        
        // Order Cancel Reject message (9)
        REQUIRED_FIELDS.put("9", new HashSet<>(Arrays.asList(8, 9, 35, 49, 56, 34, 52, 11, 37, 39, 434, 10)));
        
        // Market Data Request message (V)
        REQUIRED_FIELDS.put("V", new HashSet<>(Arrays.asList(8, 9, 35, 49, 56, 34, 52, 262, 263, 264, 265, 267, 269, 10)));
        
        // Market Data Snapshot Full Refresh message (W)
        REQUIRED_FIELDS.put("W", new HashSet<>(Arrays.asList(8, 9, 35, 49, 56, 34, 52, 262, 268, 10)));
        
        // Market Data Incremental Refresh message (X)
        REQUIRED_FIELDS.put("X", new HashSet<>(Arrays.asList(8, 9, 35, 49, 56, 34, 52, 262, 268, 10)));
        
        // Market Data Request Reject message (Y)
        REQUIRED_FIELDS.put("Y", new HashSet<>(Arrays.asList(8, 9, 35, 49, 56, 34, 52, 262, 58, 10)));
    }
    
    // Field definitions for validation
    private static final Map<Integer, FieldDefinition> FIELD_DEFINITIONS = new HashMap<>();
    
    static {
        // Common header fields
        FIELD_DEFINITIONS.put(8, new FieldDefinition(8, "BeginString", FieldType.STRING, true));
        FIELD_DEFINITIONS.put(9, new FieldDefinition(9, "BodyLength", FieldType.LENGTH, true));
        FIELD_DEFINITIONS.put(35, new FieldDefinition(35, "MsgType", FieldType.STRING, true));
        FIELD_DEFINITIONS.put(49, new FieldDefinition(49, "SenderCompID", FieldType.STRING, true));
        FIELD_DEFINITIONS.put(56, new FieldDefinition(56, "TargetCompID", FieldType.STRING, true));
        FIELD_DEFINITIONS.put(34, new FieldDefinition(34, "MsgSeqNum", FieldType.SEQNUM, true));
        FIELD_DEFINITIONS.put(52, new FieldDefinition(52, "SendingTime", FieldType.UTCTIMESTAMP, true));
        FIELD_DEFINITIONS.put(10, new FieldDefinition(10, "CheckSum", FieldType.STRING, true));
        
        // Common body fields
        FIELD_DEFINITIONS.put(11, new FieldDefinition(11, "ClOrdID", FieldType.STRING, false));
        FIELD_DEFINITIONS.put(21, new FieldDefinition(21, "HandlInst", FieldType.CHAR, false));
        FIELD_DEFINITIONS.put(37, new FieldDefinition(37, "OrderID", FieldType.STRING, false));
        FIELD_DEFINITIONS.put(38, new FieldDefinition(38, "OrderQty", FieldType.QTY, false));
        FIELD_DEFINITIONS.put(39, new FieldDefinition(39, "OrdStatus", FieldType.CHAR, false));
        FIELD_DEFINITIONS.put(40, new FieldDefinition(40, "OrdType", FieldType.CHAR, false));
        FIELD_DEFINITIONS.put(41, new FieldDefinition(41, "OrigClOrdID", FieldType.STRING, false));
        FIELD_DEFINITIONS.put(54, new FieldDefinition(54, "Side", FieldType.CHAR, false));
        FIELD_DEFINITIONS.put(55, new FieldDefinition(55, "Symbol", FieldType.STRING, false));
        FIELD_DEFINITIONS.put(60, new FieldDefinition(60, "TransactTime", FieldType.UTCTIMESTAMP, false));
        FIELD_DEFINITIONS.put(112, new FieldDefinition(112, "TestReqID", FieldType.STRING, false));
        FIELD_DEFINITIONS.put(262, new FieldDefinition(262, "MDReqID", FieldType.STRING, false));
        FIELD_DEFINITIONS.put(263, new FieldDefinition(263, "SubscriptionRequestType", FieldType.CHAR, false));
        FIELD_DEFINITIONS.put(264, new FieldDefinition(264, "MarketDepth", FieldType.INT, false));
        FIELD_DEFINITIONS.put(265, new FieldDefinition(265, "MDUpdateType", FieldType.INT, false));
        FIELD_DEFINITIONS.put(267, new FieldDefinition(267, "NoMDEntryTypes", FieldType.INT, false));
        FIELD_DEFINITIONS.put(268, new FieldDefinition(268, "NoMDEntries", FieldType.INT, false));
        FIELD_DEFINITIONS.put(269, new FieldDefinition(269, "MDEntryType", FieldType.CHAR, false));
        FIELD_DEFINITIONS.put(434, new FieldDefinition(434, "CxlRejResponseTo", FieldType.CHAR, false));
    }
    
    /**
     * Check if a message type is valid according to FIX 4.4 specification
     */
    public boolean isValidMessageType(String messageType) {
        return messageType != null && VALID_MESSAGE_TYPES.contains(messageType);
    }
    
    /**
     * Get required fields for a specific message type
     */
    public Set<Integer> getRequiredFields(String messageType) {
        return REQUIRED_FIELDS.getOrDefault(messageType, Collections.emptySet());
    }
    
    /**
     * Get all valid message types
     */
    public Set<String> getValidMessageTypes() {
        return new HashSet<>(VALID_MESSAGE_TYPES);
    }
    
    /**
     * Get field definition by tag
     */
    public FieldDefinition getFieldDefinition(int tag) {
        return FIELD_DEFINITIONS.get(tag);
    }
    
    /**
     * Check if a field is required for a message type
     */
    public boolean isFieldRequired(String messageType, int tag) {
        Set<Integer> required = getRequiredFields(messageType);
        return required.contains(tag);
    }
    
    /**
     * Validate field value against its definition
     */
    public boolean validateFieldValue(int tag, String value) {
        FieldDefinition def = getFieldDefinition(tag);
        if (def == null) {
            return true; // Unknown fields are allowed
        }
        
        return def.validateValue(value);
    }
    
    /**
     * Get all field definitions
     */
    public Map<Integer, FieldDefinition> getAllFieldDefinitions() {
        return new HashMap<>(FIELD_DEFINITIONS);
    }
    
    /**
     * Field definition class
     */
    public static class FieldDefinition {
        private final int tag;
        private final String name;
        private final FieldType type;
        private final boolean required;
        
        public FieldDefinition(int tag, String name, FieldType type, boolean required) {
            this.tag = tag;
            this.name = name;
            this.type = type;
            this.required = required;
        }
        
        public int getTag() {
            return tag;
        }
        
        public String getName() {
            return name;
        }
        
        public FieldType getType() {
            return type;
        }
        
        public boolean isRequired() {
            return required;
        }
        
        public boolean validateValue(String value) {
            if (value == null) {
                return !required;
            }
            
            return type.validate(value);
        }
    }
    
    /**
     * Field types for validation
     */
    public enum FieldType {
        STRING {
            @Override
            public boolean validate(String value) {
                return value != null && !value.isEmpty();
            }
        },
        CHAR {
            @Override
            public boolean validate(String value) {
                return value != null && value.length() == 1;
            }
        },
        INT {
            @Override
            public boolean validate(String value) {
                try {
                    Integer.parseInt(value);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        },
        QTY {
            @Override
            public boolean validate(String value) {
                try {
                    Double.parseDouble(value);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        },
        LENGTH {
            @Override
            public boolean validate(String value) {
                try {
                    int length = Integer.parseInt(value);
                    return length >= 0;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        },
        SEQNUM {
            @Override
            public boolean validate(String value) {
                try {
                    int seqNum = Integer.parseInt(value);
                    return seqNum > 0;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        },
        UTCTIMESTAMP {
            @Override
            public boolean validate(String value) {
                // Basic UTC timestamp validation (YYYYMMDD-HH:MM:SS.sss)
                return value != null && value.matches("\\d{8}-\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?");
            }
        };
        
        public abstract boolean validate(String value);
    }
} 