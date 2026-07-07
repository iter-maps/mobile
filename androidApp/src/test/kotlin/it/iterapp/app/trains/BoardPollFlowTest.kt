package it.iterapp.app.trains

import it.iterapp.core.wire.BoardEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class BoardPollFlowTest {

  private fun entry(number: String) = BoardEntry(
    trainNumber = "REG $number",
    category = "REG",
    origin = null,
    destination = "Milano Centrale",
    scheduledTime = "14:05",
    delayMinutes = 0,
  )

  @Test
  fun failedPollKeepsLastGoodBoardAsStale() = runTest {
    var calls = 0
    val entries = listOf(entry("22815"))
    val states = boardPollFlow(pollMs = 1_000) {
      calls++
      if (calls == 1) entries else throw RuntimeException("blip")
    }.take(3).toList()
    assertEquals(BoardState.Loading, states[0])
    assertEquals(BoardState.Loaded(entries), states[1])
    assertEquals(BoardState.Loaded(entries, stale = true), states[2])
  }

  @Test
  fun firstFetchFailureIsAnError() = runTest {
    val states = boardPollFlow(pollMs = 1_000) { throw RuntimeException("down") }
      .take(2).toList()
    assertEquals(listOf(BoardState.Loading, BoardState.Error), states)
  }

  @Test
  fun boardRecoversOnTheNextSuccessfulPoll() = runTest {
    var calls = 0
    val entries = listOf(entry("664"))
    val states = boardPollFlow(pollMs = 1_000) {
      calls++
      if (calls == 1) throw RuntimeException("down") else entries
    }.take(3).toList()
    assertEquals(BoardState.Error, states[1])
    assertEquals(BoardState.Loaded(entries), states[2])
  }

  @Test
  fun seededRestartKeepsTheBoardThroughAFailedFetch() = runTest {
    // Manual refresh while offline: no Loading, the seed survives as stale.
    val entries = listOf(entry("22815"))
    val states = boardPollFlow(pollMs = 1_000, initial = entries) {
      throw RuntimeException("still down")
    }.take(1).toList()
    assertEquals(listOf(BoardState.Loaded(entries, stale = true)), states)
  }

  @Test
  fun seededRestartRefreshesInPlace() = runTest {
    val old = listOf(entry("22815"))
    val fresh = listOf(entry("664"))
    val states = boardPollFlow(pollMs = 1_000, initial = old) { fresh }.take(1).toList()
    assertEquals(listOf(BoardState.Loaded(fresh)), states)
  }
}
