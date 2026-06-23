package com.streamvault.app.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.*
import com.streamvault.app.R
import com.streamvault.app.ui.components.SearchInput
import com.streamvault.app.ui.components.shell.StatusPill
import com.streamvault.app.ui.design.AppColors
import com.streamvault.app.ui.interaction.TvButton
import com.streamvault.domain.model.Result
import com.streamvault.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val providerRepository: com.streamvault.domain.repository.ProviderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(code: String) {
        if (code.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            when (val result = authRepository.loginWithCode(code)) {
                is Result.Success -> {
                    providerRepository.syncProvidersFromFirebase()
                    _uiState.value = LoginUiState.Success
                }
                is Result.Error -> _uiState.value = LoginUiState.Error(result.message)
                is Result.Loading -> _uiState.value = LoginUiState.Loading
            }
        }
    }
}

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var loginCode by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1014)), // Dark sleek background
        contentAlignment = Alignment.Center
    ) {
        // Abstract decorative orbs
        Box(
            modifier = Modifier
                .offset(x = (-200).dp, y = (-150).dp)
                .size(500.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(AppColors.Brand.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .offset(x = 250.dp, y = 150.dp)
                .size(600.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF6C20D6).copy(alpha = 0.25f), Color.Transparent)
                    )
                )
        )

        Surface(
            modifier = Modifier
                .width(480.dp)
                .padding(24.dp),
            shape = RoundedCornerShape(32.dp),
            colors = SurfaceDefaults.colors(
                containerColor = Color(0xFF181920).copy(alpha = 0.85f)
            ),
            border = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.03f))
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 48.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                StatusPill(
                    label = "KOBANI 4K",
                    containerColor = AppColors.Brand,
                    contentColor = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = AppColors.TextPrimary
                )

                Text(
                    text = "Enter your activation code to sync your playlist and start watching.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppColors.TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(AppColors.SurfaceElevated, RoundedCornerShape(16.dp))
                        .border(1.dp, AppColors.Focus.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = loginCode,
                        onValueChange = { loginCode = it },
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            color = AppColors.TextPrimary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            letterSpacing = 2.sp
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (loginCode.isEmpty()) {
                                Text(
                                    text = "Activation Code",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = AppColors.TextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                if (uiState is LoginUiState.Error) {
                    Text(
                        text = (uiState as LoginUiState.Error).message,
                        color = AppColors.Warning,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TvButton(
                    onClick = { viewModel.login(loginCode) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = uiState !is LoginUiState.Loading && loginCode.isNotBlank(),
                    colors = ButtonDefaults.colors(
                        containerColor = AppColors.Brand,
                        contentColor = Color.White,
                        focusedContainerColor = AppColors.SurfaceEmphasis
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(16.dp))
                ) {
                    if (uiState is LoginUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Text(
                            text = "Log In to KOBANI",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}
