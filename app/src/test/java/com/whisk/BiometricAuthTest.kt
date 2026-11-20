package com.whisk

import android.content.Context
import android.content.ContextWrapper
import com.whisk.security.BiometricAuthHelper
import com.whisk.security.BiometricAuthenticator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricAuthTest {

    private val dummyContext: Context = ContextWrapper(null)

    @Test
    fun testAuthenticatorFailClosedWhenBiometricUnavailable() {
        val fakeAuthenticator = object : BiometricAuthenticator {
            override fun isAvailable(context: Context): Boolean = false
            override fun authenticate(
                context: Context,
                title: String,
                subtitle: String,
                onSuccess: () -> Unit,
                onError: (String) -> Unit
            ) {
                onError("Biometric authentication unavailable")
            }
        }

        BiometricAuthHelper.authenticator = fakeAuthenticator

        var successCalled = false
        var errorCalled = false
        var errorMessage = ""

        BiometricAuthHelper.promptBiometricAuth(
            context = dummyContext,
            onSuccess = { successCalled = true },
            onError = { err ->
                errorCalled = true
                errorMessage = err
            }
        )

        assertFalse("Authentication must fail closed when unavailable", successCalled)
        assertTrue("Error callback must be invoked when unavailable", errorCalled)
        assertTrue(errorMessage.contains("unavailable"))
    }

    @Test
    fun testAuthenticatorInvokesSuccessOnlyWhenCallbackSucceeds() {
        val fakeAuthenticator = object : BiometricAuthenticator {
            override fun isAvailable(context: Context): Boolean = true
            override fun authenticate(
                context: Context,
                title: String,
                subtitle: String,
                onSuccess: () -> Unit,
                onError: (String) -> Unit
            ) {
                onSuccess()
            }
        }

        BiometricAuthHelper.authenticator = fakeAuthenticator

        var successCalled = false
        var errorCalled = false

        BiometricAuthHelper.promptBiometricAuth(
            context = dummyContext,
            onSuccess = { successCalled = true },
            onError = { errorCalled = true }
        )

        assertTrue("Success callback must be called on valid biometric auth", successCalled)
        assertFalse("Error callback must not be called on valid biometric auth", errorCalled)
    }
}
