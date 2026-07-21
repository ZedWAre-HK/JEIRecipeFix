package fr.horizonsmp.jeirecipefix.nms;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EarlyRecipeSyncTrackerTest {
    @Test
    void consumesEachMarkedPlayerOnlyOnce() {
        EarlyRecipeSyncTracker tracker = new EarlyRecipeSyncTracker();
        UUID playerId = UUID.randomUUID();

        tracker.mark(playerId);

        assertTrue(tracker.consume(playerId));
        assertFalse(tracker.consume(playerId));
    }

    @Test
    void removesDisconnectedPlayers() {
        EarlyRecipeSyncTracker tracker = new EarlyRecipeSyncTracker();
        UUID playerId = UUID.randomUUID();

        tracker.mark(playerId);
        tracker.remove(playerId);

        assertFalse(tracker.consume(playerId));
    }
}
