package org.epn.core.net;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.epn.api.Event;
import org.epn.api.EventProcessor;
import org.epn.api.EventSink;
import org.epn.api.EventSource;
import org.epn.core.BasicEvent;
import org.epn.core.BasicEventFilter;
import org.epn.core.BasicEventTransformer;
import org.epn.core.BasicFanInEventProcessor;
import org.epn.core.BasicFanOutEventProcessor;
import org.epn.core.BasicFanOutEventProcessor.Outlet;

public class EventNetwork {

  private EventNetwork() {
  }

  public static <E> EpnNode<E, TerminalEpnNode<E>> fromSource(final EventSource<E> source) {
    return new EpnNode<E, TerminalEpnNode<E>>(source);
  }

  public static <E> EpnNode<E, TerminalFanInEpnNode<E>> join(final EventSource<E> top, final EventSource<E> bottom) {
    final FanInEpnRootNode<E, TerminalEpnNode<E>> fanInEpnNode = new FanInEpnRootNode<E, TerminalEpnNode<E>>(
        new EpnNode<>(top, top), new EpnNode<>(bottom, bottom));
    return fanInEpnNode.join();
  }

  public static <E> EpnNode<E, TerminalFanInEpnNode<E>> join(final Node<E> top, final Node<E> bottom) {
    final FanInEpnRootNode<E, TerminalEpnNode<E>> fanInRoot = new FanInEpnRootNode<E, TerminalEpnNode<E>>(top, bottom);
    return fanInRoot.join();
  }

  public static <E> EpnNode<E, TerminalFanInEpnNode<E>> join(final Node<E> top, final Node<E> bottom,
      final BiFunction<E, E, E> combiner) {
    final FanInEpnRootNode<E, TerminalEpnNode<E>> fanInRoot = new FanInEpnRootNode<E, TerminalEpnNode<E>>(top, bottom);
    return fanInRoot.join(combiner);
  }

  public interface Node<E> {
    default EventSource<?> getRoot() {
      return null;
    }

    default EventSource<E> getSource() {
      return null;
    }
  };

  public static abstract class AbstractEpnNode<E, C extends Node<E>> implements Node<E> {
    protected EventSource<?> root;
    protected EventSource<E> source;
    protected C continuation;

    protected AbstractEpnNode(final EventSource<E> source) {
      this(source, source);
    }

    @SuppressWarnings("unchecked")
    public AbstractEpnNode(final EventSource<?> root, final EventSource<E> source) {
      this.root = root;
      this.source = source;
      this.continuation = (C) new TerminalEpnNode<E>(root);
    }

    protected AbstractEpnNode(final EventSource<?> root, final EventSource<E> source, final C continuation) {
      this(root, source);
      this.continuation = continuation;
    }
  }

  public static class EpnNode<E, C extends Node<E>> extends AbstractEpnNode<E, C> {

    protected EpnNode(final EventSource<E> source) {
      super(source, source);
    }

    protected EpnNode(final EventSource<?> root, final EventSource<E> source) {
      super(source, source);
    }

    protected EpnNode(final EventSource<?> root, final EventSource<E> source, final C continuation) {
      super(root, source, continuation);
    }

    public <O> EpnNode<O, C> processedBy(final EventProcessor<E, O> processor) {
      source.subscribe(processor);
      return new EpnNode<O, C>(root, processor, continuation);
    }

    public <O> EpnNode<E, C> filter(final Predicate<E> p) {
      final BasicEventFilter<E> filter = new BasicEventFilter<>(p);
      return this.processedBy(filter);
    }

    public <O> EpnNode<O, C> transform(final Function<E, O> f) {
      final Function<Event<E>, Event<O>> ft = (e) -> new BasicEvent<O>(f.apply(e.get()));
      final BasicEventTransformer<E, O> transformer = new BasicEventTransformer<>(ft);
      return this.processedBy(transformer);
    }

    public <O> FanOutEpnTopNode<E, C> split() {
      final BasicFanOutEventProcessor<E> processor = new BasicFanOutEventProcessor<>();
      return split(processor);
    }

    public <O> FanOutEpnTopNode<E, C> split(final Predicate<E> p) {
      final BasicFanOutEventProcessor<E> processor = new BasicFanOutEventProcessor<>(
          e -> p.test(e) ? Outlet.TOP : Outlet.BOTTOM);
      return split(processor);
    }

