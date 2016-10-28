package org.epn.ext.flink;

import org.epn.api.Event;
import org.epn.api.EventProcessor;
import org.reactivestreams.Subscription;

// https://ci.apache.org/projects/flink/flink-docs-master/api/java/org/apache/flink/streaming/api/functions/source/SocketTextStreamFunction.html

// https://ci.apache.org/projects/flink/flink-docs-master/api/java/org/apache/flink/streaming/api/functions/sink/SocketClientSink.html

// as a sink, writes events to a socket, where a "remote" system can observe and process them before sending them back
// as a source, listens to events on a socket returned from the "remote" system and forwards them into the local network

public class FlinkEventProcessor<I, O> extends FlinkEventSource<O> implements EventProcessor<I, O> {

  @Override
  public void onSubscribe(final Subscription s) {

  }

  @Override
  public void onNext(final Event<I> t) {

  }

  @Override
  public void onError(final Throwable t) {

  }

  @Override
  public void onComplete() {

  }

}
