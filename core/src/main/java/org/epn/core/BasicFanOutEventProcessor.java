package org.epn.core;

import org.epn.api.Event;
import org.epn.api.FanOutEventProcessor;
import org.reactivestreams.Subscription;

public class BasicFanOutEventProcessor<E> extends BasicBiEventSource<E> implements FanOutEventProcessor<E> {

  private Subscription subscription;
  
  public enum Outlet {
    TOP,
    BOTTOM;
  };
  
  @FunctionalInterface
  public interface OutletSelector<E> {
    Outlet select(E e);
  }

  private OutletSelector<E> selector;
  
  public BasicFanOutEventProcessor(OutletSelector<E> selector) {
    this.selector = selector;
  }
  
  @Override
  public void onNext(Event<E> e) {
    final Outlet outlet = selector.select(e.get());
    final BasicEventSource<E> source = (outlet.equals(Outlet.TOP)) ? top : bottom; 
    source.notifySubscribers(e);
    subscription.request(1);
  }
  
  @Override
  public void onSubscribe(Subscription s) {
    this.subscription = s;
    s.request(1);
  }
  
}
