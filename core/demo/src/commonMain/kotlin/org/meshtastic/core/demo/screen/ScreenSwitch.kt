/*
 * Copyright (c) 2026 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.meshtastic.core.demo.screen

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import org.meshtastic.core.repository.DemoMode

/**
 * Picks the real or the demo source of a screen facade, live. A flow already collected by a screen switches as soon as
 * demo mode does: it never keeps serving real data after activation, nor demo data after deactivation.
 */
internal class ScreenSwitch(private val demoMode: DemoMode) {
    val isDemo: Boolean
        get() = demoMode.isActive.value

    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T> flow(real: suspend () -> Flow<T>, demo: suspend () -> Flow<T>): Flow<T> =
        demoMode.isActive.flatMapLatest { active -> if (active) demo() else real() }

    fun <T> state(real: StateFlow<T>, demo: StateFlow<T>): StateFlow<T> =
        SwitchingStateFlow(demoMode.isActive, real, demo)

    inline fun <T> pick(real: () -> T, demo: () -> T): T = if (isDemo) demo() else real()
}

/** A [StateFlow] whose [value] is read from the side in use at that very moment, so a read is never stale. */
internal class SwitchingStateFlow<T>(
    private val demoActive: StateFlow<Boolean>,
    private val real: StateFlow<T>,
    private val demo: StateFlow<T>,
) : StateFlow<T> {
    override val value: T
        get() = if (demoActive.value) demo.value else real.value

    override val replayCache: List<T>
        get() = listOf(value)

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        demoActive.flatMapLatest { active -> if (active) demo else real }.distinctUntilChanged().collect(collector)
        awaitCancellation()
    }
}
