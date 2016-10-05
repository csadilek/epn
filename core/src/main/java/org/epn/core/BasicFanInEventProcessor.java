package org.epn.core;

import java.util.LinkedList;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.epn.api.Event;
import org.epn.api.EventSink;
import org.epn.api.EventSource;
import org.epn.api.FanInEventProcessor;
import org.reactivestreams.Subscription;

public class BasicFanInEventProcessor<E> extends BasicEventSource<E> implements FanInEventProcessor<E> {

  private final LinkedList<E> dataTop = new LinkedList<>();
  private final LinkedList<E> dataBottom = new LinkedList<>();
  private final Optional<BiFunction<E, E, E>> combiner;

  public BasicFanInEventProcessor(final EventSource<E> top, final EventSource<E> bottom) {
    this(top, bottom, Optional.empty());
  }

  public BasicFanInEventProcessor(final EventSource<E> top, final EventSource<E> bottom,
      final BiFunction<E, E, E> combiner) {

    this(top, bottom, Optional.of(combiner));
  }

  public BasicFanInEventProcessor(final EventSource<E> top, final EventSource<E> bottom,
      final Optional<BiFunction<E, E, E>> combiner) {

    top.subscribe(new FanInEventSink<>(e -> {
      dataTop.add(e);
      onNext();
    }));

    bottom.subscribe(new FanInEventSink<>(e -> {
      dataBottom.add(e);
      onNext();
    }));

    this.combiner = combiner;
  }

  private void onNext() {
    if (!dataTop.isEmpty() && !dataBottom.isEmpty()) {
      final E topEvent = dataTop.poll();
      final E bottomEvent = dataBottom.poll();
      final Optional<E> combined = combiner.map(c -> {
        return c.apply(topEvent, bottomEvent);
      });

      combined.ifPresent(e -> notifySubscribers(new BasicEvent<E>(e)));
      if (!combined.isPresent()) {
        notifySubscribers(new BasicEvent<E>(topEvent));
        notifySubscribers(new BasicEvent<E>(bottomEvent));
      }
    }
  }

  public static class FanInEventSink<E> implements EventSink<E> {

    private Subscription s;
    private final Consumer<E> consumer;

    public FanInEventSink(final Consumer<E> consumer) {
      this.consumer = consumer;
    }

    @Override
    public void onSubscribe(final Subscription s) {
      this.s = s;
      s.request(1);

    }

    @Override
    public void onNext(final Event<E> event) {
      consumer.accept(event.get());
      s.request(1);
    }

  }

}
