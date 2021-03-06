package org.epn.core;

import static org.junit.Assert.assertArrayEquals;

import org.epn.core.net.Epn;
import org.epn.core.net.EventNetwork;
import org.epn.core.net.EventNetwork.TypedNode;
import org.epn.core.net.HtmlEventNetworkVisualizer;
import org.epn.core.node.TestEventSink;
import org.epn.core.node.TestEventSource;
import org.junit.Test;

//@formatter:off
public class EventNetworkTest {


  /**
   *<pre>
   * ________         ________         ________         ________
   *|        |       |        |       |        |       |        | 
   *| Source | ----> | Filter | ----> | Filter | ----> |  Sink  |
   *|________|       |________|       |________|       |________|
   *
   */
  @Test
  public void sourceToFiltersThenSink() {
    final TestEventSource source = new TestEventSource();
    final TestEventSink sink = new TestEventSink();

    final EventNetwork n = 
        Epn
          .named("FiltersAndSink")
          .fromSource(source)
          .filter(i -> (i % 2 == 0))
          .processedBy(new BasicEventFilter<Integer>(i -> (i < 12)))
          .consumedBy(sink)
          .start();

    new HtmlEventNetworkVisualizer().visualize(n);
    
    assertArrayEquals(new Integer[] { 0, 2, 4, 6, 8, 10 }, sink.getData().toArray());
  }
  
  /**
   *<pre>
   * ________         ________         _____________         _____________         ________
   *|        |       |        |       |             |       |             |       |        | 
   *| Source | ----> | Filter | ----> | Transformer | ----> | Transformer | ----> |  Sink  |
   *|________|       |________|       |_____________|       |_____________|       |________|
   *
   */
  @Test
  public void sourceToFilterThenTransformersThenSink() {
    final TestEventSource source = new TestEventSource();
    final BasicEventSink<String> sink = new BasicEventSink<String>();

    final EventNetwork n = 
        Epn
          .named("FiltersAndTransformers")
          .fromSource(source)
          .filter(i -> (i < 6))
          .transform(i -> i.toString())
          .processedBy(new BasicEventTransformer<String, String>(i -> new BasicEvent<String>(i.get() + i.get())))
          .consumedBy(sink)
          .start();
    
    new HtmlEventNetworkVisualizer().visualize(n);

    assertArrayEquals(new String[] { "00", "11", "22", "33", "44", "55" }, sink.getData().toArray());
  }
  
  /**
   *<pre>
   *                                  
   *                                  ________         ________
   *                  _______        |        |       |        |
   * ________        |       | ----> | Filter | ----> |  Sink  |
   *|        |       |       |       |________|       |________|
   *| Source | ----> | Split |        ________         ________
   *|________|       |       |       |        |       |        |
   *                 |_______| ----> | Filter | ----> |  Sink  |
   *                                 |________|       |________|
   *
   */
  @Test
  public void sourceToFanOutThenFiltersThenSinks() {
    final TestEventSource source = new TestEventSource();
    final TestEventSink sink1 = new TestEventSink();
    final TestEventSink sink2 = new TestEventSink();

    final EventNetwork n = 
      Epn
        .named("FanOutAndFilters")
        .fromSource(source)
        .split()
        .top()
          .filter(i -> i < 10)
          .consumedBy(sink1)
        .bottom()
          .filter(i -> i >= 10 && i < 20)
          .consumedBy(sink2)        
        .start();

    new HtmlEventNetworkVisualizer().visualize(n);
    
    assertArrayEquals(new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, sink1.getData().toArray());
    assertArrayEquals(new Integer[] { 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 }, sink2.getData().toArray());
  }

  /**
   *<pre>
   *                                  
   *                                  ________         ________
   *                  _______        |        |       |        |
   * ________        |       | ----> | Filter | ----> |  Sink  |
   *|        |       |       |       |________|       |________|
   *| Source | ----> | Split |        ________         ________
   *|________|       |       |       |        |       |        |
   *                 |_______| ----> | Filter | ----> |  Sink  |
   *                                 |________|       |________|
   *
   */
  @Test
  public void sourceToFanOutWithSelectorThenFiltersThenSink() {
    final TestEventSource source = new TestEventSource();
    final TestEventSink sink1 = new TestEventSink();
    final TestEventSink sink2 = new TestEventSink();

    final EventNetwork n =
      Epn
        .named("FanOutWithSelectorsAndFilters")
        .fromSource(source)
        .split(i -> (i % 2 == 0))
        .top()
          .filter(i -> i < 10)
          .consumedBy(sink1)
        .bottom()
          .filter(i -> i >= 10 && i < 20)
          .consumedBy(sink2)
        .start();
    
    new HtmlEventNetworkVisualizer().visualize(n);

    assertArrayEquals(new Integer[] { 0, 2, 4, 6, 8 }, sink1.getData().toArray());
    assertArrayEquals(new Integer[] { 11, 13, 15, 17, 19 }, sink2.getData().toArray());
  }
  
  /**
   *<pre>
   *                                  
   *  ________        
   * |        |         ________
   * | Source |        |        |        ________
   * |________| ---->  |        |       |        |
   *  ________         |  Join  | ----> |  Sink  |
   * |        | ---->  |        |       |________|
   * | Source |        |________|
   * |________|                                
   *
   */
  @Test
  public void sourcesToFanInThenSink() {
    final TestEventSource source1 = new TestEventSource(5);
    final TestEventSource source2 = new TestEventSource(5);
    final TestEventSink sink = new TestEventSink();
    
    final EventNetwork n =
      Epn
        .named("FanIn")
        .join(source1, source2)        
        .consumedBy(sink)    
        .start();
    
    new HtmlEventNetworkVisualizer().visualize(n);
      
    assertArrayEquals(new Integer[] { 0, 0, 1, 1, 2, 2, 3, 3, 4, 4 }, sink.getData().toArray());
  }
  
  /**
   *<pre>
   *                                  
   *  ________         _____________
   * |        |       |             |        ________
   * | Source | ----> | Transformer |       |        |        ________
   * |________|       |_____________| ----> |        |       |        |
   *  ________         _____________        |  Join  | ----> |  Sink  |
   * |        |       |             | ----> |        |       |________|
   * | Source | ----> | Transformer |       |________|
   * |________|       |_____________|                         
   *
   */
  @Test
  public void sourcesToTransformersThenFanInWithCombinerThenSink() {
    final TypedNode<String> node1 = Epn.create().fromSource(new TestEventSource(5)).transform(e -> e.toString());
    final TypedNode<String> node2 = Epn.create().fromSource(new TestEventSource(5)).transform(e -> e.toString());
    final BasicEventSink<String> sink = new BasicEventSink<String>();
    
    final EventNetwork n =
      Epn
        .named("TransformersAndFanIn")
        .join(node1, node2, (e1, e2) -> e1 + e2)
        .consumedBy(sink)
        .start();
    
    new HtmlEventNetworkVisualizer().visualize(n);
      
    assertArrayEquals(new String[] { "00", "11", "22", "33", "44" }, sink.getData().toArray());
  }
}
