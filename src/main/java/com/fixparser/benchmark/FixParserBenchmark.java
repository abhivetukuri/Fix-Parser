package com.fixparser.benchmark;

import com.fixparser.core.FixMessage;
import com.fixparser.core.FixMessageBuilder;
import com.fixparser.core.FixParser;
import com.fixparser.core.FixParseException;
import com.fixparser.dictionary.FixDictionary;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FixParserBenchmark {
    
    private static final int WARMUP_ITERATIONS = 10000;
    private static final int BENCHMARK_ITERATIONS = 100000;
    private static final int MEMORY_TEST_MESSAGES = 10000;
    
    private final FixParser parser;
    private final List<String> testMessages;
    private final List<ByteBuffer> testBuffers;
    
    public FixParserBenchmark() {
        this.parser = new FixParser(new FixDictionary(), true, true);
        this.testMessages = generateTestMessages();
        this.testBuffers = generateTestBuffers();
    }
    
    public static void main(String[] args) throws FixParseException {
        System.out.println("=== FIX Parser Benchmark Suite ===\n");
        
        FixParserBenchmark benchmark = new FixParserBenchmark();
        
        benchmark.runThroughputBenchmark();
        benchmark.runMemoryBenchmark();
        benchmark.runFieldAccessBenchmark();
        benchmark.runMessageBuildingBenchmark();
        benchmark.runMultipleMessageBenchmark();
        
        System.out.println("\n=== Benchmark Complete ===");
    }
    
    private List<String> generateTestMessages() {
        List<String> messages = new ArrayList<>();
        Random random = new Random(42);
        
        String[] symbols = {"AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", "META", "NVDA", "NFLX"};
        char[] sides = {'1', '2'};
        char[] orderTypes = {'1', '2', '3'};
        
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
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
    
    private List<ByteBuffer> generateTestBuffers() {
        List<ByteBuffer> buffers = new ArrayList<>();
        for (String message : testMessages) {
            buffers.add(ByteBuffer.wrap(message.getBytes()));
        }
        return buffers;
    }
    
    private void runThroughputBenchmark() throws FixParseException {
        System.out.println("=== Throughput Benchmark ===");
        
        System.out.println("Warming up...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            parser.parse(testMessages.get(i % testMessages.size()));
        }
        
        System.gc();
        
        System.out.println("Running throughput test...");
        long startTime = System.nanoTime();
        
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            FixMessage message = parser.parse(testMessages.get(i));
            
            if (message == null) {
                throw new RuntimeException("Parsed message is null");
            }
        }
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        
        double totalTimeMs = totalTime / 1_000_000.0;
        double messagesPerSecond = (BENCHMARK_ITERATIONS * 1_000_000_000.0) / totalTime;
        double avgTimePerMessageUs = totalTime / (BENCHMARK_ITERATIONS * 1000.0);
        
        System.out.printf("Messages parsed: %,d%n", BENCHMARK_ITERATIONS);
        System.out.printf("Time taken: %.1fms%n", totalTimeMs);
        System.out.printf("Messages per second: %,.0f%n", messagesPerSecond);
        System.out.printf("Average time per message: %.1fμs%n", avgTimePerMessageUs);
        System.out.println();
    }
    
    private void runMemoryBenchmark() throws FixParseException {
        System.out.println("=== Memory Usage Benchmark ===");
        
        Runtime runtime = Runtime.getRuntime();
        
        System.gc();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
        
        List<FixMessage> parsedMessages = new ArrayList<>();
        for (int i = 0; i < MEMORY_TEST_MESSAGES; i++) {
            FixMessage message = parser.parse(testMessages.get(i % testMessages.size()));
            parsedMessages.add(message);
        }
        
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = afterMemory - beforeMemory;
        
        double memoryPerMessage = (double) memoryUsed / MEMORY_TEST_MESSAGES;
        
        System.out.printf("Messages stored: %,d%n", MEMORY_TEST_MESSAGES);
        System.out.printf("Memory used: %.2f MB%n", memoryUsed / 1_048_576.0);
        System.out.printf("Memory per message: %.1f bytes%n", memoryPerMessage);
        System.out.println();
        
        parsedMessages.clear();
    }
    
    private void runFieldAccessBenchmark() throws FixParseException {
        System.out.println("=== Field Access Benchmark ===");
        
        String testMessage = FixMessageBuilder.newOrder("AAPL", '1', 100.0, '2')
            .addField(44, 150.0)
            .addField(59, "0")
            .addField(126, "20231201-10:30:00.000")
            .buildString();
        
        FixMessage message = parser.parse(testMessage);
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            String symbol = message.getString(55);
            String side = message.getString(54);
            Double quantity = message.getDouble(38);
            String orderType = message.getString(40);
            Double price = message.getDouble(44);
            
            if (symbol == null || quantity == null) {
                throw new RuntimeException("Field access failed");
            }
        }
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        
        double totalTimeMs = totalTime / 1_000_000.0;
        double accessesPerSecond = (BENCHMARK_ITERATIONS * 5 * 1_000_000_000.0) / totalTime;
        
        System.out.printf("Field accesses: %,d%n", BENCHMARK_ITERATIONS * 5);
        System.out.printf("Time taken: %.1fms%n", totalTimeMs);
        System.out.printf("Field accesses per second: %,.0f%n", accessesPerSecond);
        System.out.println();
    }
    
    private void runMessageBuildingBenchmark() {
        System.out.println("=== Message Building Benchmark ===");
        
        Random random = new Random(42);
        String[] symbols = {"AAPL", "GOOGL", "MSFT", "AMZN"};
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < BENCHMARK_ITERATIONS / 4; i++) {
            String symbol = symbols[random.nextInt(symbols.length)];
            double quantity = 100 + random.nextDouble() * 900;
            double price = 50 + random.nextDouble() * 450;
            
            String message = FixMessageBuilder.newOrder(symbol, '1', quantity, '2')
                .setMsgSeqNum(i + 1)
                .addField(44, price)
                .addField(59, "0")
                .buildString();
            
            if (message.length() < 50) {
                throw new RuntimeException("Message building failed");
            }
        }
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        
        double totalTimeMs = totalTime / 1_000_000.0;
        double messagesPerSecond = ((BENCHMARK_ITERATIONS / 4) * 1_000_000_000.0) / totalTime;
        
        System.out.printf("Messages built: %,d%n", BENCHMARK_ITERATIONS / 4);
        System.out.printf("Time taken: %.1fms%n", totalTimeMs);
        System.out.printf("Messages per second: %,.0f%n", messagesPerSecond);
        System.out.println();
    }
    
    private void runMultipleMessageBenchmark() throws FixParseException {
        System.out.println("=== Multiple Message Buffer Benchmark ===");
        
        StringBuilder combinedMessages = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            combinedMessages.append(testMessages.get(i % testMessages.size()));
        }
        
        ByteBuffer buffer = ByteBuffer.wrap(combinedMessages.toString().getBytes());
        
        long startTime = System.nanoTime();
        
        int messageCount = 0;
        while (buffer.hasRemaining()) {
            FixMessage message = parser.parse(buffer);
            messageCount++;
            
            if (message == null) {
                break;
            }
        }
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        
        double totalTimeMs = totalTime / 1_000_000.0;
        double messagesPerSecond = (messageCount * 1_000_000_000.0) / totalTime;
        
        System.out.printf("Messages parsed from buffer: %,d%n", messageCount);
        System.out.printf("Time taken: %.1fms%n", totalTimeMs);
        System.out.printf("Messages per second: %,.0f%n", messagesPerSecond);
        System.out.println();
    }
}