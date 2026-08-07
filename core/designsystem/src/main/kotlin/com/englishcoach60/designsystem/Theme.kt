package com.englishcoach60.designsystem

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val Forest = Color(0xFF1F5C4A)
private val ForestDeep = Color(0xFF123F34)
private val Sage = Color(0xFFDCEBE3)
private val Ink = Color(0xFF17201D)
private val Paper = Color(0xFFF7F6F1)
private val Warm = Color(0xFFF2A66F)

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Sage,
    onPrimaryContainer = Color(0xFF153A30),
    secondary = Color(0xFF9A5A30),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE1CF),
    onSecondaryContainer = Color(0xFF51270D),
    tertiary = Warm,
    background = Paper,
    onBackground = Ink,
    surface = Color(0xFFFFFEFA),
    onSurface = Ink,
    surfaceVariant = Color(0xFFE9EEE9),
    onSurfaceVariant = Color(0xFF5D6863),
    outline = Color(0xFFCBD4CE),
    outlineVariant = Color(0xFFE1E6E2),
    error = Color(0xFFA13F42),
    errorContainer = Color(0xFFFFDAD9),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9ED3BB),
    onPrimary = Color(0xFF073829),
    primaryContainer = Color(0xFF254D40),
    onPrimaryContainer = Color(0xFFD3F3E3),
    secondary = Color(0xFFFFB786),
    onSecondary = Color(0xFF572D12),
    secondaryContainer = Color(0xFF704020),
    onSecondaryContainer = Color(0xFFFFDBCA),
    tertiary = Color(0xFFFFB786),
    background = Color(0xFF111714),
    onBackground = Color(0xFFE7EDE9),
    surface = Color(0xFF19211D),
    onSurface = Color(0xFFE7EDE9),
    surfaceVariant = Color(0xFF28332E),
    onSurfaceVariant = Color(0xFFB9C5BF),
    outline = Color(0xFF65726B),
    outlineVariant = Color(0xFF35413B),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF702C2D),
)

val CoachShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

private val CoachTypography = Typography(
    displaySmall = TextStyle(fontSize = 38.sp, lineHeight = 43.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.2).sp),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 26.sp, letterSpacing = 0.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = .1.sp),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = .5.sp),
)

@Composable
fun EnglishCoachTheme(themeMode: String = "SYSTEM", content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = CoachTypography,
        shapes = CoachShapes,
        content = content,
    )
}

object CoachSpacing {
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val hero = 40.dp
}

@Composable
fun CoachCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun PrimaryCoachButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) .975f else 1f,
        animationSpec = spring(dampingRatio = .7f, stiffness = 650f),
        label = "primaryButtonScale",
    )
    Button(
        onClick = onClick,
        modifier = modifier.scale(scale).heightIn(min = 58.dp),
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = 24.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SectionLabel(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .7f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(100),
    ) {
        Text(
            text.uppercase(),
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumTopAppBar(
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
fun EditorialDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, color = MaterialTheme.colorScheme.outlineVariant)
}

val PremiumForest: Color get() = ForestDeep
