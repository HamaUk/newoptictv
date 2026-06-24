package com.streamvault.app.ui.screens.login

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.*
import com.streamvault.app.R
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("login_prefs", android.content.Context.MODE_PRIVATE) }
    
    val uiState by viewModel.uiState.collectAsState()
    var loginCode by remember { mutableStateOf(sharedPreferences.getString("saved_login_code", "") ?: "") }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess()
        }
    }

    // Background animation
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090C)),
        contentAlignment = Alignment.Center
    ) {
        // Dynamic Orbs
        Box(
            modifier = Modifier
                .offset(x = (-300).dp, y = (-200).dp)
                .size(700.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(AppColors.Brand.copy(alpha = 0.2f), Color.Transparent),
                        radius = 700f + (gradientOffset / 5f)
                    )
                )
        )
        Box(
            modifier = Modifier
                .offset(x = 350.dp, y = 200.dp)
                .size(800.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF6C20D6).copy(alpha = 0.15f), Color.Transparent),
                        radius = 800f - (gradientOffset / 5f)
                    )
                )
        )

        Surface(
            modifier = Modifier
                .width(440.dp)
                .padding(24.dp),
            shape = RoundedCornerShape(32.dp),
            colors = SurfaceDefaults.colors(
                containerColor = Color(0xFF13141B).copy(alpha = 0.75f)
            ),
            border = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.02f))
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 40.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Kobani4k Logo Text
                Text(
                    text = "KOBANI4K",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.TextPrimary
                )

                Text(
                    text = "Enter your activation code below to sync your playlist.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFF1A1B22), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = loginCode,
                        onValueChange = { loginCode = it },
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            color = AppColors.TextPrimary,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (loginCode.isEmpty()) {
                                Text(
                                    text = "ACTIVATION CODE",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = AppColors.TextSecondary.copy(alpha = 0.4f),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 2.sp
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
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val isLoading = uiState is LoginUiState.Loading
                
                // Button Animation
                val buttonScale by animateFloatAsState(
                    targetValue = if (isLoading) 0.95f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "buttonScale"
                )

                TvButton(
                    onClick = { 
                        sharedPreferences.edit().putString("saved_login_code", loginCode).apply()
                        viewModel.login(loginCode) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .scale(buttonScale),
                    enabled = !isLoading && loginCode.isNotBlank(),
                    colors = ButtonDefaults.colors(
                        containerColor = AppColors.Brand,
                        contentColor = Color.White,
                        focusedContainerColor = AppColors.Brand.copy(alpha = 0.8f)
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(12.dp))
                ) {
                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Authenticating...",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    } else {
                        Text(
                            text = "Log In",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
