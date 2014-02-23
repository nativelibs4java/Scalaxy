package scalaxy.test

import org.junit._
import org.junit.Assert._

class PrivacyTest {
  // TODO: embed parser and do proper tests.
  @Test
  def privateAccess {
    """
      object Foo {
        val privateByDefault = 10
        @public val explicitlyPublic = 12
      }

      println(Foo.privateByDefault)
    """
  }
}
