package scalaxy.fx

import scala.language.implicitConversions

import javafx.beans.binding._
import javafx.beans.property._
import javafx.collections._

import scala.language.experimental.macros

private[fx] trait PropertyGetters
{


          implicit class AcceleratorGetterAndSetter(o: { def acceleratorProperty(): javafx.beans.property.ObjectProperty[javafx.scene.input.KeyCombination] }) {
            def accelerator: javafx.scene.input.KeyCombination = macro impl.PropertyGettersMacros.get[javafx.scene.input.KeyCombination]
            def accelerator_=(value: javafx.scene.input.KeyCombination): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.input.KeyCombination]
          }
        
          implicit class ActivatedGetter(o: { def activatedProperty(): javafx.beans.property.ReadOnlyBooleanProperty }) {
            def activated: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
          }
        
          implicit class AlignmentGetterAndSetter(o: { def alignmentProperty(): javafx.beans.property.ObjectProperty[javafx.geometry.Pos] }) {
            def alignment: javafx.geometry.Pos = macro impl.PropertyGettersMacros.get[javafx.geometry.Pos]
            def alignment_=(value: javafx.geometry.Pos): Unit = macro impl.PropertyGettersMacros.set[javafx.geometry.Pos]
          }
        
          implicit class AllowIndeterminateGetterAndSetter(o: { def allowIndeterminateProperty(): javafx.beans.property.BooleanProperty }) {
            def allowIndeterminate: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def allowIndeterminate_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class AnchorGetter(o: { def anchorProperty(): javafx.beans.property.ReadOnlyIntegerProperty }) {
            def anchor: Int = macro impl.PropertyGettersMacros.get[Int]
          }
        
          implicit class AnimatedGetterAndSetter(o: { def animatedProperty(): javafx.beans.property.BooleanProperty }) {
            def animated: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def animated_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class ArmedGetter(o: { def armedProperty(): javafx.beans.property.ReadOnlyBooleanProperty }) {
            def armed: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
          }
        
          implicit class ArmedGetterAndSetter(o: { def armedProperty(): javafx.beans.property.BooleanProperty }) {
            def armed: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def armed_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class AutoFixGetterAndSetter(o: { def autoFixProperty(): javafx.beans.property.BooleanProperty }) {
            def autoFix: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def autoFix_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class AutoHideGetterAndSetter(o: { def autoHideProperty(): javafx.beans.property.BooleanProperty }) {
            def autoHide: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def autoHide_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class BlendModeGetterAndSetter(o: { def blendModeProperty(): javafx.beans.property.ObjectProperty[javafx.scene.effect.BlendMode] }) {
            def blendMode: javafx.scene.effect.BlendMode = macro impl.PropertyGettersMacros.get[javafx.scene.effect.BlendMode]
            def blendMode_=(value: javafx.scene.effect.BlendMode): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.effect.BlendMode]
          }
        
          implicit class BlockIncrementGetterAndSetter(o: { def blockIncrementProperty(): javafx.beans.property.DoubleProperty }) {
            def blockIncrement: Double = macro impl.PropertyGettersMacros.get[Double]
            def blockIncrement_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class BoundsInLocalGetter(o: { def boundsInLocalProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.geometry.Bounds] }) {
            def boundsInLocal: javafx.geometry.Bounds = macro impl.PropertyGettersMacros.get[javafx.geometry.Bounds]
          }
        
          implicit class BoundsInParentGetter(o: { def boundsInParentProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.geometry.Bounds] }) {
            def boundsInParent: javafx.geometry.Bounds = macro impl.PropertyGettersMacros.get[javafx.geometry.Bounds]
          }
        
          implicit class ButtonCellGetterAndSetter[T](o: { def buttonCellProperty(): javafx.beans.property.ObjectProperty[javafx.scene.control.ListCell[T]] }) {
            def buttonCell: javafx.scene.control.ListCell[T] = macro impl.PropertyGettersMacros.get[javafx.scene.control.ListCell[T]]
            def buttonCell_=(value: javafx.scene.control.ListCell[T]): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.control.ListCell[T]]
          }
        
          implicit class CacheGetterAndSetter(o: { def cacheProperty(): javafx.beans.property.BooleanProperty }) {
            def cache: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def cache_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class CacheHintGetterAndSetter(o: { def cacheHintProperty(): javafx.beans.property.ObjectProperty[javafx.scene.CacheHint] }) {
            def cacheHint: javafx.scene.CacheHint = macro impl.PropertyGettersMacros.get[javafx.scene.CacheHint]
            def cacheHint_=(value: javafx.scene.CacheHint): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.CacheHint]
          }
        
          implicit class CancelButtonGetterAndSetter(o: { def cancelButtonProperty(): javafx.beans.property.BooleanProperty }) {
            def cancelButton: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def cancelButton_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class CaretPositionGetter(o: { def caretPositionProperty(): javafx.beans.property.ReadOnlyIntegerProperty }) {
            def caretPosition: Int = macro impl.PropertyGettersMacros.get[Int]
          }
        
          implicit class CellFactoryGetterAndSetter[T](o: { def cellFactoryProperty(): javafx.beans.property.ObjectProperty[javafx.util.Callback[javafx.scene.control.TreeView[T], javafx.scene.control.TreeCell[T]]] }) {
            def cellFactory: javafx.util.Callback[javafx.scene.control.TreeView[T], javafx.scene.control.TreeCell[T]] = macro impl.PropertyGettersMacros.get[javafx.util.Callback[javafx.scene.control.TreeView[T], javafx.scene.control.TreeCell[T]]]
            def cellFactory_=(value: javafx.util.Callback[javafx.scene.control.TreeView[T], javafx.scene.control.TreeCell[T]]): Unit = macro impl.PropertyGettersMacros.set[javafx.util.Callback[javafx.scene.control.TreeView[T], javafx.scene.control.TreeCell[T]]]
          }
        
          implicit class CellValueFactoryGetterAndSetter[S, T](o: { def cellValueFactoryProperty(): javafx.beans.property.ObjectProperty[javafx.util.Callback[javafx.scene.control.TableColumn.CellDataFeatures[S, T], javafx.beans.value.ObservableValue[T]]] }) {
            def cellValueFactory: javafx.util.Callback[javafx.scene.control.TableColumn.CellDataFeatures[S, T], javafx.beans.value.ObservableValue[T]] = macro impl.PropertyGettersMacros.get[javafx.util.Callback[javafx.scene.control.TableColumn.CellDataFeatures[S, T], javafx.beans.value.ObservableValue[T]]]
            def cellValueFactory_=(value: javafx.util.Callback[javafx.scene.control.TableColumn.CellDataFeatures[S, T], javafx.beans.value.ObservableValue[T]]): Unit = macro impl.PropertyGettersMacros.set[javafx.util.Callback[javafx.scene.control.TableColumn.CellDataFeatures[S, T], javafx.beans.value.ObservableValue[T]]]
          }
        
          implicit class ClipGetterAndSetter(o: { def clipProperty(): javafx.beans.property.ObjectProperty[javafx.scene.Node] }) {
            def clip: javafx.scene.Node = macro impl.PropertyGettersMacros.get[javafx.scene.Node]
            def clip_=(value: javafx.scene.Node): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.Node]
          }
        
          implicit class ClosableGetterAndSetter(o: { def closableProperty(): javafx.beans.property.BooleanProperty }) {
            def closable: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def closable_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class CollapsibleGetterAndSetter(o: { def collapsibleProperty(): javafx.beans.property.BooleanProperty }) {
            def collapsible: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def collapsible_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class ColumnResizePolicyGetterAndSetter(o: { def columnResizePolicyProperty(): javafx.beans.property.ObjectProperty[javafx.util.Callback[javafx.scene.control.TableView.ResizeFeatures[_], java.lang.Boolean]] }) {
            def columnResizePolicy: javafx.util.Callback[javafx.scene.control.TableView.ResizeFeatures[_], java.lang.Boolean] = macro impl.PropertyGettersMacros.get[javafx.util.Callback[javafx.scene.control.TableView.ResizeFeatures[_], java.lang.Boolean]]
            def columnResizePolicy_=(value: javafx.util.Callback[javafx.scene.control.TableView.ResizeFeatures[_], java.lang.Boolean]): Unit = macro impl.PropertyGettersMacros.set[javafx.util.Callback[javafx.scene.control.TableView.ResizeFeatures[_], java.lang.Boolean]]
          }
        
          implicit class ComparatorGetterAndSetter[T](o: { def comparatorProperty(): javafx.beans.property.ObjectProperty[java.util.Comparator[T]] }) {
            def comparator: java.util.Comparator[T] = macro impl.PropertyGettersMacros.get[java.util.Comparator[T]]
            def comparator_=(value: java.util.Comparator[T]): Unit = macro impl.PropertyGettersMacros.set[java.util.Comparator[T]]
          }
        
          implicit class ConsumeAutoHidingEventsGetterAndSetter(o: { def consumeAutoHidingEventsProperty(): javafx.beans.property.BooleanProperty }) {
            def consumeAutoHidingEvents: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def consumeAutoHidingEvents_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class ContentDisplayGetterAndSetter(o: { def contentDisplayProperty(): javafx.beans.property.ObjectProperty[javafx.scene.control.ContentDisplay] }) {
            def contentDisplay: javafx.scene.control.ContentDisplay = macro impl.PropertyGettersMacros.get[javafx.scene.control.ContentDisplay]
            def contentDisplay_=(value: javafx.scene.control.ContentDisplay): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.control.ContentDisplay]
          }
        
          implicit class ContentGetterAndSetter(o: { def contentProperty(): javafx.beans.property.ObjectProperty[javafx.scene.Node] }) {
            def content: javafx.scene.Node = macro impl.PropertyGettersMacros.get[javafx.scene.Node]
            def content_=(value: javafx.scene.Node): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.Node]
          }
        
          implicit class ContextMenuGetterAndSetter(o: { def contextMenuProperty(): javafx.beans.property.ObjectProperty[javafx.scene.control.ContextMenu] }) {
            def contextMenu: javafx.scene.control.ContextMenu = macro impl.PropertyGettersMacros.get[javafx.scene.control.ContextMenu]
            def contextMenu_=(value: javafx.scene.control.ContextMenu): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.control.ContextMenu]
          }
        
          implicit class ConverterGetterAndSetter[T](o: { def converterProperty(): javafx.beans.property.ObjectProperty[javafx.util.StringConverter[T]] }) {
            def converter: javafx.util.StringConverter[T] = macro impl.PropertyGettersMacros.get[javafx.util.StringConverter[T]]
            def converter_=(value: javafx.util.StringConverter[T]): Unit = macro impl.PropertyGettersMacros.set[javafx.util.StringConverter[T]]
          }
        
          implicit class CurrentPageIndexGetterAndSetter(o: { def currentPageIndexProperty(): javafx.beans.property.IntegerProperty }) {
            def currentPageIndex: Int = macro impl.PropertyGettersMacros.get[Int]
            def currentPageIndex_=(value: Int): Unit = macro impl.PropertyGettersMacros.set[Int]
          }
        
          implicit class CursorGetterAndSetter(o: { def cursorProperty(): javafx.beans.property.ObjectProperty[javafx.scene.Cursor] }) {
            def cursor: javafx.scene.Cursor = macro impl.PropertyGettersMacros.get[javafx.scene.Cursor]
            def cursor_=(value: javafx.scene.Cursor): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.Cursor]
          }
        
          implicit class DefaultButtonGetterAndSetter(o: { def defaultButtonProperty(): javafx.beans.property.BooleanProperty }) {
            def defaultButton: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def defaultButton_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class DepthTestGetterAndSetter(o: { def depthTestProperty(): javafx.beans.property.ObjectProperty[javafx.scene.DepthTest] }) {
            def depthTest: javafx.scene.DepthTest = macro impl.PropertyGettersMacros.get[javafx.scene.DepthTest]
            def depthTest_=(value: javafx.scene.DepthTest): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.DepthTest]
          }
        
          implicit class DisableGetterAndSetter(o: { def disableProperty(): javafx.beans.property.BooleanProperty }) {
            def disable: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def disable_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class DisabledGetter(o: { def disabledProperty(): javafx.beans.property.ReadOnlyBooleanProperty }) {
            def disabled: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
          }
        
          implicit class DisclosureNodeGetterAndSetter(o: { def disclosureNodeProperty(): javafx.beans.property.ObjectProperty[javafx.scene.Node] }) {
            def disclosureNode: javafx.scene.Node = macro impl.PropertyGettersMacros.get[javafx.scene.Node]
            def disclosureNode_=(value: javafx.scene.Node): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.Node]
          }
        
          implicit class EditableGetterAndSetter(o: { def editableProperty(): javafx.beans.property.BooleanProperty }) {
            def editable: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def editable_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class EditingCellGetter[S](o: { def editingCellProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.scene.control.TablePosition[S, _]] }) {
            def editingCell: javafx.scene.control.TablePosition[S, _] = macro impl.PropertyGettersMacros.get[javafx.scene.control.TablePosition[S, _]]
          }
        
          implicit class EditingGetter(o: { def editingProperty(): javafx.beans.property.ReadOnlyBooleanProperty }) {
            def editing: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
          }
        
          implicit class EditingIndexGetter(o: { def editingIndexProperty(): javafx.beans.property.ReadOnlyIntegerProperty }) {
            def editingIndex: Int = macro impl.PropertyGettersMacros.get[Int]
          }
        
          implicit class EditingItemGetter[T](o: { def editingItemProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.scene.control.TreeItem[T]] }) {
            def editingItem: javafx.scene.control.TreeItem[T] = macro impl.PropertyGettersMacros.get[javafx.scene.control.TreeItem[T]]
          }
        
          implicit class EditorGetter(o: { def editorProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.scene.control.TextField] }) {
            def editor: javafx.scene.control.TextField = macro impl.PropertyGettersMacros.get[javafx.scene.control.TextField]
          }
        
          implicit class EffectGetterAndSetter(o: { def effectProperty(): javafx.beans.property.ObjectProperty[javafx.scene.effect.Effect] }) {
            def effect: javafx.scene.effect.Effect = macro impl.PropertyGettersMacros.get[javafx.scene.effect.Effect]
            def effect_=(value: javafx.scene.effect.Effect): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.effect.Effect]
          }
        
          implicit class EllipsisStringGetterAndSetter(o: { def ellipsisStringProperty(): javafx.beans.property.StringProperty }) {
            def ellipsisString: String = macro impl.PropertyGettersMacros.get[String]
            def ellipsisString_=(value: String): Unit = macro impl.PropertyGettersMacros.set[String]
          }
        
          implicit class EmptyGetter(o: { def emptyProperty(): javafx.beans.property.ReadOnlyBooleanProperty }) {
            def empty: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
          }
        
          implicit class EventDispatcherGetterAndSetter(o: { def eventDispatcherProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventDispatcher] }) {
            def eventDispatcher: javafx.event.EventDispatcher = macro impl.PropertyGettersMacros.get[javafx.event.EventDispatcher]
            def eventDispatcher_=(value: javafx.event.EventDispatcher): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventDispatcher]
          }
        
          implicit class ExpandedGetterAndSetter(o: { def expandedProperty(): javafx.beans.property.BooleanProperty }) {
            def expanded: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def expanded_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class ExpandedPaneGetterAndSetter(o: { def expandedPaneProperty(): javafx.beans.property.ObjectProperty[javafx.scene.control.TitledPane] }) {
            def expandedPane: javafx.scene.control.TitledPane = macro impl.PropertyGettersMacros.get[javafx.scene.control.TitledPane]
            def expandedPane_=(value: javafx.scene.control.TitledPane): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.control.TitledPane]
          }
        
          implicit class FitToHeightGetterAndSetter(o: { def fitToHeightProperty(): javafx.beans.property.BooleanProperty }) {
            def fitToHeight: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def fitToHeight_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class FitToWidthGetterAndSetter(o: { def fitToWidthProperty(): javafx.beans.property.BooleanProperty }) {
            def fitToWidth: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def fitToWidth_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class FocusModelGetterAndSetter[T](o: { def focusModelProperty(): javafx.beans.property.ObjectProperty[javafx.scene.control.FocusModel[javafx.scene.control.TreeItem[T]]] }) {
            def focusModel: javafx.scene.control.FocusModel[javafx.scene.control.TreeItem[T]] = macro impl.PropertyGettersMacros.get[javafx.scene.control.FocusModel[javafx.scene.control.TreeItem[T]]]
            def focusModel_=(value: javafx.scene.control.FocusModel[javafx.scene.control.TreeItem[T]]): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.control.FocusModel[javafx.scene.control.TreeItem[T]]]
          }
        
          implicit class FocusTraversableGetterAndSetter(o: { def focusTraversableProperty(): javafx.beans.property.BooleanProperty }) {
            def focusTraversable: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def focusTraversable_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class FocusedGetter(o: { def focusedProperty(): javafx.beans.property.ReadOnlyBooleanProperty }) {
            def focused: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
          }
        
          implicit class FocusedIndexGetter(o: { def focusedIndexProperty(): javafx.beans.property.ReadOnlyIntegerProperty }) {
            def focusedIndex: Int = macro impl.PropertyGettersMacros.get[Int]
          }
        
          implicit class FontGetterAndSetter(o: { def fontProperty(): javafx.beans.property.ObjectProperty[javafx.scene.text.Font] }) {
            def font: javafx.scene.text.Font = macro impl.PropertyGettersMacros.get[javafx.scene.text.Font]
            def font_=(value: javafx.scene.text.Font): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.text.Font]
          }
        
          implicit class GraphicGetterAndSetter(o: { def graphicProperty(): javafx.beans.property.ObjectProperty[javafx.scene.Node] }) {
            def graphic: javafx.scene.Node = macro impl.PropertyGettersMacros.get[javafx.scene.Node]
            def graphic_=(value: javafx.scene.Node): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.Node]
          }
        
          implicit class GraphicTextGapGetterAndSetter(o: { def graphicTextGapProperty(): javafx.beans.property.DoubleProperty }) {
            def graphicTextGap: Double = macro impl.PropertyGettersMacros.get[Double]
            def graphicTextGap_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class HalignmentGetterAndSetter(o: { def halignmentProperty(): javafx.beans.property.ObjectProperty[javafx.geometry.HPos] }) {
            def halignment: javafx.geometry.HPos = macro impl.PropertyGettersMacros.get[javafx.geometry.HPos]
            def halignment_=(value: javafx.geometry.HPos): Unit = macro impl.PropertyGettersMacros.set[javafx.geometry.HPos]
          }
        
          implicit class HbarPolicyGetterAndSetter(o: { def hbarPolicyProperty(): javafx.beans.property.ObjectProperty[javafx.scene.control.ScrollPane.ScrollBarPolicy] }) {
            def hbarPolicy: javafx.scene.control.ScrollPane.ScrollBarPolicy = macro impl.PropertyGettersMacros.get[javafx.scene.control.ScrollPane.ScrollBarPolicy]
            def hbarPolicy_=(value: javafx.scene.control.ScrollPane.ScrollBarPolicy): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.control.ScrollPane.ScrollBarPolicy]
          }
        
          implicit class HeightGetter(o: { def heightProperty(): javafx.beans.property.ReadOnlyDoubleProperty }) {
            def height: Double = macro impl.PropertyGettersMacros.get[Double]
          }
        
          implicit class HideOnClickGetterAndSetter(o: { def hideOnClickProperty(): javafx.beans.property.BooleanProperty }) {
            def hideOnClick: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def hideOnClick_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class HideOnEscapeGetterAndSetter(o: { def hideOnEscapeProperty(): javafx.beans.property.BooleanProperty }) {
            def hideOnEscape: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def hideOnEscape_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class HmaxGetterAndSetter(o: { def hmaxProperty(): javafx.beans.property.DoubleProperty }) {
            def hmax: Double = macro impl.PropertyGettersMacros.get[Double]
            def hmax_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class HminGetterAndSetter(o: { def hminProperty(): javafx.beans.property.DoubleProperty }) {
            def hmin: Double = macro impl.PropertyGettersMacros.get[Double]
            def hmin_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class HoverGetter(o: { def hoverProperty(): javafx.beans.property.ReadOnlyBooleanProperty }) {
            def hover: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
          }
        
          implicit class HvalueGetterAndSetter(o: { def hvalueProperty(): javafx.beans.property.DoubleProperty }) {
            def hvalue: Double = macro impl.PropertyGettersMacros.get[Double]
            def hvalue_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class IdGetterAndSetter(o: { def idProperty(): javafx.beans.property.StringProperty }) {
            def id: String = macro impl.PropertyGettersMacros.get[String]
            def id_=(value: String): Unit = macro impl.PropertyGettersMacros.set[String]
          }
        
          implicit class IndependentGetterAndSetter(o: { def independentProperty(): javafx.beans.property.BooleanProperty }) {
            def independent: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def independent_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class IndeterminateGetter(o: { def indeterminateProperty(): javafx.beans.property.ReadOnlyBooleanProperty }) {
            def indeterminate: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
          }
        
          implicit class IndeterminateGetterAndSetter(o: { def indeterminateProperty(): javafx.beans.property.BooleanProperty }) {
            def indeterminate: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def indeterminate_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class IndexGetter(o: { def indexProperty(): javafx.beans.property.ReadOnlyIntegerProperty }) {
            def index: Int = macro impl.PropertyGettersMacros.get[Int]
          }
        
          implicit class InputMethodRequestsGetterAndSetter(o: { def inputMethodRequestsProperty(): javafx.beans.property.ObjectProperty[javafx.scene.input.InputMethodRequests] }) {
            def inputMethodRequests: javafx.scene.input.InputMethodRequests = macro impl.PropertyGettersMacros.get[javafx.scene.input.InputMethodRequests]
            def inputMethodRequests_=(value: javafx.scene.input.InputMethodRequests): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.input.InputMethodRequests]
          }
        
          implicit class ItemsGetterAndSetter[S](o: { def itemsProperty(): javafx.beans.property.ObjectProperty[javafx.collections.ObservableList[S]] }) {
            def items: javafx.collections.ObservableList[S] = macro impl.PropertyGettersMacros.get[javafx.collections.ObservableList[S]]
            def items_=(value: javafx.collections.ObservableList[S]): Unit = macro impl.PropertyGettersMacros.set[javafx.collections.ObservableList[S]]
          }
        
          implicit class LabelForGetterAndSetter(o: { def labelForProperty(): javafx.beans.property.ObjectProperty[javafx.scene.Node] }) {
            def labelFor: javafx.scene.Node = macro impl.PropertyGettersMacros.get[javafx.scene.Node]
            def labelFor_=(value: javafx.scene.Node): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.Node]
          }
        
          implicit class LabelFormatterGetterAndSetter(o: { def labelFormatterProperty(): javafx.beans.property.ObjectProperty[javafx.util.StringConverter[java.lang.Double]] }) {
            def labelFormatter: javafx.util.StringConverter[java.lang.Double] = macro impl.PropertyGettersMacros.get[javafx.util.StringConverter[java.lang.Double]]
            def labelFormatter_=(value: javafx.util.StringConverter[java.lang.Double]): Unit = macro impl.PropertyGettersMacros.set[javafx.util.StringConverter[java.lang.Double]]
          }
        
          implicit class LabelPaddingGetter(o: { def labelPaddingProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.geometry.Insets] }) {
            def labelPadding: javafx.geometry.Insets = macro impl.PropertyGettersMacros.get[javafx.geometry.Insets]
          }
        
          implicit class LayoutBoundsGetter(o: { def layoutBoundsProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.geometry.Bounds] }) {
            def layoutBounds: javafx.geometry.Bounds = macro impl.PropertyGettersMacros.get[javafx.geometry.Bounds]
          }
        
          implicit class LayoutXGetterAndSetter(o: { def layoutXProperty(): javafx.beans.property.DoubleProperty }) {
            def layoutX: Double = macro impl.PropertyGettersMacros.get[Double]
            def layoutX_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class LayoutYGetterAndSetter(o: { def layoutYProperty(): javafx.beans.property.DoubleProperty }) {
            def layoutY: Double = macro impl.PropertyGettersMacros.get[Double]
            def layoutY_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class LeafGetter(o: { def leafProperty(): javafx.beans.property.ReadOnlyBooleanProperty }) {
            def leaf: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
          }
        
          implicit class LengthGetter(o: { def lengthProperty(): javafx.beans.property.ReadOnlyIntegerProperty }) {
            def length: Int = macro impl.PropertyGettersMacros.get[Int]
          }
        
          implicit class ListViewGetter[T](o: { def listViewProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.scene.control.ListView[T]] }) {
            def listView: javafx.scene.control.ListView[T] = macro impl.PropertyGettersMacros.get[javafx.scene.control.ListView[T]]
          }
        
          implicit class LocalToParentTransformGetter(o: { def localToParentTransformProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.scene.transform.Transform] }) {
            def localToParentTransform: javafx.scene.transform.Transform = macro impl.PropertyGettersMacros.get[javafx.scene.transform.Transform]
          }
        
          implicit class LocalToSceneTransformGetter(o: { def localToSceneTransformProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.scene.transform.Transform] }) {
            def localToSceneTransform: javafx.scene.transform.Transform = macro impl.PropertyGettersMacros.get[javafx.scene.transform.Transform]
          }
        
          implicit class MajorTickUnitGetterAndSetter(o: { def majorTickUnitProperty(): javafx.beans.property.DoubleProperty }) {
            def majorTickUnit: Double = macro impl.PropertyGettersMacros.get[Double]
            def majorTickUnit_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class ManagedGetterAndSetter(o: { def managedProperty(): javafx.beans.property.BooleanProperty }) {
            def managed: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def managed_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class MaxGetterAndSetter(o: { def maxProperty(): javafx.beans.property.DoubleProperty }) {
            def max: Double = macro impl.PropertyGettersMacros.get[Double]
            def max_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class MaxHeightGetterAndSetter(o: { def maxHeightProperty(): javafx.beans.property.DoubleProperty }) {
            def maxHeight: Double = macro impl.PropertyGettersMacros.get[Double]
            def maxHeight_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class MaxPageIndicatorCountGetterAndSetter(o: { def maxPageIndicatorCountProperty(): javafx.beans.property.IntegerProperty }) {
            def maxPageIndicatorCount: Int = macro impl.PropertyGettersMacros.get[Int]
            def maxPageIndicatorCount_=(value: Int): Unit = macro impl.PropertyGettersMacros.set[Int]
          }
        
          implicit class MaxWidthGetterAndSetter(o: { def maxWidthProperty(): javafx.beans.property.DoubleProperty }) {
            def maxWidth: Double = macro impl.PropertyGettersMacros.get[Double]
            def maxWidth_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class MinGetterAndSetter(o: { def minProperty(): javafx.beans.property.DoubleProperty }) {
            def min: Double = macro impl.PropertyGettersMacros.get[Double]
            def min_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class MinHeightGetterAndSetter(o: { def minHeightProperty(): javafx.beans.property.DoubleProperty }) {
            def minHeight: Double = macro impl.PropertyGettersMacros.get[Double]
            def minHeight_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class MinWidthGetterAndSetter(o: { def minWidthProperty(): javafx.beans.property.DoubleProperty }) {
            def minWidth: Double = macro impl.PropertyGettersMacros.get[Double]
            def minWidth_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class MinorTickCountGetterAndSetter(o: { def minorTickCountProperty(): javafx.beans.property.IntegerProperty }) {
            def minorTickCount: Int = macro impl.PropertyGettersMacros.get[Int]
            def minorTickCount_=(value: Int): Unit = macro impl.PropertyGettersMacros.set[Int]
          }
        
          implicit class MnemonicParsingGetterAndSetter(o: { def mnemonicParsingProperty(): javafx.beans.property.BooleanProperty }) {
            def mnemonicParsing: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def mnemonicParsing_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class MouseTransparentGetterAndSetter(o: { def mouseTransparentProperty(): javafx.beans.property.BooleanProperty }) {
            def mouseTransparent: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def mouseTransparent_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class NeedsLayoutGetter(o: { def needsLayoutProperty(): javafx.beans.property.ReadOnlyBooleanProperty }) {
            def needsLayout: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
          }
        
          implicit class OnActionGetterAndSetter(o: { def onActionProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[javafx.event.ActionEvent]] }) {
            def onAction: javafx.event.EventHandler[javafx.event.ActionEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.event.ActionEvent]]
            def onAction_=(value: javafx.event.EventHandler[javafx.event.ActionEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.event.ActionEvent]]
          }
        
          implicit class OnAutoHideGetterAndSetter(o: { def onAutoHideProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[javafx.event.Event]] }) {
            def onAutoHide: javafx.event.EventHandler[javafx.event.Event] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.event.Event]]
            def onAutoHide_=(value: javafx.event.EventHandler[javafx.event.Event]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.event.Event]]
          }
        
          implicit class OnCloseRequestGetterAndSetter(o: { def onCloseRequestProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[javafx.stage.WindowEvent]] }) {
            def onCloseRequest: javafx.event.EventHandler[javafx.stage.WindowEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.stage.WindowEvent]]
            def onCloseRequest_=(value: javafx.event.EventHandler[javafx.stage.WindowEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.stage.WindowEvent]]
          }
        
          implicit class OnClosedGetterAndSetter(o: { def onClosedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[javafx.event.Event]] }) {
            def onClosed: javafx.event.EventHandler[javafx.event.Event] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.event.Event]]
            def onClosed_=(value: javafx.event.EventHandler[javafx.event.Event]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.event.Event]]
          }
        
          implicit class OnContextMenuRequestedGetterAndSetter(o: { def onContextMenuRequestedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.ContextMenuEvent]] }) {
            def onContextMenuRequested: javafx.event.EventHandler[_ >: javafx.scene.input.ContextMenuEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.ContextMenuEvent]]
            def onContextMenuRequested_=(value: javafx.event.EventHandler[javafx.scene.input.ContextMenuEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.ContextMenuEvent]]
          }
        
          implicit class OnDragDetectedGetterAndSetter(o: { def onDragDetectedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.MouseEvent]] }) {
            def onDragDetected: javafx.event.EventHandler[_ >: javafx.scene.input.MouseEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.MouseEvent]]
            def onDragDetected_=(value: javafx.event.EventHandler[javafx.scene.input.MouseEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.MouseEvent]]
          }
        
          implicit class OnDragDoneGetterAndSetter(o: { def onDragDoneProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.DragEvent]] }) {
            def onDragDone: javafx.event.EventHandler[_ >: javafx.scene.input.DragEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.DragEvent]]
            def onDragDone_=(value: javafx.event.EventHandler[javafx.scene.input.DragEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.DragEvent]]
          }
        
          implicit class OnDragDroppedGetterAndSetter(o: { def onDragDroppedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.DragEvent]] }) {
            def onDragDropped: javafx.event.EventHandler[_ >: javafx.scene.input.DragEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.DragEvent]]
            def onDragDropped_=(value: javafx.event.EventHandler[javafx.scene.input.DragEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.DragEvent]]
          }
        
          implicit class OnDragEnteredGetterAndSetter(o: { def onDragEnteredProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.DragEvent]] }) {
            def onDragEntered: javafx.event.EventHandler[_ >: javafx.scene.input.DragEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.DragEvent]]
            def onDragEntered_=(value: javafx.event.EventHandler[javafx.scene.input.DragEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.DragEvent]]
          }
        
          implicit class OnDragExitedGetterAndSetter(o: { def onDragExitedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.DragEvent]] }) {
            def onDragExited: javafx.event.EventHandler[_ >: javafx.scene.input.DragEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.DragEvent]]
            def onDragExited_=(value: javafx.event.EventHandler[javafx.scene.input.DragEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.DragEvent]]
          }
        
          implicit class OnDragOverGetterAndSetter(o: { def onDragOverProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.DragEvent]] }) {
            def onDragOver: javafx.event.EventHandler[_ >: javafx.scene.input.DragEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.DragEvent]]
            def onDragOver_=(value: javafx.event.EventHandler[javafx.scene.input.DragEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.DragEvent]]
          }
        
          implicit class OnEditCancelGetterAndSetter[T](o: { def onEditCancelProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[javafx.scene.control.TreeView.EditEvent[T]]] }) {
            def onEditCancel: javafx.event.EventHandler[javafx.scene.control.TreeView.EditEvent[T]] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.control.TreeView.EditEvent[T]]]
            def onEditCancel_=(value: javafx.event.EventHandler[javafx.scene.control.TreeView.EditEvent[T]]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.control.TreeView.EditEvent[T]]]
          }
        
          implicit class OnEditCommitGetterAndSetter[T](o: { def onEditCommitProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[javafx.scene.control.TreeView.EditEvent[T]]] }) {
            def onEditCommit: javafx.event.EventHandler[javafx.scene.control.TreeView.EditEvent[T]] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.control.TreeView.EditEvent[T]]]
            def onEditCommit_=(value: javafx.event.EventHandler[javafx.scene.control.TreeView.EditEvent[T]]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.control.TreeView.EditEvent[T]]]
          }
        
          implicit class OnEditStartGetterAndSetter[T](o: { def onEditStartProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[javafx.scene.control.TreeView.EditEvent[T]]] }) {
            def onEditStart: javafx.event.EventHandler[javafx.scene.control.TreeView.EditEvent[T]] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.control.TreeView.EditEvent[T]]]
            def onEditStart_=(value: javafx.event.EventHandler[javafx.scene.control.TreeView.EditEvent[T]]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.control.TreeView.EditEvent[T]]]
          }
        
          implicit class OnHiddenGetterAndSetter(o: { def onHiddenProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[javafx.stage.WindowEvent]] }) {
            def onHidden: javafx.event.EventHandler[javafx.stage.WindowEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.stage.WindowEvent]]
            def onHidden_=(value: javafx.event.EventHandler[javafx.stage.WindowEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.stage.WindowEvent]]
          }
        
          implicit class OnHidingGetterAndSetter(o: { def onHidingProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[javafx.stage.WindowEvent]] }) {
            def onHiding: javafx.event.EventHandler[javafx.stage.WindowEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.stage.WindowEvent]]
            def onHiding_=(value: javafx.event.EventHandler[javafx.stage.WindowEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.stage.WindowEvent]]
          }
        
          implicit class OnInputMethodTextChangedGetterAndSetter(o: { def onInputMethodTextChangedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.InputMethodEvent]] }) {
            def onInputMethodTextChanged: javafx.event.EventHandler[_ >: javafx.scene.input.InputMethodEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.InputMethodEvent]]
            def onInputMethodTextChanged_=(value: javafx.event.EventHandler[javafx.scene.input.InputMethodEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.InputMethodEvent]]
          }
        
          implicit class OnKeyPressedGetterAndSetter(o: { def onKeyPressedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.KeyEvent]] }) {
            def onKeyPressed: javafx.event.EventHandler[_ >: javafx.scene.input.KeyEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.KeyEvent]]
            def onKeyPressed_=(value: javafx.event.EventHandler[javafx.scene.input.KeyEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.KeyEvent]]
          }
        
          implicit class OnKeyReleasedGetterAndSetter(o: { def onKeyReleasedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.KeyEvent]] }) {
            def onKeyReleased: javafx.event.EventHandler[_ >: javafx.scene.input.KeyEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.KeyEvent]]
            def onKeyReleased_=(value: javafx.event.EventHandler[javafx.scene.input.KeyEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.KeyEvent]]
          }
        
          implicit class OnKeyTypedGetterAndSetter(o: { def onKeyTypedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.KeyEvent]] }) {
            def onKeyTyped: javafx.event.EventHandler[_ >: javafx.scene.input.KeyEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.KeyEvent]]
            def onKeyTyped_=(value: javafx.event.EventHandler[javafx.scene.input.KeyEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.KeyEvent]]
          }
        
          implicit class OnMenuValidationGetterAndSetter(o: { def onMenuValidationProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[javafx.event.Event]] }) {
            def onMenuValidation: javafx.event.EventHandler[javafx.event.Event] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.event.Event]]
            def onMenuValidation_=(value: javafx.event.EventHandler[javafx.event.Event]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.event.Event]]
          }
        
          implicit class OnMouseClickedGetterAndSetter(o: { def onMouseClickedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.MouseEvent]] }) {
            def onMouseClicked: javafx.event.EventHandler[_ >: javafx.scene.input.MouseEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.MouseEvent]]
            def onMouseClicked_=(value: javafx.event.EventHandler[javafx.scene.input.MouseEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.MouseEvent]]
          }
        
          implicit class OnMouseDragEnteredGetterAndSetter(o: { def onMouseDragEnteredProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.MouseDragEvent]] }) {
            def onMouseDragEntered: javafx.event.EventHandler[_ >: javafx.scene.input.MouseDragEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.MouseDragEvent]]
            def onMouseDragEntered_=(value: javafx.event.EventHandler[javafx.scene.input.MouseDragEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.MouseDragEvent]]
          }
        
          implicit class OnMouseDragExitedGetterAndSetter(o: { def onMouseDragExitedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.MouseDragEvent]] }) {
            def onMouseDragExited: javafx.event.EventHandler[_ >: javafx.scene.input.MouseDragEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.MouseDragEvent]]
            def onMouseDragExited_=(value: javafx.event.EventHandler[javafx.scene.input.MouseDragEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.MouseDragEvent]]
          }
        
          implicit class OnMouseDragOverGetterAndSetter(o: { def onMouseDragOverProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.MouseDragEvent]] }) {
            def onMouseDragOver: javafx.event.EventHandler[_ >: javafx.scene.input.MouseDragEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.MouseDragEvent]]
            def onMouseDragOver_=(value: javafx.event.EventHandler[javafx.scene.input.MouseDragEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.MouseDragEvent]]
          }
        
          implicit class OnMouseDragReleasedGetterAndSetter(o: { def onMouseDragReleasedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.MouseDragEvent]] }) {
            def onMouseDragReleased: javafx.event.EventHandler[_ >: javafx.scene.input.MouseDragEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.MouseDragEvent]]
            def onMouseDragReleased_=(value: javafx.event.EventHandler[javafx.scene.input.MouseDragEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.MouseDragEvent]]
          }
        
          implicit class OnMouseDraggedGetterAndSetter(o: { def onMouseDraggedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.MouseEvent]] }) {
            def onMouseDragged: javafx.event.EventHandler[_ >: javafx.scene.input.MouseEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.MouseEvent]]
            def onMouseDragged_=(value: javafx.event.EventHandler[javafx.scene.input.MouseEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.MouseEvent]]
          }
        
          implicit class OnMouseEnteredGetterAndSetter(o: { def onMouseEnteredProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.MouseEvent]] }) {
            def onMouseEntered: javafx.event.EventHandler[_ >: javafx.scene.input.MouseEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.MouseEvent]]
            def onMouseEntered_=(value: javafx.event.EventHandler[javafx.scene.input.MouseEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.MouseEvent]]
          }
        
          implicit class OnMouseExitedGetterAndSetter(o: { def onMouseExitedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.MouseEvent]] }) {
            def onMouseExited: javafx.event.EventHandler[_ >: javafx.scene.input.MouseEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.MouseEvent]]
            def onMouseExited_=(value: javafx.event.EventHandler[javafx.scene.input.MouseEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.MouseEvent]]
          }
        
          implicit class OnMouseMovedGetterAndSetter(o: { def onMouseMovedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.MouseEvent]] }) {
            def onMouseMoved: javafx.event.EventHandler[_ >: javafx.scene.input.MouseEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.MouseEvent]]
            def onMouseMoved_=(value: javafx.event.EventHandler[javafx.scene.input.MouseEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.MouseEvent]]
          }
        
          implicit class OnMousePressedGetterAndSetter(o: { def onMousePressedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.MouseEvent]] }) {
            def onMousePressed: javafx.event.EventHandler[_ >: javafx.scene.input.MouseEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.MouseEvent]]
            def onMousePressed_=(value: javafx.event.EventHandler[javafx.scene.input.MouseEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.MouseEvent]]
          }
        
          implicit class OnMouseReleasedGetterAndSetter(o: { def onMouseReleasedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.MouseEvent]] }) {
            def onMouseReleased: javafx.event.EventHandler[_ >: javafx.scene.input.MouseEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.MouseEvent]]
            def onMouseReleased_=(value: javafx.event.EventHandler[javafx.scene.input.MouseEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.MouseEvent]]
          }
        
          implicit class OnRotateGetterAndSetter(o: { def onRotateProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.RotateEvent]] }) {
            def onRotate: javafx.event.EventHandler[_ >: javafx.scene.input.RotateEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.RotateEvent]]
            def onRotate_=(value: javafx.event.EventHandler[javafx.scene.input.RotateEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.RotateEvent]]
          }
        
          implicit class OnRotationFinishedGetterAndSetter(o: { def onRotationFinishedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.RotateEvent]] }) {
            def onRotationFinished: javafx.event.EventHandler[_ >: javafx.scene.input.RotateEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.RotateEvent]]
            def onRotationFinished_=(value: javafx.event.EventHandler[javafx.scene.input.RotateEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.RotateEvent]]
          }
        
          implicit class OnRotationStartedGetterAndSetter(o: { def onRotationStartedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.RotateEvent]] }) {
            def onRotationStarted: javafx.event.EventHandler[_ >: javafx.scene.input.RotateEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.RotateEvent]]
            def onRotationStarted_=(value: javafx.event.EventHandler[javafx.scene.input.RotateEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.RotateEvent]]
          }
        
          implicit class OnScrollFinishedGetterAndSetter(o: { def onScrollFinishedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.ScrollEvent]] }) {
            def onScrollFinished: javafx.event.EventHandler[_ >: javafx.scene.input.ScrollEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.ScrollEvent]]
            def onScrollFinished_=(value: javafx.event.EventHandler[javafx.scene.input.ScrollEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.ScrollEvent]]
          }
        
          implicit class OnScrollGetterAndSetter(o: { def onScrollProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.ScrollEvent]] }) {
            def onScroll: javafx.event.EventHandler[_ >: javafx.scene.input.ScrollEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.ScrollEvent]]
            def onScroll_=(value: javafx.event.EventHandler[javafx.scene.input.ScrollEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.ScrollEvent]]
          }
        
          implicit class OnScrollStartedGetterAndSetter(o: { def onScrollStartedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.ScrollEvent]] }) {
            def onScrollStarted: javafx.event.EventHandler[_ >: javafx.scene.input.ScrollEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.ScrollEvent]]
            def onScrollStarted_=(value: javafx.event.EventHandler[javafx.scene.input.ScrollEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.ScrollEvent]]
          }
        
          implicit class OnSelectionChangedGetterAndSetter(o: { def onSelectionChangedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[javafx.event.Event]] }) {
            def onSelectionChanged: javafx.event.EventHandler[javafx.event.Event] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.event.Event]]
            def onSelectionChanged_=(value: javafx.event.EventHandler[javafx.event.Event]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.event.Event]]
          }
        
          implicit class OnShowingGetterAndSetter(o: { def onShowingProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[javafx.stage.WindowEvent]] }) {
            def onShowing: javafx.event.EventHandler[javafx.stage.WindowEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.stage.WindowEvent]]
            def onShowing_=(value: javafx.event.EventHandler[javafx.stage.WindowEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.stage.WindowEvent]]
          }
        
          implicit class OnShownGetterAndSetter(o: { def onShownProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[javafx.stage.WindowEvent]] }) {
            def onShown: javafx.event.EventHandler[javafx.stage.WindowEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.stage.WindowEvent]]
            def onShown_=(value: javafx.event.EventHandler[javafx.stage.WindowEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.stage.WindowEvent]]
          }
        
          implicit class OnSwipeDownGetterAndSetter(o: { def onSwipeDownProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.SwipeEvent]] }) {
            def onSwipeDown: javafx.event.EventHandler[_ >: javafx.scene.input.SwipeEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.SwipeEvent]]
            def onSwipeDown_=(value: javafx.event.EventHandler[javafx.scene.input.SwipeEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.SwipeEvent]]
          }
        
          implicit class OnSwipeLeftGetterAndSetter(o: { def onSwipeLeftProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.SwipeEvent]] }) {
            def onSwipeLeft: javafx.event.EventHandler[_ >: javafx.scene.input.SwipeEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.SwipeEvent]]
            def onSwipeLeft_=(value: javafx.event.EventHandler[javafx.scene.input.SwipeEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.SwipeEvent]]
          }
        
          implicit class OnSwipeRightGetterAndSetter(o: { def onSwipeRightProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.SwipeEvent]] }) {
            def onSwipeRight: javafx.event.EventHandler[_ >: javafx.scene.input.SwipeEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.SwipeEvent]]
            def onSwipeRight_=(value: javafx.event.EventHandler[javafx.scene.input.SwipeEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.SwipeEvent]]
          }
        
          implicit class OnSwipeUpGetterAndSetter(o: { def onSwipeUpProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.SwipeEvent]] }) {
            def onSwipeUp: javafx.event.EventHandler[_ >: javafx.scene.input.SwipeEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.SwipeEvent]]
            def onSwipeUp_=(value: javafx.event.EventHandler[javafx.scene.input.SwipeEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.SwipeEvent]]
          }
        
          implicit class OnTouchMovedGetterAndSetter(o: { def onTouchMovedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.TouchEvent]] }) {
            def onTouchMoved: javafx.event.EventHandler[_ >: javafx.scene.input.TouchEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.TouchEvent]]
            def onTouchMoved_=(value: javafx.event.EventHandler[javafx.scene.input.TouchEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.TouchEvent]]
          }
        
          implicit class OnTouchPressedGetterAndSetter(o: { def onTouchPressedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.TouchEvent]] }) {
            def onTouchPressed: javafx.event.EventHandler[_ >: javafx.scene.input.TouchEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.TouchEvent]]
            def onTouchPressed_=(value: javafx.event.EventHandler[javafx.scene.input.TouchEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.TouchEvent]]
          }
        
          implicit class OnTouchReleasedGetterAndSetter(o: { def onTouchReleasedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.TouchEvent]] }) {
            def onTouchReleased: javafx.event.EventHandler[_ >: javafx.scene.input.TouchEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.TouchEvent]]
            def onTouchReleased_=(value: javafx.event.EventHandler[javafx.scene.input.TouchEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.TouchEvent]]
          }
        
          implicit class OnTouchStationaryGetterAndSetter(o: { def onTouchStationaryProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.TouchEvent]] }) {
            def onTouchStationary: javafx.event.EventHandler[_ >: javafx.scene.input.TouchEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.TouchEvent]]
            def onTouchStationary_=(value: javafx.event.EventHandler[javafx.scene.input.TouchEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.TouchEvent]]
          }
        
          implicit class OnZoomFinishedGetterAndSetter(o: { def onZoomFinishedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.ZoomEvent]] }) {
            def onZoomFinished: javafx.event.EventHandler[_ >: javafx.scene.input.ZoomEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.ZoomEvent]]
            def onZoomFinished_=(value: javafx.event.EventHandler[javafx.scene.input.ZoomEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.ZoomEvent]]
          }
        
          implicit class OnZoomGetterAndSetter(o: { def onZoomProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.ZoomEvent]] }) {
            def onZoom: javafx.event.EventHandler[_ >: javafx.scene.input.ZoomEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.ZoomEvent]]
            def onZoom_=(value: javafx.event.EventHandler[javafx.scene.input.ZoomEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.ZoomEvent]]
          }
        
          implicit class OnZoomStartedGetterAndSetter(o: { def onZoomStartedProperty(): javafx.beans.property.ObjectProperty[javafx.event.EventHandler[_ >: javafx.scene.input.ZoomEvent]] }) {
            def onZoomStarted: javafx.event.EventHandler[_ >: javafx.scene.input.ZoomEvent] = macro impl.PropertyGettersMacros.get[javafx.event.EventHandler[javafx.scene.input.ZoomEvent]]
            def onZoomStarted_=(value: javafx.event.EventHandler[javafx.scene.input.ZoomEvent]): Unit = macro impl.PropertyGettersMacros.set[javafx.event.EventHandler[javafx.scene.input.ZoomEvent]]
          }
        
          implicit class OpacityGetterAndSetter(o: { def opacityProperty(): javafx.beans.property.DoubleProperty }) {
            def opacity: Double = macro impl.PropertyGettersMacros.get[Double]
            def opacity_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class OrientationGetterAndSetter(o: { def orientationProperty(): javafx.beans.property.ObjectProperty[javafx.geometry.Orientation] }) {
            def orientation: javafx.geometry.Orientation = macro impl.PropertyGettersMacros.get[javafx.geometry.Orientation]
            def orientation_=(value: javafx.geometry.Orientation): Unit = macro impl.PropertyGettersMacros.set[javafx.geometry.Orientation]
          }
        
          implicit class OwnerNodeGetter(o: { def ownerNodeProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.scene.Node] }) {
            def ownerNode: javafx.scene.Node = macro impl.PropertyGettersMacros.get[javafx.scene.Node]
          }
        
          implicit class OwnerWindowGetter(o: { def ownerWindowProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.stage.Window] }) {
            def ownerWindow: javafx.stage.Window = macro impl.PropertyGettersMacros.get[javafx.stage.Window]
          }
        
          implicit class PageCountGetterAndSetter(o: { def pageCountProperty(): javafx.beans.property.IntegerProperty }) {
            def pageCount: Int = macro impl.PropertyGettersMacros.get[Int]
            def pageCount_=(value: Int): Unit = macro impl.PropertyGettersMacros.set[Int]
          }
        
          implicit class PageFactoryGetterAndSetter(o: { def pageFactoryProperty(): javafx.beans.property.ObjectProperty[javafx.util.Callback[java.lang.Integer, javafx.scene.Node]] }) {
            def pageFactory: javafx.util.Callback[java.lang.Integer, javafx.scene.Node] = macro impl.PropertyGettersMacros.get[javafx.util.Callback[java.lang.Integer, javafx.scene.Node]]
            def pageFactory_=(value: javafx.util.Callback[java.lang.Integer, javafx.scene.Node]): Unit = macro impl.PropertyGettersMacros.set[javafx.util.Callback[java.lang.Integer, javafx.scene.Node]]
          }
        
          implicit class PannableGetterAndSetter(o: { def pannableProperty(): javafx.beans.property.BooleanProperty }) {
            def pannable: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def pannable_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class ParentColumnGetter[S](o: { def parentColumnProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.scene.control.TableColumn[S, _]] }) {
            def parentColumn: javafx.scene.control.TableColumn[S, _] = macro impl.PropertyGettersMacros.get[javafx.scene.control.TableColumn[S, _]]
          }
        
          implicit class ParentGetter(o: { def parentProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.scene.Parent] }) {
            def parent: javafx.scene.Parent = macro impl.PropertyGettersMacros.get[javafx.scene.Parent]
          }
        
          implicit class ParentMenuGetter(o: { def parentMenuProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.scene.control.Menu] }) {
            def parentMenu: javafx.scene.control.Menu = macro impl.PropertyGettersMacros.get[javafx.scene.control.Menu]
          }
        
          implicit class ParentPopupGetter(o: { def parentPopupProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.scene.control.ContextMenu] }) {
            def parentPopup: javafx.scene.control.ContextMenu = macro impl.PropertyGettersMacros.get[javafx.scene.control.ContextMenu]
          }
        
          implicit class PickOnBoundsGetterAndSetter(o: { def pickOnBoundsProperty(): javafx.beans.property.BooleanProperty }) {
            def pickOnBounds: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def pickOnBounds_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class PlaceholderGetterAndSetter(o: { def placeholderProperty(): javafx.beans.property.ObjectProperty[javafx.scene.Node] }) {
            def placeholder: javafx.scene.Node = macro impl.PropertyGettersMacros.get[javafx.scene.Node]
            def placeholder_=(value: javafx.scene.Node): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.Node]
          }
        
          implicit class PopupSideGetterAndSetter(o: { def popupSideProperty(): javafx.beans.property.ObjectProperty[javafx.geometry.Side] }) {
            def popupSide: javafx.geometry.Side = macro impl.PropertyGettersMacros.get[javafx.geometry.Side]
            def popupSide_=(value: javafx.geometry.Side): Unit = macro impl.PropertyGettersMacros.set[javafx.geometry.Side]
          }
        
          implicit class PrefColumnCountGetterAndSetter(o: { def prefColumnCountProperty(): javafx.beans.property.IntegerProperty }) {
            def prefColumnCount: Int = macro impl.PropertyGettersMacros.get[Int]
            def prefColumnCount_=(value: Int): Unit = macro impl.PropertyGettersMacros.set[Int]
          }
        
          implicit class PrefHeightGetterAndSetter(o: { def prefHeightProperty(): javafx.beans.property.DoubleProperty }) {
            def prefHeight: Double = macro impl.PropertyGettersMacros.get[Double]
            def prefHeight_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class PrefRowCountGetterAndSetter(o: { def prefRowCountProperty(): javafx.beans.property.IntegerProperty }) {
            def prefRowCount: Int = macro impl.PropertyGettersMacros.get[Int]
            def prefRowCount_=(value: Int): Unit = macro impl.PropertyGettersMacros.set[Int]
          }
        
          implicit class PrefViewportHeightGetterAndSetter(o: { def prefViewportHeightProperty(): javafx.beans.property.DoubleProperty }) {
            def prefViewportHeight: Double = macro impl.PropertyGettersMacros.get[Double]
            def prefViewportHeight_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class PrefViewportWidthGetterAndSetter(o: { def prefViewportWidthProperty(): javafx.beans.property.DoubleProperty }) {
            def prefViewportWidth: Double = macro impl.PropertyGettersMacros.get[Double]
            def prefViewportWidth_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class PrefWidthGetterAndSetter(o: { def prefWidthProperty(): javafx.beans.property.DoubleProperty }) {
            def prefWidth: Double = macro impl.PropertyGettersMacros.get[Double]
            def prefWidth_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class PressedGetter(o: { def pressedProperty(): javafx.beans.property.ReadOnlyBooleanProperty }) {
            def pressed: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
          }
        
          implicit class ProgressGetterAndSetter(o: { def progressProperty(): javafx.beans.property.DoubleProperty }) {
            def progress: Double = macro impl.PropertyGettersMacros.get[Double]
            def progress_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class PromptTextGetterAndSetter(o: { def promptTextProperty(): javafx.beans.property.StringProperty }) {
            def promptText: String = macro impl.PropertyGettersMacros.get[String]
            def promptText_=(value: String): Unit = macro impl.PropertyGettersMacros.set[String]
          }
        
          implicit class ResizableGetterAndSetter(o: { def resizableProperty(): javafx.beans.property.BooleanProperty }) {
            def resizable: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def resizable_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class RootGetterAndSetter[T](o: { def rootProperty(): javafx.beans.property.ObjectProperty[javafx.scene.control.TreeItem[T]] }) {
            def root: javafx.scene.control.TreeItem[T] = macro impl.PropertyGettersMacros.get[javafx.scene.control.TreeItem[T]]
            def root_=(value: javafx.scene.control.TreeItem[T]): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.control.TreeItem[T]]
          }
        
          implicit class RotateGetterAndSetter(o: { def rotateProperty(): javafx.beans.property.DoubleProperty }) {
            def rotate: Double = macro impl.PropertyGettersMacros.get[Double]
            def rotate_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class RotateGraphicGetterAndSetter(o: { def rotateGraphicProperty(): javafx.beans.property.BooleanProperty }) {
            def rotateGraphic: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def rotateGraphic_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class RotationAxisGetterAndSetter(o: { def rotationAxisProperty(): javafx.beans.property.ObjectProperty[javafx.geometry.Point3D] }) {
            def rotationAxis: javafx.geometry.Point3D = macro impl.PropertyGettersMacros.get[javafx.geometry.Point3D]
            def rotationAxis_=(value: javafx.geometry.Point3D): Unit = macro impl.PropertyGettersMacros.set[javafx.geometry.Point3D]
          }
        
          implicit class RowFactoryGetterAndSetter[S](o: { def rowFactoryProperty(): javafx.beans.property.ObjectProperty[javafx.util.Callback[javafx.scene.control.TableView[S], javafx.scene.control.TableRow[S]]] }) {
            def rowFactory: javafx.util.Callback[javafx.scene.control.TableView[S], javafx.scene.control.TableRow[S]] = macro impl.PropertyGettersMacros.get[javafx.util.Callback[javafx.scene.control.TableView[S], javafx.scene.control.TableRow[S]]]
            def rowFactory_=(value: javafx.util.Callback[javafx.scene.control.TableView[S], javafx.scene.control.TableRow[S]]): Unit = macro impl.PropertyGettersMacros.set[javafx.util.Callback[javafx.scene.control.TableView[S], javafx.scene.control.TableRow[S]]]
          }
        
          implicit class ScaleXGetterAndSetter(o: { def scaleXProperty(): javafx.beans.property.DoubleProperty }) {
            def scaleX: Double = macro impl.PropertyGettersMacros.get[Double]
            def scaleX_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class ScaleYGetterAndSetter(o: { def scaleYProperty(): javafx.beans.property.DoubleProperty }) {
            def scaleY: Double = macro impl.PropertyGettersMacros.get[Double]
            def scaleY_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class ScaleZGetterAndSetter(o: { def scaleZProperty(): javafx.beans.property.DoubleProperty }) {
            def scaleZ: Double = macro impl.PropertyGettersMacros.get[Double]
            def scaleZ_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class SceneGetter(o: { def sceneProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.scene.Scene] }) {
            def scene: javafx.scene.Scene = macro impl.PropertyGettersMacros.get[javafx.scene.Scene]
          }
        
          implicit class ScrollLeftGetterAndSetter(o: { def scrollLeftProperty(): javafx.beans.property.DoubleProperty }) {
            def scrollLeft: Double = macro impl.PropertyGettersMacros.get[Double]
            def scrollLeft_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class ScrollTopGetterAndSetter(o: { def scrollTopProperty(): javafx.beans.property.DoubleProperty }) {
            def scrollTop: Double = macro impl.PropertyGettersMacros.get[Double]
            def scrollTop_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class SelectedGetter(o: { def selectedProperty(): javafx.beans.property.ReadOnlyBooleanProperty }) {
            def selected: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
          }
        
          implicit class SelectedGetterAndSetter(o: { def selectedProperty(): javafx.beans.property.BooleanProperty }) {
            def selected: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def selected_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class SelectedIndexGetter(o: { def selectedIndexProperty(): javafx.beans.property.ReadOnlyIntegerProperty }) {
            def selectedIndex: Int = macro impl.PropertyGettersMacros.get[Int]
          }
        
          implicit class SelectedTextGetter(o: { def selectedTextProperty(): javafx.beans.property.ReadOnlyStringProperty }) {
            def selectedText: String = macro impl.PropertyGettersMacros.get[String]
          }
        
          implicit class SelectedToggleGetter(o: { def selectedToggleProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.scene.control.Toggle] }) {
            def selectedToggle: javafx.scene.control.Toggle = macro impl.PropertyGettersMacros.get[javafx.scene.control.Toggle]
          }
        
          implicit class SelectionGetter(o: { def selectionProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.scene.control.IndexRange] }) {
            def selection: javafx.scene.control.IndexRange = macro impl.PropertyGettersMacros.get[javafx.scene.control.IndexRange]
          }
        
          implicit class SelectionModeGetterAndSetter(o: { def selectionModeProperty(): javafx.beans.property.ObjectProperty[javafx.scene.control.SelectionMode] }) {
            def selectionMode: javafx.scene.control.SelectionMode = macro impl.PropertyGettersMacros.get[javafx.scene.control.SelectionMode]
            def selectionMode_=(value: javafx.scene.control.SelectionMode): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.control.SelectionMode]
          }
        
          implicit class SelectionModelGetterAndSetter[T](o: { def selectionModelProperty(): javafx.beans.property.ObjectProperty[javafx.scene.control.MultipleSelectionModel[javafx.scene.control.TreeItem[T]]] }) {
            def selectionModel: javafx.scene.control.MultipleSelectionModel[javafx.scene.control.TreeItem[T]] = macro impl.PropertyGettersMacros.get[javafx.scene.control.MultipleSelectionModel[javafx.scene.control.TreeItem[T]]]
            def selectionModel_=(value: javafx.scene.control.MultipleSelectionModel[javafx.scene.control.TreeItem[T]]): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.control.MultipleSelectionModel[javafx.scene.control.TreeItem[T]]]
          }
        
          implicit class ShowRootGetterAndSetter(o: { def showRootProperty(): javafx.beans.property.BooleanProperty }) {
            def showRoot: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def showRoot_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class ShowTickLabelsGetterAndSetter(o: { def showTickLabelsProperty(): javafx.beans.property.BooleanProperty }) {
            def showTickLabels: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def showTickLabels_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class ShowTickMarksGetterAndSetter(o: { def showTickMarksProperty(): javafx.beans.property.BooleanProperty }) {
            def showTickMarks: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def showTickMarks_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class ShowingGetter(o: { def showingProperty(): javafx.beans.property.ReadOnlyBooleanProperty }) {
            def showing: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
          }
        
          implicit class SideGetterAndSetter(o: { def sideProperty(): javafx.beans.property.ObjectProperty[javafx.geometry.Side] }) {
            def side: javafx.geometry.Side = macro impl.PropertyGettersMacros.get[javafx.geometry.Side]
            def side_=(value: javafx.geometry.Side): Unit = macro impl.PropertyGettersMacros.set[javafx.geometry.Side]
          }
        
          implicit class SkinGetterAndSetter(o: { def skinProperty(): javafx.beans.property.ObjectProperty[javafx.scene.control.Skin[_]] }) {
            def skin: javafx.scene.control.Skin[_] = macro impl.PropertyGettersMacros.get[javafx.scene.control.Skin[_]]
          }
        
          implicit class SnapToTicksGetterAndSetter(o: { def snapToTicksProperty(): javafx.beans.property.BooleanProperty }) {
            def snapToTicks: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def snapToTicks_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class SortNodeGetterAndSetter(o: { def sortNodeProperty(): javafx.beans.property.ObjectProperty[javafx.scene.Node] }) {
            def sortNode: javafx.scene.Node = macro impl.PropertyGettersMacros.get[javafx.scene.Node]
            def sortNode_=(value: javafx.scene.Node): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.Node]
          }
        
          implicit class SortTypeGetterAndSetter(o: { def sortTypeProperty(): javafx.beans.property.ObjectProperty[javafx.scene.control.TableColumn.SortType] }) {
            def sortType: javafx.scene.control.TableColumn.SortType = macro impl.PropertyGettersMacros.get[javafx.scene.control.TableColumn.SortType]
            def sortType_=(value: javafx.scene.control.TableColumn.SortType): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.control.TableColumn.SortType]
          }
        
          implicit class SortableGetterAndSetter(o: { def sortableProperty(): javafx.beans.property.BooleanProperty }) {
            def sortable: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def sortable_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class StyleGetterAndSetter(o: { def styleProperty(): javafx.beans.property.StringProperty }) {
            def style: String = macro impl.PropertyGettersMacros.get[String]
            def style_=(value: String): Unit = macro impl.PropertyGettersMacros.set[String]
          }
        
          implicit class TabClosingPolicyGetterAndSetter(o: { def tabClosingPolicyProperty(): javafx.beans.property.ObjectProperty[javafx.scene.control.TabPane.TabClosingPolicy] }) {
            def tabClosingPolicy: javafx.scene.control.TabPane.TabClosingPolicy = macro impl.PropertyGettersMacros.get[javafx.scene.control.TabPane.TabClosingPolicy]
            def tabClosingPolicy_=(value: javafx.scene.control.TabPane.TabClosingPolicy): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.control.TabPane.TabClosingPolicy]
          }
        
          implicit class TabMaxHeightGetterAndSetter(o: { def tabMaxHeightProperty(): javafx.beans.property.DoubleProperty }) {
            def tabMaxHeight: Double = macro impl.PropertyGettersMacros.get[Double]
            def tabMaxHeight_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class TabMaxWidthGetterAndSetter(o: { def tabMaxWidthProperty(): javafx.beans.property.DoubleProperty }) {
            def tabMaxWidth: Double = macro impl.PropertyGettersMacros.get[Double]
            def tabMaxWidth_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class TabMinHeightGetterAndSetter(o: { def tabMinHeightProperty(): javafx.beans.property.DoubleProperty }) {
            def tabMinHeight: Double = macro impl.PropertyGettersMacros.get[Double]
            def tabMinHeight_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class TabMinWidthGetterAndSetter(o: { def tabMinWidthProperty(): javafx.beans.property.DoubleProperty }) {
            def tabMinWidth: Double = macro impl.PropertyGettersMacros.get[Double]
            def tabMinWidth_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class TabPaneGetter(o: { def tabPaneProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.scene.control.TabPane] }) {
            def tabPane: javafx.scene.control.TabPane = macro impl.PropertyGettersMacros.get[javafx.scene.control.TabPane]
          }
        
          implicit class TableColumnGetter[S, T](o: { def tableColumnProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.scene.control.TableColumn[S, T]] }) {
            def tableColumn: javafx.scene.control.TableColumn[S, T] = macro impl.PropertyGettersMacros.get[javafx.scene.control.TableColumn[S, T]]
          }
        
          implicit class TableMenuButtonVisibleGetterAndSetter(o: { def tableMenuButtonVisibleProperty(): javafx.beans.property.BooleanProperty }) {
            def tableMenuButtonVisible: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def tableMenuButtonVisible_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class TableRowGetter(o: { def tableRowProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.scene.control.TableRow[_]] }) {
            def tableRow: javafx.scene.control.TableRow[_] = macro impl.PropertyGettersMacros.get[javafx.scene.control.TableRow[_]]
          }
        
          implicit class TableViewGetter[T](o: { def tableViewProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.scene.control.TableView[T]] }) {
            def tableView: javafx.scene.control.TableView[T] = macro impl.PropertyGettersMacros.get[javafx.scene.control.TableView[T]]
          }
        
          implicit class TextAlignmentGetterAndSetter(o: { def textAlignmentProperty(): javafx.beans.property.ObjectProperty[javafx.scene.text.TextAlignment] }) {
            def textAlignment: javafx.scene.text.TextAlignment = macro impl.PropertyGettersMacros.get[javafx.scene.text.TextAlignment]
            def textAlignment_=(value: javafx.scene.text.TextAlignment): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.text.TextAlignment]
          }
        
          implicit class TextFillGetterAndSetter(o: { def textFillProperty(): javafx.beans.property.ObjectProperty[javafx.scene.paint.Paint] }) {
            def textFill: javafx.scene.paint.Paint = macro impl.PropertyGettersMacros.get[javafx.scene.paint.Paint]
            def textFill_=(value: javafx.scene.paint.Paint): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.paint.Paint]
          }
        
          implicit class TextGetterAndSetter(o: { def textProperty(): javafx.beans.property.StringProperty }) {
            def text: String = macro impl.PropertyGettersMacros.get[String]
            def text_=(value: String): Unit = macro impl.PropertyGettersMacros.set[String]
          }
        
          implicit class TextOverrunGetterAndSetter(o: { def textOverrunProperty(): javafx.beans.property.ObjectProperty[javafx.scene.control.OverrunStyle] }) {
            def textOverrun: javafx.scene.control.OverrunStyle = macro impl.PropertyGettersMacros.get[javafx.scene.control.OverrunStyle]
            def textOverrun_=(value: javafx.scene.control.OverrunStyle): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.control.OverrunStyle]
          }
        
          implicit class ToggleGroupGetterAndSetter(o: { def toggleGroupProperty(): javafx.beans.property.ObjectProperty[javafx.scene.control.ToggleGroup] }) {
            def toggleGroup: javafx.scene.control.ToggleGroup = macro impl.PropertyGettersMacros.get[javafx.scene.control.ToggleGroup]
            def toggleGroup_=(value: javafx.scene.control.ToggleGroup): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.control.ToggleGroup]
          }
        
          implicit class TooltipGetterAndSetter(o: { def tooltipProperty(): javafx.beans.property.ObjectProperty[javafx.scene.control.Tooltip] }) {
            def tooltip: javafx.scene.control.Tooltip = macro impl.PropertyGettersMacros.get[javafx.scene.control.Tooltip]
            def tooltip_=(value: javafx.scene.control.Tooltip): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.control.Tooltip]
          }
        
          implicit class TranslateXGetterAndSetter(o: { def translateXProperty(): javafx.beans.property.DoubleProperty }) {
            def translateX: Double = macro impl.PropertyGettersMacros.get[Double]
            def translateX_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class TranslateYGetterAndSetter(o: { def translateYProperty(): javafx.beans.property.DoubleProperty }) {
            def translateY: Double = macro impl.PropertyGettersMacros.get[Double]
            def translateY_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class TranslateZGetterAndSetter(o: { def translateZProperty(): javafx.beans.property.DoubleProperty }) {
            def translateZ: Double = macro impl.PropertyGettersMacros.get[Double]
            def translateZ_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class TreeItemGetter[T](o: { def treeItemProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.scene.control.TreeItem[T]] }) {
            def treeItem: javafx.scene.control.TreeItem[T] = macro impl.PropertyGettersMacros.get[javafx.scene.control.TreeItem[T]]
          }
        
          implicit class TreeViewGetter[T](o: { def treeViewProperty(): javafx.beans.property.ReadOnlyObjectProperty[javafx.scene.control.TreeView[T]] }) {
            def treeView: javafx.scene.control.TreeView[T] = macro impl.PropertyGettersMacros.get[javafx.scene.control.TreeView[T]]
          }
        
          implicit class UnderlineGetterAndSetter(o: { def underlineProperty(): javafx.beans.property.BooleanProperty }) {
            def underline: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def underline_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class UnitIncrementGetterAndSetter(o: { def unitIncrementProperty(): javafx.beans.property.DoubleProperty }) {
            def unitIncrement: Double = macro impl.PropertyGettersMacros.get[Double]
            def unitIncrement_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class UseSystemMenuBarGetterAndSetter(o: { def useSystemMenuBarProperty(): javafx.beans.property.BooleanProperty }) {
            def useSystemMenuBar: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def useSystemMenuBar_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class ValignmentGetterAndSetter(o: { def valignmentProperty(): javafx.beans.property.ObjectProperty[javafx.geometry.VPos] }) {
            def valignment: javafx.geometry.VPos = macro impl.PropertyGettersMacros.get[javafx.geometry.VPos]
            def valignment_=(value: javafx.geometry.VPos): Unit = macro impl.PropertyGettersMacros.set[javafx.geometry.VPos]
          }
        
          implicit class ValueChangingGetterAndSetter(o: { def valueChangingProperty(): javafx.beans.property.BooleanProperty }) {
            def valueChanging: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def valueChanging_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class ValueGetterAndSetter(o: { def valueProperty(): javafx.beans.property.DoubleProperty }) {
            def value: Double = macro impl.PropertyGettersMacros.get[Double]
            def value_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class VbarPolicyGetterAndSetter(o: { def vbarPolicyProperty(): javafx.beans.property.ObjectProperty[javafx.scene.control.ScrollPane.ScrollBarPolicy] }) {
            def vbarPolicy: javafx.scene.control.ScrollPane.ScrollBarPolicy = macro impl.PropertyGettersMacros.get[javafx.scene.control.ScrollPane.ScrollBarPolicy]
            def vbarPolicy_=(value: javafx.scene.control.ScrollPane.ScrollBarPolicy): Unit = macro impl.PropertyGettersMacros.set[javafx.scene.control.ScrollPane.ScrollBarPolicy]
          }
        
          implicit class ViewportBoundsGetterAndSetter(o: { def viewportBoundsProperty(): javafx.beans.property.ObjectProperty[javafx.geometry.Bounds] }) {
            def viewportBounds: javafx.geometry.Bounds = macro impl.PropertyGettersMacros.get[javafx.geometry.Bounds]
            def viewportBounds_=(value: javafx.geometry.Bounds): Unit = macro impl.PropertyGettersMacros.set[javafx.geometry.Bounds]
          }
        
          implicit class VisibleAmountGetterAndSetter(o: { def visibleAmountProperty(): javafx.beans.property.DoubleProperty }) {
            def visibleAmount: Double = macro impl.PropertyGettersMacros.get[Double]
            def visibleAmount_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class VisibleGetterAndSetter(o: { def visibleProperty(): javafx.beans.property.BooleanProperty }) {
            def visible: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def visible_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class VisibleRowCountGetterAndSetter(o: { def visibleRowCountProperty(): javafx.beans.property.IntegerProperty }) {
            def visibleRowCount: Int = macro impl.PropertyGettersMacros.get[Int]
            def visibleRowCount_=(value: Int): Unit = macro impl.PropertyGettersMacros.set[Int]
          }
        
          implicit class VisitedGetterAndSetter(o: { def visitedProperty(): javafx.beans.property.BooleanProperty }) {
            def visited: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def visited_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class VmaxGetterAndSetter(o: { def vmaxProperty(): javafx.beans.property.DoubleProperty }) {
            def vmax: Double = macro impl.PropertyGettersMacros.get[Double]
            def vmax_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class VminGetterAndSetter(o: { def vminProperty(): javafx.beans.property.DoubleProperty }) {
            def vmin: Double = macro impl.PropertyGettersMacros.get[Double]
            def vmin_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class VvalueGetterAndSetter(o: { def vvalueProperty(): javafx.beans.property.DoubleProperty }) {
            def vvalue: Double = macro impl.PropertyGettersMacros.get[Double]
            def vvalue_=(value: Double): Unit = macro impl.PropertyGettersMacros.set[Double]
          }
        
          implicit class WidthGetter(o: { def widthProperty(): javafx.beans.property.ReadOnlyDoubleProperty }) {
            def width: Double = macro impl.PropertyGettersMacros.get[Double]
          }
        
          implicit class WrapTextGetterAndSetter(o: { def wrapTextProperty(): javafx.beans.property.BooleanProperty }) {
            def wrapText: Boolean = macro impl.PropertyGettersMacros.get[Boolean]
            def wrapText_=(value: Boolean): Unit = macro impl.PropertyGettersMacros.set[Boolean]
          }
        
          implicit class XGetter(o: { def xProperty(): javafx.beans.property.ReadOnlyDoubleProperty }) {
            def x: Double = macro impl.PropertyGettersMacros.get[Double]
          }
        
          implicit class YGetter(o: { def yProperty(): javafx.beans.property.ReadOnlyDoubleProperty }) {
            def y: Double = macro impl.PropertyGettersMacros.get[Double]
          }
        
}
