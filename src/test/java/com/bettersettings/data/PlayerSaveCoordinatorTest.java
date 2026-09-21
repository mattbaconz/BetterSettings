package com.bettersettings.data;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSaveCoordinatorTest {

    @Test
    void olderSnapshotCannotOverwriteANewerSnapshotThatFinishedFirst() {
        PlayerSaveCoordinator coordinator = new PlayerSaveCoordinator();
        UUID uuid = UUID.randomUUID();
        PlayerSaveCoordinator.Ticket older = coordinator.reserve(uuid);
        PlayerSaveCoordinator.Ticket newer = coordinator.reserve(uuid);
        AtomicReference<String> disk = new AtomicReference<>("initial");

        assertTrue(coordinator.execute(newer, () -> {
            disk.set("newer");
            return true;
        }));
        assertTrue(coordinator.execute(older, () -> {
            disk.set("older");
            return true;
        }));

        assertEquals("newer", disk.get());
        assertEquals(0, coordinator.retainedPlayerCount());
    }
}
