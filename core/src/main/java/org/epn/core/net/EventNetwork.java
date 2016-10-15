package org.epn.core.net;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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

  private final String name;

  private final Set<TypedNode<?>> sources = new HashSet<>();
  private final Set<TypedNode<?>> sinks = new HashSet<>();

  EventNetwork(final String name) {
    this.name = name;
  }

  public <E> EpnNode<E, TerminalEpnNode<E>> fromSource(final EventSource<E> source) {
    final EpnNode<E, TerminalEpnNode<E>> root = new EpnNode<>(this, source);
    sources.add(root);
    return root;
  }

  public <E> EpnNode<E, TerminalEpnNode<E>> join(final EventSource<E> top, final EventSource<E> bottom) {
    final FanInEpnRootNode<E, TerminalEpnNode<E>> fanInEpnNode = new FanInEpnRootNode<>(this, new EpnNode<>(this, top),
        new EpnNode<>(this, bottom));
    this.addSources(top, bottom);
    return fanInEpnNode.join();
  }

  public <E> EpnNode<E, TerminalEpnNode<E>> join(final TypedNode<E> top, final TypedNode<E> bottom) {
    final FanInEpnRootNode<E, TerminalEpnNode<E>> fanInRoot = new FanInEpnRootNode<>(this, top, bottom);
    this.addSources(top, bottom);
    return fanInRoot.join();
  }

  public <E> EpnNode<E, TerminalEpnNode<E>> join(final TypedNode<E> top, final TypedNode<E> bottom,
      final BiFunction<E, E, E> combiner) {
    final FanInEpnRootNode<E, TerminalEpnNode<E>> fanInRoot = new FanInEpnRootNode<>(this, top, bottom);
    this.addSources(top, bottom);
    return fanInRoot.join(combiner);
  }

  String getName() {
    return name;
  }

  void addSinks(final TypedNode<?>... sinks) {
    this.sinks.addAll(Arrays.asList(sinks));
  }

  Set<TypedNode<?>> getSinks() {
    return sinks;
  }

  void addSources(final EventSource<?>... sources) {
    Arrays.stream(sources).forEach(s -> this.sources.add(new EpnNode<>(this, s)));
  }

  void addSources(final TypedNode<?>... sources) {
    Arrays.stream(sources).forEach(n -> this.sources.addAll(n.getNetwork().getSources()));
  }

  Set<TypedNode<?>> getSources() {
    return sources;
  }

  void start() {
    sources.forEach(n -> n.getSource().start());
  }

  public interface Node {
  };

  public static abstract class TypedNode<E> implements Node {
    TypedNode<?> getParent() {
      return null;
    }

    EventSource<E> getSource() {
      return null;
    }

    EventNetwork getNetwork() {
      return null;
    }
  };

  public static abstract class AbstractEpnNode<E, C extends Node> extends TypedNode<E> {
    protected TypedNode<?> parent;
    protected EventSource<E> source;
    protected C continuation;
    protected EventNetwork network;
    protected Optional<String> name;

    protected AbstractEpnNode(final EventNetwork network) {
      this.network = network;
      this.name = Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public AbstractEpnNode(final EventNetwork network, final EventSource<E> source) {
      this.network = network;
      this.source = source;
      this.continuation = (C) new TerminalEpnNode<>(network);
      this.name = Optional.empty();
    }

    protected AbstractEpnNode(final EventNetwork network, final TypedNode<?> parent, final EventSource<E> source,
        final C continuation) {
      this(network, source);
      this.parent = parent;
      this.continuation = continuation;
      this.name = Optional.empty();
    }

    protected AbstractEpnNode(final EventNetwork network, final TypedNode<?> parent, final EventSource<E> source,
        final C continuation, final String name) {
      this(network, source);
      this.parent = parent;
      this.continuation = continuation;
      this.name = Optional.of(name);
    }

    @Override
    EventNetwork getNetwork() {
      return network;
    }

    @Override
    EventSource<E> getSource() {
      return source;
    }

    @Override
    TypedNode<?> getParent() {
      return parent;
    }

    @Override
    public String toString() {
      return name.orElse(source.getClass().getSimpleName());
    }

  }

  public static class EpnNode<E, C extends Node> extends AbstractEpnNode<E, C> {

    protected EpnNode(final EventNetwork network, final EventSource<E> source) {
      super(network, source);
    }

    protected EpnNode(final EventNetwork network, final TypedNode<?> parent, final EventSource<E> source,
        final C continuation) {
      super(network, parent, source, continuation);
    }

    protected EpnNode(final EventNetwork network, final TypedNode<?> parent, final EventSource<E> source,
        final C continuation, final String name) {
      super(network, parent, source, continuation, name);
    }

    public <O> EpnNode<O, C> processedBy(final EventProcessor<E, O> processor) {
      source.subscribe(processor);
      return new EpnNode<O, C>(network, this, processor, continuation);
    }

    public EpnNode<E, C> filter(final Predicate<E> p) {
      final BasicEventFilter<E> filter = new BasicEventFilter<>(p);
      return this.processedBy(filter);
    }

    public <O> EpnNode<O, C> transform(final Function<E, O> f) {
      final Function<Event<E>, Event<O>> ft = (e) -> new BasicEvent<O>(f.apply(e.get()));
      final BasicEventTransformer<E, O> transformer = new BasicEventTransformer<>(ft);
      return this.processedBy(transformer);
    }

    public FanOutEpnNode<E, C> split() {
      final BasicFanOutEventProcessor<E> processor = new BasicFanOutEventProcessor<>();
      return split(processor);
    }

    public FanOutEpnNode<E, C> split(final Predicate<E> p) {
      final BasicFanOutEventProcessor<E> processor = new BasicFanOutEventProcessor<>(
          e -> p.test(e) ? Outlet.TOP : Outlet.BOTTOM);
      return split(processor);
    }

    public FanOutEpnNode<E, C> split(final BasicFanOutEventProcessor<E> processor) {
      source.subscribe(processor);
      return new FanOutEpnNode<>(network, this, continuation, processor.getTop(), processor.getBottom());
    }

    public C consumedBy(final EventSink<E> sink) {
      source.subscribe(sink);
      network.addSinks(new EpnSinkNode<>(sink, this));
      return done();
    }

    public C done() {
      return continuation;
    }

  }

  public static class TerminalEpnNode<E> implements Node {
    private final EventNetwork network;

    public TerminalEpnNode(final EventNetwork network) {
      this.network = network;
    }

    public EventNetwork start() {
      network.start();
      return network;
    }
  }

  public static class EpnSinkNode<E> extends TypedNode<E> {
    private final EventSink<E> sink;
    private final TypedNode<?> parent;

    public EpnSinkNode(final EventSink<E> sink, final TypedNode<?> parent) {
      this.sink = sink;
      this.parent = parent;
    }

    @Override
    TypedNode<?> getParent() {
      return parent;
    }

    @Override
    public String toString() {
      return sink.getClass().getSimpleName();
    }

  }

  public static class FanOutEpnNode<E, C extends Node> extends AbstractEpnNode<E, C> {

    private final EventSource<E> bottom;

    public FanOutEpnNode(final EventNetwork network, final TypedNode<E> parent, final C continuation,
        final EventSource<E> top, final EventSource<E> bottom) {
      super(network, parent, top, continuation, "Fan out");
      this.bottom = bottom;
    }

    public EpnNode<E, FanOutEpnBottomNode<E, C>> top() {
      return new EpnNode<>(network, this, source,
          new FanOutEpnBottomNode<>(network, this, continuation, source, bottom), "top");
    }

  }

  public static class FanOutEpnBottomNode<E, C extends Node> extends AbstractEpnNode<E, C> {
    private final EventSource<E> top;

    public FanOutEpnBottomNode(final EventNetwork network, final TypedNode<E> parent, final C continuation,
        final EventSource<E> top, final EventSource<E> bottom) {
      super(network, parent, bottom, continuation, "bottom");
      this.top = top;
    }

    public EpnNode<E, FanInEpnNode<E, C>> bottom() {
      return new EpnNode<>(network, this, source, new FanInEpnNode<>(network, this, continuation, top, source));
    }

  }

  public static class FanInEpnNode<E, C extends Node> extends AbstractEpnNode<E, C> {
    private final EventSource<E> top;
    private final EventSource<E> bottom;

    public FanInEpnNode(final EventNetwork network, final TypedNode<E> parent, final C continuation,
        final EventSource<E> top, final EventSource<E> bottom) {
      super(network, parent, top, continuation);
      this.top = top;
      this.bottom = bottom;
      this.network.addSources(new EpnNode<>(network, top), new EpnNode<>(network, bottom));
    }

    public EpnNode<E, TerminalEpnNode<E>> join() {
      final BasicFanInEventProcessor<E> processor = new BasicFanInEventProcessor<>(top, bottom);
      return new EpnNode<>(network, this, processor, new TerminalEpnNode<>(network));
    }

    public EpnNode<E, TerminalEpnNode<E>> join(final BiFunction<E, E, E> combiner) {
      final BasicFanInEventProcessor<E> processor = new BasicFanInEventProcessor<>(top, bottom, combiner);
      return new EpnNode<>(network, this, processor, new TerminalEpnNode<>(network));
    }

    public EventNetwork start() {
      network.start();
      return network;
    }
  }

  public static class FanInEpnRootNode<E, C extends Node> extends AbstractEpnNode<E, C> {
    private final TypedNode<E> top;
    private final TypedNode<E> bottom;

    public FanInEpnRootNode(final EventNetwork network, final TypedNode<E> top, final TypedNode<E> bottom) {
      super(network);
      this.top = top;
      this.bottom = bottom;
    }

    public EpnNode<E, TerminalEpnNode<E>> join() {
      final BasicFanInEventProcessor<E> processor = new BasicFanInEventProcessor<E>(top.getSource(),
          bottom.getSource());
      final EpnNode<E, TerminalEpnNode<E>> epnNode = new EpnNode<>(network, this, processor,
          new TerminalEpnNode<>(network));
      return epnNode;
    }

    public EpnNode<E, TerminalEpnNode<E>> join(final BiFunction<E, E, E> combiner) {
      final BasicFanInEventProcessor<E> processor = new BasicFanInEventProcessor<>(top.getSource(), bottom.getSource(),
          combiner);
      return new EpnNode<>(network, this, processor, new TerminalEpnNode<>(network));
    }

    public EventNetwork start() {
      network.start();
      return network;
    }
  }

}
