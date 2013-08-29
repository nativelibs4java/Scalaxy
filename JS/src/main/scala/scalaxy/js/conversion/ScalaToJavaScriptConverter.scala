package scalaxy.js
import ast._

import scala.reflect.api.Universe

class ScalaToJavaScriptConverter(val global: Universe)
extends ApiMappings with ASTConverter with ScalaToJavaScriptConversion
