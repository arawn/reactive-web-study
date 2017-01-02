package io.github.arawn.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ykpark@woowahan.com
 */
public class AwkwardChaosMonkey {

    public static final AtomicBoolean STOP = new AtomicBoolean(false);
    final AtomicInteger counter = new AtomicInteger(5);

    boolean doItNow() {
        if (STOP.get()) {
            return false;
        }

        if (counter.decrementAndGet() < 1) {
            reset();
            return true;
        }
        return false;
    }

    void reset() {
        counter.set((int) (Math.random() * 15 + 5));
    }

}
