package org.epn.api;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public interface EventSink<E> extends Subscriber<Event<E>> {

  @Override
  default void onSubscribe(Subscription s) {}
  
  @Override
  default void onError(Throwable t) {}

  @Override
  default void onComplete() {}

}
