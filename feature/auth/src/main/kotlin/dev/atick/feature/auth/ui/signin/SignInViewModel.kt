/*
 * Copyright 2025 Atick Faisal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.atick.feature.auth.ui.signin

import android.app.Activity
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.atick.core.extensions.isEmailValid
import dev.atick.core.extensions.isPasswordValid
import dev.atick.core.ui.utils.TextFiledData
import dev.atick.core.ui.utils.UiState
import dev.atick.core.ui.utils.updateState
import dev.atick.core.ui.utils.updateWith
import dev.atick.data.repository.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import dev.atick.data.repository.profile.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers


/**
 * [ViewModel] for [SignInScreen].
 *
 * @param authRepository [AuthRepository].
 */
@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {
    companion object {
        private const val TAG = "Auth/SignInVM"
    }
    private val _signInUiState = MutableStateFlow(UiState(SignInScreenData()))
    val signInUiState = _signInUiState.asStateFlow()

    fun updateEmail(email: String) {
        _signInUiState.updateState {
            copy(
                email = TextFiledData(
                    value = email,
                    errorMessage = if (email.isEmailValid()) null else "Email Not Valid",
                ),
            )
        }
    }

    fun updatePassword(password: String) {
        _signInUiState.updateState {
            copy(
                password = TextFiledData(
                    value = password,
                    errorMessage = if (password.isPasswordValid()) null else "Password Not Valid",
                ),
            )
        }
    }

    fun signInWithSavedCredentials(activity: Activity) {
        _signInUiState.updateWith(viewModelScope) {
            Log.d(TAG, "ENTER signInWithSavedCredentials")
            val result = authRepository.signInWithSavedCredentials(activity)
            Log.d(TAG, "signInWithSavedCredentials result=$result")

            if (!result.isSuccess) return@updateWith result
            val u = auth.currentUser ?: run {
                Log.w(TAG, "SavedCreds ok but currentUser is null")
                return@updateWith result
            }
            val display = (u.displayName ?: "").ifBlank { "Unknown User" }

            // Run ensure in its own job so UI-state cancellation doesn't kill it
            viewModelScope.launch(Dispatchers.IO) {
                Log.d(TAG, "ensureUserDoc[SavedCreds] START uid=${u.uid}")
                runCatching {
                    withContext(NonCancellable) {
                        userRepository.ensure(u.uid, u.email, display)
                    }
                }.onSuccess {
                    Log.d(TAG, "ensureUserDoc[SavedCreds] DONE uid=${u.uid}")
                }.onFailure { e ->
                    Log.e(TAG, "ensureUserDoc[SavedCreds] failed", e)
                }
            }

            result
        }
    }

    fun signInWithGoogle(activity: Activity) {
        _signInUiState.updateWith(viewModelScope) {
            Log.d(TAG, "ENTER signInWithGoogle")
            val result = authRepository.signInWithGoogle(activity)
            Log.d(TAG, "signInWithGoogle result=$result")

            if (!result.isSuccess) return@updateWith result
            val u = auth.currentUser ?: run {
                Log.w(TAG, "Google sign-in ok but currentUser is null")
                return@updateWith result
            }
            val display = (u.displayName ?: "").ifBlank { "Unknown User" }

            viewModelScope.launch(Dispatchers.IO) {
                Log.d(TAG, "ensureUserDoc[Google] START uid=${u.uid}")
                runCatching {
                    withContext(NonCancellable) {
                        userRepository.ensure(u.uid, u.email, display)
                    }
                }.onSuccess {
                    Log.d(TAG, "ensureUserDoc[Google] DONE uid=${u.uid}")
                }.onFailure { e ->
                    Log.e(TAG, "ensureUserDoc[Google] failed", e)
                }
            }

            result
        }
    }

    fun loginWithEmailAndPassword() {
        _signInUiState.updateWith(viewModelScope) {
            Log.d(TAG, "ENTER loginWithEmailAndPassword")
            val result = authRepository.signInWithEmailAndPassword(
                email = email.value,
                password = password.value,
            )
            Log.d(TAG, "loginWithEmailAndPassword result=$result")

            if (!result.isSuccess) return@updateWith result
            val u = auth.currentUser ?: run {
                Log.w(TAG, "Email sign-in ok but currentUser is null")
                return@updateWith result
            }
            val display = u.displayName ?: "Unknown User"

            // 🔧 Run Firestore write in a separate job that won't be cancelled by updateWith
            viewModelScope.launch(Dispatchers.IO) {
                Log.d(TAG, "ensureUserDoc[Email] START uid=${u.uid}")
                runCatching {
                    withContext(NonCancellable) {
                        userRepository.ensure(u.uid, u.email, display)
                    }
                }.onSuccess {
                    Log.d(TAG, "ensureUserDoc[Email] DONE uid=${u.uid}")
                }.onFailure { e ->
                    Log.e(TAG, "ensureUserDoc[Email] failed", e)
                }
            }

            result
        }
    }
}

/**
 * Data for [SignInScreen].
 *
 * @param email [TextFiledData].
 * @param password [TextFiledData].
 */
@Immutable
data class SignInScreenData(
    val email: TextFiledData = TextFiledData(String()),
    val password: TextFiledData = TextFiledData(String()),
)
