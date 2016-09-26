package org.epn.core;

import java.util.ArrayList;
import java.util.List;

import org.epn.api.Event;
import org.epn.api.EventSink;
import org.reactivestreams.Subscription;

public class BasicEventSink<T> implements EventSink<T> {

  private List<T> data = new ArrayList<T>();
  
  @Override
  public void onSubscribe(Subscription s) {
    s.request(Long.MAX_VALUE);
  }

  @Override
  public void onNext(Event<T> t) {
    data.add(t.get());
  }
  
  public List<T> getData() {
    return data;
  }

}
