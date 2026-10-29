package com.gqo6.tiertag.data;

import com.gqo6.tiertag.config.TierConfig;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class McpvpClient {
    public static final String BASE_URL = "https://www.mcpvp.com/@";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);

    private final TierCache cache;
    private final TierConfig config;
    private final HttpClient http;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, CompletableFuture<PlayerTiers>> inFlight = new ConcurrentHashMap<>();
    private volatile long lastRequestAt;

    public McpvpClient(TierCache cache, TierConfig config) {
        this.cache = cache;
        this.config = config;
        this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(5))
                .executor(Executors.newFixedThreadPool(2, runnable -> {
                    Thread thread = new Thread(runnable, "tiertag-http");
                    thread.setDaemon(true);
                    return thread;
                }))
                .build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "tiertag-sched");
            thread.setDaemon(true);
            return thread;
        });
    }

    public PlayerTiers lookupCached(UUID uuid, String username, long now) {
        return cache.getIfFresh(username, config.cacheTtlMillis(), now);
    }

    public void request(UUID uuid, String username, long now) {
        if (username == null || username.isBlank()) {
            return;
        }
        long ttl = config.cacheTtlMillis();
        if (cache.getIfFresh(username, ttl, now) != null) {
            return;
        }
        TierCache.Entry entry = cache.computeIfAbsent(username);
        if (now < entry.notFoundUntil || now < entry.backoffUntil) {
            return;
        }
        CompletableFuture<PlayerTiers> existing = inFlight.get(key(username));
        if (existing != null && !existing.isDone()) {
            return;
        }
        CompletableFuture<PlayerTiers> future = new CompletableFuture<>();
        CompletableFuture<PlayerTiers> claimed = inFlight.putIfAbsent(key(username), future);
        if (claimed != null && !claimed.isDone()) {
            return;
        }
        long interval = config.requestIntervalMillis();
        long wait = Math.max(0, interval - (now - lastRequestAt));
        scheduler.schedule(() -> fetch(username, future), wait, TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<PlayerTiers> lookupNow(UUID uuid, String username) {
        long now = System.currentTimeMillis();
        PlayerTiers cached = cache.getIfFresh(username, config.cacheTtlMillis(), now);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        TierCache.Entry entry = cache.computeIfAbsent(username);
        if (now < entry.notFoundUntil) {
            return CompletableFuture.completedFuture(entry.tiers == null
                    ? PlayerTiers.notFound(username, now) : entry.tiers);
        }
        CompletableFuture<PlayerTiers> existing = inFlight.get(key(username));
        if (existing != null) {
            return existing;
        }
        CompletableFuture<PlayerTiers> future = new CompletableFuture<>();
        inFlight.put(key(username), future);
        lastRequestAt = now;
        fetch(username, future);
        return future;
    }

    private void fetch(String username, CompletableFuture<PlayerTiers> future) {
        lastRequestAt = System.currentTimeMillis();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + encode(username)))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "TierTag/1.0 (Minecraft Fabric client mod)")
                .header("Accept", "text/html")
                .GET()
                .build();
        http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .whenComplete((response, error) -> {
                    long now = System.currentTimeMillis();
                    if (error != null) {
                        cache.recordFailure(username, now);
                        inFlight.remove(key(username));
                        future.completeExceptionally(error);
                        LOGGER.debug("MCPVP lookup failed for {}: {}", username, error.toString());
                        return;
                    }
                    int status = response.statusCode();
                    if (status == 200) {
                        PlayerTiers tiers = ProfileParser.parse(response.body(), username, now);
                        cache.put(tiers, config.negativeTtlMillis(), now);
                        inFlight.remove(key(username));
                        future.complete(tiers);
                        return;
                    }
                    if (status >= 300 && status < 400 || status == 404) {
                        PlayerTiers tiers = PlayerTiers.notFound(username, now);
                        cache.put(tiers, config.negativeTtlMillis(), now);
                        inFlight.remove(key(username));
                        future.complete(tiers);
                        return;
                    }
                    cache.recordFailure(username, now);
                    inFlight.remove(key(username));
                    future.completeExceptionally(new IllegalStateException("HTTP " + status + " from MCPVP"));
                    LOGGER.debug("MCPVP lookup for {} returned HTTP {}", username, status);
                });
    }

    public void clearCache() {
        cache.clear();
        inFlight.clear();
    }

    private static String key(String username) {
        return username.toLowerCase();
    }

    private static String encode(String username) {
        StringBuilder sb = new StringBuilder();
        for (char c : username.toCharArray()) {
            if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_') {
                sb.append(c);
            } else {
                sb.append(String.format("%%%02X", (int) c));
            }
        }
        return sb.toString();
    }
}
