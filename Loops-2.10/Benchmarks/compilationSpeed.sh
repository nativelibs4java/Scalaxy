#!/bin/bash

set -e

OPTIM_FLAGS="-optimise -Yclosure-elim -Yinline"
#-Xprint:inliner -Xprint:typer

#JAR="$HOME/.ivy2/local/com.nativelibs4java/scalaxy-loops_2.10/0.3-SNAPSHOT/jars/scalaxy-loops_2.10-0.3-SNAPSHOT.jar"
JAR="$HOME/.ivy2/local/com.nativelibs4java/scalaxy-loops_2.10/0.3-SNAPSHOT/jars/scalaxy-loops_2.10.jar"
NORMAL_ARGS="TestIntRangeLoops.scala TestUtils.scala"
SCALAXY_ARGS="TestIntRangeLoopsOptimized.scala TestUtils.scala -cp $JAR"

N=10

function scalac() {
  /opt/local/bin/scalac-2.10 "$@"
}

function normalBuild {
    for ((i = 0; i < $N; i += 1)); do
        SCALAXY_STREAMS_OPTIMIZE=0 SCALAXY_LOOPS_OPTIMIZED=0 scalac $NORMAL_ARGS -d normal
    done
}
function normalOptimizedBuild {
    for ((i = 0; i < $N; i += 1)); do
        SCALAXY_STREAMS_OPTIMIZE=0 SCALAXY_LOOPS_OPTIMIZED=0 scalac $NORMAL_ARGS $OPTIM_FLAGS -d normal-opt
    done
}
function scalaxyBuild {
    for ((i = 0; i < $N; i += 1)); do
        SCALAXY_STREAMS_OPTIMIZE=1 SCALAXY_LOOPS_OPTIMIZED=1 scalac $SCALAXY_ARGS -d scalaxy
    done
}
function scalaxyOptimizedBuild {
    for ((i = 0; i < $N; i += 1)); do
        SCALAXY_STREAMS_OPTIMIZE=1 SCALAXY_LOOPS_OPTIMIZED=1 scalac $SCALAXY_ARGS $OPTIM_FLAGS -d scalaxy-opt
    done
}

function announce {
    echo "#
# $@
#"
}

cd $(dirname $0)

rm -fR normal normal-opt scalaxy scalaxy-opt
mkdir normal normal-opt scalaxy scalaxy-opt

# Have sbt fetch scalaxy-loops and put it in Ivy cache. 
sbt update

announce "Scalaxy build"
time -p scalaxyBuild

announce "Scalaxy optimised build"
time -p scalaxyOptimizedBuild

announce "Normal build"
time -p normalBuild

announce "Normal optimised build"
time -p normalOptimizedBuild

du -h normal normal-opt scalaxy scalaxy-opt
for F in normal normal-opt scalaxy scalaxy-opt; do jar -cf $F.zip $F ; done
ls -l *.zip
