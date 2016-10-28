package org.epn.ext.flink;

import org.apache.commons.configuration.event.EventSource;
import org.apache.flink.streaming.api.functions.source.SocketTextStreamFunction;
import org.epn.core.BasicEventSource;

/**
 * {@link EventSource} that reads events from a socket using
 * {@link SocketTextStreamFunction} and publishes them into the local event
 * network.
 */
public class FlinkEventSource<T> extends BasicEventSource<T> {

}
