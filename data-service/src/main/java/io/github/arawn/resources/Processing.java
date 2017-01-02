package io.github.arawn.resources;

public class Processing {

    public static void no() {
    }

    public static void veryfast() {
        delay(10, 5);
    }

    public static void fast() {
        delay(200, 100);
    }

    public static void medium() {
        delay(1000, 500);
    }

    public static void slow() {
        delay(2000, 1000);
    }

    public static void veryslow() {
        delay(5000, 2000);
    }

    public static void delay(int max, int min) {
        try {
            Thread.sleep((long) (Math.random() * max + min));
        } catch (InterruptedException ignore) {
        }
    }

    public static void delay(int time) {
        try {
            Thread.sleep((long) time);
        } catch (InterruptedException ignore) {
        }
    }

}
