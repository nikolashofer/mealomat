package com.example.mealomat.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mealomat.auth.AuthRepository
import com.example.mealomat.data.repo.PrepBlockRepository
import com.example.mealomat.domain.DayNav
import com.example.mealomat.domain.weekNav
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
