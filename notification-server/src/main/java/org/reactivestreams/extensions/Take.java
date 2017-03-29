package org.reactivestreams.extensions;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class Take<T> implements PublisherX<T> {

    final PublisherX<T> source;
    final long count;

    public Take(PublisherX<T> source, long count) {
        this.source = source;
        this.count = count;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        source.subscribe(new TakeSubscriber(subscriber, count));
    }


    class TakeSubscriber<T> implements Subscriber<T> {

        Subscriber<? super T> origin;
        long remaining;
        boolean complete = false;

        Subscription subscription;

        public TakeSubscriber(Subscriber<? super T> subscriber, long count) {
            this.origin = subscriber;
            this.remaining = count;
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

            --remaining;
            origin.onNext(t);

            if (remaining == 0) {
                subscription.cancel();
                onComplete();
            }
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
