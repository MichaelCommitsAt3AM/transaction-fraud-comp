package io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.Transaction;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TransactionCacheManager {
    private final StatefulRedisConnection<String, String> redisConnection;
    private final ObjectMapper objectMapper;
    private static final int HISTORY_WINDOW_SECONDS = 86400;

    public TransactionCacheManager(StatefulRedisConnection<String, String> redisConnection) {
        this.redisConnection = redisConnection;
        this.objectMapper = new ObjectMapper();
        // Crucial: Register module to handle Java 8 dates
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void addTransaction(Transaction t) {
        RedisCommands<String, String> syncCommands = redisConnection.sync();
        String key = "history:" + t.getUserId();
        long score = t.getTimestamp().toEpochSecond(ZoneOffset.UTC);

        String json = serialize(t);
        if (json != null) {
            syncCommands.zadd(key, score, json);
            syncCommands.expire(key, HISTORY_WINDOW_SECONDS);
        }
    }

    public List<Transaction> getRecentTransactions(String userId) {
        RedisCommands<String, String> syncCommands = redisConnection.sync();
        String key = "history:" + userId;

        // ZRANGE retrieves members in order
        List<String> jsonList = syncCommands.zrange(key, 0, -1);

        return jsonList.stream()
                .map(this::deserialize)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private String serialize(Transaction t) {
        try {
            return objectMapper.writeValueAsString(t);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Transaction deserialize(String json) {
        try {
            return objectMapper.readValue(json, Transaction.class);
        } catch (JsonProcessingException e) {
            // This will still print for any "dirty" data left in Redis
            System.err.println("Error deserializing transaction: " + e.getMessage());
            return null;
        }
    }
}