package org.epn.api;

import org.reactivestreams.Publisher;

public interface EventSource<E> extends Publisher<Event<E>> {
  
  // TODO context?
  default void start() {};
  
}
