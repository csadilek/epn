package org.epn.api;

public interface FanOutEventProcessor<E> extends EventSink<E>, BiEventSource<E> {


}
