package com.dshagents.app.feature.sessions

import com.dshagents.app.feature.devices.DeviceRuntime
import com.dshagents.app.feature.devices.DeviceRuntimeStatus
import java.util.Locale

fun activeNewSessionRuntimes(runtimes: List<DeviceRuntime>): List<DeviceRuntime> = runtimes
    .filter { it.configured && it.active && it.status == DeviceRuntimeStatus.Running }
    .sortedWith(compareBy<DeviceRuntime> { it.name.lowercase(Locale.ROOT) }.thenBy { it.id })

fun newSessionInventoryNeedsSettling(runtimes: List<DeviceRuntime>): Boolean =
    runtimes.isEmpty() || runtimes.any { it.configured && it.active && it.status != DeviceRuntimeStatus.Running }
