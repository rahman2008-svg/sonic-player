package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.data.model.VideoEntity
import com.example.ui.SonicPlayerViewModel
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.MusicPlayerDetailView
import com.example.ui.components.VideoPlayerView
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MusicLibraryScreen
import com.example.ui.screens.VideoLibraryScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var _viewModel: SonicPlayerViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val viewModel = ViewModelProvider(
            this,
            SonicPlayerViewModel.Factory(application)
        )[SonicPlayerViewModel::class.java]
        
        _viewModel = viewModel

        setContent {
            MyApplicationTheme {
                var selectedTab by remember { mutableIntStateOf(0) }
                val currentSong by viewModel.playbackManager.currentSong.collectAsState()
                val isPlaying by viewModel.playbackManager.isPlaying.collectAsState()
                val position by viewModel.playbackManager.currentPosition.collectAsState()
                val duration by viewModel.playbackManager.duration.collectAsState()
                
                var showPlayerDetail by remember { mutableStateOf(false) }
                var activeVideoToPlay by remember { mutableStateOf<VideoEntity?>(null) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (activeVideoToPlay == null) {
                            Column {
                                // Floating Mini Player
                                if (currentSong != null) {
                                    MiniPlayerBar(
                                        currentSong = currentSong!!,
                                        isPlaying = isPlaying,
                                        position = position,
                                        duration = duration,
                                        onTogglePlay = { viewModel.playbackManager.togglePlayPause() },
                                        onClick = { showPlayerDetail = true },
                                        onNext = { viewModel.playbackManager.skipToNext() }
                                    )
                                }
                                
                                // Standard Navigation Bar
                                NavigationBar(
                                    modifier = Modifier.testTag("main_bottom_nav"),
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 0.dp
                                ) {
                                    val navItemColors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    NavigationBarItem(
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0 },
                                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                        label = { Text("Home") },
                                        colors = navItemColors,
                                        modifier = Modifier.testTag("nav_home_tab")
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1 },
                                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                                        label = { Text("Music") },
                                        colors = navItemColors,
                                        modifier = Modifier.testTag("nav_music_tab")
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 2,
                                        onClick = { selectedTab = 2 },
                                        icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                                        label = { Text("Videos") },
                                        colors = navItemColors,
                                        modifier = Modifier.testTag("nav_video_tab")
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 3,
                                        onClick = { selectedTab = 3 },
                                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                                        label = { Text("About") },
                                        colors = navItemColors,
                                        modifier = Modifier.testTag("nav_about_tab")
                                    )
                                }
                            }
                        }
                    },
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            0 -> HomeScreen(
                                viewModel = viewModel,
                                onNavigateToMusic = { selectedTab = 1 },
                                onNavigateToVideos = { selectedTab = 2 },
                                onPlayVideo = { video -> activeVideoToPlay = video }
                            )
                            1 -> MusicLibraryScreen(viewModel = viewModel)
                            2 -> VideoLibraryScreen(
                                viewModel = viewModel,
                                onPlayVideo = { video -> activeVideoToPlay = video }
                            )
                            3 -> AboutScreen()
                        }
                    }
                }

                // Full Screen Music Player Detail
                AnimatedVisibility(
                    visible = showPlayerDetail,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    MusicPlayerDetailView(
                        playbackManager = viewModel.playbackManager,
                        onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                        onCollapse = { showPlayerDetail = false }
                    )
                }

                // Video Player Screen Overlay
                if (activeVideoToPlay != null) {
                    val video = activeVideoToPlay!!
                    VideoPlayerView(
                        uri = video.uriString,
                        title = video.title,
                        onClose = { activeVideoToPlay = null }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        _viewModel?.playbackManager?.release()
        super.onDestroy()
    }
}
