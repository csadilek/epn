package org.epn.core.node;

import org.epn.api.Event;
import org.epn.api.EventProcessor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class TestEventProcessor implements EventProcessor<Integer, Integer> {

  public void onSubscribe(Subscription s) {

  }

  public void onNext(Event<Integer> t) {

  }

  public void onError(Throwable t) {

  }

  public void onComplete() {

  }

  public void subscribe(Subscriber<? super Event<Integer>> s) {

  }

}
