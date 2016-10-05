package org.epn.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.epn.api.Event;
import org.epn.api.EventSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class BasicEventSource<E> implements EventSource<E> {

  protected final Map<Subscriber<? super Event<E>>, AtomicLong> subscribers = new ConcurrentHashMap<>();

  @Override
  public void subscribe(final Subscriber<? super Event<E>> s) {
    subscribers.put(s, new AtomicLong());
    final Subscription subscription = new Subscription() {
      @Override
      public void request(final long n) {
        subscribers.get(s).addAndGet(n);
      }

      @Override
      public void cancel() {
        subscribers.remove(s);
      }
    };
    onNewSubscription(s, subscription);
  }

  protected void onNewSubscription(final Subscriber<? super Event<E>> subscriber, final Subscription subscription) {
    subscriber.onSubscribe(subscription);
  }

  protected void notifySubscribers(final Event<E> data) {
    subscribers.forEach((s, d) -> notifySubscriber(s, d, data));
  }

  protected void notifySubscriber(final Subscriber<? super Event<E>> subscriber, final AtomicLong demand,
      final Event<E> data) {
    if (demand.get() > 0) {
      subscriber.onNext(data);
      decreaseDemand(subscriber);
    }
  }

  protected void decreaseDemand(final Subscriber<? super Event<E>> s) {
    subscribers.get(s).decrementAndGet();
  }

}
