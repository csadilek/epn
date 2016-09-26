package org.epn.core;

import org.epn.api.BiEventSource;
import org.epn.api.EventSource;

public class BasicBiEventSource<E> implements BiEventSource<E> {
  
  protected final BasicEventSource<E> top;
  protected final BasicEventSource<E> bottom;

  public BasicBiEventSource() {
    top = new BasicEventSource<E>();
    bottom = new BasicEventSource<E>();
  }
  
  public BasicBiEventSource(BasicEventSource<E> top, BasicEventSource<E> bottom) {
    this.top = top;
    this.bottom = bottom;
  }
  
  @Override
  public EventSource<E> getTop() {
    return top;
  }

  @Override
  public EventSource<E> getBottom() {
    return bottom;
  }

}
