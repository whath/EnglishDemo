package com.englishcoach60.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.englishcoach60.app.navigation.*
import com.englishcoach60.app.presentation.home.HomeScreen
import com.englishcoach60.app.presentation.home.HomeViewModel
import com.englishcoach60.app.presentation.library.LibraryScreen
import com.englishcoach60.app.presentation.library.LibraryViewModel
import com.englishcoach60.app.presentation.search.SearchScreen
import com.englishcoach60.app.presentation.search.SearchViewModel
import com.englishcoach60.app.presentation.settings.SettingsScreen
import com.englishcoach60.app.presentation.settings.SettingsViewModel
import com.englishcoach60.app.presentation.training.TrainingScreen
import com.englishcoach60.app.presentation.training.TrainingViewModel
import com.englishcoach60.designsystem.EnglishCoachTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val backStack = rememberNavBackStack(HomeRoute)
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.state.collectAsStateWithLifecycle()
            EnglishCoachTheme(settings.settings.themeMode) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = entryProvider {
                        entry<HomeRoute> {
                            val vm: HomeViewModel = hiltViewModel()
                            HomeScreen(
                                vm,
                                onTraining = { backStack.add(TrainingRoute) },
                                onLibrary = { backStack.add(LibraryRoute) },
                                onSettings = { backStack.add(SettingsRoute) },
                                onSearch = { backStack.add(SearchRoute) },
                            )
                        }
                        entry<TrainingRoute> {
                            val vm: TrainingViewModel = hiltViewModel()
                            TrainingScreen(vm, onClose = { backStack.removeAt(backStack.lastIndex) }, onCompleted = {
                                backStack.clear(); backStack.add(HomeRoute)
                            })
                        }
                        entry<ReviewRoute> {
                            val vm: TrainingViewModel = hiltViewModel()
                            TrainingScreen(vm, onClose = { backStack.removeAt(backStack.lastIndex) }, onCompleted = {
                                backStack.clear(); backStack.add(HomeRoute)
                            })
                        }
                        entry<LibraryRoute> {
                            val vm: LibraryViewModel = hiltViewModel()
                            LibraryScreen(vm, onBack = { backStack.removeAt(backStack.lastIndex) })
                        }
                        entry<SettingsRoute> {
                            SettingsScreen(settingsViewModel, onBack = { backStack.removeAt(backStack.lastIndex) })
                        }
                        entry<SearchRoute> {
                            val vm: SearchViewModel = hiltViewModel()
                            SearchScreen(vm, onBack = { backStack.removeAt(backStack.lastIndex) })
                        }
                    },
                )
            }
        }
    }
}
