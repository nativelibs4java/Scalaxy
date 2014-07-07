package scalaxy.streams;

import org.junit.runners.Parameterized
import org.junit.runners.model.RunnerScheduler
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors

private[streams] class ThreadPoolScheduler extends RunnerScheduler
{
  private[this] val numThreads = Integer.parseInt(
    System.getProperty(
      "junit.parallel.threads",
      Runtime.getRuntime().availableProcessors + ""))
  private[this] val executor = Executors.newFixedThreadPool(numThreads)

  override def finished() {
    executor.shutdown()
    try {
        executor.awaitTermination(30, TimeUnit.MINUTES)
    } catch {
      case ex: InterruptedException =>
        throw new RuntimeException(ex)
    }
  }

  override def schedule(statement: Runnable) = executor.submit(statement)
}

class Parallelized(cls: Class[_]) extends Parameterized(cls) {
  setScheduler(new ThreadPoolScheduler());
}
