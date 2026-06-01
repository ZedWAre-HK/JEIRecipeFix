package fr.horizonsmp.jeirecipefix.util;

import java.util.function.Supplier;

public final class Lazy<T> {

    private final Supplier<T> supplier;
    private volatile boolean computed;
    private volatile T value;

    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        if (!computed) {
            synchronized (this) {
                if (!computed) {
                    value = supplier.get();
                    computed = true;
                }
            }
        }
        return value;
    }

    public synchronized void invalidate() {
        value = null;
        computed = false;
    }
}
