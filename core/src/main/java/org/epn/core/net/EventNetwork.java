package org.epn.core.net;

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

  private EventNetwork() {}

  public static <E> EpnNode<E, TerminalEpnNode<E>> fromSource(EventSource<E> source) {
    return new EpnNode<E, TerminalEpnNode<E>>(source);
  }
  
  public static <E> EpnNode<E, TerminalFanInEpnNode<E>> join(EventSource<E> top, EventSource<E> bottom, BiFunction<E, E, E> joiner) {
    FanInEpnNode<E, TerminalEpnNode<E>> fanInEpnNode = new FanInEpnNode<E, TerminalEpnNode<E>>(top, bottom);
    return fanInEpnNode.join(joiner);
  }

  public interface Node {};
  
  public static abstract class AbstractEpnNode<E, C extends Node> implements Node {
    protected EventSource<?> root;
    protected EventSource<E> source;
    protected C continuation;
    
    protected AbstractEpnNode(EventSource<E> source) {
      this(source, source);
    }
    
    @SuppressWarnings("unchecked")
    public AbstractEpnNode(EventSource<?> root, EventSource<E> source) {
      this.root = root;
      this.source = source;
      this.continuation = (C) new TerminalEpnNode<E>(root);
    }
    
    protected AbstractEpnNode(EventSource<?> root, EventSource<E> source, C continuation) {
      this(root, source);
      this.continuation = continuation;
    }
  }

  public static class EpnNode<E, C extends Node> extends AbstractEpnNode<E, C> {

    protected EpnNode(EventSource<E> source) {
      super(source, source);
    }

    protected EpnNode(EventSource<?> root, EventSource<E> source) {
      super(source, source);
    }

    protected EpnNode(EventSource<?> root, EventSource<E> source, C continuation) {
      super (root, source, continuation);
    }

    public <O> EpnNode<O, C> processedBy(EventProcessor<E, O> processor) {
      source.subscribe(processor);
      return new EpnNode<O, C>(root, processor, continuation);
    }

    public <O> EpnNode<E, C> filter(Predicate<E> p) {
      final BasicEventFilter<E> filter = new BasicEventFilter<>(p);
      return this.processedBy(filter);
    }

    public <O> EpnNode<O, C> transform(Function<E, O> f) {
      final Function<Event<E>, Event<O>> ft = (e) -> new BasicEvent<O>(f.apply(e.get()));
          
      final BasicEventTransformer<E, O> transformer = 
          new BasicEventTransformer<>(ft);
      
      return this.processedBy(transformer);
    }
    
    public <O> FanOutEpnTopNode<E, C> split(Predicate<E> p) {
      final BasicFanOutEventProcessor<E> processor = new BasicFanOutEventProcessor<>(
          e -> p.test(e) ? Outlet.TOP : Outlet.BOTTOM);

      source.subscribe(processor);
      return new FanOutEpnTopNode<E, C>(root, continuation, processor.getTop(), processor.getBottom());
    }

    public C consumedBy(EventSink<E> sink) {
      source.subscribe(sink);
      return done();
    }

    public C done() {
      return continuation;
    }

  }

  public static class TerminalEpnNode<E> implements Node {
    private EventSource<?> root;

    public TerminalEpnNode(EventSource<?> root) {
      this.root = root;
    }

    public void start() {
      root.start();
    }
    
  }

  public static class FanOutEpnTopNode<E, C extends Node> extends AbstractEpnNode<E, C> {
    private EventSource<E> bottom;

    public FanOutEpnTopNode(EventSource<?> root, C continuation, EventSource<E> top, EventSource<E> bottom) {
      super(root, top, continuation);
      this.bottom = bottom;
    }

    public EpnNode<E, FanOutEpnBottomNode<E, C>> top() {
      return new EpnNode<E, FanOutEpnBottomNode<E, C>>(root, source, 
          new FanOutEpnBottomNode<>(root, continuation, source, bottom));
    }
  }

  public static class FanOutEpnBottomNode<E, C extends Node> extends AbstractEpnNode<E, C> {
    private EventSource<E> top;
    public FanOutEpnBottomNode(EventSource<?> root, C continuation, EventSource<E> top, EventSource<E> bottom) {
      super(root, bottom, continuation);
      this.top = top;
    }

    public EpnNode<E, FanInEpnNode<E, C>> bottom() {
      return new EpnNode<E, FanInEpnNode<E, C>>(root, source, 
          new FanInEpnNode<>(root, continuation, top, source));
    }
  }
  
  public static class FanInEpnNode<E, C extends Node> extends AbstractEpnNode<E, C> {
    private EventSource<E> top;
    private EventSource<E> bottom;
    
    public FanInEpnNode(EventSource<E> top, EventSource<E> bottom) {
      this(null, null, top, bottom);
    }
    
    public FanInEpnNode(EventSource<?> root, C continuation, EventSource<E> top, EventSource<E> bottom) {
      super(root, top, continuation);
      this.top = top;
      this.bottom = bottom;
    }

    public EpnNode<E, TerminalFanInEpnNode<E>> join(final BiFunction<E, E, E> joiner) {
      final BasicFanInEventProcessor<E> processor = new BasicFanInEventProcessor<E>(top, bottom, joiner);
      return new EpnNode<>(root, processor, new TerminalFanInEpnNode<E>(root, top, bottom) );
    }
    
    public void start() {
      root.start();
    }
  }
    
  public static class TerminalFanInEpnNode<E> implements Node {
    private EventSource<?> root;
    private EventSource<E> top;
    private EventSource<E> bottom;

    public TerminalFanInEpnNode(EventSource<?> root, EventSource<E> top, EventSource<E> bottom) {
      this.root = root;
      this.top = top;
      this.bottom = bottom;
    }

    public void start() {
      if (root != null) {
        root.start();
      } else {
        top.start();
        bottom.start();
      }
    }
  }

}
