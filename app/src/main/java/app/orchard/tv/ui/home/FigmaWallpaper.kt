package app.orchard.tv.ui.home

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.*
import app.orchard.tv.R
import app.orchard.tv.settings.AerialSettings
import app.orchard.tv.settings.Effects
import app.orchard.tv.wallpaper.WallpaperKind
import app.orchard.tv.wallpaper.Wallpapers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class DesktopPage { Settings, Wallpapers, Phone }
private data class SettingDetail(val label: Int, val hint: Int?, val preview: Boolean, val value: String?)
private data class SettingOptions(
    val title: Int,
    val hint: Int?,
    val values: List<String>,
    val selected: Int,
    val choose: (Int) -> Unit,
)

@Composable
internal fun FigmaWallpaper(
    visible: Boolean,
    onDismiss: () -> Unit,
    onHidden: () -> Unit,
    hiddenCount: Int,
    onHomeLock: (Boolean) -> Unit,
    onSystemScreensaver: () -> Unit,
    onStartSystemScreensaver: () -> Unit,
) {
    if (!visible) return
    val context = LocalContext.current
    val first = remember { FocusRequester() }
    val phone = remember { FocusRequester() }
    val settingsList = rememberLazyListState()
    val focusedRow = remember { mutableStateOf(SettingDetail(R.string.wp_wallpaper, null, true, null)) }
    val returnFocus = remember { mutableStateOf<FocusRequester?>(null) }
    val choiceFocus = remember { mutableMapOf<Int, FocusRequester>() }
    var options by remember { mutableStateOf<SettingOptions?>(null) }
    val openOptions: (SettingOptions) -> Unit = { options = it }
    var page by remember { mutableStateOf(DesktopPage.Settings) }
    var fromPhone by remember { mutableStateOf(false) }
    val home = remember { Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME) }
    fun defaultHomeStatus(): Int {
        val resolved = context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolved?.activityInfo?.packageName == context.packageName) return R.string.figma_default_home_active
        if (Build.VERSION.SDK_INT >= 29 && context.getSystemService(RoleManager::class.java)
                ?.isRoleHeld(RoleManager.ROLE_HOME) == true) {
            return R.string.figma_default_home_overridden
        }
        return R.string.figma_default_home_inactive
    }
    var homeStatus by remember(context) { mutableIntStateOf(defaultHomeStatus()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(context, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) homeStatus = defaultHomeStatus()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val defaultValue = stringResource(homeStatus)

    if (page == DesktopPage.Phone) {
        FigmaWallpaperQr(onDismiss = { fromPhone = true; page = DesktopPage.Settings })
        return
    }
    BackHandler {
        if (options != null) options = null
        else if (page == DesktopPage.Wallpapers) page = DesktopPage.Settings
        else onDismiss()
    }
    LaunchedEffect(page, options) {
        if (options != null) return@LaunchedEffect
        val target = when {
            page == DesktopPage.Settings && fromPhone -> phone
            page == DesktopPage.Settings -> returnFocus.value ?: first
            else -> first
        }
        withFrameNanos { }
        runCatching { target.requestFocus() }
        fromPhone = false
    }
    val choice = options
    if (choice != null) {
        DesktopOptionPage(choice) { options = null }
        return
    }
    FigmaTheme {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(horizontal = 58.dp, vertical = 28.dp)) {
            Text(
                stringResource(if (page == DesktopPage.Wallpapers) R.string.wp_wallpaper else R.string.figma_desktop_settings),
                modifier = Modifier.semantics { heading() },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(16.dp))
            if (page == DesktopPage.Wallpapers) {
                WallpaperGrid(first)
            } else {
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        state = settingsList,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item { DesktopSection(R.string.wp_wallpaper) }
                        item {
                            DesktopAction(R.string.wp_wallpaper, R.drawable.figma_symbol_image, Modifier.focusRequester(first),
                                detail = focusedRow, preview = true) {
                                returnFocus.value = first
                                page = DesktopPage.Wallpapers
                            }
                        }
                        item {
                            DesktopAction(R.string.wp_phone_title, R.drawable.figma_symbol_cast, Modifier.focusRequester(phone),
                                detail = focusedRow, hint = R.string.figma_wallpaper_intro) {
                                page = DesktopPage.Phone
                            }
                        }
                        if (Wallpapers.selectedId == Wallpapers.ROTATE_PHOTOS) item {
                            val minutes = AerialSettings.PHOTO_MINUTES
                            DesktopChoice(R.string.st_photo_every, R.drawable.figma_symbol_calendar_month, R.string.st_photo_every_hint,
                                minutes.map { duration ->
                                    when {
                                        duration >= AerialSettings.DAY_MINUTES -> stringResource(R.string.st_photo_every_day)
                                        duration >= 60 -> stringResource(R.string.kids_h, duration / 60)
                                        else -> stringResource(R.string.kids_m, duration)
                                    }
                                }, minutes.indexOf(AerialSettings.photoMinutes), focusedRow, returnFocus, choiceFocus, openOptions) {
                                AerialSettings.choosePhotoMinutes(minutes[it])
                                Wallpapers.rotateIfDue()
                            }
                        }
                        item { DesktopSection(R.string.st_home_layout) }
                        item {
                            DesktopAction(R.string.figma_default_home, R.drawable.figma_drawer_home,
                                value = defaultValue, detail = focusedRow, hint = R.string.figma_default_home_hint) {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(context, R.string.figma_default_home_unavailable, Toast.LENGTH_LONG).show()
                                } catch (_: SecurityException) {
                                    Toast.makeText(context, R.string.figma_default_home_unavailable, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        item {
                            val columns = AerialSettings.HOME_COLUMNS.toList()
                            DesktopChoice(R.string.st_columns, R.drawable.figma_symbol_view_column, R.string.st_columns_hint,
                                columns.map(Int::toString), columns.indexOf(AerialSettings.homeColumns),
                                focusedRow, returnFocus, choiceFocus, openOptions) { AerialSettings.chooseHomeColumns(columns[it]) }
                        }
                        item {
                            DesktopChoice(R.string.st_layout, R.drawable.figma_symbol_view_agenda, R.string.st_home_layout_hint,
                                listOf(stringResource(R.string.st_home_standard), stringResource(R.string.st_home_one_row)),
                                if (AerialSettings.oneRowHome) 1 else 0,
                                focusedRow, returnFocus, choiceFocus, openOptions) { AerialSettings.chooseOneRowHome(it == 1) }
                        }
                        if (AerialSettings.oneRowHome) item {
                            DesktopChoice(R.string.st_peek, R.drawable.figma_symbol_expand_more, R.string.st_peek_hint,
                                listOf(stringResource(R.string.st_peek_hide), stringResource(R.string.st_peek_show)),
                                if (AerialSettings.peekNextRow) 1 else 0,
                                focusedRow, returnFocus, choiceFocus, openOptions) { AerialSettings.choosePeekNextRow(it == 1) }
                        }
                        item {
                            DesktopChoice(R.string.st_names, R.drawable.figma_symbol_label, R.string.st_names_hint,
                                listOf(stringResource(R.string.st_names_focused), stringResource(R.string.st_names_all)),
                                if (AerialSettings.showAllNames) 1 else 0,
                                focusedRow, returnFocus, choiceFocus, openOptions) { AerialSettings.chooseShowAllNames(it == 1) }
                        }
                        item {
                            DesktopChoice(R.string.st_effects, R.drawable.figma_symbol_animation, R.string.st_effects_hint,
                                listOf(stringResource(R.string.st_effects_auto), stringResource(R.string.st_effects_full), stringResource(R.string.st_effects_lite)),
                                Effects.entries.indexOf(AerialSettings.effects), focusedRow, returnFocus, choiceFocus, openOptions) {
                                AerialSettings.chooseEffects(Effects.entries[it])
                            }
                        }
                        item { DesktopSection(R.string.google_your_apps) }
                        item {
                            DesktopAction(R.string.st_hidden_apps, R.drawable.figma_symbol_visibility_off,
                                value = hiddenCount.toString(), detail = focusedRow, hint = R.string.st_hidden_apps_hint,
                                onClick = onHidden)
                        }
                        item {
                            DesktopChoice(R.string.st_home_lock, R.drawable.figma_symbol_lock, R.string.st_home_lock_hint,
                                listOf(stringResource(R.string.st_off), stringResource(R.string.st_on)),
                                if (AerialSettings.homeLocked) 1 else 0,
                                focusedRow, returnFocus, choiceFocus, openOptions) { onHomeLock(it == 1) }
                        }
                        item {
                            DesktopAction(R.string.figma_start_ambient, R.drawable.figma_drawer_tv, detail = focusedRow,
                                hint = R.string.figma_start_ambient_hint, onClick = onStartSystemScreensaver)
                        }
                        item {
                            DesktopAction(R.string.figma_saver_settings, R.drawable.figma_drawer_tv, detail = focusedRow,
                                hint = R.string.figma_system_saver_hint,
                                onClick = onSystemScreensaver)
                        }
                    }
                    SettingsDetails(focusedRow.value, Modifier.weight(1f).padding(top = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun DesktopChoice(
    label: Int,
    icon: Int,
    hint: Int,
    values: List<String>,
    selected: Int,
    detail: MutableState<SettingDetail>,
    returnFocus: MutableState<FocusRequester?>,
    choiceFocus: MutableMap<Int, FocusRequester>,
    onOptions: (SettingOptions) -> Unit,
    choose: (Int) -> Unit,
) {
    val focus = choiceFocus.getOrPut(label) { FocusRequester() }
    DesktopAction(label, icon, Modifier.focusRequester(focus), values.getOrNull(selected),
        detail = detail, hint = hint) {
        returnFocus.value = focus
        onOptions(SettingOptions(label, hint, values, selected, choose))
    }
}

@Composable
private fun DesktopOptionPage(options: SettingOptions, onDone: () -> Unit) = FigmaTheme {
    val colors = MaterialTheme.colorScheme
    val focus = remember { FocusRequester() }
    LaunchedEffect(options) {
        withFrameNanos { }
        runCatching { focus.requestFocus() }
    }
    Row(
        Modifier.fillMaxSize().background(colors.surface).padding(horizontal = 58.dp, vertical = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(options.title), Modifier.semantics { heading() },
                color = colors.onSurface, style = MaterialTheme.typography.headlineMedium)
            options.hint?.let {
                Text(stringResource(it), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(options.values) { index, label ->
                val selected = index == options.selected
                val initial = index == options.selected.coerceAtLeast(0)
                ListItem(
                    selected = selected,
                    onClick = { options.choose(index); onDone() },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .then(if (initial) Modifier.focusRequester(focus) else Modifier),
                    headlineContent = { Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 2) },
                    trailingContent = if (selected) { { DesktopIcon(R.drawable.figma_symbol_check) } } else null,
                )
            }
        }
    }
}

@Composable
private fun SettingsDetails(detail: SettingDetail, modifier: Modifier) {
    if (detail.preview) {
        WallpaperPreview(modifier)
        return
    }
    val colors = MaterialTheme.colorScheme
    Column(modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(detail.label), color = colors.onSurface, style = MaterialTheme.typography.headlineSmall)
        detail.value?.let { Text(it, color = colors.primary, style = MaterialTheme.typography.titleMedium) }
        detail.hint?.let { Text(stringResource(it), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge) }
    }
}

@Composable
private fun WallpaperPreview(modifier: Modifier) {
    val id = Wallpapers.selectedId
    val image by produceState<ImageBitmap?>(null, id) {
        value = withContext(Dispatchers.IO) { Wallpapers.thumbnail(id)?.asImageBitmap() }
    }
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.wp_wallpaper), style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
        val picture = image
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(colors.surfaceVariant, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center) {
            if (picture == null) DesktopIcon(R.drawable.figma_symbol_image, 32.dp, colors.onSurfaceVariant)
            else Image(picture, contentDescription = null, Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium), contentScale = ContentScale.Crop)
        }
        val title = when (id) {
            Wallpapers.LATEST_DAILY -> stringResource(R.string.figma_daily)
            Wallpapers.LIVE -> stringResource(R.string.figma_scenic)
            Wallpapers.ROTATE_PHOTOS -> stringResource(R.string.wp_photos_rotate)
            else -> Wallpapers.items.firstOrNull { it.id == id }?.title.orEmpty()
        }
        Text(title, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DesktopIcon(
    icon: Int,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    tint: androidx.compose.ui.graphics.Color = LocalContentColor.current,
) {
    Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(size), tint = tint)
}

@Composable
private fun DesktopSection(label: Int) {
    Text(
        stringResource(label),
        modifier = Modifier.padding(top = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun DesktopAction(
    label: Int,
    icon: Int,
    modifier: Modifier = Modifier,
    value: String? = null,
    enabled: Boolean = true,
    detail: MutableState<SettingDetail>,
    hint: Int? = null,
    preview: Boolean = false,
    onClick: () -> Unit,
) {
    val description = SettingDetail(label, hint, preview, value)
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focused, description) { if (focused) detail.value = description }
    val subtitle: (@Composable () -> Unit)? = value?.let { text ->
        { Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis) }
    }
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = if (value == null) 60.dp else 76.dp)
            .onFocusChanged { focused = it.hasFocus },
        enabled = enabled,
        headlineContent = { Text(stringResource(label), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = subtitle,
        leadingContent = { DesktopIcon(icon) },
    )
}

@Composable
private fun WallpaperGrid(first: FocusRequester) {
    val candidates = Wallpapers.items.filter { it.kind != WallpaperKind.Source }
    val walls = buildList {
        if (candidates.count { it.kind == WallpaperKind.Photo } > 1) {
            add(Triple(Wallpapers.ROTATE_PHOTOS, stringResource(R.string.wp_photos_rotate), Wallpapers.ROTATE_PHOTOS))
        }
        candidates.forEach {
            add(Triple(it.id, if (it.id == Wallpapers.LIVE) stringResource(R.string.figma_scenic) else it.title, it.id))
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(walls, key = { it.first }) { (id, title, thumbnail) ->
            val picture by produceState<ImageBitmap?>(null, thumbnail) {
                value = withContext(Dispatchers.IO) { Wallpapers.thumbnail(thumbnail)?.asImageBitmap() }
            }
            val selected = Wallpapers.selectedId == id
            Card(
                onClick = { Wallpapers.select(id) },
                modifier = Modifier.fillMaxWidth().then(if (id == walls.firstOrNull()?.first) Modifier.focusRequester(first) else Modifier),
            ) {
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentAlignment = Alignment.Center) {
                    val image = picture
                    if (image != null) Image(image, contentDescription = null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else DesktopIcon(R.drawable.figma_symbol_image, 28.dp)
                }
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selected) DesktopIcon(R.drawable.figma_symbol_check, 20.dp)
                    Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
