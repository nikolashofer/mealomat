package com.example.mealomat.feature.logbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mealomat.data.repo.DayRepository
import com.example.mealomat.data.repo.IngredientRepository
import com.example.mealomat.data.repo.PrepBlockRepository
import com.example.mealomat.data.repo.PrepRepository
import com.example.mealomat.data.repo.ShoppingRepository
import com.example.mealomat.domain.Day
import com.example.mealomat.domain.DayTotals
import com.example.mealomat.domain.ingredientUses
import com.example.mealomat.domain.totalsOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class LogbookViewModel(
    private val days: DayRepository,
    private val ingredients: IngredientRepository,
    private val prepBlocks: PrepBlockRepository,
    private val shopping: ShoppingRepository,
    private val prep: PrepRepository,
) : ViewModel() {

    private val _day = MutableStateFlow<Day?>(null)
    val day: StateFlow<Day?> = _day.asStateFlow()

    private val _totals = MutableStateFlow<DayTotals?>(null)
    val totals: StateFlow<DayTotals?> = _totals.asStateFlow()

    private val _meals = MutableStateFlow(emptyList<MealRow>())
    val meals: StateFlow<List<MealRow>> = _meals.asStateFlow()

    private val _sessions = MutableStateFlow(emptyList<SessionTile>())
    val sessions: StateFlow<List<SessionTile>> = _sessions.asStateFlow()

    fun show(date: LocalDate) {
        val day = days.byDate(date)
        val library = day?.ingredients().orEmpty()
        _day.value = day
        _totals.value = day?.let { totalsOf(it, library) }
        _meals.value = day?.let { logbookRows(it, library) }.orEmpty()
        _sessions.value = sessionsOn(date)
    }

    fun tick(date: LocalDate, planItemId: String) {
        viewModelScope.launch {
            days.tickOff(date, planItemId)
            show(date)
        }
    }

    private fun sessionsOn(date: LocalDate): List<SessionTile> {
        val blocks = prepBlocks.list()
        val shops = blocks.firstOrNull { it.shopping_weekday == date.dayOfWeek }
        val preps = blocks.firstOrNull { it.prep_weekday == date.dayOfWeek }

        return listOfNotNull(
            shops?.let { block ->
                val trip = shopping.open()?.takeIf { it.prep_block_id == block.id }
                shoppingTile(
                    blockId = block.id,
                    needs = trip?.let { shopping.needsOf(it.id) } ?: shopping.needsFor(block.id, date),
                    steps = trip?.let { shopping.stepsOf(it.id) }.orEmpty(),
                )
            },
            preps?.let { prepTile(it.id, prep.stepsFor(it.id, date)) },
        )
    }

    private fun Day.ingredients() = ingredientUses()
        .map { it.ingredientId }
        .distinct()
        .mapNotNull { ingredients.byId(it) }
        .associateBy { it.id }
}
