package org.saswata;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class BatchExecutor implements AutoCloseable {
  private final int BATCH_SIZE;

  private final Consumer<Object[]> action;
  private final ExecutorService executorService;
  private final ArrayList<String> batch;
  private long count = 0L;

  public BatchExecutor(final Consumer<Object[]> action, final int batch_size, final int pool_size) {
    this.action = action;
    BATCH_SIZE = batch_size;

    batch = new ArrayList<>(BATCH_SIZE);

    // fixed thread pool
    executorService = new ThreadPoolExecutor(pool_size, pool_size,
        0L, TimeUnit.MILLISECONDS,
        new ExecutorBlockingQueue<>(pool_size));
  }

  public void submit(String task) {
    batch.add(task);
    if (batch.size() >= BATCH_SIZE) execute();
  }

  private void execute() {
    if (batch.isEmpty()) return;

    executorService.submit(() -> action.accept(batch.toArray()));
    count += batch.size();
    System.out.println("progress," + count);
    batch.clear();
  }

  @Override
  public void close() throws Exception {
    execute();
    executorService.shutdown();
    if (!executorService.awaitTermination(10, TimeUnit.MINUTES)) {
      System.err.println("Waited for 10 minutes, forced exit");
      System.exit(0);
    }
  }
}
