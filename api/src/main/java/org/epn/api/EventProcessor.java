package org.epn.api;

public interface EventProcessor<I, O> extends EventSink<I>, EventSource<O> {

}
