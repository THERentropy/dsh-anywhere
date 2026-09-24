package com.dshagents.app.ui.screens.home

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.stringResource
import com.dshagents.app.R
import com.dshagents.app.feature.sessions.ProjectSessionStatusFilter
import com.dshagents.app.ui.designsystem.AAAnchoredDropdownMenu
import com.dshagents.app.ui.designsystem.AAAnchoredPopup
import com.dshagents.app.ui.designsystem.AADropdownMenuItem
import com.dshagents.app.ui.designsystem.AADropdownMenuLabel

@Composable
internal fun HomeProjectAnchoredPopup(
    anchorBounds: Rect,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    AAAnchoredPopup(anchorBounds = anchorBounds, onDismissRequest = onDismiss, content = content)
}

@Composable
internal fun HomeProjectFilterMenu(
    anchorBounds: Rect,
    selected: ProjectSessionStatusFilter,
    onDismiss: () -> Unit,
    onSelect: (ProjectSessionStatusFilter) -> Unit,
) {
    AAAnchoredDropdownMenu(anchorBounds = anchorBounds, onDismissRequest = onDismiss) {
        item("label") { AADropdownMenuLabel(stringResource(R.string.home_project_session_status)) }
        items(ProjectSessionStatusFilter.entries, key = { it.name }) { status ->
            val label = when (status) {
                ProjectSessionStatusFilter.Active -> R.string.home_project_filter_active
                ProjectSessionStatusFilter.Archived -> R.string.home_project_filter_archived
                ProjectSessionStatusFilter.All -> R.string.home_project_filter_all
            }
            AADropdownMenuItem(
                text = stringResource(label),
                selected = selected == status,
                onClick = { onSelect(status); onDismiss() },
            )
        }
    }
}
