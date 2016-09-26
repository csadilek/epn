package org.epn.core;

import java.util.function.Predicate;

import org.epn.api.Event;
import org.epn.api.EventProcessor;
import org.reactivestreams.Subscription;

public class BasicEventFilter<I> extends BasicEventSource<I> implements EventProcessor<I, I> {

  private Predicate<I> p;
  private Subscription s;
  
  public BasicEventFilter(Predicate<I> p) {
    this.p = p;
  }
  
  @Override
  public void onNext(Event<I> t) {
    if (p.test(t.get())) {
      notifySubscribers(t);
    }
    s.request(1);
  }

  @Override
  public void onSubscribe(Subscription s) {
    this.s = s;
    s.request(1);
  }

}
