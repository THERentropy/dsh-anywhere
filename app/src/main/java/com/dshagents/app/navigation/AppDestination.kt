package com.dshagents.app.navigation

enum class AppDestination(val title: String) {
    ModePicker("Home"),
    DshPairing("DSH Pairing"),
    DshWeb("DSH Remote"),
    LoginMethods("Login"),
    ServerSetup("Server"),
    QrLogin("QR Login"),
    QrWaiting("Waiting"),
    Sessions("Sessions"),
    NewSession("New Session"),
    NewProject("New Project"),
    Devices("Devices"),
    Terminal("Terminal"),
    Files("Files"),
    DeviceDetail("Device Detail"),
    SessionDetail("Session"),
    ArchivedSessions("Archived Sessions"),
    DeviceSetup("Device Setup"),
}
