package com.dshagents.app.feature.sessions

import com.dshagents.app.model.runtimeTypeLabel

fun String.runtimeLabel(): String {
    return runtimeTypeLabel()
}
