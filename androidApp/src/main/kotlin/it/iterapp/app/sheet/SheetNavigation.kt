package it.iterapp.app.sheet

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.iterapp.core.model.SearchResult

/**
 * Pages of the universal sheet (ADR 0008). Not NavHost destinations: they all
 * live in the one sheet, stacked in [SheetNavigator], sharing the activity's
 * ViewModelStore so koinViewModel() returns the same instance across pages.
 */
sealed interface SheetPage {
  val openAnchor: SheetAnchor
  val peek: Dp get() = 156.dp

  /** Transient pages never enter the back-stack history. */
  val transient: Boolean get() = false

  data object Home : SheetPage {
    override val openAnchor = SheetAnchor.Bottom
  }

  data object Search : SheetPage {
    override val openAnchor = SheetAnchor.Full
  }

  data class PlaceDetail(val place: SearchResult) : SheetPage {
    override val openAnchor = SheetAnchor.Half
  }

  data object Planning : SheetPage {
    override val openAnchor = SheetAnchor.Half
  }

  data object PlanningDetail : SheetPage {
    override val openAnchor = SheetAnchor.Half
  }

  /** Address autocomplete for the From ([from]=true) or To field. */
  data class PlanningPicker(val from: Boolean) : SheetPage {
    override val openAnchor = SheetAnchor.Full
  }

  data class TrainBoard(
    val stationQuery: String? = null,
    /** Preselects the board directly when a `^S\d+$` id is already known. */
    val stationId: String? = null,
  ) : SheetPage {
    override val openAnchor = SheetAnchor.Full
  }

  /** Basemap style picker: compact content-fit sheet, skipped by back. */
  data object MapLayers : SheetPage {
    override val openAnchor = SheetAnchor.Content
    override val transient = true
  }

  data object Offline : SheetPage {
    override val openAnchor = SheetAnchor.Full
  }

  data object Settings : SheetPage {
    override val openAnchor = SheetAnchor.Full
  }
}

/** Observable back-stack of sheet pages. */
@Stable
class SheetNavigator {
  private val backStack = mutableStateListOf<SheetPage>(SheetPage.Home)

  val current: SheetPage get() = backStack.last()
  val canPop: Boolean get() = backStack.size > 1

  /** Direction of the last navigation, for the page slide animation. */
  var lastWasPop: Boolean = false
    private set

  fun push(page: SheetPage) {
    val top = backStack.last()
    if (top == page) return
    lastWasPop = false
    if (top.transient) backStack.removeAt(backStack.lastIndex)
    backStack.add(page)
  }

  fun pop(): Boolean {
    if (backStack.size <= 1) return false
    lastWasPop = true
    backStack.removeAt(backStack.lastIndex)
    return true
  }

  fun resetToHome() {
    if (backStack.size > 1) lastWasPop = true
    while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
  }
}
