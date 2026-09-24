package com.dshagents.app.ui.screens.modepicker

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dshagents.app.BuildConfig
import com.dshagents.app.R
import com.dshagents.app.ui.designsystem.DshAgentsTheme
import com.dshagents.app.ui.designsystem.LocalAAColors
import com.dshagents.app.ui.designsystem.ScreenScaffold
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Monitor
import com.composables.icons.lucide.QrCode

@Composable
fun ModePickerScreen(
    onOpenDshRemote: () -> Unit,
    onOpenAgentsAnywhere: () -> Unit,
) {
    val colors = LocalAAColors.current

    ScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 26.dp)
                .padding(top = 88.dp, bottom = 26.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    color = colors.ink,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 40.sp,
                    letterSpacing = (-0.5).sp,
                )
                Text(
                    text = stringResource(R.string.mode_picker_tagline),
                    color = colors.muted,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                )
            }

            Spacer(modifier = Modifier.height(46.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ModeCard(
                    title = stringResource(R.string.mode_picker_dsh_title),
                    subtitle = stringResource(R.string.mode_picker_dsh_subtitle),
                    icon = Lucide.QrCode,
                    accentColors = listOf(Color(0xFF5B7CFF), Color(0xFF8FA6FF)),
                    onClick = onOpenDshRemote,
                )
                ModeCard(
                    title = stringResource(R.string.mode_picker_aa_title),
                    subtitle = stringResource(R.string.mode_picker_aa_subtitle),
                    icon = Lucide.Monitor,
                    accentColors = listOf(Color(0xFF34C77B), Color(0xFF7FE3B1)),
                    onClick = onOpenAgentsAnywhere,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "v${BuildConfig.VERSION_NAME}",
                color = colors.faint,
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColors: List<Color>,
    onClick: () -> Unit,
) {
    val colors = LocalAAColors.current
    val shape = RoundedCornerShape(22.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = tween(120),
        label = "modeCardScale",
    )
    val borderBrush = remember(colors.isDark) {
        if (colors.isDark) {
            Brush.linearGradient(listOf(Color(0x335B7CFF), Color(0x1AFFFFFF)))
        } else {
            Brush.linearGradient(listOf(Color(0x225B7CFF), Color(0x11000000)))
        }
    }
    val iconTint by animateColorAsState(
        targetValue = if (pressed) accentColors[0] else colors.ink,
        animationSpec = tween(140),
        label = "modeCardIconTint",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
            .background(colors.raisedSurface)
            .border(1.2.dp, borderBrush, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                    Brush.linearGradient(
                        accentColors.map { it.copy(alpha = if (colors.isDark) 0.22f else 0.14f) },
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                color = colors.ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 22.sp,
            )
            Text(
                text = subtitle,
                color = colors.muted,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
            )
        }
        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = colors.faint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(name = "Mode Picker Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ModePickerLightPreview() {
    DshAgentsTheme {
        ModePickerScreen(onOpenDshRemote = {}, onOpenAgentsAnywhere = {})
    }
}

@Preview(
    name = "Mode Picker Dark",
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ModePickerDarkPreview() {
    DshAgentsTheme {
        ModePickerScreen(onOpenDshRemote = {}, onOpenAgentsAnywhere = {})
    }
}
