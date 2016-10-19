package org.epn.core.net;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.epn.core.net.EventNetwork.TypedNode;

public class HtmlEventNetworkVisualizer implements EventNetworkVisualizer {
  private static final String HTML_TEMPLATE;

  private final Map<TypedNode<?>, Integer> nodeIds = new HashMap<>();
  private final Map<Integer, String> nodes = new HashMap<>();
  private final Set<Edge> edges = new HashSet<>();
  private int nodeCnt = 0;

  static {
    final InputStream templateStream = Thread.currentThread().getContextClassLoader()
        .getResourceAsStream("epn-template.html");

    try {
      HTML_TEMPLATE = IOUtils.toString(templateStream, "UTF-8");
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void visualize(final EventNetwork network) {
    network.getSinks().forEach(s -> process(s));
    writeFiles(network.getName());
  }

  private void process(final TypedNode<?> node) {
    process(node, null);
  }

  private void process(final TypedNode<?> node, final Integer childId) {
    if (node == null)
      return;

    final int id = nodeIds.computeIfAbsent(node, n -> nodeCnt++);
    nodes.put(id, node.toString());
    if (childId != null) {
      edges.add(new Edge(id, childId));
    }

    node.getParents().forEach(p -> process(p, id));
  }

  private void writeFiles(final String networkName) {
    try {
      final Path tmp = Files.createTempDirectory("epn_" + networkName);
      writeHtml(tmp, networkName);
      writeJson(tmp);
      System.out.println("Network visualization at: " + tmp.toString());
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void writeHtml(final Path tmp, final String networkName) throws IOException {
    final File file = new File(tmp.toString(), "index.html");
    FileUtils.writeStringToFile(file, HTML_TEMPLATE.replace("{{name}}", networkName), "UTF-8");
  }

  private void writeJson(final Path tmp) throws IOException {
    final StringBuilder sb = new StringBuilder();

    sb.append("var _nodes = [\n");
    nodes.forEach((k, v) -> sb.append("{id:").append(k).append(",").append(" label: '").append(v).append("'},\n"));
    sb.append("];\n");

    sb.append("var _edges = [\n");
    edges.forEach(
        e -> sb.append("{from:").append(e.from).append(",").append(" to: ").append(e.to).append(", arrows:'to'},\n"));
    sb.append("];\n");

    final File file = new File(tmp.toString(), "epn.json");
    FileUtils.writeStringToFile(file, sb.toString(), "UTF-8");
  }

  private static class Edge {

    int from;
    int to;

    public Edge(final int from, final int to) {
      this.from = from;
      this.to = to;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + from;
      result = prime * result + to;
      return result;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      final Edge other = (Edge) obj;
      if (from != other.from)
        return false;
      if (to != other.to)
        return false;
      return true;
    }
  }

}
