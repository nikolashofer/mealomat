package com.example.mealomat.feature.logbook

import androidx.lifecycle.ViewModel
import com.example.mealomat.data.repo.DayRepository
import com.example.mealomat.domain.Day
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate

class LogbookViewModel(private val days: DayRepository) : ViewModel() {

    private val _day = MutableStateFlow<Day?>(null)
    val day: StateFlow<Day?> = _day.asStateFlow()

    fun show(date: LocalDate) {
        _day.value = days.byDate(date)
    }
}
