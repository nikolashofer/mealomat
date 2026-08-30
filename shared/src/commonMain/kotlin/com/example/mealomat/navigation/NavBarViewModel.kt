package com.example.mealomat.navigation

import androidx.lifecycle.ViewModel
import com.example.mealomat.data.repo.PrepBlockRepository
import com.example.mealomat.domain.DayNav
import com.example.mealomat.domain.weekNav
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class NavBarViewModel(prepBlocks: PrepBlockRepository, clock: Clock) : ViewModel() {

    val today: LocalDate = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    val week: List<DayNav> = weekNav(today, prepBlocks.list())
}
