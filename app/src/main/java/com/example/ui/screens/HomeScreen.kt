package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Project
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToEditor: (String) -> Unit
) {
    val prompt by viewModel.prompt.collectAsStateWithLifecycle()
    val enhancePromptEnabled by viewModel.enhancePromptEnabled.collectAsStateWithLifecycle()
    val recentProjects by viewModel.recentProjects.collectAsStateWithLifecycle()
    val generationState by viewModel.generationState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(generationState) {
        if (generationState is GenerationState.Complete) {
            // Navigation handled via callback if we want, but it's cleaner to reset and navigate
            viewModel.resetState()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RectangleShape,
                modifier = Modifier.width(360.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Projects", style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { viewModel.onPromptChange("") }) {
                            Icon(Icons.Default.Add, contentDescription = "New Project")
                        }
                    }

                    if (recentProjects.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No projects yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentProjects) { project ->
                                ProjectCard(project) { 
                                    scope.launch { drawerState.close() }
                                    onNavigateToEditor(project.id) 
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                // Floating Nav Bar
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 32.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VEYRA",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { viewModel.onPromptChange("") }) {
                        Icon(Icons.Default.Add, contentDescription = "New Project", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "History", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }

                // Main Content Area
                Column(
                    modifier = Modifier
                        .widthIn(max = 800.dp)
                        .fillMaxWidth()
                        .padding(top = 100.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Describe your vision.",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { viewModel.onPromptChange(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .defaultMinSize(minHeight = 160.dp),
                        placeholder = {
                            Text("Describe the website you want to build...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        shape = RoundedCornerShape(4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.toggleEnhancePrompt() }
                                .padding(8.dp)
                        ) {
                            Switch(
                                checked = enhancePromptEnabled,
                                onCheckedChange = { viewModel.toggleEnhancePrompt() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Enhance Prompt",
                                tint = if (enhancePromptEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Enhance with AI",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (enhancePromptEnabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { viewModel.generateWebsite(onNavigateToEditor) },
                            enabled = prompt.isNotBlank() && generationState is GenerationState.Idle,
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Generate", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    // Example Prompts
                    Text("Try an example:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ExampleChip("Premium barber website in Rhodes", viewModel)
                        ExampleChip("Minimalist restaurant", viewModel)
                        ExampleChip("Luxury real-estate", viewModel)
                    }

                    // Generation Status
                    AnimatedVisibility(visible = generationState !is GenerationState.Idle) {
                        GenerationStatusCard(generationState)
                    }
                }
            }
        }
    }
}

@Composable
fun ExampleChip(text: String, viewModel: HomeViewModel) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { viewModel.setExamplePrompt(text) }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProjectCard(project: Project, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Code,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = project.originalPrompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GenerationStatusCard(state: GenerationState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = when (state) {
                GenerationState.Understanding -> "Understanding request..."
                GenerationState.Enhancing -> "AI Architect is enhancing prompt..."
                GenerationState.Architecting -> "Planning website structure..."
                GenerationState.Generating -> "Generating components and code..."
                GenerationState.PreparingPreview -> "Preparing live preview..."
                GenerationState.Complete -> "Complete!"
                is GenerationState.Error -> "Error: ${state.message}"
                GenerationState.Idle -> ""
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
