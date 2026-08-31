package com.example.mealomat.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

fun weekStart(date: LocalDate): LocalDate =
    date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

fun datesOf(weekStart: LocalDate): List<LocalDate> =
    (0..6).map { weekStart.plus(it, DateTimeUnit.DAY) }
