package app.orchard.tv.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.*
import app.orchard.tv.R
import app.orchard.tv.settings.AerialSettings
import app.orchard.tv.ui.theme.Glyph
import app.orchard.tv.ui.theme.GlyphIcon
import app.orchard.tv.wallpaper.PhoneServer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun FigmaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            surface = Color(0xFF15191E), onSurface = Color(0xFFE3E8EF),
            surfaceVariant = Color(0xFF29313A), onSurfaceVariant = Color(0xFFBEC7D3),
            primary = Color(0xFFB6D6F5), onPrimary = Color(0xFF10283C),
            inverseSurface = Color(0xFFE3E8EF), inverseOnSurface = Color(0xFF20262D),
        ),
        content = content,
    )
}

@Composable
internal fun FigmaHomeStatus(now: LocalDateTime, modifier: Modifier = Modifier) = FigmaTheme {
    val statusStyle = MaterialTheme.typography.titleMedium.copy(
        color = Color.White.copy(alpha = .66f),
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        letterSpacing = 0.sp,
    )
    Row(
        modifier.height(52.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(now.format(DateTimeFormatter.ofPattern("HH:mm")), style = statusStyle)
        Spacer(Modifier.width(1.dp).height(12.dp).background(statusStyle.color))
        Text(stringResource(R.string.app_name), style = statusStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun FigmaWallpaperQr(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val close = remember { FocusRequester() }
    var url by remember { mutableStateOf<String?>(null) }
    var qr by remember { mutableStateOf<ImageBitmap?>(null) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        close.requestFocus()
        val result = withContext(Dispatchers.IO) {
            PhoneServer.start(context)?.let { it to PhoneServer.qr(it).asImageBitmap() }
        }
        url = result?.first
        qr = result?.second
        loaded = true
    }
    BackHandler(onBack = onDismiss)
    FigmaTheme {
        Row(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(horizontal = 56.dp, vertical = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.wp_phone_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    stringResource(if (loaded && url == null) R.string.wp_phone_offline else R.string.figma_wallpaper_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                url?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Button(onClick = onDismiss, modifier = Modifier.focusRequester(close)) { Text(stringResource(R.string.figma_done)) }
            }
            Box(Modifier.size(192.dp), contentAlignment = Alignment.Center) {
                qr?.let {
                    Image(it, stringResource(R.string.wp_phone_title), Modifier.size(192.dp).background(Color.White).padding(12.dp), filterQuality = FilterQuality.None)
                }
            }
        }
    }
}

@Composable
internal fun FigmaAppCard(
    app: app.orchard.tv.apps.AppEntry,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    focusRequester: FocusRequester,
    enabled: Boolean,
    moving: Boolean,
    onFocused: () -> Unit,
    onClick: (Rect?) -> Unit,
    onLongPress: (Rect?) -> Unit,
    modifier: Modifier = Modifier,
    rowEnd: Boolean = false,
    left: FocusRequester? = null,
    showName: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    var bounds by remember { mutableStateOf<Rect?>(null) }
    val lite = AerialSettings.lite
    val accent = remember(app.art.tint) {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(app.art.tint.toArgb(), hsl)
        if (hsl[1] < .10f) Color(0xFFB6D6F5)
        else {
            hsl[1] = (hsl[1] * 1.4f).coerceIn(.55f, .87f)
            hsl[2] = .58f
            Color(ColorUtils.HSLToColor(hsl))
        }
    }
    FigmaTheme {
        Box(modifier.size(width, height).zIndex(if (focused) 1f else 0f)) {
            Card(
                onClick = { onClick(bounds) },
                onLongClick = { onLongPress(bounds) },
                modifier = Modifier.fillMaxSize()
                    .semantics { contentDescription = app.label }
                    .focusRequester(focusRequester)
                    .focusProperties {
                        canFocus = enabled
                        if (rowEnd) right = FocusRequester.Cancel
                        left?.let { this.left = it }
                    }
                    .onFocusChanged { state ->
                        focused = state.isFocused
                        if (state.isFocused) onFocused()
                    }
                    .onGloballyPositioned { bounds = it.boundsInWindow() },
                glow = CardDefaults.glow(
                    focusedGlow = if (lite) Glow.None else Glow(accent, 28.dp),
                    pressedGlow = if (lite) Glow.None else Glow(accent, 14.dp),
                ),
                border = CardDefaults.border(focusedBorder = Border.None, pressedBorder = Border.None),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    fun paint(image: ImageBitmap, at: Rect) = drawImage(
                        image,
                        dstOffset = IntOffset((at.left * size.width).roundToInt(), (at.top * size.height).roundToInt()),
                        dstSize = IntSize((at.width * size.width).roundToInt().coerceAtLeast(1), (at.height * size.height).roundToInt().coerceAtLeast(1)),
                        filterQuality = FilterQuality.Medium,
                    )
                    paint(app.art.back, Rect(0f, 0f, 1f, 1f))
                    app.art.fore?.let { paint(it, app.art.foreAt) }
                    if (moving) drawRect(Color.Black.copy(alpha = .24f))
                }
            }
            if (focused || showName) Text(
                app.label, Modifier.align(Alignment.BottomCenter).offset(y = 30.dp).fillMaxWidth(),
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                color = Color.White, style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
