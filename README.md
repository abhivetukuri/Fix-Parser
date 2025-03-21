# FIX Parser

A high-performance FIX (Financial Information eXchange) 4.4 protocol parser built in Java with ByteBuffer optimization and zero-copy parsing techniques.

## Features

### Performance Optimizations
- **Zero-copy parsing** using ByteBuffer operations
- **Minimal GC overhead** through reusable buffers and object pooling
- **High-throughput** message processing (100K+ messages/second)
- **Memory-efficient** field access using ByteBuffer views

### Core Functionality
- **FIX 4.4 protocol support** with comprehensive message type validation
- **ByteBuffer-based parsing** for optimal performance
- **Checksum verification** with automatic validation
- **Data dictionary validation** against FIX 4.4 specification
- **Flexible input formats** (String, byte[], ByteBuffer)

### Validation & Error Handling
- **Message type validation** against FIX 4.4 data dictionary
- **Required field validation** for each message type
- **Field value validation** with type checking
- **Comprehensive error reporting** with detailed exception information
- **Buffer state preservation** on parsing errors

### Message Building
- **Fluent API** for easy message construction
- **Static factory methods** for common message types
- **Automatic checksum calculation** and header generation
- **Type-safe field addition** with overloaded methods

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>com.fixparser</groupId>
    <artifactId>fix-parser</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Basic Usage

```java
import com.fixparser.core.*;

FixParser parser = new FixParser();

String fixMessage = "8=FIX.4.4\u00019=20\u000135=0\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000110=123\u0001";

try {
    FixMessage message = parser.parse(fixMessage);
    
    String messageType = message.getMessageType(); // "0"
    String sender = message.getString(49);        // "CLIENT"
    String target = message.getString(56);        // "SERVER"
    int sequence = message.getInt(34);            // 1
    int checksum = message.getChecksum();         // 123
    
} catch (FixParseException e) {
    System.err.println("Parse error: " + e.getMessage());
}
```

### Message Building

```java
String heartbeat = FixMessageBuilder.heartbeat()
    .setMsgSeqNum(42)
    .buildString();

String newOrder = FixMessageBuilder.newOrder("AAPL", '1', 100.0, '2')
    .setMsgSeqNum(10)
    .addField(60, "20231201-10:30:00.000") 
    .buildString();

String custom = new FixMessageBuilder("FIX.4.4", "CLIENT", "SERVER")
    .setMessageType("V") // Market Data Request
    .setMsgSeqNum(100)
    .addField(262, "MDREQ001") // MDReqID
    .addField(263, "1")        // SubscriptionRequestType
    .addField(264, "1")        // MarketDepth
    .buildString();
```

### ByteBuffer Operations

```java
ByteBuffer buffer = ByteBuffer.wrap(fixMessage.getBytes());
FixMessage message = parser.parse(buffer);

FixMessage.FieldInfo fieldInfo = message.getField(55); // Symbol
if (fieldInfo != null) {
    String symbol = fieldInfo.getValueAsString();
    ByteBuffer valueBuffer = fieldInfo.getValueBuffer();
    int valueStart = fieldInfo.getValueStart();
    int valueLength = fieldInfo.getValueLength();
}

String message1 = "8=FIX.4.4\u00019=20\u000135=0\u000149=CLIENT\u000156=SERVER\u000134=1\u000152=20231201-10:30:00.000\u000110=123\u0001";
String message2 = "8=FIX.4.4\u00019=20\u000135=0\u000149=CLIENT\u000156=SERVER\u000134=2\u000152=20231201-10:30:01.000\u000110=124\u0001";
String combined = message1 + message2;

ByteBuffer buffer = ByteBuffer.wrap(combined.getBytes());
FixMessage msg1 = parser.parse(buffer);
FixMessage msg2 = parser.parse(buffer);
```

## Architecture

### Core Components

1. **FixParser** - Main parsing engine with ByteBuffer optimization
2. **FixMessage** - Immutable message representation with zero-copy field access
3. **FixMessageBuilder** - Fluent API for message construction
4. **FixDictionary** - FIX 4.4 data dictionary for validation
5. **FixParseException** - Detailed exception handling

