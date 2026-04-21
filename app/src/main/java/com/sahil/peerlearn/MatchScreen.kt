package com.sahil.peerlearn

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahil.peerlearn.ui.theme.SpaceBlack
import kotlinx.coroutines.launch

@Composable
fun MatchScreen(
    uid: String,
    onPeerClick: (String) -> Unit,
    viewModel: MatchViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return MatchViewModel(PeerRepository(UserRepository()), uid) as T
        }
    })
) {
    val recommendedPeers by viewModel.recommendedPeersWithMatch.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState) {
        uiState?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiState()
        }
    }

    Scaffold(
        containerColor = SpaceBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(recommendedPeers) { (peer, match) ->
                PeerCard(
                    user = peer,
                    onViewProfile = { onPeerClick(peer.uid) },
                    // ✅ FIXED: Ab actually connection request bhejta hai
                    onConnectClick = {
                        scope.launch {
                            viewModel.sendConnectionRequest(peer.uid)
                        }
                    },
                    matchPercentage = match,
                    isOnline = true,
                    currentUid = uid,
                    viewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return HomeViewModel(HomeRepository(UserRepository())) as T
                        }
                    })
                )
            }
        }
    }
}