package org.reactivestreams.extensions;

import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class Logged<T> implements PublisherX<T> {

    final org.apache.commons.logging.Log log = LogFactory.getLog(Logged.class);
    final PublisherX<T> source;

    public Logged(PublisherX<T> source) {
        this.source = source;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        source.subscribe(new LogSubscriber(subscriber));
    }


    class LogSubscriber<T> implements Subscriber<T> {

        Subscriber<? super T> origin;
        Subscription subscription;

        public LogSubscriber(Subscriber<? super T> subscriber) {
            this.origin = subscriber;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            log.info("onSubscribe: " + subscription);

            this.subscription = subscription;
            this.origin.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    log.info("reqeust: " + n);

                    subscription.request(n);
                }

                @Override
                public void cancel() {
                    log.info("cancel");

                    subscription.cancel();
                }
            });
        }

        @Override
        public void onNext(T t) {
            log.info("onNext: " + t);

            origin.onNext(t);
        }

        @Override
        public void onError(Throwable error) {
            log.info("onError: " + error);

            origin.onError(error);
        }

        @Override
        public void onComplete() {
            log.info("onComplete");

            origin.onComplete();
        }

    }

}
