package org.epn.core;

import java.util.LinkedList;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.epn.api.Event;
import org.epn.api.EventSink;
import org.epn.api.EventSource;
import org.epn.api.FanInEventProcessor;
import org.reactivestreams.Subscription;

public class BasicFanInEventProcessor<E> extends BasicEventSource<E> implements FanInEventProcessor<E> {

  private LinkedList<E> dataTop = new LinkedList<>();
  private LinkedList<E> dataBottom = new LinkedList<>();
  private BiFunction<E, E, E> joiner;

  public BasicFanInEventProcessor(EventSource<E> top, EventSource<E> bottom, BiFunction<E, E, E> joiner) {
    this.joiner = joiner;
    
    top.subscribe(new FanInEventSink<>(e -> {
      dataTop.add(e);
      onNext();
    }));
    
    bottom.subscribe(new FanInEventSink<>(e -> {
      dataBottom.add(e);
      onNext();
    }));
  }

  private void onNext() {
    if (!dataTop.isEmpty() && !dataBottom.isEmpty()) {
      E combined = joiner.apply(dataTop.poll(), dataBottom.poll());
      notifySubscribers(new BasicEvent<E>(combined));
    }
  }

  public static class FanInEventSink<E> implements EventSink<E> {

    private Subscription s;
    private Consumer<E> consumer;

    public FanInEventSink(Consumer<E> consumer) {
      this.consumer = consumer;
    }

    @Override
    public void onSubscribe(Subscription s) {
      this.s = s;
      s.request(1);

    }

    @Override
    public void onNext(Event<E> event) {
      consumer.accept(event.get());
      s.request(1);
    }

  }

}
