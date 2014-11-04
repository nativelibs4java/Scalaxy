#!/bin/bash

set -e

sbt "project scalaxy-streams" "test" \
    "project scalaxy-loops" "test" \
    "project scalaxy-loops-210" "+test"

sbt "project scalaxy-streams" "; publish-local ; publishSigned" \
    "project scalaxy-loops" "; publish-local ; publishSigned" \
    "project scalaxy-loops-210" "; +publish-local ; +publishSigned"
