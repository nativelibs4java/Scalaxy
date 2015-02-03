object Generator extends App
{
  val controlClassNames = List(
    "Accordion",
    "AccordionBuilder",
    "Button",
    "ButtonBase",
    "ButtonBaseBuilder",
    "ButtonBuilder",
    "Cell",
    "CellBuilder",
    "CheckBox",
    "CheckBoxBuilder",
    "CheckBoxTreeItem",
    "CheckBoxTreeItemBuilder",
    "CheckMenuItem",
    "CheckMenuItemBuilder",
    "ChoiceBox",
    "ChoiceBoxBuilder",
    "ColorPicker",
    "ColorPickerBuilder",
    "ComboBox",
    "ComboBoxBase",
    "ComboBoxBaseBuilder",
    "ComboBoxBuilder",
    "ContentDisplay",
    "ContextMenu",
    "ContextMenuBuilder",
    "Control",
    "ControlBuilder",
    "CustomMenuItem",
    "CustomMenuItemBuilder",
    "FocusModel",
    "Hyperlink",
    "HyperlinkBuilder",
    "IndexedCell",
    "IndexedCellBuilder",
    "IndexRange",
    "IndexRangeBuilder",
    "Label",
    "LabelBuilder",
    "Labeled",
    "LabeledBuilder",
    "ListCell",
    "ListCellBuilder",
    "ListView",
    "ListViewBuilder",
    "Menu",
    "MenuBar",
    "MenuBarBuilder",
    "MenuBuilder",
    "MenuButton",
    "MenuButtonBuilder",
    "MenuItem",
    "MenuItemBuilder",
    "MultipleSelectionModel",
    "MultipleSelectionModelBase",
    "MultipleSelectionModelBuilder",
    "OverrunStyle",
    "Pagination",
    "PaginationBuilder",
    "PasswordField",
    "PasswordFieldBuilder",
    "PopupControl",
    "PopupControlBuilder",
    "ProgressBar",
    "ProgressBarBuilder",
    "ProgressIndicator",
    "ProgressIndicatorBuilder",
    "RadioButton",
    "RadioButtonBuilder",
    "RadioMenuItem",
    "RadioMenuItemBuilder",
    "ScrollBar",
    "ScrollBarBuilder",
    "ScrollPane",
    "ScrollPaneBuilder",
    "SelectionMode",
    "SelectionModel",
    "Separator",
    "SeparatorBuilder",
    "SeparatorMenuItem",
    "SeparatorMenuItemBuilder",
    "SingleSelectionModel",
    "Skin",
    "Skinnable",
    "Slider",
    "SliderBuilder",
    "SplitMenuButton",
    "SplitMenuButtonBuilder",
    "SplitPane",
    "SplitPaneBuilder",
    "Tab",
    "TabBuilder",
    "TableCell",
    "TableCellBuilder",
    "TableColumn",
    "TableColumnBuilder",
    "TablePosition",
    "TablePositionBuilder",
    "TableRow",
    "TableRowBuilder",
    "TableView",
    "TableViewBuilder",
    "TabPane",
    "TabPaneBuilder",
    "TextArea",
    "TextAreaBuilder",
    "TextField",
    "TextFieldBuilder",
    "TextInputControl",
    "TextInputControlBuilder",
    "TitledPane",
    "TitledPaneBuilder",
    "Toggle",
    "ToggleButton",
    "ToggleButtonBuilder",
    "ToggleGroup",
    "ToggleGroupBuilder",
    "ToolBar",
    "ToolBarBuilder",
    "Tooltip",
    "TooltipBuilder",
    "TreeCell",
    "TreeCellBuilder",
    "TreeItem",
    "TreeItemBuilder",
    "TreeView",
    "TreeViewBuilder",
    "UAStylesheetLoader"
  ).map("javafx.scene.control." + _)

  val hasTypeParam = Set(
    "javafx.scene.control.TableRow",
    "javafx.scene.control.TableView$ResizeFeatures"
  )
  val skipSetter = Set(
    //("javafx.scene.control.Control", "skin")
    "skin"
  )

  import javafx.beans.property._
  import java.lang.reflect.{ ParameterizedType, Type, TypeVariable, WildcardType }

  import java.io._

  // def getRawTypeName(t: Type) = t match {
  //   case c: Class[_] => c.getName
  //   case pt: ParameterizedType => pt.getRawType.asInstanceOf[Class[_]].getName
  //   case tp: TypeVariable[_] => tp.getName
  // }
  def javaTypeToScalaString(t: Type): String = {
    def helper(t: Type): String = t match {
      case c: Class[_] if c.getEnclosingClass != null =>
        helper(c.getEnclosingClass) + "." + c.getSimpleName

      case c: Class[_] =>
        c.getName

      case pt: ParameterizedType =>
        helper(pt.getRawType) +
        pt.getActualTypeArguments
          .map(javaTypeToScalaString)
          .mkString("[", ", ", "]")

      case tv @ ((_: TypeVariable[_] | _: WildcardType)) =>
        tv.toString
          .replace("? super ", "_ >: ")
          .replace("?", "_")

      // case _ =>
      //   t.toString
      //     .replaceAll("^(class|interface) ", "")
      //     .replaceAll("^javafx\\.beans\\.property\\.", "")
      //     .replace("<", "[")
      //     .replace(">", "]")
      //     //.replaceAll("\\.javafx\\.scene\\.control\\.(\\w+)\\$", "#")
      //     .replaceAll("\\.javafx\\.scene\\.control\\.(\\w+).\\$", ".")
      //     .replace("$", ".")
      //     .replace("? super ", "_ >: ")
      //     .replace("?", "_")
    }
    val h = helper(t)
    val res = t match {
      case c: Class[_] if hasTypeParam(c.getName) =>
        h + "[_]"
      case _ =>
        h
    }
    if (res.contains(".javafx")) {
      println(s"t = $t -> res = $res")
      sys.exit(1)
    }
    res
  }

  val sout = new StringWriter
  val out = new PrintWriter(sout)

  val list = (
    for (className <- controlClassNames;
         cls = Class.forName(className);
         clsTypeParams = cls.getTypeParameters;
         method <- cls.getMethods;
         methodName = method.getName;
         if method.getParameterTypes.isEmpty &&
            methodName.endsWith("Property") &&
            !methodName.contains("_");
         fieldName = methodName.replaceAll("Property$", "");
         if method.getReturnType.getName.endsWith("Property");
         fieldTpe = method.getReturnType;
         genericFieldTpe = method.getGenericReturnType;
         // foo = { println(s"className = $className, methodName = $methodName, genericFieldTpe = $genericFieldTpe"); 1 };
         //fieldTpeName = method.getReturnType.getName;
         fieldTpeName = javaTypeToScalaString(genericFieldTpe);
         isReadOnly = genericFieldTpe.toString.contains(
           "javafx.beans.property.ReadOnly");
         get = fieldTpe.getMethod("get");
         valueTpeName = get.getGenericReturnType match {
           case t if get.getReturnType.isPrimitive => t.toString.capitalize
           case _ if fieldTpe == classOf[StringProperty] ||
             fieldTpe == classOf[ReadOnlyStringProperty] => "String"
           case t =>
             val s = t.toString
             if (s == "T") {
               try {
                 val Array(pt) = genericFieldTpe.asInstanceOf[ParameterizedType].getActualTypeArguments

                 javaTypeToScalaString(pt)
               } catch { case ex =>
                 sys.error(s"cls = $cls, method = $method, genericFieldTpe = $genericFieldTpe: $ex")
               }
             } else {
              s
             }
         };
         if valueTpeName != "T") yield {

      val extClassName = fieldName.capitalize + "Getter" +
        (if (isReadOnly) "" else "AndSetter")

      val typeParamsSuffix = {
        val tparams = clsTypeParams.filter(p => fieldTpeName.matches(".*?\\b" + p + "\\b.*"))
        if (tparams.isEmpty)
          ""
        else 
          tparams.mkString("[", ", ", "]")
      }

      val macroTypeParam = valueTpeName.replace("_ >: ", "")

      (extClassName, (converterName: String) => {
        val methods = List(
          Some(s"def ${fieldName}: ${valueTpeName} = " +
            s"macro impl.PropertyGettersMacros.get[$macroTypeParam]"),
          if (isReadOnly || skipSetter(fieldName))//skipSetter((className, fieldName)))
            None
          else
            Some(s"def ${fieldName}_=(value: $macroTypeParam): Unit = " +
              s"macro impl.PropertyGettersMacros.set[$macroTypeParam]"))
        out.print(s"""
          implicit class $converterName$typeParamsSuffix(o: { def ${methodName}(): $fieldTpeName }) {
            ${methods.flatten.mkString("\n            ")}
          }
        """)
      })
    }
  ).toMap

  out.println("""package scalaxy.fx

import scala.language.implicitConversions

import javafx.beans.binding._
import javafx.beans.property._
import javafx.collections._

import scala.language.experimental.macros

private[fx] trait PropertyGetters
{
""")
  for ((baseName, group) <- list.toSeq.groupBy(_._1);
       ((_, printer), i) <- group.zipWithIndex) {
    printer(if (group.size == 1) baseName else baseName + (i + 1))
  }
  out.println("\n}")
  out.close()

  val file = new File("src/main/scala/scalaxy/fx/PropertyGetters.scala")
  val fout = new PrintWriter(new FileWriter(file))
  fout.print(sout)
  fout.close()

  // for ((n, list) <- list.groupBy(_._1._1); if list.size > 1) {
  //   print("Conflicts for " + n + ": " + list)
  // }
  println("Wrote " + list.size + " extensions.")
}
