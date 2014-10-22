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
    with StreamTransforms
{
  import global._

  scalaxy.streams.impl.verbose = true
  // scalaxy.streams.impl.veryVerbose = true
  // scalaxy.streams.impl.debug = true

  val fnRx = raw".*scala\.Function0\.apply.*"

  // @Ignore
  // @Test
  // def testArrayMapFilterMap {
  //   // (_: Array[Int]).map(_ + 2).filter(_ < 3).map(_.hashCode)
  //   val SomeStream(stream) = typecheck(q"""
  //     (null: Array[String]).map(_ + "ha").filter(_.length < 3).map(_.hashCode)
  //   """)
  //   val Stream(_, ArrayStreamSource(_, _, _), ops, ArrayBuilderSink, false) = stream
  //   val List(ArrayOpsOp, MapOp(_, _), ArrayOpsOp, FilterOp(_, false, "filter"), ArrayOpsOp, MapOp(_, _)) = ops
  // }

  @Ignore
  @Test
  def testTuple {
    val src = """
      val o = Option(10)
      def foo(v: Option[Option[Int]]) = v.flatMap(a => {
        val m = a//.map(_.toString)

        m
      })
    """

    { import scalaxy.streams.strategy.safe
      testMessages(src, streamMsg("Option.flatMap -> Option")) }
  }
}

/*

      def inferForApproxPt =
        if (isFullyDefined(pt)) {
          inferFor(pt.instantiateTypeParams(ptparams, ptparams map (x => WildcardType))) flatMap { targs =>
            val ctorTpInst = tree.tpe.instantiateTypeParams(undetparams, targs)
            val resTpInst  = skipImplicit(ctorTpInst.finalResultType)
            val ptvars     =
              ptparams map {
                // since instantiateTypeVar wants to modify the skolem that corresponds to the method's type parameter,
                // and it uses the TypeVar's origin to locate it, deskolemize the existential skolem to the method tparam skolem
                // (the existential skolem was created by adaptConstrPattern to introduce the type slack necessary to soundly deal with variant type parameters)
                case skolem if skolem.isGADTSkolem => freshVar(skolem.deSkolemize.asInstanceOf[TypeSymbol])
                case p => freshVar(p)
              }

            val ptV        = pt.instantiateTypeParams(ptparams, ptvars)

            if (isPopulated(resTpInst, ptV)) {
              ptvars foreach instantiateTypeVar
              debuglog("isPopulated "+ resTpInst +", "+ ptV +" vars= "+ ptvars)
              Some(targs)
            } else None
          }
        } else None
*/
