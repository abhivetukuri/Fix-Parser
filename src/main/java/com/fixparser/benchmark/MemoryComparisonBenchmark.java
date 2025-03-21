package com.fixparser.benchmark;

import com.fixparser.core.FixMessage;
import com.fixparser.core.FixMessageBuilder;
import com.fixparser.core.FixParser;
import com.fixparser.core.FixParseException;
import com.fixparser.dictionary.FixDictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MemoryComparisonBenchmark {
    
    private static final int TEST_MESSAGES = 10000;
    private static final int GC_ITERATIONS = 100000;
    
    public static void main(String[] args) throws FixParseException {
        System.out.println("=== Memory Comparison Benchmark ===\n");
        
        MemoryComparisonBenchmark benchmark = new MemoryComparisonBenchmark();
        
        benchmark.runByteBufferParserMemoryTest();
        benchmark.runSimulatedStringParserMemoryTest();
        benchmark.runGCImpactTest();
        
        System.out.println("\n=== Memory Comparison Complete ===");
    }
    
    private void runByteBufferParserMemoryTest() throws FixParseException {
        System.out.println("=== ByteBuffer-based Parser Memory Test ===");
        
        FixParser parser = new FixParser(new FixDictionary(), true, true);
        List<String> testMessages = generateTestMessages();
        
        Runtime runtime = Runtime.getRuntime();
        
        System.gc();
        Thread.yield();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
        
        List<FixMessage> parsedMessages = new ArrayList<>();
        for (String message : testMessages) {
            FixMessage parsed = parser.parse(message);
            parsedMessages.add(parsed);
        }
        
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = afterMemory - beforeMemory;
        
        System.out.printf("Messages parsed: %,d%n", TEST_MESSAGES);
        System.out.printf("Memory used: %.2f MB%n", memoryUsed / 1_048_576.0);
        System.out.printf("Memory per message: %.0f bytes%n", (double) memoryUsed / TEST_MESSAGES);
        System.out.println();
        
        parsedMessages.clear();
    }
    
    private void runSimulatedStringParserMemoryTest() {
        System.out.println("=== Simulated String-based Parser Memory Test ===");
        
        List<String> testMessages = generateTestMessages();
        
        Runtime runtime = Runtime.getRuntime();
        
        System.gc();
        Thread.yield();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
        
        List<Map<String, String>> parsedMessages = new ArrayList<>();
        for (String message : testMessages) {
            Map<String, String> parsed = simulateStringBasedParsing(message);
            parsedMessages.add(parsed);
        }
        
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = afterMemory - beforeMemory;
        
        System.out.printf("Messages parsed: %,d%n", TEST_MESSAGES);
        System.out.printf("Memory used: %.2f MB%n", memoryUsed / 1_048_576.0);
        System.out.printf("Memory per message: %.0f bytes%n", (double) memoryUsed / TEST_MESSAGES);
        System.out.println();
        
        parsedMessages.clear();
    }
    
    private void runGCImpactTest() throws FixParseException {
        System.out.println("=== GC Impact Comparison ===");
        
        FixParser parser = new FixParser(new FixDictionary(), false, false);
        List<String> testMessages = generateTestMessages();
        
        Runtime runtime = Runtime.getRuntime();
        
        long byteBufferGCTime = measureGCTime(() -> {
            try {
                for (int i = 0; i < GC_ITERATIONS; i++) {
                    FixMessage message = parser.parse(testMessages.get(i % testMessages.size()));
                    
                    message.getString(35);
                    message.getString(49);
                    message.getString(56);
                    
                    if (i % 1000 == 0) {
                        Thread.yield();
                    }
                }
            } catch (FixParseException e) {
                throw new RuntimeException(e);
            }
        });
        
        long stringBasedGCTime = measureGCTime(() -> {
            for (int i = 0; i < GC_ITERATIONS; i++) {
                Map<String, String> message = simulateStringBasedParsing(testMessages.get(i % testMessages.size()));
                
                message.get("35");
                message.get("49");
                message.get("56");
                
                if (i % 1000 == 0) {
                    Thread.yield();
                }
            }
        });
        
        System.out.printf("ByteBuffer parser GC time: %dms%n", byteBufferGCTime);
        System.out.printf("String-based parser GC time: %dms%n", stringBasedGCTime);
        System.out.printf("GC time reduction: %.1f%%%n", 
            ((double)(stringBasedGCTime - byteBufferGCTime) / stringBasedGCTime) * 100);
        System.out.println();
    }
    
    private long measureGCTime(Runnable task) {
        Runtime runtime = Runtime.getRuntime();
        
        System.gc();
        Thread.yield();
        
        long beforeGC = getTotalGCTime();
        
        task.run();
        
        System.gc();
        Thread.yield();
        
        long afterGC = getTotalGCTime();
        
        return afterGC - beforeGC;
    }
    
    private long getTotalGCTime() {
        return java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()
            .stream()
            .mapToLong(gcBean -> gcBean.getCollectionTime())
            .sum();
    }
    
    private Map<String, String> simulateStringBasedParsing(String message) {
        Map<String, String> fields = new HashMap<>();
        
        String[] pairs = message.split("\u0001");
        for (String pair : pairs) {
            if (pair.contains("=")) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    fields.put(keyValue[0], keyValue[1]);
                }
            }
        }
        
        return fields;
    }
    
    private List<String> generateTestMessages() {
        List<String> messages = new ArrayList<>();
        Random random = new Random(42);
        
        String[] symbols = {"AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", "META", "NVDA", "NFLX"};
        char[] sides = {'1', '2'};
        char[] orderTypes = {'1', '2', '3'};
        
        for (int i = 0; i < TEST_MESSAGES; i++) {
            String symbol = symbols[random.nextInt(symbols.length)];
            char side = sides[random.nextInt(sides.length)];
            char orderType = orderTypes[random.nextInt(orderTypes.length)];
            double quantity = 100 + random.nextDouble() * 900;
            double price = 50 + random.nextDouble() * 450;
            
            if (i % 3 == 0) {
                messages.add(FixMessageBuilder.heartbeat()
                    .setMsgSeqNum(i + 1)
                    .buildString());
            } else if (i % 3 == 1) {
                messages.add(FixMessageBuilder.newOrder(symbol, side, quantity, orderType)
                    .setMsgSeqNum(i + 1)
                    .addField(44, price)
                    .buildString());
            } else {
                messages.add(FixMessageBuilder.logon()
                    .setMsgSeqNum(i + 1)
                    .buildString());
            }
        }
        
        return messages;
    }
}