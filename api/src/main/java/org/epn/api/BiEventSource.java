package org.epn.api;

public interface BiEventSource<E> {

  EventSource<E> getTop();
  EventSource<E> getBottom();

}
