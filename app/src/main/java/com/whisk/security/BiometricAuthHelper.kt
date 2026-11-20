package com.whisk.security

import android.content.Context

object BiometricAuthHelper {
    var authenticator: BiometricAuthenticator = DefaultBiometricAuthenticator()

    fun isBiometricAvailable(context: Context): Boolean {
        return authenticator.isAvailable(context)
    }

    fun promptBiometricAuth(
        context: Context,
        title: String = "Authenticate to unlock secret note",
        subtitle: String = "Use your fingerprint, face, or device PIN",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        authenticator.authenticate(
            context = context,
            title = title,
            subtitle = subtitle,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
