package org.reactivestreams.extensions;

import org.reactivestreams.Publisher;

import java.util.function.Function;

public interface PublisherX<T> extends Publisher<T> {

    default <R> PublisherX<R> map(Function<T, R> mapper) {
        return new Map<>(this, mapper);
    }

    default PublisherX<T> take(long number) {
        return new Take<>(this, number);
    }

    default PublisherX<T> log() {
        return new Logged<>(this);
    }

}
