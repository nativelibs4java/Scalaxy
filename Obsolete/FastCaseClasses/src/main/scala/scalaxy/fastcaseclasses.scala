package scalaxy

import scala.language.experimental.macros

import scala.reflect.macros.blackbox.Context
import scala.collection.mutable

package object fastcaseclasses {

  var verbose = System.getenv("SCALAXY_FASTCASECLASSES_VERBOSE") != "0"

  var enabled = System.getenv("SCALAXY_FASTCASECLASSES_ENABLED") != "0"
}
