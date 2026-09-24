package com.dshagents.app.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.dshagents.app.feature.auth.WebLoginViewModel
import com.dshagents.app.feature.devices.DeviceRuntime
import com.dshagents.app.feature.devices.DevicePairingStatus
import com.dshagents.app.feature.devices.DeviceRuntimeList
import com.dshagents.app.feature.devices.DeviceSetupCredential
import com.dshagents.app.feature.files.FilesController
import com.dshagents.app.feature.realtime.SessionRealtimeController
import com.dshagents.app.feature.sessiondetail.SessionDetailController
import com.dshagents.app.feature.sessions.NewSessionCreateDraft
import com.dshagents.app.feature.sessions.NewSessionCreateOutcome
import com.dshagents.app.feature.sessions.NewSessionDirectory
import com.dshagents.app.feature.sessions.NewSessionDraft
import com.dshagents.app.feature.sessions.ProjectSessionLoadKey
import com.dshagents.app.feature.sessions.ProjectSessionStatusFilter
import com.dshagents.app.feature.sessions.NewSessionModelCatalog
import com.dshagents.app.feature.sessions.NewSessionPermissionCatalog
import com.dshagents.app.feature.sessions.NewSessionRuntimeCapabilities
import com.dshagents.app.feature.sessions.SessionBatchUpdate
import com.dshagents.app.feature.sessions.SessionsState
import com.dshagents.app.feature.dshremote.DshHostStore
import com.dshagents.app.feature.dshremote.DshLaunchRequest
import com.dshagents.app.feature.terminal.RemoteTerminalPool
import com.dshagents.app.model.AgentDevice
import com.dshagents.app.model.AgentProject
import com.dshagents.app.model.AgentSession
import com.dshagents.app.model.MobileLoginQrPayload
import com.dshagents.app.navigation.AppDestination
import com.dshagents.app.ui.designsystem.LocalAAColors
import com.dshagents.app.ui.screens.auth.LoginMethodsScreen
import com.dshagents.app.ui.screens.auth.QrLoginScreen
import com.dshagents.app.ui.screens.auth.QrWaitingScreen
import com.dshagents.app.ui.screens.auth.WebLoginHostScreen
import com.dshagents.app.ui.screens.devices.AddDeviceScreen
import com.dshagents.app.ui.screens.devices.DeviceDetailScreen
import com.dshagents.app.ui.screens.devices.DevicesScreen
import com.dshagents.app.ui.screens.devices.rememberDeviceAgentPreviews
import com.dshagents.app.ui.screens.dshremote.DshPairingScreen
import com.dshagents.app.ui.screens.dshremote.DshWebScreen
import com.dshagents.app.ui.screens.files.FilesScreen
import com.dshagents.app.ui.screens.home.ArchivedSessionsScreen
import com.dshagents.app.ui.screens.home.HomeScreen
import com.dshagents.app.ui.screens.home.HomeTab
import com.dshagents.app.ui.screens.home.NewSessionScreen
import com.dshagents.app.ui.screens.modepicker.ModePickerScreen
import com.dshagents.app.ui.screens.sessiondetail.SessionComposerDraftStore
import com.dshagents.app.ui.screens.sessiondetail.SessionDetailScreen
import com.dshagents.app.ui.screens.terminal.TerminalScreen

