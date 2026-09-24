package com.dshagents.app.ui.screens.dshremote

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.dshagents.app.BuildConfig
import com.dshagents.app.R
import com.dshagents.app.feature.dshremote.DshHostStore
import com.dshagents.app.feature.dshremote.DshLaunchRequest
import com.dshagents.app.feature.dshremote.dshOriginOf
import com.dshagents.app.ui.designsystem.LocalAAColors
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.CircleAlert

/**
 * Full-screen WebView container that runs the official DSH Web GUI through the
 * dsh-remote-web-ui pairing chain. The device credential lives in the WebView
 * cookie store and DOM storage (both persisted by the system WebView), so a
 * paired host reopens without scanning again.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DshWebScreen(
    launchRequest: DshLaunchRequest,
    hostStore: DshHostStore,
    onExit: () -> Unit,
    onRepair: () -> Unit,
) {
    val colors = LocalAAColors.current
    var progress by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var unpaired by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val initialUrl = remember(launchRequest) {
        when (launchRequest) {
            is DshLaunchRequest.PairUrl -> launchRequest.url
            is DshLaunchRequest.ResumeHost -> {
                hostStore.find(launchRequest.hostId)?.baseUrl?.trimEnd('/')?.plus("/pair-app")
            }
        }
    }
    val resumeHostId = (launchRequest as? DshLaunchRequest.ResumeHost)?.hostId

    androidx.compose.runtime.LaunchedEffect(initialUrl) {
        if (initialUrl == null) onRepair()
    }
    if (initialUrl == null) return

    val webView = remember { mutableStateOf<WebView?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val callback = filePathCallback ?: return@rememberLauncherForActivityResult
        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        callback.onReceiveValue(uris)
        filePathCallback = null
    }

    BackHandler {
        val view = webView.value
        if (view != null && canGoBack) {
            view.goBack()
        } else {
            onExit()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            CookieManager.getInstance().flush()
            webView.value?.destroy()
            webView.value = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                WebView(viewContext).apply {
                    if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadsImagesAutomatically = true
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        builtInZoomControls = false
                        displayZoomControls = false
                    }
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    webViewClient = object : WebViewClient() {
                        private var retriedPairApp = false

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            val scheme = request.url.scheme?.lowercase()
                            if (scheme == "http" || scheme == "https") return false
                            // External schemes (intent://, market:, …) are not part of the GUI.
                            return true
                        }

                        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                            loading = true
                            loadFailed = false
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            loading = false
                            canGoBack = view.canGoBack()
                            val origin = dshOriginOf(url) ?: return
                            val path = Uri.parse(url).path.orEmpty()
                            val settledInApp = path == "/" || path.startsWith("/pair-app")
                            if (!unpaired && settledInApp) {
                                if (resumeHostId != null) {
                                    hostStore.touch(resumeHostId)
                                } else {
                                    val uri = Uri.parse(url)
                                    val name = uri.host.orEmpty() + (uri.port.takeIf { it > 0 }?.let { ":$it" } ?: "")
                                    hostStore.upsert(baseUrl = origin, name = name)
                                }
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView,
                            request: WebResourceRequest,
                            errorResponse: WebResourceResponse,
                        ) {
                            if (!request.isForMainFrame) return
                            when (errorResponse.statusCode) {
                                403 -> {
                                    // The plugin gate rejected the device: revoked or expired.
                                    unpaired = true
                                }
                                401 -> {
                                    // Harness browser-auth page (e.g. reopening bare "/" on plain
                                    // HTTP LAN where no reopen service worker exists). Route the
                                    // navigation through the plugin-owned landing once.
                                    if (!retriedPairApp) {
                                        retriedPairApp = true
                                        val origin = dshOriginOf(request.url.toString())
                                        if (origin != null) {
                                            view.loadUrl("$origin/pair-app")
                                            return
                                        }
                                    }
                                    unpaired = true
                                }
                            }
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError,
                        ) {
                            if (request.isForMainFrame) loadFailed = true
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            progress = newProgress
                            if (newProgress >= 100) loading = false
                        }

                        override fun onShowFileChooser(
                            view: WebView,
                            callback: ValueCallback<Array<Uri>>,
                            params: FileChooserParams,
                        ): Boolean {
                            filePathCallback?.onReceiveValue(null)
                            filePathCallback = callback
                            return runCatching {
                                fileChooserLauncher.launch(
                                    Intent.createChooser(params.createIntent(), null),
                                )
                            }.isSuccess
                        }
                    }

                    loadUrl(initialUrl)
                    webView.value = this
                }
            },
        )

        if (loading && !unpaired && !loadFailed) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .align(Alignment.TopCenter),
                color = androidx.compose.ui.graphics.Color(0xFF5B7CFF),
                trackColor = androidx.compose.ui.graphics.Color.Transparent,
            )
        }

        if (loadFailed && !unpaired) {
            DshWebOverlay(
                title = stringResource(R.string.dsh_web_load_failed),
                actionLabel = stringResource(R.string.dsh_web_retry),
                onAction = {
                    loadFailed = false
                    loading = true
                    webView.value?.reload()
                },
                secondaryLabel = stringResource(R.string.dsh_web_close),
                onSecondary = onExit,
            )
        }

        if (unpaired) {
            DshWebOverlay(
                title = stringResource(R.string.dsh_web_unpaired_title),
                body = stringResource(R.string.dsh_web_unpaired_body),
                actionLabel = stringResource(R.string.dsh_web_repair_action),
                onAction = {
                    CookieManager.getInstance().flush()
                    onRepair()
                },
                secondaryLabel = stringResource(R.string.dsh_web_close),
                onSecondary = onExit,
            )
        }
    }
}

@Composable
private fun DshWebOverlay(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    body: String? = null,
) {
    val colors = LocalAAColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(horizontal = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Lucide.CircleAlert,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(46.dp),
        )
        Text(
            modifier = Modifier.padding(top = 20.dp),
            text = title,
            color = colors.ink,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 25.sp,
            textAlign = TextAlign.Center,
        )
        if (body != null) {
            Text(
                modifier = Modifier.padding(top = 10.dp),
                text = body,
                color = colors.muted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(colors.primaryAction)
                .clickable(onClick = onAction)
                .padding(horizontal = 30.dp, vertical = 13.dp),
        ) {
            Text(
                text = actionLabel,
                color = colors.onPrimaryAction,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            modifier = Modifier
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(9.dp))
                .clickable(onClick = onSecondary)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            text = secondaryLabel,
            color = colors.muted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
