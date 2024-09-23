package com.onandor.nesemu.viewmodels

import androidx.lifecycle.ViewModel
import com.onandor.nesemu.navigation.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val navManager: NavigationManager
) : ViewModel() {
}