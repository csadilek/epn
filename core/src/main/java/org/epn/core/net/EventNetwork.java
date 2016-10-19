package org.epn.core.net;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
    final FanInEpnNode<E, TerminalEpnNode<E>> fanInEpnNode = new FanInEpnNode<>(this, new EpnNode<>(this, top),
        new EpnNode<>(this, bottom));
    this.addSources(top, bottom);
    return fanInEpnNode.join();
  }

  public <E> EpnNode<E, TerminalEpnNode<E>> join(final TypedNode<E> top, final TypedNode<E> bottom) {
    final FanInEpnNode<E, TerminalEpnNode<E>> fanInRoot = new FanInEpnNode<>(this, top, bottom);
    this.addSources(top, bottom);
    return fanInRoot.join();
  }

  public <E> EpnNode<E, TerminalEpnNode<E>> join(final TypedNode<E> top, final TypedNode<E> bottom,
      final BiFunction<E, E, E> combiner) {
    final FanInEpnNode<E, TerminalEpnNode<E>> fanInRoot = new FanInEpnNode<>(this, top, bottom);
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
    abstract List<TypedNode<?>> getParents();

    abstract EventSource<E> getSource();

    abstract EventNetwork getNetwork();
  };

  public static abstract class AbstractEpnNode<E, C extends Node> extends TypedNode<E> {
    protected List<TypedNode<?>> parents = new ArrayList<>();
    protected EventSource<E> source;
    protected C continuation;
    protected EventNetwork network;
    protected Optional<String> name;

    protected AbstractEpnNode(final EventNetwork network) {
      this.network = network;
      this.name = Optional.empty();
    }

    protected AbstractEpnNode(final EventNetwork network, final String name) {
      this.network = network;
      this.name = Optional.of(name);
    }

    @SuppressWarnings("unchecked")
    public AbstractEpnNode(final EventNetwork network, final EventSource<E> source) {
      this.network = network;
      this.source = source;
      this.continuation = (C) new TerminalEpnNode<>(network);
      this.name = Optional.empty();
    }

    public AbstractEpnNode(final EventNetwork network, final TypedNode<?> parent) {
      this.network = network;
      this.parents.add(parent);
    }

    protected AbstractEpnNode(final EventNetwork network, final TypedNode<?> parent, final EventSource<E> source,
        final C continuation) {
      this(network, parent);
      this.source = source;
      this.continuation = continuation;
      this.name = Optional.empty();
    }

    protected AbstractEpnNode(final EventNetwork network, final TypedNode<?> parent, final EventSource<E> source,
        final C continuation, final String name) {
      this(network, parent, source, continuation);
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
    List<TypedNode<?>> getParents() {
      return parents;
    }

    EventNetwork start() {
      network.start();
      return network;
    }

    @Override
    public String toString() {
      return name.orElseGet(() -> source.getClass().getSimpleName());
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

    public EpnNode(final EventNetwork network, final String name) {
      super(network, name);
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

    public FanOutNode<E, C> split() {
      final BasicFanOutEventProcessor<E> processor = new BasicFanOutEventProcessor<>();
      return split(processor);
    }

    public FanOutNode<E, C> split(final Predicate<E> p) {
      final BasicFanOutEventProcessor<E> processor = new BasicFanOutEventProcessor<>(
          e -> p.test(e) ? Outlet.TOP : Outlet.BOTTOM);
      return split(processor);
    }

    public FanOutNode<E, C> split(final BasicFanOutEventProcessor<E> processor) {
      source.subscribe(processor);
      return new FanOutEpnNode<>(network, this, continuation, processor.getTop(), processor.getBottom());
    }

    public C consumedBy(final EventSink<E> sink) {
      source.subscribe(sink);
      network.addSinks(new EpnSinkNode<>(network, sink, this));
      return done();
    }

    public C done() {
      return continuation;
    }

  }

  public static class TerminalEpnNode<E> extends AbstractEpnNode<E, Node> {

    public TerminalEpnNode(final EventNetwork network) {
      super(network);
    }

    @Override
    public EventNetwork start() {
      return super.start();
    }
  }

  public static class EpnSinkNode<E, C extends Node> extends AbstractEpnNode<E, C> {

    private final EventSink<E> sink;

    public EpnSinkNode(final EventNetwork network, final EventSink<E> sink, final TypedNode<?> parent) {
      super(network, parent);
      this.sink = sink;
    }

    @Override
    public String toString() {
      return sink.getClass().getSimpleName();
    }
  }

  public interface FanOutNode<E, C extends Node> extends Node {
    EpnNode<E, FanOutBottomNode<E, C>> top();
  }

  public static class FanOutEpnNode<E, C extends Node> extends AbstractEpnNode<E, C> implements FanOutNode<E, C> {

    private final EventSource<E> bottom;

    public FanOutEpnNode(final EventNetwork network, final TypedNode<E> parent, final C continuation,
        final EventSource<E> top, final EventSource<E> bottom) {
      super(network, parent, top, continuation, "Fan out");
      this.bottom = bottom;
    }

    @Override
    public EpnNode<E, FanOutBottomNode<E, C>> top() {
      return new EpnNode<>(network, this, source,
          new FanOutEpnBottomNode<>(network, this, continuation, source, bottom), "top");
    }

  }

  public interface FanOutBottomNode<E, C extends Node> extends Node {
    EpnNode<E, FanInNode<E, C>> bottom();
  }

  public static class FanOutEpnBottomNode<E, C extends Node> extends EpnNode<E, C> implements FanOutBottomNode<E, C> {
    private final EventSource<E> top;

    public FanOutEpnBottomNode(final EventNetwork network, final TypedNode<E> parent, final C continuation,
        final EventSource<E> top, final EventSource<E> bottom) {
      super(network, parent, bottom, continuation, "bottom");
      this.top = top;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EpnNode<E, FanInNode<E, C>> bottom() {
      this.continuation = (C) new FanInEpnNode<E, C>(network, this, continuation, top, source);
      return (EpnNode<E, FanInNode<E, C>>) this;
    }

  }

  public interface FanInNode<E, C extends Node> extends Node {
    EpnNode<E, TerminalEpnNode<E>> join();

    EpnNode<E, TerminalEpnNode<E>> join(final BiFunction<E, E, E> combiner);

    EventNetwork start();
  }

  public static class FanInEpnNode<E, C extends Node> extends EpnNode<E, C> implements FanInNode<E, C> {
    private final TypedNode<E> top;
    private final TypedNode<E> bottom;

    public FanInEpnNode(final EventNetwork network, final TypedNode<E> top, final TypedNode<E> bottom) {
      super(network, "Fan in");
      this.top = top;
      this.bottom = bottom;
      this.parents.add(top);
      this.parents.add(bottom);
    }

    public FanInEpnNode(final EventNetwork network, final TypedNode<E> parent, final C continuation,
        final EventSource<E> top, final EventSource<E> bottom) {
      this(network, new EpnNode<>(network, top), new EpnNode<>(network, bottom));
    }

    @Override
    public EpnNode<E, TerminalEpnNode<E>> join() {
      this.source = new BasicFanInEventProcessor<E>(top.getSource(), bottom.getSource());
      return _join();
    }

    @Override
    public EpnNode<E, TerminalEpnNode<E>> join(final BiFunction<E, E, E> combiner) {
      this.source = new BasicFanInEventProcessor<E>(top.getSource(), bottom.getSource(), combiner);
      return _join();
    }

    @SuppressWarnings("unchecked")
    private EpnNode<E, TerminalEpnNode<E>> _join() {
      this.continuation = (C) new TerminalEpnNode<>(network);
      return (EpnNode<E, TerminalEpnNode<E>>) this;
    }

    @Override
    public EventNetwork start() {
      return super.start();
    }
  }

}
