package scalaxy.js

import scala.reflect.api.Universe

trait QuasiQuoteHacks {

  val global: Universe
  import global._

  /**
    Quasiquotes generate a constructor like this for traits:

      def <init>() = {
        super.<init>;
        ()
      };

    This transformer will simply add the () to get super.<init>(); 
  */
  private lazy val traitsFixer = new Transformer {
    override def transform(tree: Tree) = tree match {
      case Block(List(Select(target, nme.CONSTRUCTOR)), value) =>
        Block(List(Apply(Select(target, nme.CONSTRUCTOR), Nil)), value)
      case ClassDef(mods, name, tparams, Template(parents, self, body)) if mods.hasFlag(Flag.TRAIT) =>
        println("FOUND TRAIT")
        ClassDef(
          mods,
          name,
          tparams,
          Template(
            parents,
            self,
            body.filter({
              case d: DefDef if d.name == nme.CONSTRUCTOR => false
              case _ => true
            })
          )
        )
      case _ =>
        super.transform(tree)
    }
  }

  def fixTraits(tree: Tree): Tree = traitsFixer.transform(tree)
}
