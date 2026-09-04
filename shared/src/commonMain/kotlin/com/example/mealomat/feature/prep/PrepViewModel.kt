package com.example.mealomat.feature.prep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mealomat.data.repo.IngredientRepository
import com.example.mealomat.data.repo.PrepRepository
import com.example.mealomat.feature.prep.model.PrepLine
import com.example.mealomat.feature.prep.model.PrepLineState
import com.example.mealomat.feature.prep.model.prepLines
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class PrepViewModel(
    private val prep: PrepRepository,
    private val ingredients: IngredientRepository,
) : ViewModel() {

    private val _lines = MutableStateFlow(emptyList<PrepLine>())
    val lines: StateFlow<List<PrepLine>> = _lines.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _active = MutableStateFlow<PrepLine?>(null)
    val active: StateFlow<PrepLine?> = _active.asStateFlow()

    private var sessionId: String? = null

    private var focused: String? = null

    fun start(blockId: String, date: LocalDate) {
        if (sessionId != null) return
        viewModelScope.launch {
            sessionId = prep.forBlock(blockId, date)
            refresh()
            _ready.value = true
        }
    }

    fun focus(line: PrepLine) {
        if (line.state == PrepLineState.Done) return
        focused = line.key
        refreshActive()
    }

    // Moves on without recording anything; the step stays waiting for later in the session.
    fun later(line: PrepLine) {
        focused = nextAfter(line.key)
        refreshActive()
    }

    fun make(line: PrepLine) {
        val id = sessionId ?: return
        viewModelScope.launch {
            prep.completeStep(id, line.key)
            refresh()
            focused = nextAfter(line.key)
            refreshActive()
            if (_lines.value.none { it.state == PrepLineState.Pending }) prep.complete(id)
        }
    }

    private fun nextAfter(key: String): String? {
        val lines = _lines.value
        val from = lines.indexOfFirst { it.key == key }
        if (from < 0) return null
        val below = lines.drop(from + 1).firstOrNull { it.state == PrepLineState.Pending }
        return (below ?: lines.firstOrNull { it.state == PrepLineState.Pending })?.key
    }

    private fun refresh() {
        val id = sessionId ?: return
        val steps = prep.stepsOf(id)
        val library = steps
            .flatMap { step -> step.totals.map { it.ingredientId } }
            .distinct()
            .mapNotNull { ingredients.byId(it) }
            .associateBy { it.id }
        _lines.value = prepLines(steps, library)
        refreshActive()
    }

    private fun refreshActive() {
        val lines = _lines.value
        _active.value = lines.firstOrNull { it.key == focused && it.state != PrepLineState.Done }
            ?: lines.firstOrNull { it.state == PrepLineState.Pending }
    }
}
