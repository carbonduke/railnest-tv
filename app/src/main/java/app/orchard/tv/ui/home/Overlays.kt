package app.orchard.tv.ui.home

import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.orchard.tv.R
import app.orchard.tv.apps.AppEntry
import app.orchard.tv.apps.CustomIcons
import app.orchard.tv.apps.SourceDevice
import app.orchard.tv.apps.SourceLooks
import app.orchard.tv.ui.components.Pressable
import app.orchard.tv.ui.components.consumeTaps
import app.orchard.tv.ui.components.dismissOnTap
import app.orchard.tv.ui.components.isSelect
import app.orchard.tv.ui.components.liquidGlass
import app.orchard.tv.ui.components.smoothBackground
import app.orchard.tv.ui.components.smoothBorder
import app.orchard.tv.ui.components.smoothCut
import app.orchard.tv.ui.theme.Glyph
import app.orchard.tv.ui.theme.GlyphIcon
import app.orchard.tv.ui.theme.Motion
import app.orchard.tv.ui.theme.Palette
import app.orchard.tv.ui.theme.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Keeps the last non-null value, so an overlay still has its content while it animates out. */
@Composable
fun <T : Any> rememberLatest(value: T?): T? {
    val holder = remember { arrayOfNulls<Any>(1) }
    if (value != null) holder[0] = value
    @Suppress("UNCHECKED_CAST")
    return holder[0] as T?
}

