package com.dshagents.app.ui.screens.dshremote

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dshagents.app.R
import com.dshagents.app.feature.dshremote.DshHost
import com.dshagents.app.feature.dshremote.DshHostStore
import com.dshagents.app.feature.dshremote.parseDshPairingLink
import com.dshagents.app.ui.designsystem.AuthErrorNotice
import com.dshagents.app.ui.designsystem.BackPill
import com.dshagents.app.ui.designsystem.DshAgentsTheme
import com.dshagents.app.ui.designsystem.LocalAAColors
import com.dshagents.app.ui.designsystem.ScreenScaffold
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Flashlight
import com.composables.icons.lucide.FlashlightOff
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Monitor
import com.composables.icons.lucide.Trash2
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.text.DateFormat
import java.util.Date

@Composable
fun DshPairingScreen(
    hostStore: DshHostStore,
    onPairLink: (String) -> Unit,
    onResumeHost: (String) -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalAAColors.current
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    var hasCameraPermission by remember {
        mutableStateOf(
            isPreview || ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = if (isPreview) {
        null
    } else {
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted -> hasCameraPermission = granted }
    }
    var hosts by remember { mutableStateOf(hostStore.list()) }
    var pastedLink by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var torchController by remember { mutableStateOf<LifecycleCameraController?>(null) }

    fun submitLink(raw: String) {
        val parsed = parseDshPairingLink(raw)
        if (parsed == null) {
            errorMessage = context.getString(R.string.dsh_pairing_invalid_link)
            return
        }
        errorMessage = null
        onPairLink(parsed.url)
    }

    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        if (!isPreview && !hasCameraPermission) {
            permissionLauncher?.launch(Manifest.permission.CAMERA)
        }
    }

    ScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp)
                .padding(top = 26.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackPill(label = stringResource(R.string.common_back), onClick = onBack)
                Text(
                    modifier = Modifier.padding(start = 14.dp),
                    text = stringResource(R.string.dsh_pairing_title),
                    color = colors.ink,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp,
                )
            }

            Box {
                DshScannerFrame(
                    hasCameraPermission = hasCameraPermission,
                    onQrValue = ::submitLink,
                    onCameraController = { torchController = it },
                )
                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable {
                                torchOn = !torchOn
                                torchController?.enableTorch(torchOn)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (torchOn) Lucide.FlashlightOff else Lucide.Flashlight,
                            contentDescription = stringResource(R.string.dsh_pairing_torch),
                            tint = Color.White,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.dsh_pairing_help_title),
                    color = colors.ink,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.dsh_pairing_help_body),
                    color = colors.muted,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.dsh_pairing_paste_title),
                    color = colors.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 17.sp,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(15.dp))
                        .background(colors.raisedSurface)
                        .border(1.2.dp, colors.border, RoundedCornerShape(15.dp))
                        .padding(start = 15.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        modifier = Modifier.weight(1f),
                        value = pastedLink,
                        onValueChange = {
                            pastedLink = it
                            errorMessage = null
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go,
                        ),
                        keyboardActions = KeyboardActions(onGo = { submitLink(pastedLink) }),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = colors.ink,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                        ),
                        cursorBrush = SolidColor(colors.ink),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (pastedLink.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.dsh_pairing_paste_hint),
                                        color = colors.faint,
                                        fontSize = 13.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(colors.primaryAction)
                            .clickable { submitLink(pastedLink) }
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.dsh_pairing_paste_action),
                            color = colors.onPrimaryAction,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                errorMessage?.let { AuthErrorNotice(message = it) }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.dsh_pairing_hosts_title),
                    color = colors.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 17.sp,
                )
                if (hosts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.dsh_pairing_hosts_empty),
                        color = colors.faint,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(17.dp))
                            .background(colors.raisedSurface)
                            .border(1.2.dp, colors.border, RoundedCornerShape(17.dp)),
                    ) {
                        hosts.forEachIndexed { index, host ->
                            if (index > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .height(1.dp)
                                        .background(colors.border),
                                )
                            }
                            DshHostRow(
                                host = host,
                                onOpen = { onResumeHost(host.id) },
                                onRemove = {
                                    hostStore.remove(host.id)
                                    hosts = hostStore.list()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DshHostRow(
    host: DshHost,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = LocalAAColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(colors.subtle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Lucide.Monitor, contentDescription = null, tint = colors.ink, modifier = Modifier.size(19.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = host.name,
                color = colors.ink,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (host.lastUsedAt > 0L) {
                    stringResource(
                        R.string.dsh_pairing_last_used,
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(host.lastUsedAt)),
                    )
                } else {
                    host.baseUrl
                },
                color = colors.muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = colors.faint,
            modifier = Modifier.size(17.dp),
        )
        Icon(
            imageVector = Lucide.Trash2,
            contentDescription = stringResource(R.string.dsh_pairing_host_remove),
            tint = colors.muted,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove)
                .padding(6.dp),
        )
    }
}

@Composable
private fun DshScannerFrame(
    hasCameraPermission: Boolean,
    onQrValue: (String) -> Unit,
    onCameraController: (LifecycleCameraController) -> Unit,
) {
    val colors = LocalAAColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(264.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.raisedSurface)
            .border(1.2.dp, colors.border, RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (hasCameraPermission) {
            DshCameraPreview(onQrValue = onQrValue, onCameraController = onCameraController)
            DshScannerOverlay()
        } else {
            Text(
                modifier = Modifier.padding(horizontal = 30.dp),
                text = stringResource(R.string.dsh_pairing_camera_needed),
                color = colors.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DshCameraPreview(
    onQrValue: (String) -> Unit,
    onCameraController: (LifecycleCameraController) -> Unit,
) {
    if (LocalInspectionMode.current) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnQrValue by rememberUpdatedState(onQrValue)
    val emissionGate = remember { DshQrEmissionGate() }
    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }
    val cameraController = remember { LifecycleCameraController(context) }

    DisposableEffect(scanner, cameraController) {
        onCameraController(cameraController)
        onDispose {
            cameraController.unbind()
            scanner.close()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { viewContext ->
            PreviewView(viewContext).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                controller = cameraController
            }
        },
        update = { previewView ->
            cameraController.setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(context),
                MlKitAnalyzer(
                    listOf(scanner),
                    COORDINATE_SYSTEM_VIEW_REFERENCED,
                    ContextCompat.getMainExecutor(context),
                ) { result ->
                    val qrValue = result
                        ?.getValue(scanner)
                        ?.firstOrNull()
                        ?.rawValue
                    if (qrValue.isNullOrBlank()) {
                        emissionGate.clearVisibleCode()
                    } else if (emissionGate.shouldEmit(qrValue, SystemClock.elapsedRealtime())) {
                        currentOnQrValue(qrValue)
                    }
                },
            )
            cameraController.bindToLifecycle(lifecycleOwner)
            previewView.controller = cameraController
        },
    )
}

@Composable
private fun DshScannerOverlay() {
    val transition = rememberInfiniteTransition(label = "dsh-scan-line")
    val scanProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dsh-scan-progress",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val inset = 24.dp.toPx()
        val length = 22.dp.toPx()
        val strokeWidth = 2.3.dp.toPx()
        val cornerColor = Color.White
        drawLine(cornerColor, Offset(inset, inset), Offset(inset + length, inset), strokeWidth = strokeWidth, cap = StrokeCap.Square)
        drawLine(cornerColor, Offset(inset, inset), Offset(inset, inset + length), strokeWidth = strokeWidth, cap = StrokeCap.Square)
        drawLine(cornerColor, Offset(size.width - inset, inset), Offset(size.width - inset - length, inset), strokeWidth = strokeWidth, cap = StrokeCap.Square)
        drawLine(cornerColor, Offset(size.width - inset, inset), Offset(size.width - inset, inset + length), strokeWidth = strokeWidth, cap = StrokeCap.Square)
        drawLine(cornerColor, Offset(inset, size.height - inset), Offset(inset + length, size.height - inset), strokeWidth = strokeWidth, cap = StrokeCap.Square)
        drawLine(cornerColor, Offset(inset, size.height - inset), Offset(inset, size.height - inset - length), strokeWidth = strokeWidth, cap = StrokeCap.Square)
        drawLine(cornerColor, Offset(size.width - inset, size.height - inset), Offset(size.width - inset - length, size.height - inset), strokeWidth = strokeWidth, cap = StrokeCap.Square)
        drawLine(cornerColor, Offset(size.width - inset, size.height - inset), Offset(size.width - inset, size.height - inset - length), strokeWidth = strokeWidth, cap = StrokeCap.Square)
        val lineY = inset + ((size.height - inset * 2f) * scanProgress)
        drawLine(
            color = Color(0xFF4F7BFF),
            start = Offset(size.width * 0.24f, lineY),
            end = Offset(size.width * 0.76f, lineY),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private class DshQrEmissionGate {
    private var lastVisibleQrValue: String? = null
    private var lastEmissionAtMillis: Long = 0

    fun clearVisibleCode() {
        lastVisibleQrValue = null
    }

    fun shouldEmit(qrValue: String, nowMillis: Long): Boolean {
        val sameVisibleQr = qrValue == lastVisibleQrValue
        if (sameVisibleQr && nowMillis - lastEmissionAtMillis < SameQrRetryDelayMillis) {
            return false
        }
        lastVisibleQrValue = qrValue
        lastEmissionAtMillis = nowMillis
        return true
    }

    companion object {
        private const val SameQrRetryDelayMillis = 1_200L
    }
}

@Preview(name = "DSH Pairing Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DshPairingLightPreview() {
    DshAgentsTheme {
        DshPairingScreen(
            hostStore = DshHostStore(LocalContext.current),
            onPairLink = {},
            onResumeHost = {},
            onBack = {},
        )
    }
}

@Preview(
    name = "DSH Pairing Dark",
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun DshPairingDarkPreview() {
    DshAgentsTheme {
        DshPairingScreen(
            hostStore = DshHostStore(LocalContext.current),
            onPairLink = {},
            onResumeHost = {},
            onBack = {},
        )
    }
}
