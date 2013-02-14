#!/bin/bash

OPTIM_FLAGS="-optimise -Yclosure-elim -Yinline"
#-Xprint:inliner -Xprint:typer

NORMAL_ARGS="TestIntRangeLoops.scala TestUtils.scala"
SCALAXY_ARGS="TestIntRangeLoopsOptimized.scala TestUtils.scala -feature -cp $HOME/.ivy2/cache/com.nativelibs4java/scalaxy-loops_2.10/jars/scalaxy-loops_2.10-0.3-SNAPSHOT.jar"

N=10

function normalBuild {
    for ((i = 0; i < $N; i += 1)); do
        scalac $NORMAL_ARGS || exit 1
    done
}
function normalOptimizedBuild {
    for ((i = 0; i < $N; i += 1)); do
        scalac $NORMAL_ARGS $OPTIM_FLAGS || exit 1
    done
}
function scalaxyBuild {
    for ((i = 0; i < $N; i += 1)); do
        scalac $SCALAXY_ARGS || exit 1
    done
}
function scalaxyOptimizedBuild {
    for ((i = 0; i < $N; i += 1)); do
        scalac $SCALAXY_ARGS $OPTIM_FLAGS || exit 1
    done
}

function announce {
    echo "#
# $@
#"
}

# Have sbt fetch scalaxy-loops and put it in Ivy cache. 
cd $(dirname $0)
sbt update || exit 1

announce "Scalaxy build"
time -p scalaxyBuild || exit 1

announce "Scalaxy optimised build"
time -p scalaxyOptimizedBuild || exit 1

announce "Normal build"
time -p normalBuild || exit 1

announce "Normal optimised build"
time -p normalOptimizedBuild || exit 1

