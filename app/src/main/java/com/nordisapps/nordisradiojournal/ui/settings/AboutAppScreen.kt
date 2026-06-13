package com.nordisapps.nordisradiojournal.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.nordisapps.nordisradiojournal.BuildConfig

@Composable
fun AboutScreen() {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Radio Journal",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = BuildConfig.VERSION_NAME,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "© 2026 Nordis",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.weight(5f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = { uriHandler.openUri("https://github.com/ROSTYSLAV4IK22/nordis-radio-journal/blob/main/LICENSE") }
            ) {
                Text(
                    text = "License",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            TextButton(
                onClick = { uriHandler.openUri("https://github.com/ROSTYSLAV4IK22/nordis-radio-journal/blob/main/EULA.md") }
            ) {
                Text(
                    text = "EULA",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}