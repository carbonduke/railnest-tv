package app.orchard.tv.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import app.orchard.tv.R
import app.orchard.tv.apps.AppEntry

@Composable
internal fun FigmaSearch(visible: Boolean, apps: List<AppEntry>, onDismiss: () -> Unit, onLaunch: (AppEntry) -> Unit) {
    if (!visible) return
    var query by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val search = remember { FocusRequester() }
    val first = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val matches = remember(apps, query) {
        val term = query.trim()
        apps.filter { it.folder == null && it.source == null && it.label.contains(term, ignoreCase = true) }
    }
    BackHandler {
        keyboard?.hide()
        onDismiss()
    }
    LaunchedEffect(Unit) { search.requestFocus() }
    FigmaTheme {
        val colors = MaterialTheme.colorScheme
        val type = MaterialTheme.typography
        val shape = RoundedCornerShape(12.dp)
        Column(
            Modifier.fillMaxSize().background(colors.surface).padding(horizontal = 58.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.figma_search), color = colors.onSurface, style = type.headlineSmall)
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().focusRequester(search)
                    .focusProperties { if (matches.isNotEmpty()) down = first }
                    .onFocusChanged { focused = it.hasFocus }
                    .border(2.dp, if (focused) colors.primary else colors.onSurfaceVariant.copy(alpha = .65f), shape)
                    .background(colors.surfaceVariant, shape).padding(16.dp),
                singleLine = true,
                textStyle = type.bodyLarge.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboard?.hide()
                    if (matches.isNotEmpty()) runCatching { first.requestFocus() }
                }),
                decorationBox = { field ->
                    Box {
                        if (query.isEmpty()) Text(stringResource(R.string.figma_search_apps), color = colors.onSurfaceVariant)
                        field()
                    }
                },
            )
            if (matches.isEmpty()) Text(stringResource(R.string.figma_search_empty), color = colors.onSurfaceVariant)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(matches, key = { it.key }) { app ->
                    val top = app.key == matches.firstOrNull()?.key
                    ListItem(
                        selected = false,
                        onClick = { keyboard?.hide(); onLaunch(app) },
                        modifier = Modifier.fillMaxWidth().then(if (top) Modifier.focusRequester(first)
                            .focusProperties { up = search } else Modifier),
                        headlineContent = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        }
    }
}
