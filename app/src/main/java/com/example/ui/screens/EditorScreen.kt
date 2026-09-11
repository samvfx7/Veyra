package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Project

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit
) {
    val project by viewModel.project.collectAsStateWithLifecycle()
    val editInstruction by viewModel.editInstruction.collectAsStateWithLifecycle()
    val isEditing by viewModel.isEditing.collectAsStateWithLifecycle()
    val editError by viewModel.editError.collectAsStateWithLifecycle()
    val lastChange by viewModel.lastChangeDescription.collectAsStateWithLifecycle()
    val systemNotice by viewModel.systemNotice.collectAsStateWithLifecycle()

    var viewMode by remember { mutableStateOf(ViewMode.PREVIEW) }
    var selectedTab by remember { mutableStateOf(CodeTab.HTML) }
    var viewportMode by remember { mutableStateOf(ViewportMode.DESKTOP) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val currentProject = project!!

    if (showRenameDialog) {
        RenameProjectDialog(
            currentName = currentProject.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                viewModel.renameProject(newName)
                showRenameDialog = false
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top Action & Navigation Bar
                EditorTopBar(
                    project = currentProject,
                    viewMode = viewMode,
                    viewportMode = viewportMode,
                    canUndo = currentProject.previousHtml != null,
                    onNavigateBack = onNavigateBack,
                    onRenameClick = { showRenameDialog = true },
                    onUndoClick = { viewModel.undoEdit() },
                    onRefreshClick = { reloadKey++ },
                    onShareClick = { shareProject(context, currentProject) },
                    onCopyCodeClick = {
                        val fullHtml = buildSelfContainedHtml(currentProject)
                        clipboardManager.setText(AnnotatedString(fullHtml))
                        Toast.makeText(context, "Full website bundle copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    onViewportModeChange = { viewportMode = it },
                    onViewModeChange = { viewMode = it }
                )

                // Main Content Workspace (Preview vs Code)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (viewMode == ViewMode.PREVIEW) {
                        WebsitePreview(
                            project = currentProject,
                            viewportMode = viewportMode,
                            reloadTrigger = reloadKey
                        )
                    } else {
                        CodeViewer(
                            project = currentProject,
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                            onCopyContent = { content, title ->
                                clipboardManager.setText(AnnotatedString(content))
                                Toast.makeText(context, "$title copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                // AI Edit & Iterate Panel
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Status & Error Messages
                        AnimatedVisibility(visible = lastChange != null && editError == null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = lastChange ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (currentProject.previousHtml != null) {
                                    TextButton(
                                        onClick = { viewModel.undoEdit() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Undo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = systemNotice != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = systemNotice ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.dismissNotice() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss notice",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = editError != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = editError ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.dismissError() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // Quick Suggestion Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            val suggestions = listOf(
                                "🎨 Warm earthy color palette",
                                "🌙 Switch to dark mode",
                                "📐 Expand spacing & whitespace",
                                "💬 Add contact inquiry form",
                                "⭐ Add client testimonials",
                                "📱 Optimize mobile drawer"
                            )
                            items(suggestions) { suggestion ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            viewModel.onEditInstructionChange(suggestion.substringAfter(" "))
                                        }
                                ) {
                                    Text(
                                        text = suggestion,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Input Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = editInstruction,
                                onValueChange = { viewModel.onEditInstructionChange(it) },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text("Ask AI to modify design, add features...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isEditing,
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                            Spacer(Modifier.width(10.dp))
                            Button(
                                onClick = { viewModel.applyEdit() },
                                enabled = editInstruction.isNotBlank() && !isEditing,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(52.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                if (isEditing) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Apply Edit",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditorTopBar(
    project: Project,
    viewMode: ViewMode,
    viewportMode: ViewportMode,
    canUndo: Boolean,
    onNavigateBack: () -> Unit,
    onRenameClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyCodeClick: () -> Unit,
    onViewportModeChange: (ViewportMode) -> Unit,
    onViewModeChange: (ViewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Back Button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }

        // Project Name with edit trigger
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onRenameClick)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.Edit,
                contentDescription = "Rename project",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(24.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        // Undo action
        if (canUndo) {
            IconButton(
                onClick = onUndoClick,
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo last edit",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Refresh preview action
        IconButton(
            onClick = onRefreshClick,
            modifier = Modifier
                .size(38.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Refresh Preview",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        // Share action
        IconButton(
            onClick = onShareClick,
            modifier = Modifier
                .size(38.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = "Share Code",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        // Copy bundle code action
        IconButton(
            onClick = onCopyCodeClick,
            modifier = Modifier
                .size(38.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy Full HTML Bundle",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(24.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        // Viewport switcher (Desktop / Mobile) - only visible in Preview mode
        if (viewMode == ViewMode.PREVIEW) {
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onViewportModeChange(ViewportMode.DESKTOP) },
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (viewportMode == ViewportMode.DESKTOP) MaterialTheme.colorScheme.surface else Color.Transparent,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Computer,
                        contentDescription = "Desktop view",
                        tint = if (viewportMode == ViewportMode.DESKTOP) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { onViewportModeChange(ViewportMode.MOBILE) },
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (viewportMode == ViewportMode.MOBILE) MaterialTheme.colorScheme.surface else Color.Transparent,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Mobile view",
                        tint = if (viewportMode == ViewportMode.MOBILE) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // View Mode Switcher (Preview / Code)
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                .padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ViewModeButton(
                text = "Preview",
                icon = Icons.Default.PlayArrow,
                selected = viewMode == ViewMode.PREVIEW,
                onClick = { onViewModeChange(ViewMode.PREVIEW) }
            )
            ViewModeButton(
                text = "Code",
                icon = Icons.Default.Code,
                selected = viewMode == ViewMode.CODE,
                onClick = { onViewModeChange(ViewMode.CODE) }
            )
        }
    }
}

@Composable
fun ViewModeButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = contentColor, style = MaterialTheme.typography.labelMedium)
    }
}

enum class ViewMode { PREVIEW, CODE }
enum class CodeTab { HTML, CSS, JS, BUNDLE }
enum class ViewportMode { DESKTOP, MOBILE }

@Composable
fun WebsitePreview(
    project: Project,
    viewportMode: ViewportMode,
    reloadTrigger: Int
) {
    val htmlContent = project.htmlContent
    val cssContent = project.cssContent
    val jsContent = project.jsContent

    // Track last injected payload to avoid flickering upon external recompositions
    var lastLoadedPayload by remember { mutableStateOf("") }
    var lastTrigger by remember { mutableIntStateOf(-1) }

    val contentPayload = "$htmlContent|$cssContent|$jsContent|$reloadTrigger"

    if (viewportMode == ViewportMode.MOBILE) {
        // High-polish smartphone frame simulation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight()
                    .shadow(16.dp, RoundedCornerShape(36.dp))
                    .background(Color(0xFF10131A), RoundedCornerShape(36.dp))
                    .border(2.5.dp, Color(0xFF2C3246), RoundedCornerShape(36.dp))
                    .padding(top = 10.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Notch / Camera Pill
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(14.dp)
                        .background(Color(0xFF07080C), RoundedCornerShape(50))
                )
                Spacer(Modifier.height(8.dp))

                // The Screen Surface
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                ) {
                    PreviewWebView(
                        project = project,
                        reloadTrigger = reloadTrigger,
                        contentPayload = contentPayload,
                        lastLoadedPayload = lastLoadedPayload,
                        onPayloadLoaded = { lastLoadedPayload = it }
                    )
                }

                Spacer(Modifier.height(8.dp))
                // Bottom Gesture Bar
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(4.dp)
                        .background(Color(0xFF5A627A), RoundedCornerShape(50))
                )
            }
        }
    } else {
        // Desktop Full Bleed Preview
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
        ) {
            PreviewWebView(
                project = project,
                reloadTrigger = reloadTrigger,
                contentPayload = contentPayload,
                lastLoadedPayload = lastLoadedPayload,
                onPayloadLoaded = { lastLoadedPayload = it }
            )
        }
    }
}

@Composable
fun PreviewWebView(
    project: Project,
    reloadTrigger: Int,
    contentPayload: String,
    lastLoadedPayload: String,
    onPayloadLoaded: (String) -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.allowFileAccessFromFileURLs = false
                settings.allowUniversalAccessFromFileURLs = false
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val url = request?.url?.toString() ?: ""
                        if (url.endsWith("styles.css")) {
                            return WebResourceResponse(
                                "text/css",
                                "UTF-8",
                                project.cssContent.byteInputStream()
                            )
                        }
                        if (url.endsWith("script.js")) {
                            return WebResourceResponse(
                                "application/javascript",
                                "UTF-8",
                                project.jsContent.byteInputStream()
                            )
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }
            }
        },
        update = { webView ->
            // CRITICAL FIX: Only reload if the actual content or reload trigger changed!
            // This prevents recompositions caused by typing in text inputs from reloading the WebView.
            if (contentPayload != lastLoadedPayload) {
                val finalHtml = buildSelfContainedHtml(project)
                webView.loadDataWithBaseURL("https://veyra.local", finalHtml, "text/html", "UTF-8", null)
                onPayloadLoaded(contentPayload)
            }
        }
    )
}

fun buildSelfContainedHtml(project: Project): String {
    val rawHtml = project.htmlContent
    val css = project.cssContent
    val js = project.jsContent

    val viewportMeta = """<meta name="viewport" content="width=device-width, initial-scale=1.0">"""
    val utf8Meta = """<meta charset="UTF-8">"""
    val styleBlock = "\n$utf8Meta\n$viewportMeta\n<style>\n$css\n</style>\n"
    val scriptBlock = "\n<script>\n$js\n</script>\n"

    // 1. Insert CSS / head tags safely using index slicing (immune to $ and \ in CSS)
    val headIndex = rawHtml.indexOf("<head>", ignoreCase = true)
    val htmlIndex = rawHtml.indexOf("<html>", ignoreCase = true)

    val htmlWithHead = when {
        headIndex != -1 -> {
            val insertPoint = headIndex + "<head>".length
            rawHtml.substring(0, insertPoint) + styleBlock + rawHtml.substring(insertPoint)
        }
        htmlIndex != -1 -> {
            val insertPoint = htmlIndex + "<html>".length
            rawHtml.substring(0, insertPoint) + "<head>$styleBlock</head>" + rawHtml.substring(insertPoint)
        }
        else -> {
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>$styleBlock</head>\n<body>\n$rawHtml"
        }
    }

    // 2. Insert JS script block before </body> or </html> safely using lastIndexOf (immune to $ and \ in JS)
    val bodyCloseIndex = htmlWithHead.lastIndexOf("</body>", ignoreCase = true)
    val htmlCloseIndex = htmlWithHead.lastIndexOf("</html>", ignoreCase = true)

    return when {
        bodyCloseIndex != -1 -> {
            htmlWithHead.substring(0, bodyCloseIndex) + scriptBlock + htmlWithHead.substring(bodyCloseIndex)
        }
        htmlCloseIndex != -1 -> {
            htmlWithHead.substring(0, htmlCloseIndex) + scriptBlock + htmlWithHead.substring(htmlCloseIndex)
        }
        else -> {
            "$htmlWithHead$scriptBlock\n</body>\n</html>"
        }
    }
}

@Composable
fun CodeViewer(
    project: Project,
    selectedTab: CodeTab,
    onTabSelected: (CodeTab) -> Unit,
    onCopyContent: (String, String) -> Unit
) {
    val (codeText, tabTitle) = when (selectedTab) {
        CodeTab.HTML -> Pair(project.htmlContent, "index.html")
        CodeTab.CSS -> Pair(project.cssContent, "styles.css")
        CodeTab.JS -> Pair(project.jsContent, "script.js")
        CodeTab.BUNDLE -> Pair(buildSelfContainedHtml(project), "bundle.html")
    }

    val lines = remember(codeText) { codeText.lines() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab row & Header with copy button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                edgePadding = 4.dp,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == CodeTab.HTML,
                    onClick = { onTabSelected(CodeTab.HTML) },
                    text = { Text("index.html", style = MaterialTheme.typography.labelMedium) }
                )
                Tab(
                    selected = selectedTab == CodeTab.CSS,
                    onClick = { onTabSelected(CodeTab.CSS) },
                    text = { Text("styles.css", style = MaterialTheme.typography.labelMedium) }
                )
                Tab(
                    selected = selectedTab == CodeTab.JS,
                    onClick = { onTabSelected(CodeTab.JS) },
                    text = { Text("script.js", style = MaterialTheme.typography.labelMedium) }
                )
                Tab(
                    selected = selectedTab == CodeTab.BUNDLE,
                    onClick = { onTabSelected(CodeTab.BUNDLE) },
                    text = { Text("Full Bundle", style = MaterialTheme.typography.labelMedium) }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${lines.size} lines",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { onCopyContent(codeText, tabTitle) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Code Area with line numbers gutter
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090A0E))
                .padding(12.dp)
        ) {
            items(lines.size) { index ->
                val lineNumber = index + 1
                val lineContent = lines[index]

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$lineNumber",
                        modifier = Modifier
                            .width(36.dp)
                            .padding(end = 12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFF4A5568),
                            textAlign = TextAlign.End
                        )
                    )
                    Text(
                        text = lineContent.ifEmpty { " " },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun RenameProjectDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Project", style = MaterialTheme.typography.titleMedium) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Project Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
                enabled = text.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun shareProject(context: Context, project: Project) {
    val fullHtml = buildSelfContainedHtml(project)
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, fullHtml)
        putExtra(Intent.EXTRA_SUBJECT, "${project.name} - HTML Website")
        type = "text/html"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Website Code")
    context.startActivity(shareIntent)
}
