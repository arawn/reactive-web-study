package org.reactivestreams.extensions;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PublisherXTest {

    PublisherX<Customer> customers;

    @Before
    public void setUp() {
        customers = new SimplePublisher<>(Stream.of(
                new Customer(new Address("서울특별시 서초구 나루터로4길 39")),
                new Customer(new Address("경기도 시흥시 정왕대로53번길 7")),
                new Customer(new Address("서울특별시 강남구 도곡동 467 타워팰리스")),
                new Customer(new Address("경기도 수원시 영통구 매영로 346")),
                new Customer(new Address("서울특별시 강동구 천호대로175길 42"))));
    }

    @Test
    public void map() {
        AtomicInteger count = new AtomicInteger();

        customers.map(Customer::getAddress)
                 .map(Address::getStreet)
                 .subscribe(new Subscriber<String>() {
                     @Override
                     public void onSubscribe(Subscription s) {
                         s.request(Long.MAX_VALUE);
                     }

                     @Override
                     public void onNext(String street) {
                         System.out.println("street = [" + street + "]");
                         count.incrementAndGet();
                     }

                     @Override
                     public void onError(Throwable t) {

                     }

                     @Override
                     public void onComplete() {

                     }
                 });

        assertThat(count.get(), is(5));
    }

    @Test
    public void take() {
        AtomicInteger count = new AtomicInteger();

        customers.take(2)
                 .subscribe(new Subscriber<Customer>() {
                     @Override
                     public void onSubscribe(Subscription s) {
                         s.request(Long.MAX_VALUE);
                     }

                     @Override
                     public void onNext(Customer customer) {
                         System.out.println("customer = [" + customer + "]");
                         count.incrementAndGet();
                     }

                     @Override
                     public void onError(Throwable t) {

                     }

                     @Override
                     public void onComplete() {

                     }
                 });

        assertThat(count.get(), is(2));
    }

    @Test
    public void log() {
        customers.log()
                 .subscribe(new Subscriber<Customer>() {
                     @Override
                     public void onSubscribe(Subscription s) {
                         s.request(Long.MAX_VALUE);
                     }
                     public void onNext(Customer customer) {
                     }
                     public void onError(Throwable t) {
                     }
                     public void onComplete() {
                     }
                });
    }


    class SimplePublisher<T> implements PublisherX<T> {

        final Stream<T> stream;
        final List<Subscriber<? super T>> subscribers = new CopyOnWriteArrayList<>();
        final AtomicBoolean cancel = new AtomicBoolean(false);

        SimplePublisher(Stream<T> stream) {
            this.stream = stream;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            final Iterator<T> iterator = stream.iterator();

            if (subscribers.add(subscriber)) {
                subscriber.onSubscribe(new Subscription() {

                    @Override
                    public void request(long n) {
                        for (int idx = 0; idx <= n; idx++) {
                            if (cancel.get()) {
                                return;
                            }

                            try {
                                if (iterator.hasNext()) {
                                    subscribers.forEach(s -> s.onNext(iterator.next()));
                                } else {
                                    subscribers.forEach(Subscriber::onComplete);
                                    break;
                                }
                            } catch (Throwable error) {
                                subscribers.forEach(s -> s.onError(error));
                            }
                        }
                    }

                    @Override
                    public void cancel() {
                        cancel.set(true);
                    }

                });
            }
        }
    }

    class Customer {

        Address address;

        Customer(Address address) {
            this.address = address;
        }

        Address getAddress() {
            return address;
        }

        @Override
        public String toString() {
            return "Customer{" +
                    "address=" + address +
                    '}';
        }

    }

    class Address {

        String street;

        Address(String street) {
            this.street = street;
        }

        String getStreet() {
            return street;
        }

        @Override
        public String toString() {
            return "Address{" +
                    "street='" + street + '\'' +
                    '}';
        }

    }

}