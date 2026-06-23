package com.streamvault.app.navigation

import androidx.lifecycle.ViewModel
import com.streamvault.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    val repository: AuthRepository
) : ViewModel()
