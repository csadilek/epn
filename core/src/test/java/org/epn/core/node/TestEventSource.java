package org.epn.core.node;

import java.util.stream.IntStream;

import org.epn.core.BasicEventSource;
import org.epn.core.BasicEvent;

public class TestEventSource extends BasicEventSource<Integer> {

  private int limit = 100;
  
  public TestEventSource() {}
  
  public TestEventSource(int limit) {
    this.limit = limit;
  }
  
  @Override
  public void start() {
    IntStream.iterate(0, i -> i + 1).limit(limit).forEach( i -> notifySubscribers(new BasicEvent<Integer>(i)));    
  }

}