    public <O> FanOutEpnTopNode<E, C> split(final BasicFanOutEventProcessor<E> processor) {
      source.subscribe(processor);
      return new FanOutEpnTopNode<E, C>(root, continuation, processor.getTop(), processor.getBottom());
    }

    public C consumedBy(final EventSink<E> sink) {
      source.subscribe(sink);
      return done();
    }

    public C done() {
      return continuation;
    }

    @Override
    public EventSource<?> getRoot() {
      return root;
    }

    @Override
    public EventSource<E> getSource() {
      return source;
    }

  }

  public static class TerminalEpnNode<E> implements Node<E> {
    private final EventSource<?> root;

    public TerminalEpnNode(final EventSource<?> root) {
      this.root = root;
    }

    public void start() {
      root.start();
    }

  }

  public static class FanOutEpnTopNode<E, C extends Node<E>> extends AbstractEpnNode<E, C> {
    private final EventSource<E> bottom;

    public FanOutEpnTopNode(final EventSource<?> root, final C continuation, final EventSource<E> top,
        final EventSource<E> bottom) {
      super(root, top, continuation);
      this.bottom = bottom;
    }

    public EpnNode<E, FanOutEpnBottomNode<E, C>> top() {
      return new EpnNode<E, FanOutEpnBottomNode<E, C>>(root, source,
          new FanOutEpnBottomNode<>(root, continuation, source, bottom));
    }
  }

  public static class FanOutEpnBottomNode<E, C extends Node<E>> extends AbstractEpnNode<E, C> {
    private final EventSource<E> top;

    public FanOutEpnBottomNode(final EventSource<?> root, final C continuation, final EventSource<E> top,
        final EventSource<E> bottom) {
      super(root, bottom, continuation);
      this.top = top;
    }

    public EpnNode<E, FanInEpnNode<E, C>> bottom() {
      return new EpnNode<E, FanInEpnNode<E, C>>(root, source, new FanInEpnNode<>(root, continuation, top, source));
    }
  }

  public static class FanInEpnNode<E, C extends Node<E>> extends AbstractEpnNode<E, C> {
    private final EventSource<E> top;
    private final EventSource<E> bottom;

    public FanInEpnNode(final EventSource<?> root, final C continuation, final EventSource<E> top,
        final EventSource<E> bottom) {
      super(root, top, continuation);
      this.top = top;
      this.bottom = bottom;
    }

    public EpnNode<E, TerminalFanInEpnNode<E>> join() {
      final BasicFanInEventProcessor<E> processor = new BasicFanInEventProcessor<E>(top, bottom);
      return new EpnNode<>(root, processor, new TerminalFanInEpnNode<E>(top, bottom));
    }

    public EpnNode<E, TerminalFanInEpnNode<E>> join(final BiFunction<E, E, E> combiner) {
      final BasicFanInEventProcessor<E> processor = new BasicFanInEventProcessor<E>(top, bottom, combiner);
      return new EpnNode<>(root, processor, new TerminalFanInEpnNode<E>(top, bottom));
    }

    public void start() {
      root.start();
    }
  }

  public static class FanInEpnRootNode<E, C extends Node<E>> extends AbstractEpnNode<E, C> {
    private final Node<E> top;
    private final Node<E> bottom;

    public FanInEpnRootNode(final Node<E> top, final Node<E> bottom) {
      super(top.getSource());
      this.top = top;
      this.bottom = bottom;
    }

    public EpnNode<E, TerminalFanInEpnNode<E>> join() {
      final BasicFanInEventProcessor<E> processor = new BasicFanInEventProcessor<E>(top.getSource(),
          bottom.getSource());
      return new EpnNode<>(root, processor, new TerminalFanInEpnNode<E>(top.getRoot(), bottom.getRoot()));
    }

    public EpnNode<E, TerminalFanInEpnNode<E>> join(final BiFunction<E, E, E> combiner) {
      final BasicFanInEventProcessor<E> processor = new BasicFanInEventProcessor<E>(top.getSource(), bottom.getSource(),
          combiner);
      return new EpnNode<>(root, processor, new TerminalFanInEpnNode<E>(top.getRoot(), bottom.getRoot()));
    }

    public void start() {
      root.start();
    }
  }

  public static class TerminalFanInEpnNode<E> implements Node<E> {
    private final Collection<EventSource<?>> roots = new ArrayList<>();

    public TerminalFanInEpnNode(final EventSource<?>... roots) {
      this.roots.addAll(Arrays.asList(roots));
    }

    public void start() {
      roots.forEach(r -> r.start());
    }
  }

}
