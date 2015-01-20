package scalaxy.streams

package test

import org.junit._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized.Parameters

import scala.collection.JavaConversions._

import IntegrationTests.{streamMsg, potentialSideEffectMsgs}

/** This is just a testbed for "fast" manual tests */
class AdHocManualTest
    extends StreamComponentsTestBase
    with PerformanceTestBase
    with StreamTransforms
{
  import global._

  scalaxy.streams.impl.verbose = true

  // scalaxy.streams.impl.veryVerbose = true
  // scalaxy.streams.impl.debug = true
  // scalaxy.streams.impl.quietWarnings = true

  val fnRx = raw".*scala\.Function0\.apply.*"

/*
          def debug(title: String, t: Tree) = new Traverser {
            override def traverse(tree: Tree) = {
              for (s <- Option(tree.symbol); if s != NoSymbol && s.name.toString == "foo") {
                println(s"""
                $title
                  symbol: ${s}
                  owner: ${s.owner}
                  ownerChain: ${ownerChain(s)}
                """)
              }
              super.traverse(tree)
            }
          } traverse t
*/


  // @Test
  def testComp2 {

    // assertPluginCompilesSnippetFine(src)
    val src = """
      def foo[T](v: List[(Int, T)]) = v.map(_._2).filter(_ != null);
      foo(List((1, "a")))
    """
    // val src = s"""
    //   def f1(x: Any) = x.toString + "1"
    //   def f2(x: Any) = x.toString + "2"
    //   def f3(x: Any) = x.toString + "3"

    //   List(
    //     ${{
    //       val options = List(
    //         "None",
    //         "(None: Option[Int])",
    //         "Option[Any](null)",
    //         "Option[String](null)",
    //         "Option[String](\"Y\")",
    //         "Some(0)",
    //         "Some(\"X\")")
    //       for (lhs <- options; rhs <- options) yield
    //         s"$lhs.map(f1).orElse($rhs.map(f2)).map(f3)"
    //     }.mkString(",\n        ")}
    //   )
    // """
    // println(src)

    {
      import scalaxy.streams.strategy.foolish
      testMessages(src, streamMsg("List.map.filter -> List"),
        expectWarningRegexp = Some(List("there were \\d+ inliner warnings; re-run with -Yinline-warnings for details")))
    }
  }
  // @Test
  def foo {
    val src = """
    object Test {
      case class MyExample(x: Int)

      def example = {
        val ex: Array[MyExample] = new Array[MyExample](10)
        optimize {
          for (MyExample(x) <- ex) {
            println(x)
          }
        }
      }

      example
    }
    """
    // """
    //   class Ident(val name: String)
    //   object Ident {
    //     def unapply(id: Ident): Option[String] = Some(id.name)
    //   }
    //   def foo {
    //     val ints = List(1, 2, 3)
    //     val idents = List("a", "b", "c").map(new Ident(_))
    //     for ((i, id @ Ident(name)) <- ints zip idents) {
    //       if (i > 0) {
    //         println(s"$i: $name ($id)")
    //       }
    //     }
    //   }
    //   foo
    //     // for ((superAcc, superArg @ Ident(name)) <- superParamAccessors zip superArgs) {
    //     //   if (mexists(vparamss)(_.symbol == superArg.symbol)) {
    //     //     val alias = (
    //     //       superAcc.initialize.alias
    //     //         orElse (superAcc getter superAcc.owner)
    //     //         filter (alias => superClazz.info.nonPrivateMember(alias.name) == alias)
    //     //     )
    //     //     if (alias.exists && !alias.accessed.isVariable && !isRepeatedParamType(alias.accessed.info)) {
    //     //       val ownAcc = clazz.info decl name suchThat (_.isParamAccessor) match {
    //     //         case acc if !acc.isDeferred && acc.hasAccessorFlag => acc.accessed
    //     //         case acc                                           => acc
    //     //       }
    //     //       ownAcc match {
    //     //         case acc: TermSymbol if !acc.isVariable =>
    //     //           debuglog(s"$acc has alias ${alias.fullLocationString}")
    //     //           acc setAlias alias
    //     //         case _ =>
    //     //       }
    //     //     }
    //     //   }
    //     // }
    // """

    val (_, messages) = compileFast(src)
    println(messages)

    // assertPluginCompilesSnippetFine(src)

    // import scalaxy.streams.strategy.foolish
    // testMessages(src, streamMsg("List.map -> List", "List.withFilter.foreach"))
  }

  // @Test
  def testTake {
    val src = """
      val map = Map(1 -> 2, 4 -> 5);
      val keys = List(1, 3);
      def isOk(v: Int) = v % 2 == 0;
      var res = 0;
      for (k <- keys) {
        for (v <- map get k if isOk(k)) {
          res += v
        }
      }
      res
            // for (tree <- context.unit.synthetics get sym if shouldAdd(sym)) { // OPT: shouldAdd is usually true. Call it here, rather than in the outer loop
            //   newStats += typedStat(tree) // might add even more synthetics to the scope
            //   context.unit.synthetics -= sym
            // }
    """

    import scalaxy.streams.strategy.safe
    testMessages(src, streamMsg("Option.get"))

    //   // val (_, messages) = compileFast(src)
    //   // println(messages)
  }

  // @Ignore
  // @Test
  def testComp {
    // val src = """(
    //   Some[Any](1).orNull
    // )"""
    val src = """
      val msg = {
        try {
          val foo = 10
          Some(foo)
        } catch {
          case ex: Throwable => None
        }
      } get

      msg
    """

    // assertPluginCompilesSnippetFine(src)
    // val src = """
    //   (
    //     (None: Option[Int]).orElse(None),
    //       Option[Any](null).orElse(None),
    //       Some(1).orElse(None),
    //     (None: Option[Int]).orElse(Some(2)),
    //       Option[Any](null).orElse(Some(2)),
    //       Some(1).orElse(Some(2)),
    //     (None: Option[Int]).orElse(Option(3)),
    //       Option[Any](null).orElse(Option(3)),
    //       Some(1).orElse(Option(3)),
    //     (None: Option[String]).orElse(Option(null)),
    //       Option[String](null).orElse(Option(null)),
    //       Some("a").orElse(Option(null))
    //   )
    // """

    {
      import scalaxy.streams.strategy.foolish
      testMessages(src, streamMsg())

    //   // val (_, messages) = compileFast(src)
    //   // println(messages)
    }
  }

  // @Test
  def testPerf {
    // streamMsg("Range.map.flatMap(Range.map.withFilter.flatMap(Range.map)) -> IndexedSeq"),

    ensureFasterCodeWithSameResult(
      decls = "",
      // val n = 20;
      code = """
        for (i <- 0 to n;
             ii = i * i;
             j <- i to n;
             jj = j * j;
             if (ii - jj) % 2 == 0;
             k <- (i + j) to n)
          yield ii * jj * k
      """,
      params = Array(2, 10, 100),
      minFaster = 30)
  }
}
