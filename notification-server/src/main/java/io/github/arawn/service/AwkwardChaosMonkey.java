package io.github.arawn.service;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AwkwardChaosMonkey {

    final static AtomicBoolean STOP = new AtomicBoolean(false);
    final static Random RANDOM = new Random();
    final static AtomicInteger MIN = new AtomicInteger(50);
    final static AtomicInteger MAX = new AtomicInteger(100);

    final AtomicInteger counter = new AtomicInteger((int) (Math.random() * MAX.get() + MIN.get()));

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
        counter.set(RANDOM.nextInt(MAX.get()) + MIN.get());
    }


    public static void enable() {
        STOP.set(false);
        MIN.set(50);
        MAX.set(100);
    }

    public static void enable(int min, int max) {
        STOP.set(false);
        MIN.set(min);
        MAX.set(max);
    }

    public static void disable() {
        STOP.set(true);
    }

}
