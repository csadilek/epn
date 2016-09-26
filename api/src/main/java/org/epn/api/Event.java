package org.epn.api;

public interface Event<T> {
  T get();
  
  // TODO access to meta data
}