### Performance Characteristics

- **Zero-copy parsing**: Uses ByteBuffer views instead of String copies
- **Reusable buffers**: Minimizes object allocation
- **Field caching**: Efficient field lookup and access
- **Memory efficiency**: ~29% less memory usage compared to string-based parsers
- **High throughput**: 411K+ messages/second on modern hardware

### Memory Management

- **ByteBuffer pooling**: Reuses buffers to reduce GC pressure
- **Field view optimization**: Direct ByteBuffer access without copying
- **Minimal object creation**: Reuses objects where possible
- **Efficient string handling**: Lazy string conversion only when needed

## Advanced Usage

### Custom Validation

```java
FixParser fastParser = new FixParser(new FixDictionary(), false, false);

FixParser customParser = new FixParser(
    new FixDictionary(), 
    true,                 
    false                 
);
```

### Dictionary Validation

```java
FixDictionary dictionary = new FixDictionary();

boolean isValid = dictionary.isValidMessageType("D"); 

Set<Integer> requiredFields = dictionary.getRequiredFields("D");
// Returns: [8, 9, 35, 49, 56, 34, 52, 11, 21, 55, 54, 60, 10]

boolean isValidField = dictionary.validateFieldValue(34, "123"); // true
```

### Error Handling

```java
try {
    FixMessage message = parser.parse(invalidMessage);
} catch (FixParseException e) {
    System.err.println("Parse error: " + e.getMessage());
    System.err.println("Position: " + e.getPosition());
    System.err.println("Field: " + e.getField());
}
```

## Performance Benchmarks

### Throughput Test (with validation)
```
Messages parsed: 100,000
Time taken: 243ms
Messages per second: 411,560
Average time per message: 2.4μs
```

### Field Access Performance
```
Field accesses: 500,000
Time taken: 53ms
Field accesses per second: 9,481,939
```

### Message Building Performance
```
Messages built: 25,000
Time taken: 116ms
Messages per second: 216,292
```

### Multiple Message Buffer Parsing
```
Messages parsed from buffer: 1,000
Time taken: 2.0ms
Messages per second: 488,182
```

### Memory Usage Comparison
- **Traditional String-based parser**: ~19.0MB for 10K messages (1,990 bytes/message)
- **This ByteBuffer parser**: ~13.6MB for 10K messages (1,421 bytes/message)
- **Memory reduction**: ~28.6%

### GC Impact
- **ByteBuffer parser GC time**: 14ms for 100K operations
- **String-based parser GC time**: 17ms for 100K operations
- **GC time reduction**: ~17.6%

## Supported Message Types

### Session Layer
- `0` - Heartbeat
- `1` - Test Request
- `2` - Resend Request
- `3` - Reject
- `4` - Sequence Reset
- `5` - Logout
- `A` - Logon

### Application Layer
- `D` - New Order Single
- `E` - New Order List
- `F` - Order Cancel Request
- `G` - Order Cancel Replace Request
- `H` - Order Status Request
- `8` - Execution Report
- `9` - Order Cancel Reject

### Market Data
- `V` - Market Data Request
- `W` - Market Data Snapshot Full Refresh
- `X` - Market Data Incremental Refresh
- `Y` - Market Data Request Reject

## Building and Testing

### Prerequisites
- Java 11 or higher
- Maven 3.6 or higher

### Build
```bash
mvn clean compile
```

### Test
```bash
mvn test
```

### Run Example
```bash
mvn exec:java -Dexec.mainClass="com.fixparser.example.FixParserExample"
```

### Run Benchmarks
```bash
# Main performance benchmark
mvn exec:java -Dexec.mainClass="com.fixparser.benchmark.FixParserBenchmark"

# Memory comparison benchmark
mvn exec:java -Dexec.mainClass="com.fixparser.benchmark.MemoryComparisonBenchmark"
```

## Acknowledgments

- FIX Protocol specification (FIX 4.4)
- Java NIO ByteBuffer for zero-copy operations
- JUnit 5 for comprehensive testing
