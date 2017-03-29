package io.github.arawn.resources;

public enum ServiceProcessing {

    NO(1, 1),
    FIX(500, 500),
    VERYFAST(30, 20),
    FAST(200, 150),
    MEDIUM(1000, 500),
    SLOW(2000, 1000),
    VERYSLOW(5000, 2000);

    final int max;
    final int min;

    ServiceProcessing(int max, int min) {
        this.max = max;
        this.min = min;
    }

    public long getDelayMillis() {
        return (long) (Math.random() * max + min);
    }

}
