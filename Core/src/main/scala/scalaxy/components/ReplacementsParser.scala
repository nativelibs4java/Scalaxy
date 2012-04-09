package scalaxy; package components

object ReplacementDefinitions {
  import scala.reflect.api._
  import scala.reflect.runtime._
  import scala.reflect.runtime.Mirror._
  import definitions._
  
  private lazy val ReplacementClass = staticClass("scalaxy.Replacement")//definitions.getClass(newTypeName("scalaxy.Replacement"))
  
  private lazy val defaultValues = Map(
    IntClass.tpe -> 0,
    ShortClass.tpe -> (0: Short),
    LongClass.tpe -> (0: Long),
    ByteClass.tpe -> (0: Byte),
    FloatClass.tpe -> 0f,
    DoubleClass.tpe -> 0.0,
    BooleanClass.tpe -> false,
    CharClass.tpe -> '\0'
  )
  private def getDefaultValue(tpe: Type): Any = 
    defaultValues.get(tpe).getOrElse(null)
  
  def invokeMethod(target: AnyRef, method: Symbol, params: List[AnyRef]) = {
    //println("target = " + target)
    if (true) {
      val methodName = method.name.toString
      val javaMethod = target.getClass.getDeclaredMethods.find(_.getName == methodName).getOrElse(
        throw new RuntimeException("No method '" + methodName + "' found in " + target.getClass.getName + ":\n" + getClass.getDeclaredMethods.mkString("\n"))
      )
      javaMethod.invoke(target, params:_*)
      //println("mm = " + mm)
    } else {
      invoke(target, method)(params)
    }
  }
  def getReplacementDefinitions(holder: AnyRef): Seq[(String, Replacement)] = {
    //val holder = Example
    //val holderSym = staticClass("scalaxy.Example")
    //val holder = companionInstance(holderSym)
    val holderType = typeOfInstance(holder)
    //val holderCls = classOf[Example]
    //val holderSym = classToSymbol(holderCls)//staticClass("scalaxy.Example")
    //val holderType = classToType(holderCls)//holderSym.tpe//.deconst.dealias
    //val holder = holderCls.newInstance.asInstanceOf[AnyRef]
    
    def dbgStr(t: Type) = {
      t + " (" + t.getClass.getName + " <- " + t.getClass.getSuperclass.getName + ") = " + debugString(t)
    }
    object FollowMethodResult {
      def unapply(t: Type): Option[Type] = t match {
        case MethodType(_, r) =>
          unapply(r)
        case _ =>
          Some(t)
      }
    }
    holderType.members.filter(_.isMethod).map(m => (m, m.tpe)).collect { 
      case (m, PolyType(paramsyms, mt @ FollowMethodResult(result)))
      if result == ReplacementClass.tpe =>
        def getParamTypes(t: Type): List[Type] = t match {
          case MethodType(tt, r) =>
            tt.map(_.tpe) ++ getParamTypes(r)
          case _ =>
            List()
        }
        val actualParamTypes = getParamTypes(mt)
        val defaultParams = 
          actualParamTypes.map(getDefaultValue(_).asInstanceOf[AnyRef])
          
        val r = invokeMethod(holder, m, defaultParams)
        //println("r = " + r)
        (holder + "." + m.name, r.asInstanceOf[Replacement])
    }
  }
}
