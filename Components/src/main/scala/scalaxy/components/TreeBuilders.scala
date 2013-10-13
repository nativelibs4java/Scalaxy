/*
 * ScalaCL - putting Scala on the GPU with JavaCL / OpenCL
 * http://scalacl.googlecode.com/
 *
 * Copyright (c) 2009-2013, Olivier Chafik (http://ochafik.com/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package scalaxy.components

import scala.language.implicitConversions
import scala.language.postfixOps

import scala.reflect.api.Universe

trait TreeBuilders
    extends MiscMatchers {
  val global: Universe

  import global._
  import definitions._

  type TreeGen = () => Tree

  def withSymbol[T <: Tree](sym: Symbol, tpe: Type = NoType)(tree: T): T
  def typed[T <: Tree](tree: T): T
  def typeCheck(tree: Tree, pt: Type): Tree
  def fresh(s: String): String
  def inferImplicitValue(pt: Type): Tree
  def setInfo(sym: Symbol, tpe: Type): Symbol
  def setType(sym: Symbol, tpe: Type): Symbol
  def setType(tree: Tree, tpe: Type): Tree
  def setPos(tree: Tree, pos: Position): Tree

  def replaceOccurrences(
    tree: Tree,
    mappingsSym: Map[Symbol, TreeGen],
    symbolReplacements: Map[Symbol, Symbol],
    treeReplacements: Map[Tree, TreeGen]) =
    {
      def key(s: Symbol) =
        ownerChain(s).map(_.toString)

      val mappings = mappingsSym.map {
        case (sym, treeGen) =>
          (key(sym), (sym, treeGen))
      }
      val result = new Transformer {
        override def transform(tree: Tree): Tree = {
          treeReplacements.get(tree).map(_()).getOrElse(
            tree match {
              case Ident(n) if tree.symbol != NoSymbol =>
                val treeKey = key(tree.symbol)
                mappings.get(treeKey).map({
                  case (sym, treeGen) =>
                    // typeCheck(
                    treeGen() //,
                  //   if (tree.tpe != null && tree.tpe != NoType)
                  //     tree.tpe
                  //   else
                  //     tree.symbol.typeSignature
                  // )
                }).getOrElse(super.transform(tree))
              case _ =>
                super.transform(tree)
            }
          )
        }
      }.transform(tree)

      //for ((fromSym, toSym) <- symbolReplacements)
      //  new ChangeOwnerTraverser(fromSym, toSym).traverse(result)

      typed {
        result
      }
    }

  def primaryConstructor(tpe: Type): Symbol = {
    tpe.members.iterator
      .find(s => s.isMethod && s.asMethod.isPrimaryConstructor)
      .getOrElse(sys.error("No primary constructor for " + tpe))
  }

  def apply(sym: Symbol)(target: Tree, args: List[Tree]) = {
    withSymbol(sym) {
      Apply(
        withSymbol(sym) {
          target
        },
        args
      )
    }
  }
  def typeApply(sym: Symbol)(target: Tree, targs: List[TypeTree]) = {
    withSymbol(sym) {
      TypeApply(
        withSymbol(sym) {
          target
        },
        targs
      )
    }
  }

  def defaultValue(tpe: Type): Any = tpe.normalize match {
    case IntTpe => 0
    case BooleanTpe => false
    case ByteTpe => 0: Byte
    case ShortTpe => 0: Short
    case CharTpe => '\0'
    case LongTpe => 0L
    case FloatTpe => 0.0f
    case DoubleTpe => 0.0
    case s => null
  }

  // TreeGen.mkIsInstanceOf adds an extra Apply (and does not set all symbols), which makes it apparently useless in our case(s)
  def newIsInstanceOf(tree: Tree, tpe: Type) = {
    TypeApply(
      Select(
        tree,
        N("isInstanceOf")
      ),
      List(TypeTree(tpe))
    )
  }
  def newApplyCall(array: Tree, index: Tree) = {
    Apply(
      Select(
        array,
        N("apply")
      ),
      List(index)
    )
  }

  def newSelect(target: Tree, name: Name, typeArgs: List[TypeTree] = Nil) =
    newApply(target, name, typeArgs, null)

  def newApply(target: Tree /*, targetType: Type*/ , name: Name, typeArgs: List[TypeTree] = Nil, args: List[Tree] = Nil) = {
    val select = Select(target, name)
    if (!typeArgs.isEmpty)
      Apply(
        TypeApply(select, typeArgs),
        args
      )
    else if (args != null)
      Apply(select, args)
    else
      select
  }

  def newInstance(tpe: Type, constructorArgs: List[Tree]) = {
    Apply(
      Select(
        New(TypeTree(tpe)),
        nme.CONSTRUCTOR
      ),
      constructorArgs
    )
  }

  def newCollectionApply(collectionModuleTree: => Tree, typeExpr: TypeTree, values: Tree*) =
    newApply(collectionModuleTree, applyName, List(typeExpr), values.toList)

  def newScalaPackageTree =
    Ident(ScalaPackage)

  def newScalaCollectionPackageTree =
    Ident(ScalaCollectionPackage) /*
    withSymbol(ScalaCollectionPackage) { 
      Select(newScalaPackageTree, N("collection")) 
    }*/

  def newSomeModuleTree = typed {
    Ident(SomeModule)
    /*withSymbol(SomeModule) { 
      Select(newScalaPackageTree, N("Some"))
    }*/
  }

  def newNoneModuleTree = typed {
    Ident(NoneModule) /*
    withSymbol(NoneModule) {
      Select(newScalaPackageTree, N("None"))
    }*/
  }

  def newSeqModuleTree = typed {
    Ident(SeqModule) /*
    withSymbol(SeqModule) {
      Select(newScalaCollectionPackageTree, N("Seq"))
    }*/
  }

  def newSetModuleTree = typed {
    Ident(SetModule) /*
    withSymbol(SetModule) {
      Select(newScalaCollectionPackageTree, N("Set"))
    }*/
  }

  def newArrayModuleTree = typed {
    Ident(ArrayModule) /*
    withSymbol(ArrayModule) {
      Select(newScalaPackageTree, N("Array"))
    }*/
  }

  def newSeqApply(typeExpr: TypeTree, values: Tree*) =
    newApply(newSeqModuleTree, applyName, List(typeExpr), values.toList)

  def newSomeApply(tpe: Type, value: Tree) =
    newApply(newSomeModuleTree, applyName, List(TypeTree(tpe)), List(value))

  def newArrayApply(typeExpr: TypeTree, values: Tree*) =
    newApply(newArrayModuleTree, applyName, List(typeExpr), values.toList)

  def newArrayMulti(arrayType: Type, componentTpe: Type, lengths: => List[Tree], manifest: Tree) =
    typed {
      val sym = (ArrayModule.asModule.moduleClass.asType.toType member newTermName("ofDim"))
        .suchThat(s => s.isMethod && s.asMethod.paramss.flatten.size == lengths.size + 1)
      //.getOrElse(sys.error("No Array.ofDim found"))
      withSymbol(sym) {
        Apply(
          withSymbol(sym) {
            Apply(
              TypeApply(
                withSymbol(sym) {
                  Select(
                    Ident(
                      ArrayModule
                    ),
                    N("ofDim")
                  )
                },
                List(TypeTree(componentTpe))
              ),
              lengths
            )
          },
          List(manifest)
        )
      }
    }

  def newArray(componentType: Type, length: => Tree) =
    newArrayWithArrayType(appliedType(ArrayClass.asType.toType, List(componentType)), length)

  def newArrayWithArrayType(arrayType: Type, length: => Tree) =
    typed {
      val sym = primaryConstructor(arrayType)
      apply(sym)(
        Select(
          New(TypeTree(arrayType)),
          sym
        ),
        List(length)
      )
    }

  def newUpdate(pos: Position, array: => Tree, index: => Tree, value: => Tree) = {
    val a = array
    assert(a.tpe != null)
    val sym = a.tpe member updateName()
    typed {
      atPos(pos) {
        apply(sym)(
          Select(
            a,
            N("update")
          ),
          List(index, typed { value })
        )
      }
    }
  }

  def binOp(a: Tree, op: TermName, b: Tree) =
    Apply(
      Select(a, op),
      List(b))

  def newIsNotNull(target: Tree) = typed {
    binOp(target, NE, newNull(target.tpe))
  }

  def newArrayLength(a: Tree) =
    withSymbol(a.tpe.member(lengthName()), IntTpe) {
      Select(a, lengthName())
    }

  def boolAnd(a: Tree, b: Tree) = typed {
    if (a == null)
      b
    else if (b == null)
      a
    else
      binOp(a, ZAND /* AMPAMP */ , b)
  }
  def boolOr(a: Tree, b: Tree) = typed {
    if (a == null)
      b
    else if (b == null)
      a
    else
      binOp(a, ZOR, b)
  }
  def ident(sym: Symbol, tpe: Type, n: Name, pos: Position = NoPosition): Ident = {
    assert(sym != NoSymbol)
    val i = Ident(sym)
    try {
      typeCheck(
        i,
        sym.typeSignature
      ).asInstanceOf[Ident]
    } catch {
      case _: Throwable =>
        i
    }
    /*val v = Ident(sym)
    //val tpe = sym.typeSignature
    setPos(v, pos)
    withSymbol(sym, tpe) { v }
    */
  }

  def boolNot(a: Tree) = {
    Select(a, UNARY_!)
  }

  def intAdd(a: => Tree, b: => Tree) =
    binOp(a, PLUS, b)

  def intDiv(a: => Tree, b: => Tree) =
    binOp(a, DIV, b)

  def intSub(a: => Tree, b: => Tree) =
    binOp(a, MINUS, b)

  def newAssign(target: IdentGen, value: Tree) =
    Assign(target(), value)

  def incrementIntVar(identGen: IdentGen, value: Tree = newInt(1)) =
    newAssign(identGen, intAdd(identGen(), value))

  def decrementIntVar(identGen: IdentGen, value: Tree) = typed {
    //identGen() === intSub(identGen(), value)
    Assign(
      identGen(),
      intSub(identGen(), value)
    )
  }

  def whileLoop(cond: Tree, body: Tree): Tree = {
    val lab = newTermName(fresh("while$"))
    LabelDef(
      lab,
      Nil,
      If(
        cond,
        Block(
          if (body == null)
            Nil
          else
            List(body),
          Apply(
            Ident(lab),
            Nil
          )
        ),
        newUnit
      )
    )
  }

  type IdentGen = () => Ident

  private lazy val anyValTypeInfos = Seq[(Class[_], Type, AnyVal)](
    (classOf[java.lang.Boolean], BooleanTpe, false),
    (classOf[java.lang.Integer], IntTpe, 0),
    (classOf[java.lang.Long], LongTpe, 0: Long),
    (classOf[java.lang.Short], ShortTpe, 0: Short),
    (classOf[java.lang.Byte], ByteTpe, 0: Byte),
    (classOf[java.lang.Character], CharTpe, 0.asInstanceOf[Char]),
    (classOf[java.lang.Double], DoubleTpe, 0.0),
    (classOf[java.lang.Float], FloatTpe, 0.0f)
  )
  lazy val classToType: Map[Class[_], Type] =
    (anyValTypeInfos.map { case (cls, tpe, defVal) => cls -> tpe }).toMap

  lazy val typeToDefaultValue: Map[Type, AnyVal] =
    (anyValTypeInfos.map { case (cls, tpe, defVal) => tpe -> defVal }).toMap

  def newConstant(v: Any, tpe: Type = null) = typed {
    Literal(Constant(v))
  } /*.setType(
      if (tpe != null) 
        tpe
      else if (v.isInstanceOf[String])
        StringClass.tpe
      else
        classToType(v.getClass)
    )
  }*/

  def newBool(v: Boolean) = newConstant(v)
  def newInt(v: Int) = newConstant(v)
  def newLong(v: Long) = newConstant(v)

  def newNull(tpe: Type) = newConstant(null, tpe)

  def newDefaultValue(tpe: Type) = {
    if (isAnyVal(tpe))
      newConstant(typeToDefaultValue(tpe), tpe)
    else
      newNull(tpe)
  }

  def newOneValue(tpe: Type) = {
    assert(isAnyVal(tpe))
    newConstant(1: Byte)
  }

  def newUnit() =
    newConstant(())

  case class ValueDef(rawIdentGen: IdentGen, definition: ValDef, tpe: Type) {
    var identUsed = false
    val identGen: IdentGen = () => {
      identUsed = true
      rawIdentGen()
    }
    def apply() = identGen()

    def defIfUsed = ifUsed(definition)
    def ifUsed[V](v: => V) = if (identUsed) Some(v) else None
  }
  implicit def ValueDef2IdentGen(vd: ValueDef) = if (vd == null) null else vd.identGen

  def simpleBuilderResult(builder: Tree): Tree =
    newApply(builder, resultName)

  def addAssign(target: Tree, toAdd: Tree) = {
    // val sym = (target.tpe member addAssignName())
    //apply(sym)(
    Apply(
      Select(
        target,
        addAssignName()
      ),
      List(toAdd)
    )
  }

  lazy val TypeRef(manifestPre, manifestSym, _) = typeOf[Manifest[Int]]
  def toArray(tree: Tree, componentType: Type) = {
    val manifest = inferImplicitValue(typeRef(manifestPre, manifestSym, List(componentType)))
    assert(manifest != EmptyTree, "Failed to get manifest for " + componentType)

    Apply(
      TypeApply(
        Select(
          tree,
          toArrayName
        ),
        List(TypeTree(componentType))
      ),
      List(manifest)
    )
  }

  def newIf(cond: Tree, thenTree: Tree, elseTree: Tree = null) =
    If(cond, thenTree, Option(elseTree).getOrElse(EmptyTree))

  def newVal(prefix: String, value: Tree, tpe: Type) =
    newValueDef(prefix, false, value, tpe)

  def newVar(prefix: String, value: Tree, tpe: Type) =
    newValueDef(prefix, true, value, tpe)

  private def newValueDef(prefix: String, mutable: Boolean, value: Tree, tpe: Type) = {
    val vd = ValDef(
      if (mutable) Modifiers(Flag.MUTABLE) else NoMods,
      fresh(prefix): TermName,
      TypeTree(tpe),
      value)
    ValueDef(() => Ident(vd.name), vd, tpe)
  }
}

