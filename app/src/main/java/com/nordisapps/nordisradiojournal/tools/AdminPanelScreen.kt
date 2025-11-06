package com.nordisapps.nordisradiojournal.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.nordisapps.nordisradiojournal.Station
import com.nordisapps.nordisradiojournal.UiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    uiState: UiState,
    imageLoader: ImageLoader,
    onAddStationClicked: () -> Unit,
    onEditStationClicked: (Station) -> Unit,
    onDeleteStationClicked: (Station) -> Unit
) {
    var stationToDelete by remember { mutableStateOf<Station?>(null) }
    val listState = rememberLazyListState()
    var isFabVisible by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemScrollOffset }
            .map { it > 0 } // Проверяем, сдвинулся ли список с самого верха
            .distinctUntilChanged() // Реагируем только на реальное изменение направления
            .collect { isScrollingDown ->
                isFabVisible = !isScrollingDown
            }
    }

    Scaffold(
        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible,
                enter = slideInVertically(initialOffsetY = { it * 2 }),
                exit = slideOutVertically(targetOffsetY = { it * 2 })
            ) {
                FloatingActionButton(onClick = onAddStationClicked) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp), // Отступы для всего списка
            verticalArrangement = Arrangement.spacedBy(12.dp) // Расстояние между элементами
        ) {
            // Используем items для отображения списка станций
            items(uiState.stations, key = { it.id ?: it.name ?: "" }) { station ->
                AdminStationItem(
                    station = station,
                    imageLoader = imageLoader,
                    onEdit = { onEditStationClicked(station) },
                    onDelete = { stationToDelete = station }
                )
            }
        }
    }
    if (stationToDelete != null) {
        AlertDialog(
            onDismissRequest = { stationToDelete = null },
            title = { Text("Подтверждение") },
            text = { Text("Вы уверены, что хотите удалить станцию \"${stationToDelete?.name}\"? Это действие необратимо.") },
            confirmButton = {
                Button(
                    onClick = {
                        stationToDelete?.let {
                            onDeleteStationClicked(it)
                        }
                        stationToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { stationToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun AdminStationItem(
    station: Station,
    imageLoader: ImageLoader,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = station.icon,
                    contentDescription = station.name,
                    imageLoader = imageLoader,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(station.name ?: "No Name", style = MaterialTheme.typography.titleMedium)
                    // Отображаем ID, так как это важно для администрирования
                    Text(
                        station.id ?: "No ID",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Кнопки "Редактировать" и "Удалить"
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp), // Отступы для содержимого
                    verticalArrangement = Arrangement.spacedBy(4.dp) // Расстояние между строками
                ) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp)) // Разделитель

                    // Выводим все поля станции с подписями
                    InfoRow("Icon URL:", station.icon)
                    InfoRow("Stream URL:", station.stream)
                    InfoRow("Frequency:", station.freq)
                    InfoRow("City:", station.stationCity)
                    InfoRow("Main City:", station.mainCity)
                    InfoRow("Location:", station.location)
                    InfoRow("PS:", station.ps)
                    InfoRow("RT:", station.rt)
                    InfoRow("Country:", station.country)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(120.dp) // Фиксированная ширина для метки
        )
        Text(
            text = value ?: "-", // Если значение null, показываем прочерк
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


