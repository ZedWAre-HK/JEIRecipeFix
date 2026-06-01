package fr.horizonsmp.jeirecipefix.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LazyTest {

    @Test
    void computesOnceUntilInvalidated() {
        AtomicInteger calls = new AtomicInteger();
        Lazy<Integer> lazy = new Lazy<>(calls::incrementAndGet);

        assertEquals(1, lazy.get());
        assertEquals(1, lazy.get());
        assertEquals(1, calls.get());

        lazy.invalidate();

        assertEquals(2, lazy.get());
        assertEquals(2, calls.get());
    }
}
