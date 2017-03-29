package org.reactivestreams.extensions;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.function.Function;

public class Map<T, R> implements PublisherX<R> {

    final PublisherX<T> source;
    final Function<T, R> mapper;

    public Map(PublisherX<T> source, Function<T, R> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public void subscribe(Subscriber<? super R> subscriber) {
        source.subscribe(new MapSubscriber(subscriber, mapper));
    }


    class MapSubscriber<T, R> implements Subscriber<T> {

        Subscriber<? super R> origin;
        Function<T, R> mapper;
        boolean complete = false;

        Subscription subscription;

        public MapSubscriber(Subscriber<? super R> subscriber, Function<T, R> mapper) {
            this.origin = subscriber;
            this.mapper = mapper;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            this.origin.onSubscribe(subscription);
        }

        @Override
        public void onNext(T t) {
            if (complete) {
                return;
            }

            origin.onNext(mapper.apply(t));
        }

        @Override
        public void onError(Throwable error) {
            if (complete) {
                return;
            }

            origin.onError(error);
        }

        @Override
        public void onComplete() {
            if (complete) {
                return;
            }

            complete = true;
            origin.onComplete();
        }

    }

}
