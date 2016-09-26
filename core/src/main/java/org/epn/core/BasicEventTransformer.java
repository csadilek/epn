package org.epn.core;

import java.util.function.Function;

import org.epn.api.Event;
import org.epn.api.EventProcessor;
import org.reactivestreams.Subscription;

public class BasicEventTransformer<I, O> extends BasicEventSource<O> implements EventProcessor<I, O> {

  private Function<Event<I>, Event<O>> f;
  private Subscription s;
  
  public BasicEventTransformer(Function<Event<I>, Event<O>> f) {
    this.f = f;
  }
  
  @Override
  public void onNext(Event<I> e) {
    Event<O> o = f.apply(e);
    notifySubscribers(o);
    s.request(1);
  }

  @Override
  public void onSubscribe(Subscription s) {
    this.s = s;
    s.request(1);
  }

}
