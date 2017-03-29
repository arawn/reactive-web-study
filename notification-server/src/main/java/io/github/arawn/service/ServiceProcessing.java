package io.github.arawn.service;

public enum ServiceProcessing {

    NO {
        @Override
        void execute() {
            delay(1);
        }
    },
    FIX {
        @Override
        void execute() {
            delay(500);
        }
    },
    VERYFAST {
        @Override
        void execute() {
            delay(30, 20);
        }
    },
    FAST {
        @Override
        void execute() {
            delay(200, 150);
        }
    },
    MEDIUM {
        @Override
        void execute() {
            delay(1000, 500);
        }
    },
    SLOW {
        @Override
        void execute() {
            delay(2000, 1000);
        }
    },
    VERYSLOW {
        @Override
        void execute() {
            delay(5000, 2000);
        }
    };


    void execute() {

    }

    public static void delay(int max, int min) {
        try {
            Thread.sleep((long) (Math.random() * max + min));
        } catch (InterruptedException error) {
            throw new ServiceInterruptedException(error);
        }
    }

    public static void delay(int time) {
        try {
            Thread.sleep((long) time);
        } catch (InterruptedException error) {
            throw new ServiceInterruptedException(error);
        }
    }


    static class ServiceInterruptedException extends RuntimeException {

        public ServiceInterruptedException(Throwable cause) {
            super(cause);
        }

    }

}
