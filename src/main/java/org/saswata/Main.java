package org.saswata;

import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.SigV4WebSocketChannelizer;
import org.apache.tinkerpop.gremlin.driver.Tokens;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.driver.ser.Serializers;
import org.apache.tinkerpop.gremlin.process.remote.RemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;


public class Main {
  public static void main(String[] args) {
    final String neptune = args[0];
    final String bulkInputLoc = args[1]; // path to file containing neptune bulk load
    final char itemType = args[2].toUpperCase().charAt(0); // item type E : edges V : vertices
    final int BATCH_SIZE = Integer.parseInt(args[3]); // num of items to drop at once
    final int POOL_SIZE = Integer.parseInt(args[4]); // num of threads

    final Cluster cluster = clusterProvider(neptune, POOL_SIZE);
    final GraphTraversalSource g = graphProvider(cluster).
        with(Tokens.ARGS_EVAL_TIMEOUT, TimeUnit.MINUTES.toMillis(5)).
        withSideEffect("Neptune#repeatMode", "CHUNKED_DFS");

    final Consumer<Object[]> action = itemType == 'V' ? dropVertices(g) : dropEdges(g);
    runSuite(bulkInputLoc, action, BATCH_SIZE, POOL_SIZE);

    cluster.close();
  }

  static void runSuite(String bulkInputLoc, Consumer<Object[]> action, int BATCH_SIZE, int POOL_SIZE) {

    try (Stream<String> in = Files.lines(Paths.get(bulkInputLoc));
         BatchExecutor batchExecutor = new BatchExecutor(action, BATCH_SIZE, POOL_SIZE)) {

      in.forEach(acc -> batchExecutor.submit(acc.trim()));

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static Cluster clusterProvider(String neptune, int POOL_SIZE) {

    Cluster.Builder clusterBuilder = Cluster.build()
        .addContactPoint(neptune)
        .port(8182)
        .enableSsl(true)
        .channelizer(SigV4WebSocketChannelizer.class)
        .serializer(Serializers.GRAPHBINARY_V1D0)
        .maxInProcessPerConnection(1)
        .minInProcessPerConnection(1)
        .maxSimultaneousUsagePerConnection(1)
        .minSimultaneousUsagePerConnection(1)
        .minConnectionPoolSize(POOL_SIZE)
        .maxConnectionPoolSize(POOL_SIZE);

    return clusterBuilder.create();
  }

  static GraphTraversalSource graphProvider(Cluster cluster) {
    RemoteConnection connection = DriverRemoteConnection.using(cluster);
    return AnonymousTraversalSource.traversal().withRemote(connection);
  }

  static Consumer<Object[]> dropEdges(GraphTraversalSource g) {
    return (Object[] edges) -> {
      long start = Instant.now().toEpochMilli();

      g.E(edges).drop().iterate();

      long stop = Instant.now().toEpochMilli();
      System.out.println("duration," + (stop - start));
    };
  }

  static Consumer<Object[]> dropVertices(GraphTraversalSource g) {
    return (Object[] vertices) -> {
      long start = Instant.now().toEpochMilli();

      g.V(vertices).drop().iterate();

      long stop = Instant.now().toEpochMilli();
      System.out.println("duration," + (stop - start));
    };
  }
}
