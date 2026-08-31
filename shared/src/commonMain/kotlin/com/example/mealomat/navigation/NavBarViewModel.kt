package com.example.mealomat.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mealomat.auth.AuthRepository
import com.example.mealomat.data.db.Prep_block
import com.example.mealomat.data.repo.PrepBlockRepository
import com.example.mealomat.domain.datesOf
import com.example.mealomat.domain.weekStart
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class DayNav(
    val date: LocalDate,
    val isPast: Boolean,
    val shops: Boolean,
    val preps: Boolean,
)

class NavBarViewModel(
    prepBlocks: PrepBlockRepository,
    clock: Clock,
    private val auth: AuthRepository,
) : ViewModel() {

    val today: LocalDate = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    val week: List<DayNav> = weekNav(today, prepBlocks.list())

    // TODO: REMOVE just for testing
    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }
}

// Builds the nav bar's week: this week's dates, each flagged with what happens on it.
fun weekNav(today: LocalDate, blocks: List<Prep_block>): List<DayNav> =
    datesOf(weekStart(today)).map { date ->
        DayNav(
            date = date,
            isPast = date < today,
            shops = blocks.any { it.shopping_weekday == date.dayOfWeek },
            preps = blocks.any { it.prep_weekday == date.dayOfWeek },
        )
    }
