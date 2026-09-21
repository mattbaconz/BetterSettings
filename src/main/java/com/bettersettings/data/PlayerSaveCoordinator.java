package com.bettersettings.data;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/** Serializes each player's writes and discards snapshots made stale before they start. */
final class PlayerSaveCoordinator {
    private static final int STRIPE_COUNT = 256;
    private final Stripe[] stripes = new Stripe[STRIPE_COUNT];

    PlayerSaveCoordinator() {
        for (int index = 0; index < stripes.length; index++) stripes[index] = new Stripe();
    }

    Ticket reserve(UUID uuid) {
        Stripe stripe = stripe(uuid);
        synchronized (stripe) {
            long generation = ++stripe.nextGeneration;
            stripe.latestGeneration.put(uuid, generation);
            return new Ticket(uuid, generation, stripe);
        }
    }

    boolean execute(Ticket ticket, BooleanSupplier writer) {
        synchronized (ticket.stripe) {
            Long latest = ticket.stripe.latestGeneration.get(ticket.uuid);
            if (latest == null || ticket.generation < latest) return true;
            try {
                return writer.getAsBoolean();
            } finally {
                ticket.stripe.latestGeneration.remove(ticket.uuid, ticket.generation);
            }
        }
    }

    int retainedPlayerCount() {
        int total = 0;
        for (Stripe stripe : stripes) {
            synchronized (stripe) {
                total += stripe.latestGeneration.size();
            }
        }
        return total;
    }

    private Stripe stripe(UUID uuid) {
        return stripes[(uuid.hashCode() & Integer.MAX_VALUE) % stripes.length];
    }

    static final class Ticket {
        private final UUID uuid;
        private final long generation;
        private final Stripe stripe;

        private Ticket(UUID uuid, long generation, Stripe stripe) {
            this.uuid = uuid;
            this.generation = generation;
            this.stripe = stripe;
        }

        UUID uuid() { return uuid; }
        long generation() { return generation; }
    }

    private static final class Stripe {
        private long nextGeneration;
        private final Map<UUID, Long> latestGeneration = new HashMap<>();
    }
}
