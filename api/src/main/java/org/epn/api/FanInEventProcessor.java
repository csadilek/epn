package org.epn.api;

public interface FanInEventProcessor<E> extends BiEventSink<E>, EventSource<E> {

}
