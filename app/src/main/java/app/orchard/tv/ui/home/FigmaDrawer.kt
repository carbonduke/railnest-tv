package app.orchard.tv.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import app.orchard.tv.R
import app.orchard.tv.ui.theme.LocalReducedMotion
import app.orchard.tv.ui.theme.Motion

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun FigmaDrawer(
    homeRequester: FocusRequester,
    desktopRequester: FocusRequester,
    searchRequester: FocusRequester,
    contentRequester: FocusRequester?,
    onExpanded: (Boolean) -> Unit,
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onNetwork: () -> Unit,
    onDesktop: () -> Unit,
    onSettings: () -> Unit,
) = FigmaTheme {
    val state = rememberDrawerState(DrawerValue.Closed)
    val expanded = state.currentValue == DrawerValue.Open
    LaunchedEffect(expanded) { onExpanded(expanded) }
    val shade = animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = if (LocalReducedMotion.current) snap() else tween(230, easing = Motion.EaseOut),
        label = "drawer-scrim",
    )
    val scrimColor = Color(0xFF101115)
    ModalNavigationDrawer(
        modifier = Modifier.fillMaxSize().drawBehind {
            val progress = shade.value
            drawRect(
                Brush.horizontalGradient(
                    0f to scrimColor.copy(alpha = .70f + .28f * progress),
                    .23f to scrimColor.copy(alpha = .51f + .41f * progress),
                    .51f to scrimColor.copy(alpha = .25f + .44f * progress),
                    .77f to scrimColor.copy(alpha = .06f + .27f * progress),
                    1f to scrimColor.copy(alpha = 0f),
                    endX = minOf(size.width, (158.dp + 760.dp * progress).toPx()),
                ),
            )
        },
        drawerState = state,
        scrimBrush = Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
        drawerContent = { value ->
            Column(
                Modifier.fillMaxHeight().padding(horizontal = 12.dp, vertical = 28.dp),
            ) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                ) {
                    DrawerEntry(value, R.string.figma_search, R.drawable.figma_drawer_search,
                        onSearch, contentRequester, searchRequester)
                    DrawerEntry(value, R.string.google_home, R.drawable.figma_drawer_home,
                        onHome, contentRequester, homeRequester, selected = true)
                    DrawerEntry(value, R.string.figma_network_settings, R.drawable.figma_drawer_wifi,
                        onNetwork, contentRequester)
                    DrawerEntry(value, R.string.figma_desktop_settings, R.drawable.figma_drawer_tv,
                        onDesktop, contentRequester, desktopRequester)
                }
                Box(Modifier.height(80.dp), contentAlignment = Alignment.CenterStart) {
                    DrawerEntry(value, R.string.figma_system_settings, R.drawable.figma_drawer_settings,
                        onSettings, contentRequester)
                }
            }
        },
        content = {},
    )
}


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavigationDrawerScope.DrawerEntry(
    value: DrawerValue,
    label: Int,
    icon: Int,
    onClick: () -> Unit,
    contentRequester: FocusRequester?,
    requester: FocusRequester? = null,
    selected: Boolean = false,
) {
    val title = stringResource(label)
    // The TV Material item supplies the 56/256 dp widths and 48 dp one-line height.
    NavigationDrawerItem(
        selected = selected,
        onClick = onClick,
        leadingContent = {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
        },
        modifier = Modifier
            .then(requester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .semantics { if (value == DrawerValue.Closed) contentDescription = title }
            .focusProperties {
                left = FocusRequester.Cancel
                right = contentRequester ?: FocusRequester.Cancel
            },
    ) {
        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
