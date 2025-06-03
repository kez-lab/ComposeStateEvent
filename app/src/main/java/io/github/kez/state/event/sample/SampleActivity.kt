package io.github.kez.state.event.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kez.state.event.sample.screen.detail.SampleDetailActivity
import io.github.kez.state.event.sample.ui.theme.StateEventSampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StateEventSampleTheme {
                SampleScreen()
            }
        }
    }
}

@Composable
fun SampleScreen(
    viewModel: SampleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    HandleStateEvent(
        uiState = uiState,
        stateEventHandler = viewModel,
        onShowSuccessMessage = { message ->
            snackBarHostState.showSnackbar(message)
        },
        onErrorMessage = { message ->
            snackBarHostState.showSnackbar("Error: $message")
        },
        onNavigateToDetail = { item ->
            // 🚀 Navigate to DetailActivity using our library!
            context.startActivity(SampleDetailActivity.newIntent(context, item))
        }
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "State Event Sample",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = { viewModel.loadData() },
                enabled = !uiState.isLoading
            ) {
                Text("Load Data")
            }

            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.data) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.onItemClick(item) }
                    ) {
                        Text(
                            text = item,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SampleScreenPreview() {
    StateEventSampleTheme {
        SampleScreen()
    }
}