/** An action in the app context menu. */
@Composable
fun MenuRow(
    glyph: Glyph,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    destructive: Boolean = false,
    trailing: String? = null,
) {
    var selectStartedHere by remember { mutableStateOf(false) }
    FigmaTheme {
        Button(
            onClick = onClick,
            modifier = modifier.fillMaxWidth()
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                .onFocusChanged { if (!it.isFocused) selectStartedHere = false }
                .onPreviewKeyEvent { event ->
                    // Opening the menu moves focus while the remote's OK key is still held.
                    if (!event.isSelect()) return@onPreviewKeyEvent false
                    when (event.type) {
                        KeyEventType.KeyDown -> {
                            if (event.nativeKeyEvent.repeatCount > 0) true
                            else {
                                selectStartedHere = true
                                false
                            }
                        }
                        KeyEventType.KeyUp -> {
                            val consume = !selectStartedHere
                            selectStartedHere = false
                            consume
                        }
                        else -> false
                    }
                },
        ) {
            GlyphIcon(glyph, LocalContentColor.current, 20.dp)
            Spacer(Modifier.width(10.dp))
            Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                color = if (destructive) MaterialTheme.colorScheme.error else LocalContentColor.current)
            trailing?.let { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

private enum class MenuPage { Main, Rename, Device }

private val MenuWidth = 250.dp

/**
 * Long-press menu, opening out of the tile it belongs to. Choosing a folder or a folder's name
 * opens a list in the same menu; Back returns from it.
 */
@Composable
fun AppMenu(
    app: AppEntry?,
    anchor: Rect?,
    /** The menu belongs to an app inside a folder. */
    inFolder: Boolean,
    onMove: (String) -> Unit,
    onHide: (String) -> Unit,
    onInfo: (String) -> Unit,
    onUninstall: (String) -> Unit,
    onUnpin: (String) -> Unit,
    onResetIcon: (String) -> Unit,
    /** What is plugged into a pinned input, by its input id; null puts the input's own tile back. */
    onSourceDevice: (String, SourceDevice?) -> Unit,
    onOutOfFolder: (String) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onUngroup: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val visible = app != null
    val shownApp = rememberLatest(app) ?: return
    val shownAnchor = rememberLatest(anchor) ?: Rect.Zero
    val first = remember { FocusRequester() }
    var origin by remember { mutableStateOf(TransformOrigin.Center) }
    val scroll = rememberScrollState()
    var page by remember(shownApp.key, visible) { mutableStateOf(MenuPage.Main) }
    val folder = shownApp.folder
    // A look at the disk (and a hash, for an input): once per app, not on each recomposition of the
    // menu as it grows in.
    val customIcon = remember(shownApp.key, CustomIcons.version) { CustomIcons.has(shownApp.key) }

    BackHandler(enabled = visible) { if (page != MenuPage.Main) page = MenuPage.Main else onDismiss() }
    LaunchedEffect(visible, page) {
        if (visible) {
            scroll.scrollTo(0)
            withFrameNanos { }
            runCatching { first.requestFocus() }
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (visible) .55f else 0f))
            .then(if (visible) Modifier.dismissOnTap(onDismiss) else Modifier).padding(24.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(160, easing = Motion.EaseOut)) + scaleIn(tween(220, easing = Motion.EaseOut), initialScale = 0.92f, transformOrigin = origin),
            exit = fadeOut(tween(120, easing = Motion.EaseOut)) + scaleOut(tween(120, easing = Motion.EaseOut), targetScale = 0.96f, transformOrigin = origin),
        ) {
            Column(
                Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .consumeTaps()
                    .background(Color(0xFF15191E))
                    .verticalScroll(scroll)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val title = when (page) {
                    MenuPage.Main -> shownApp.label
                    MenuPage.Rename -> stringResource(R.string.menu_rename_folder)
                    MenuPage.Device -> stringResource(R.string.menu_source_device)
                }
                Text(
                    title, color = Color(0xFFE3E8EF), style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 6.dp),
                )
                when (page) {
                    MenuPage.Main -> when {
                        folder != null -> {
                            MenuRow(Glyph.Move, stringResource(R.string.menu_move), { onMove(shownApp.key) }, focusRequester = first)
                            MenuRow(Glyph.Pencil, stringResource(R.string.menu_rename_folder), { page = MenuPage.Rename }, trailing = "›")
                            MenuRow(Glyph.Folder, stringResource(R.string.menu_ungroup), { onUngroup(folder.id) })
                        }
                        else -> {
                            // In a folder too: its apps keep an order of their own (issue #1344).
                            MenuRow(Glyph.Move, stringResource(R.string.menu_move), { onMove(shownApp.key) }, focusRequester = first)
                            if (inFolder) MenuRow(Glyph.Folder, stringResource(R.string.menu_out_of_folder), { onOutOfFolder(shownApp.key) })
                            if (shownApp.source != null) {
                                // A pinned TV input: no app info or uninstall, and "remove" unpins it.
                                MenuRow(Glyph.Input, stringResource(R.string.menu_source_device), { page = MenuPage.Device }, trailing = "›")
                                if (customIcon) {
                                    MenuRow(Glyph.Photo, stringResource(R.string.menu_reset_icon), { onResetIcon(shownApp.key) })
                                }
                                MenuRow(Glyph.EyeSlash, stringResource(R.string.menu_unpin_source), { onUnpin(shownApp.key) })
                            } else {
                                MenuRow(Glyph.EyeSlash, stringResource(R.string.menu_hide), { onHide(shownApp.key) })
                                // A picture chosen on the phone page; changing it is done there.
                                if (customIcon) {
                                    MenuRow(Glyph.Photo, stringResource(R.string.menu_reset_icon), { onResetIcon(shownApp.key) })
                                }
                                if (shownApp.shortcut == null) MenuRow(Glyph.Info, stringResource(R.string.menu_info), { onInfo(shownApp.key) })
                            }
                            if (shownApp.removable) {
                                MenuRow(Glyph.Trash, stringResource(R.string.menu_uninstall), { onUninstall(shownApp.key) }, destructive = true)
                            }
                        }
                    }
                    MenuPage.Device -> {
                        // Picking one names the tile after the device, as folders take a name from
                        // a list: typing on a remote is slow. A name of one's own is typed on the
                        // phone page. The tile's current choice comes first, ticked.
                        val source = shownApp.source
                        val current = source?.let { SourceLooks.of(it.id)?.device }
                        val choices = listOfNotNull(current) + listOf(null) + SourceDevice.entries.filter { it != current }
                        choices.forEachIndexed { i, device ->
                            MenuRow(
                                if (device == current) Glyph.Check else Glyph.Input,
                                device?.let { stringResource(it.label) } ?: (source?.label ?: ""),
                                { source?.let { onSourceDevice(it.id, device) } },
                                focusRequester = if (i == 0) first else null,
                                trailing = if (device == null) stringResource(R.string.menu_source_plain) else null,
                            )
                        }
                    }
                    MenuPage.Rename -> {
                        val names = LocalContext.current.resources.getStringArray(R.array.folder_names).toList()
                        val current = folder?.name
                        // The folder's own name first, ticked, so OK on it changes nothing.
                        (listOfNotNull(current) + names.filter { it != current }).forEachIndexed { i, name ->
                            MenuRow(
                                if (name == current) Glyph.Check else Glyph.Pencil, name,
                                { folder?.let { onRenameFolder(it.id, name) } },
                                focusRequester = if (i == 0) first else null,
                            )
                        }
                    }
                }
            }
        }
    }
}

