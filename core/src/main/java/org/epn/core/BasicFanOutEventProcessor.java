package org.epn.core;

import java.util.Optional;

import org.epn.api.Event;
import org.epn.api.FanOutEventProcessor;
import org.reactivestreams.Subscription;

public class BasicFanOutEventProcessor<E> extends BasicBiEventSource<E> implements FanOutEventProcessor<E> {

  private Subscription subscription;
  
  public enum Outlet {
    TOP,
    BOTTOM,
    BOTH;
  };
  
  @FunctionalInterface
  public interface OutletSelector<E> {
    Outlet select(E e);
  }

  private Optional<OutletSelector<E>> selector;
  
  public BasicFanOutEventProcessor() {
    this.selector = Optional.empty();
  }
  
  public BasicFanOutEventProcessor(OutletSelector<E> selector) {
    this.selector =  Optional.of(selector);
  }
  
  @Override
  public void onNext(Event<E> event) {
    final Outlet outlet = selector.orElse(e -> Outlet.BOTH).select(event.get());
    if (outlet == Outlet.BOTH) {
      top.notifySubscribers(event);
      bottom.notifySubscribers(event);
    }
    else {
      final BasicEventSource<E> source = (outlet == Outlet.TOP) ? top : bottom; 
      source.notifySubscribers(event);
    }
    subscription.request(1);
  }
  
  @Override
  public void onSubscribe(Subscription s) {
    this.subscription = s;
    s.request(1);
  }
  
}
