package com.example.mealomat.feature.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mealomat.data.repo.IngredientRepository
import com.example.mealomat.data.repo.ShoppingRepository
import com.example.mealomat.feature.shopping.model.LineState
import com.example.mealomat.feature.shopping.model.ShoppingLine
import com.example.mealomat.feature.shopping.model.shoppingLines
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class ShoppingViewModel(
    private val shopping: ShoppingRepository,
    private val ingredients: IngredientRepository,
) : ViewModel() {

    private val _lines = MutableStateFlow(emptyList<ShoppingLine>())
    val lines: StateFlow<List<ShoppingLine>> = _lines.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _active = MutableStateFlow<ShoppingLine?>(null)
    val active: StateFlow<ShoppingLine?> = _active.asStateFlow()

    private var tripId: String? = null

    private var focused: String? = null

    fun start(blockId: String, date: LocalDate) {
        if (tripId != null) return
        viewModelScope.launch {
            tripId = shopping.forBlock(blockId, date)
            refresh()
            _ready.value = true
        }
    }

    fun focus(line: ShoppingLine) {
        if (line.state == LineState.Bought) return
        focused = line.ingredientId
        refreshActive()
    }

    fun buy(line: ShoppingLine) = act(line) { id -> shopping.buyStep(id, line.ingredientId, line.buyAmount) }

    fun skip(line: ShoppingLine) = act(line) { id -> shopping.skipStep(id, line.ingredientId) }

    private fun act(line: ShoppingLine, write: suspend (String) -> Unit) {
        val id = tripId ?: return
        viewModelScope.launch {
            write(id)
            refresh()
            focused = nextAfter(line.ingredientId)
            refreshActive()
            if (_lines.value.none { it.state == LineState.Pending }) shopping.complete(id)
        }
    }

    private fun nextAfter(ingredientId: String): String? {
        val lines = _lines.value
        val from = lines.indexOfFirst { it.ingredientId == ingredientId }
        if (from < 0) return null
        val below = lines.drop(from + 1).firstOrNull { it.state == LineState.Pending }
        return (below ?: lines.firstOrNull { it.state == LineState.Pending })?.ingredientId
    }

    private fun refresh() {
        val id = tripId ?: return
        val needs = shopping.needsOf(id)
        val steps = shopping.stepsOf(id)
        val library = (needs.map { it.ingredientId } + steps.map { it.ingredient_id })
            .distinct()
            .mapNotNull { ingredients.byId(it) }
            .associateBy { it.id }
        _lines.value = shoppingLines(needs, steps, library)
        refreshActive()
    }

    private fun refreshActive() {
        val lines = _lines.value
        _active.value = lines.firstOrNull { it.ingredientId == focused && it.state != LineState.Bought }
            ?: lines.firstOrNull { it.state == LineState.Pending }
    }
}
