package org.epn.core.net;

import java.util.UUID;

public class Epn {

  public static EventNetwork named(final String name) {
    return new EventNetwork(name);
  }

  public static EventNetwork create() {
    return new EventNetwork(UUID.randomUUID().toString());
  }
}
