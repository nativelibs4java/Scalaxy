These jmh-based benchmarks are not (yet) a real benchmark suite. To test performance, run:

    SCALAXY_TEST_PERF=1 sbt "project scalaxy-streams" "; clean ; ~test"

(hopefully the perf tests will all be migrated to using jmh eventually, but I might not have enough time to do it myself)
