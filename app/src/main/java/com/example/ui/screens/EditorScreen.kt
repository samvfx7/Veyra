package com.example.ui.screens

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

    var viewMode by remember { mutableStateOf(ViewMode.PREVIEW) }
    var selectedTab by remember { mutableStateOf(CodeTab.HTML) }
    var viewportMode by remember { mutableStateOf(ViewportMode.DESKTOP) }

    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Spacer(modifier = Modifier.height(100.dp)) // Space for floating nav bar

                // Main Content Area (Preview or Code)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (viewMode == ViewMode.PREVIEW) {
                        WebsitePreview(project!!, viewportMode)
                    } else {
                        CodeViewer(project!!, selectedTab) { selectedTab = it }
                    }
                }

                // AI Editor Panel
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (lastChange != null) {
                            Text(
                                text = "Last Edit: $lastChange",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        if (editError != null) {
                            Text(
                                text = "Error: $editError",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = editInstruction,
                                onValueChange = { viewModel.onEditInstructionChange(it) },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("e.g. Make hero luxurious...") },
                                shape = RoundedCornerShape(4.dp),
                                enabled = !isEditing,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )
                            Spacer(Modifier.width(16.dp))
                            IconButton(
                                onClick = { viewModel.applyEdit() },
                                enabled = editInstruction.isNotBlank() && !isEditing,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                    .size(56.dp)
                            ) {
                                if (isEditing) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Apply Edit",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating Pill Nav Bar
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp, start = 16.dp, end = 16.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = project!!.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.widthIn(max = 120.dp).padding(horizontal = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                Spacer(modifier = Modifier.width(8.dp))
                
                if (viewMode == ViewMode.PREVIEW) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewportMode = ViewportMode.DESKTOP }) {
                            Icon(Icons.Default.Computer, contentDescription = "Desktop", tint = if (viewportMode == ViewportMode.DESKTOP) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewportMode = ViewportMode.MOBILE }) {
                            Icon(Icons.Default.Phone, contentDescription = "Mobile", tint = if (viewportMode == ViewportMode.MOBILE) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Row(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(2.dp)
                ) {
                    ViewModeButton(
                        text = "Preview",
                        icon = Icons.Default.PlayArrow,
                        selected = viewMode == ViewMode.PREVIEW,
                        onClick = { viewMode = ViewMode.PREVIEW }
                    )
                    ViewModeButton(
                        text = "Code",
                        icon = Icons.Default.Code,
                        selected = viewMode == ViewMode.CODE,
                        onClick = { viewMode = ViewMode.CODE }
                    )
                }
            }
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = contentColor, style = MaterialTheme.typography.labelLarge)
    }
}

enum class ViewMode { PREVIEW, CODE }
enum class CodeTab { HTML, CSS, JS }
enum class ViewportMode { DESKTOP, MOBILE }

@Composable
fun WebsitePreview(project: Project, viewportMode: ViewportMode) {
    val htmlContent = project.htmlContent
    val cssContent = project.cssContent
    val jsContent = project.jsContent
    
    val modifier = if (viewportMode == ViewportMode.MOBILE) {
        Modifier
            .fillMaxWidth(0.9f)
            .widthIn(max = 400.dp)
            .fillMaxHeight()
            .padding(vertical = 16.dp)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .animateContentSize()
    } else {
        Modifier
            .fillMaxSize()
            .animateContentSize()
    }
    
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                // Secure the webview
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.allowFileAccessFromFileURLs = false
                settings.allowUniversalAccessFromFileURLs = false
                settings.domStorageEnabled = false
                
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
                                cssContent.byteInputStream()
                            )
                        }
                        if (url.endsWith("script.js")) {
                            return WebResourceResponse(
                                "application/javascript",
                                "UTF-8",
                                jsContent.byteInputStream()
                            )
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }
            }
        },
        update = { webView ->
            // Injecting CSS/JS directly into head and body to be absolutely sure it applies
            // Some generated HTML might not have `<link rel="stylesheet" href="styles.css">`
            val injectedHtml = buildString {
                if (htmlContent.contains("<head>")) {
                    append(htmlContent.replace("<head>", "<head><style>$cssContent</style>"))
                } else if (htmlContent.contains("<html>")) {
                    append(htmlContent.replace("<html>", "<html><head><style>$cssContent</style></head>"))
                } else {
                    append("<html><head><style>$cssContent</style></head><body>$htmlContent")
                }
            }
            
            val finalHtml = if (injectedHtml.contains("</body>")) {
                injectedHtml.replace("</body>", "<script>$jsContent</script></body>")
            } else {
                "$injectedHtml<script>$jsContent</script></body></html>"
            }
            
            webView.loadDataWithBaseURL("https://veyra.local", finalHtml, "text/html", "UTF-8", null)
        }
    )
}

@Composable
fun CodeViewer(project: Project, selectedTab: CodeTab, onTabSelected: (CodeTab) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Tab(selected = selectedTab == CodeTab.HTML, onClick = { onTabSelected(CodeTab.HTML) }, text = { Text("index.html") })
            Tab(selected = selectedTab == CodeTab.CSS, onClick = { onTabSelected(CodeTab.CSS) }, text = { Text("styles.css") })
            Tab(selected = selectedTab == CodeTab.JS, onClick = { onTabSelected(CodeTab.JS) }, text = { Text("script.js") })
        }
        
        val codeText = when (selectedTab) {
            CodeTab.HTML -> project.htmlContent
            CodeTab.CSS -> project.cssContent
            CodeTab.JS -> project.jsContent
        }
        
        // Basic Code display
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = codeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}
