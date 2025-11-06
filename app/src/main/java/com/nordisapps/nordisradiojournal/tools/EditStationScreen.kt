package com.nordisapps.nordisradiojournal.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nordisapps.nordisradiojournal.Station
import com.nordisapps.nordisradiojournal.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStationScreen(
    stationId: String?, // ID станции, которую редактируем. Null, если это новая станция.
    uiState: UiState,
    onNavigateBack: () -> Unit,
    onSaveStation: (Station) -> Unit
) {
    // 1. Находим станцию в общем списке по ID
    val stationToEdit = remember(stationId, uiState.stations) {
        uiState.stations.find { it.id == stationId }
    }

    // 2. Создаем состояния для каждого текстового поля
    var displayId by remember {
        mutableStateOf(stationToEdit?.displayId?.toString() ?: "")
    }
    var name by remember { mutableStateOf(stationToEdit?.name ?: "") }
    var iconUrl by remember { mutableStateOf(stationToEdit?.icon ?: "") }
    var streamUrl by remember { mutableStateOf(stationToEdit?.stream ?: "") }
    var freq by remember { mutableStateOf(stationToEdit?.freq ?: "") }
    var city by remember { mutableStateOf(stationToEdit?.mainCity ?: "") }
    var stationCity by remember { mutableStateOf(stationToEdit?.stationCity ?: "") }
    var country by remember { mutableStateOf(stationToEdit?.country ?: "") }
    var location by remember { mutableStateOf(stationToEdit?.location ?: "") }
    var ps by remember { mutableStateOf(stationToEdit?.ps ?: "") }
    var rt by remember { mutableStateOf(stationToEdit?.rt ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (stationId == null) "Новая станция" else "Редактирование") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val updatedStation = Station(
                    id = stationToEdit?.id,
                    displayId = displayId.toIntOrNull(),
                    name = name,
                    icon = iconUrl,
                    stream = streamUrl,
                    freq = freq,
                    country = country,
                    mainCity = city,
                    stationCity = stationCity,
                    location = location,
                    ps = ps,
                    rt = rt
                )
                onSaveStation(updatedStation)
            }) {
                Icon(Icons.Default.Save, contentDescription = "Сохранить")
            }
        }
    ) { paddingValues ->
        // Используем Column с прокруткой, так как полей будет много
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EditTextField(label = "ID", value = displayId, onValueChange = { displayId = it})
            EditTextField(label = "Название", value = name, onValueChange = { name = it })
            EditTextField(label = "URL иконки", value = iconUrl, onValueChange = { iconUrl = it }, keyboardType = KeyboardType.Uri)
            EditTextField(label = "URL потока", value = streamUrl, onValueChange = { streamUrl = it }, keyboardType = KeyboardType.Uri)
            EditTextField(label = "Частота", value = freq, onValueChange = { freq = it })
            EditTextField(label = "Город станции", value = stationCity, onValueChange = { stationCity = it })
            EditTextField(label = "Главный город", value = city, onValueChange = { city = it })
            EditTextField(label = "Локация", value = location, onValueChange = { location = it })
            EditTextField(label = "PS", value = ps, onValueChange = { ps = it })
            EditTextField(label = "RT", value = rt, onValueChange = { rt = it })
            EditTextField(label = "Страна", value = country, onValueChange = { country = it })
        }
    }
}

@Composable
private fun EditTextField(label: String, value: String, onValueChange: (String) -> Unit, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}
