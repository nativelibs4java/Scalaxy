#!/bin/bash

rm -fR ~/.ivy2/cache/ ~/.ivy2/local/

function cleanSbtProject {
  rm -fR target project/target
}

#SCALAXY_VERY_VERBOSE=1 scalac -Xplugin:/Users/ochafik/.ivy2/local/com.nativelibs4java/scalaxy-compilets-plugin_2.10/0.3-SNAPSHOT/jars/scalaxy-compilets-plugin_2.10-assembly.jar -Xplugin:/Users/ochafik/.ivy2/local/com.nativelibs4java/scalaxy-default-compilets_2.10/0.3-SNAPSHOT/jars/scalaxy-default-compilets_2.10.jar Run.scala -Xplugin:/Users/ochafik/.ivy2/local/com.nativelibs4java/custom-compilets-example_2.10/1.0-SNAPSHOT/jars/custom-compilets-example_2.10.jar

#SCALAXY_VERY_VERBOSE=1 scalac -Xplugin:/Users/ochafik/.ivy2/cache/com.nativelibs4java/scalaxy-compilets-plugin_2.10/jars/scalaxy-compilets-plugin_2.10-0.3-SNAPSHOT.jar -Xplugin:/Users/ochafik/.ivy2/cache/com.nativelibs4java/scalaxy-default-compilets_2.10/jars/scalaxy-default-compilets_2.10-0.3-SNAPSHOT.jar Run.scala -Xplugin:/Users/ochafik/.ivy2/local/com.nativelibs4java/custom-compilets-example_2.10/1.0-SNAPSHOT/jars/custom-compilets-example_2.10.jar


EXAMPLES_DIR="$(dirname $0)/Examples"

cd $EXAMPLES_DIR/CustomCompilets
cleanSbtProject
sbt publish-local || exit 1

cd $EXAMPLES_DIR/CustomCompilets/Usage
cleanSbtProject
sbt run | grep 667 || exit 1

cd $EXAMPLES_DIR/DSLWithOptimizingCompilets
cleanSbtProject
sbt publish-local || exit 1


cd $EXAMPLES_DIR/DSLWithOptimizingCompilets/Usage
cleanSbtProject
sbt run || exit 1

