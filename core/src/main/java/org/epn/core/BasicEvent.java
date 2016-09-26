package org.epn.core;

import org.epn.api.Event;

public class BasicEvent<T> implements Event<T> {

  private T data;
  
  public BasicEvent(final T data) {
    this.data = data;
  }
  
  @Override
  public T get() {
    return data;
  }

  @Override
  public String toString() {
    return "Event [data=" + data + "]";
  }  
  
}