@Composable
internal fun DshAgentsNavHost(
    currentDestination: AppDestination,
    sessionsState: SessionsState,
    isRefreshingSessions: Boolean,
    selectedSessionId: String?,
    preparedSessionDraft: NewSessionDraft?,
    selectedDeviceId: String?,
    deviceDetailReturnDestination: AppDestination,
    deviceSetupReturnDestination: AppDestination,
    selectedHomeTab: HomeTab,
    userId: String,
    role: String,
    serverUrl: String,
    appearanceMode: String,
    languageMode: String,
    sidebarViewMode: String,
    projectSessionsById: Map<String, List<AgentSession>>,
    loadingProjectRequests: Set<ProjectSessionLoadKey>,
    projectSessionErrors: Map<ProjectSessionLoadKey, String>,
    initialNewSessionProjectId: String?,
    sessionDetailController: SessionDetailController,
    sessionRealtimeController: SessionRealtimeController,
    filesController: FilesController,
    remoteTerminalPool: RemoteTerminalPool,
    pendingMobileLoginQr: MobileLoginQrPayload?,
    webLoginViewModel: WebLoginViewModel,
    dshHostStore: DshHostStore,
    dshLaunchRequest: DshLaunchRequest?,
    onDshLaunchRequest: (DshLaunchRequest?) -> Unit,
    agentsAnywhereEntry: AppDestination,
    navigate: (AppDestination) -> Unit,
    onRefreshSessions: () -> Unit,
    onLoadMoreSessions: (Boolean) -> Unit,
    onOpenSession: (AgentSession) -> Unit,
    onOpenDevice: (AgentDevice) -> Unit,
    onHomeTabSelected: (HomeTab) -> Unit,
    onAppearanceModeChange: (String) -> Unit,
    onLanguageModeChange: (String) -> Unit,
    onSidebarViewModeChange: (String) -> Unit,
    onLoadAccount: suspend () -> Result<com.dshagents.app.api.AuthMeResponse>,
    onLoadAccountAuthConfig: suspend () -> Result<com.dshagents.app.api.AuthConfigResponse>,
    onUpdateDisplayName: suspend (String) -> Result<com.dshagents.app.api.AuthMeResponse>,
    onSendEmailCode: suspend (String) -> Result<com.dshagents.app.api.EmailCodeResponse>,
    onBindEmail: suspend (String, String?) -> Result<com.dshagents.app.api.AuthMeResponse>,
    onUpdateAvatar: suspend (String) -> Result<com.dshagents.app.api.AuthMeResponse>,
    onClearAvatar: suspend () -> Result<com.dshagents.app.api.AuthMeResponse>,
    onChangePassword: suspend (String) -> Result<Unit>,
    onSignOut: () -> Unit,
    onRenameDevice: suspend (String, String) -> Result<AgentDevice>,
    onDeleteDevice: suspend (String) -> Result<Unit>,
    onPrepareDeviceSetup: suspend (String) -> Result<DeviceSetupCredential>,
    onCreateDeviceSetup: suspend (String) -> Result<DeviceSetupCredential>,
    onClaimDevicePairCode: suspend (DeviceSetupCredential, String) -> Result<AgentDevice>,
    devicePairingStates: Map<String, DevicePairingStatus>,
    onWaitForPairingDevice: (String) -> Unit,
    onClearDevicePairing: (String) -> Unit,
    onDevicePairingComplete: () -> Unit,
    onListDeviceRuntimes: suspend (String) -> Result<DeviceRuntimeList>,
    onSetDeviceRuntimeActive: suspend (String, String, Boolean) -> Result<DeviceRuntime>,
    onDeleteDeviceRuntimeConfig: suspend (String, String) -> Result<DeviceRuntime>,
    onBulkSetSessionsArchived: suspend (List<String>, Boolean) -> Result<SessionBatchUpdate>,
    onArchiveAllDeviceSessions: suspend (String, Boolean, String) -> Result<List<AgentSession>>,
    onRenameSession: suspend (String, String) -> Result<AgentSession>,
    onSetSessionPinned: suspend (String, Boolean) -> Result<AgentSession>,
    onSetSessionArchived: suspend (String, Boolean) -> Result<AgentSession>,
    onLoadProjectSessions: (String, ProjectSessionStatusFilter) -> Unit,
    onLoadProjects: suspend () -> Result<List<AgentProject>>,
    onLoadArchivedPage: suspend (String?, String?) -> Result<com.dshagents.app.feature.sessions.SessionPageAppend>,
    onRestoreProject: suspend (String) -> Result<List<AgentSession>>,
    onUpdateProject: suspend (String, String?, Boolean?) -> Result<AgentProject>,
    onArchiveProjectSessions: suspend (String) -> Result<List<AgentSession>>,
    onCreateProject: suspend (String, String, String) -> Result<AgentProject>,
    onNewSessionInProject: (AgentProject) -> Unit,
    onCreateSession: suspend (NewSessionCreateDraft) -> NewSessionCreateOutcome,
    onPrepareSession: (NewSessionDraft) -> Unit,
    onPreparedSessionCreated: (AgentSession) -> Unit,
    onListDirectory: suspend (String, String, String) -> Result<NewSessionDirectory>,
    onListNewSessionRuntimes: suspend (String) -> Result<DeviceRuntimeList>,
    onLoadNewSessionRuntimeCapabilities: suspend (String, String) -> Result<NewSessionRuntimeCapabilities>,
    onLoadNewSessionModelCatalog: suspend (String, String) -> Result<NewSessionModelCatalog>,
    onLoadNewSessionPermissionCatalog: suspend (String, String) -> Result<NewSessionPermissionCatalog>,
    onSessionChanged: (AgentSession) -> Unit,
    onMobileLoginQrRequested: (MobileLoginQrPayload) -> Unit,
) {
    val context = LocalContext.current
    val colors = LocalAAColors.current
    var profileOpen by rememberSaveable(serverUrl, userId) { mutableStateOf(false) }
    var deviceAgentPreviewRefreshKey by remember { mutableLongStateOf(0L) }
    val deviceAgentPreviews = rememberDeviceAgentPreviews(
        devices = sessionsState.devices,
        isRefreshing = isRefreshingSessions,
        refreshKey = deviceAgentPreviewRefreshKey,
        onListDeviceRuntimes = onListDeviceRuntimes,
    )
    val sessionComposerDraftStore = remember(context, userId) {
        SessionComposerDraftStore(context.applicationContext, userId)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.canvas,
    ) {
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val enterOffset: (Int) -> Int = { width -> if (forward) width / 5 else -width / 5 }
                val exitOffset: (Int) -> Int = { width -> if (forward) -width / 5 else width / 5 }

                slideInHorizontally(
                    animationSpec = tween(durationMillis = 260),
                    initialOffsetX = enterOffset,
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 180),
                ) togetherWith slideOutHorizontally(
                    animationSpec = tween(durationMillis = 260),
                    targetOffsetX = exitOffset,
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 160),
                )
            },
            label = "App destination transition",
        ) { destination ->
            when (destination) {
                AppDestination.ModePicker -> ModePickerScreen(
                    onOpenDshRemote = { navigate(AppDestination.DshPairing) },
                    onOpenAgentsAnywhere = { navigate(agentsAnywhereEntry) },
                )
                AppDestination.DshPairing -> DshPairingScreen(
                    hostStore = dshHostStore,
                    onPairLink = { link ->
                        onDshLaunchRequest(DshLaunchRequest.PairUrl(link))
                        navigate(AppDestination.DshWeb)
                    },
                    onResumeHost = { hostId ->
                        onDshLaunchRequest(DshLaunchRequest.ResumeHost(hostId))
                        navigate(AppDestination.DshWeb)
                    },
                    onBack = { navigate(AppDestination.ModePicker) },
                )
                AppDestination.DshWeb -> {
                    val request = dshLaunchRequest
                    if (request == null) {
                        LaunchedEffect(Unit) { navigate(AppDestination.DshPairing) }
                    } else {
                        DshWebScreen(
                            launchRequest = request,
                            hostStore = dshHostStore,
                            onExit = { navigate(AppDestination.DshPairing) },
                            onRepair = {
                                onDshLaunchRequest(null)
                                navigate(AppDestination.DshPairing)
                            },
                        )
                    }
                }
                AppDestination.LoginMethods -> {
                    BackHandler { navigate(AppDestination.ModePicker) }
                    LoginMethodsScreen(navigate)
                }
                AppDestination.ServerSetup -> WebLoginHostScreen(webLoginViewModel, navigate)
                AppDestination.QrLogin -> QrLoginScreen(
                    navigate = navigate,
                    onMobileLoginQrRequested = onMobileLoginQrRequested,
                )
                AppDestination.QrWaiting -> QrWaitingScreen(
                    navigate = navigate,
                    mobileLoginQr = pendingMobileLoginQr,
                )
                AppDestination.Sessions -> {
                    BackHandler { navigate(AppDestination.ModePicker) }
                    HomeScreen(
                    navigate = navigate,
                    state = sessionsState,
                    selectedTab = selectedHomeTab,
                    onLoadProjects = onLoadProjects,
                    isRefreshing = isRefreshingSessions,
                    userId = userId,
                    role = role,
                    serverUrl = serverUrl,
                    appearanceMode = appearanceMode,
                    languageMode = languageMode,
                    sidebarViewMode = sidebarViewMode,
                    projectSessionsById = projectSessionsById,
                    loadingProjectRequests = loadingProjectRequests,
                    projectSessionErrors = projectSessionErrors,
                    onRefresh = onRefreshSessions,
                    onLoadMore = { tab -> onLoadMoreSessions(tab == HomeTab.Archived) },
                    onTabSelected = onHomeTabSelected,
                    onAppearanceModeChange = onAppearanceModeChange,
                    onLanguageModeChange = onLanguageModeChange,
                    onSidebarViewModeChange = onSidebarViewModeChange,
                    profileOpen = profileOpen,
                    onProfileOpenChange = { profileOpen = it },
                    onOpenArchivedSessions = { navigate(AppDestination.ArchivedSessions) },
                    onLoadAccount = onLoadAccount,
                    onLoadAccountAuthConfig = onLoadAccountAuthConfig,
                    onUpdateDisplayName = onUpdateDisplayName,
                    onSendEmailCode = onSendEmailCode,
                    onBindEmail = onBindEmail,
                    onUpdateAvatar = onUpdateAvatar,
                    onClearAvatar = onClearAvatar,
                    onChangePassword = onChangePassword,
                    onSignOut = onSignOut,
                    onRenameSession = onRenameSession,
                    onSetSessionPinned = onSetSessionPinned,
                    onSetSessionArchived = onSetSessionArchived,
                    onLoadProjectSessions = onLoadProjectSessions,
                    onUpdateProject = onUpdateProject,
                    onArchiveProjectSessions = onArchiveProjectSessions,
                    onNewSessionInProject = onNewSessionInProject,
                    onOpenSession = onOpenSession,
                    onOpenDevice = onOpenDevice,
                    deviceAgentPreviews = deviceAgentPreviews,
                    onPairDevice = { navigate(AppDestination.DeviceSetup) },
                    )
                }
                AppDestination.NewSession, AppDestination.NewProject -> androidx.compose.runtime.key(serverUrl, userId, destination) { NewSessionScreen(
                    navigate = navigate,
                    sessionsState = sessionsState,
                    projectSessionsById = projectSessionsById,
                    serverUrl = serverUrl,
                    userId = userId,
                    onListDirectory = onListDirectory,
                    onListRuntimes = onListNewSessionRuntimes,
                    onLoadRuntimeCapabilities = onLoadNewSessionRuntimeCapabilities,
                    onLoadModelCatalog = onLoadNewSessionModelCatalog,
                    onLoadPermissionCatalog = onLoadNewSessionPermissionCatalog,
                    onPrepareSession = onPrepareSession,
                    onRefreshDevices = onRefreshSessions,
                    devicesRefreshing = isRefreshingSessions,
                    initialProjectId = initialNewSessionProjectId.takeIf { destination == AppDestination.NewSession },
                    projectOnly = destination == AppDestination.NewProject,
                    sidebarViewMode = sidebarViewMode,
                    onLoadProjects = onLoadProjects,
                    onCreateProject = onCreateProject,
                ) }
                AppDestination.SessionDetail -> SessionDetailScreen(
                    navigate = navigate,
                    sessionId = selectedSessionId,
                    initialSession = preparedSessionDraft?.previewSession() ?: sessionsState.sessions
                        .asSequence()
                        .plus(sessionsState.archivedSessions.asSequence())
                        .firstOrNull { it.id == selectedSessionId },
                    preparedSession = preparedSessionDraft,
                    onCreatePreparedSession = onCreateSession,
                    onPreparedSessionCreated = onPreparedSessionCreated,
                    onLoadPreparedModelCatalog = onLoadNewSessionModelCatalog,
                    onLoadPreparedPermissionCatalog = onLoadNewSessionPermissionCatalog,
                    devices = sessionsState.devices,
                    controller = sessionDetailController,
                    realtimeController = sessionRealtimeController,
                    filesController = filesController,
                    terminalPool = remoteTerminalPool,
                    composerDraftStore = sessionComposerDraftStore,
                    onSessionChanged = onSessionChanged,
                )
                AppDestination.DeviceDetail -> DeviceDetailScreen(
                    navigate = navigate,
                    state = sessionsState,
                    selectedDeviceId = selectedDeviceId,
                    backDestination = deviceDetailReturnDestination,
                    onOpenSession = onOpenSession,
                    onRenameDevice = onRenameDevice,
                    onDeleteDevice = onDeleteDevice,
                    onPrepareDeviceSetup = onPrepareDeviceSetup,
                    onClaimDevicePairCode = onClaimDevicePairCode,
                    onListDeviceRuntimes = onListDeviceRuntimes,
                    onSetDeviceRuntimeActive = { connectorId, runtime, active ->
                        onSetDeviceRuntimeActive(connectorId, runtime, active).onSuccess {
                            deviceAgentPreviewRefreshKey += 1L
                        }
                    },
                    onDeleteDeviceRuntimeConfig = { connectorId, runtime ->
                        onDeleteDeviceRuntimeConfig(connectorId, runtime).onSuccess {
                            deviceAgentPreviewRefreshKey += 1L
                        }
                    },
                    onBulkSetSessionsArchived = onBulkSetSessionsArchived,
                    onArchiveAllDeviceSessions = onArchiveAllDeviceSessions,
                )
                AppDestination.Devices -> DevicesScreen(
                    state = sessionsState,
                    isRefreshing = isRefreshingSessions,
                    onRefresh = onRefreshSessions,
                    onOpenDevice = onOpenDevice,
                    onBack = { navigate(AppDestination.Sessions) },
                    agentPreviews = deviceAgentPreviews,
                    onAddDevice = { navigate(AppDestination.DeviceSetup) },
                )
                AppDestination.Terminal -> TerminalScreen(
                    navigate = navigate,
                    state = sessionsState,
                    terminalPool = remoteTerminalPool,
                    onPairDevice = { navigate(AppDestination.DeviceSetup) },
                )
                AppDestination.Files -> FilesScreen(
                    navigate = navigate,
                    state = sessionsState,
                    controller = filesController,
                    onPairDevice = { navigate(AppDestination.DeviceSetup) },
                )
                AppDestination.DeviceSetup -> androidx.compose.runtime.key(serverUrl, userId) {
                    AddDeviceScreen(
                        devices = sessionsState.devices,
                        pairingStates = devicePairingStates,
                        onBack = { navigate(deviceSetupReturnDestination) },
                        onComplete = {
                            onDevicePairingComplete()
                            navigate(deviceSetupReturnDestination)
                        },
                        onCreateCredential = onCreateDeviceSetup,
                        onRenameDevice = onRenameDevice,
                        onClaimPairCode = onClaimDevicePairCode,
                        onWaitForDevice = onWaitForPairingDevice,
                        onClearPairing = onClearDevicePairing,
                    )
                }
                AppDestination.ArchivedSessions -> androidx.compose.runtime.key(serverUrl, userId) {
                    ArchivedSessionsScreen(
                        projects = sessionsState.projects,
                        onLoadPage = onLoadArchivedPage,
                        onRestoreSession = { onSetSessionArchived(it, false) },
                        onRestoreProject = onRestoreProject,
                        onBack = { navigate(AppDestination.Sessions) },
                    )
                }
            }
        }
    }
}
