package fr.horizonsmp.jeirecipefix.nms;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class EarlyRecipeSyncTracker {
    private final Set<UUID> players = ConcurrentHashMap.newKeySet();

    void mark(UUID playerId) {
        if (playerId != null) {
            players.add(playerId);
        }
    }

    boolean consume(UUID playerId) {
        return playerId != null && players.remove(playerId);
    }

    void remove(UUID playerId) {
        if (playerId != null) {
            players.remove(playerId);
        }
    }
}
