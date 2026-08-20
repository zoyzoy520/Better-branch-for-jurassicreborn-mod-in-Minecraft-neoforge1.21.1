package net.example.jrtameall;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared cache for the render-time model measurement. Render (client
 * thread) writes, getPassengerRidingPosition (client tick thread) reads,
 * hence ConcurrentHashMap.
 */
public final class RidingSeatCache {

    private record Seat(double maxTop, double backTop) {
    }

    private static final ConcurrentHashMap<Integer, Seat> SEATS = new ConcurrentHashMap<>();

    private RidingSeatCache() {
    }

    public static void put(int entityId, double maxTop, double backTop) {
        SEATS.put(entityId, new Seat(maxTop, backTop));
    }

    /** Model's highest point above the entity's feet, in blocks; 0 if not measured yet. */
    public static double maxTop(int entityId) {
        Seat seat = SEATS.get(entityId);
        return seat == null ? 0.0D : seat.maxTop();
    }

    /** Volume-weighted mean box top (the back) above the entity's feet, in blocks; 0 if not measured yet. */
    public static double backTop(int entityId) {
        Seat seat = SEATS.get(entityId);
        return seat == null ? 0.0D : seat.backTop();
    }
}