private val MiniCorner = 10.dp

/** A small, flat copy of a tile for lists. */
@Composable
fun MiniTile(app: AppEntry, focused: Boolean, modifier: Modifier = Modifier) {
    // As AppTile: the artwork cut to the continuous outline once, in the background, and until then
    // drawn as it is inside the layer's plain rounded clip, which holds the outline.
    var px by remember { mutableStateOf(IntSize.Zero) }
    val corner = with(LocalDensity.current) { MiniCorner.toPx() }
    val face by produceState<ImageBitmap?>(null, app.art.back, px) {
        if (px.width > 0 && px.height > 0) {
            value = withContext(Dispatchers.Default) { smoothCut(app.art.back.asAndroidBitmap(), px.width, px.height, corner).asImageBitmap() }
        }
    }
    Canvas(
        modifier
            .onSizeChanged { px = it }
            .clip(RoundedCornerShape(MiniCorner))
            .smoothBorder(if (focused) 2.dp else 1.dp, if (focused) Palette.Focus else Palette.Hairline, MiniCorner),
    ) {
        drawImage(face ?: app.art.back, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = FilterQuality.Medium)
        app.art.fore?.let {
            val at = app.art.foreAt
            drawImage(
                it,
                dstOffset = IntOffset((at.left * size.width).roundToInt(), (at.top * size.height).roundToInt()),
                dstSize = IntSize((at.width * size.width).roundToInt(), (at.height * size.height).roundToInt()),
                filterQuality = FilterQuality.Medium,
            )
        }
    }
}

/** Hidden apps, to put back on Home. Reached from Control Center. */
@Composable
fun HiddenAppsSheet(
    visible: Boolean,
    apps: List<AppEntry>,
    onUnhide: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val first = remember { FocusRequester() }
    BackHandler(onBack = onDismiss)
    LaunchedEffect(apps.size) { first.requestFocus() }
    FigmaTheme {
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState()).padding(horizontal = 56.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.hidden_title), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(if (apps.isEmpty()) R.string.hidden_empty else R.string.hidden_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onDismiss, modifier = Modifier.focusRequester(first)) { Text(stringResource(R.string.menu_back)) }
            apps.forEach { app ->
                Button(onClick = { onUnhide(app.key) }) { Text(app.label) }
            }
        }
    }
}

/**
 * A line at the top of the screen that confirms a choice and goes away by itself (HomeViewModel
 * .notify): glass, like the menus, falling in from the top edge.
 */
@Composable
fun NoticeBanner(text: String?, modifier: Modifier = Modifier, glyph: Glyph = Glyph.Power) {
    val shown = rememberLatest(text) ?: return
    AnimatedVisibility(
        visible = text != null,
        modifier = modifier,
        enter = fadeIn(tween(200, easing = Motion.EaseOut)) + slideInVertically(tween(320, easing = Motion.EaseOut)) { -it / 2 },
        exit = fadeOut(tween(200, easing = Motion.EaseOut)) + slideOutVertically(tween(240, easing = Motion.EaseOut)) { -it / 3 },
    ) {
        Row(
            Modifier
                .widthIn(max = 640.dp)
                .background(Color(0xFF29313A), RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GlyphIcon(glyph, Palette.Label, 16.dp)
            Text(shown, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE3E8EF), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
