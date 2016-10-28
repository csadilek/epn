package org.epn.ext.flink;

import org.apache.flink.streaming.api.functions.sink.SocketClientSink;
import org.epn.api.EventSink;
import org.epn.core.BasicEventSink;

/**
 * {@link EventSink} that forwards events to a socket using a
 * {@link SocketClientSink}.
 */
public class FlinkEventSink<T> extends BasicEventSink<T> {

}
