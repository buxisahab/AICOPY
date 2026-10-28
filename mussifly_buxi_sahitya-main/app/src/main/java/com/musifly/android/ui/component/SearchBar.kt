@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.musifly.android.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarColors
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import com.musifly.android.constants.AppBarHeight
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.layerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.LayerBackdrop
import kotlin.math.max

val LocalSearchBackdrop = compositionLocalOf<LayerBackdrop?> { null }

@ExperimentalMaterial3Api
@Composable
fun TopSearch(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = SearchBarDefaults.inputFieldShape,
    colors: SearchBarColors = SearchBarDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ),
    tonalElevation: Dp = SearchBarDefaults.TonalElevation,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    focusRequester: FocusRequester = remember { FocusRequester() },
    content: @Composable ColumnScope.() -> Unit,
) {
    val animationProgress: Float by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = AnimationDurationMillis),
        label = "SearchBarAnimation",
    )

    val defaultInputFieldShape = SearchBarDefaults.inputFieldShape
    val defaultFullScreenShape = SearchBarDefaults.fullScreenShape
    val animatedShape by remember {
        derivedStateOf {
            when {
                shape == defaultInputFieldShape -> {
                    val animatedRadius = SearchBarCornerRadius * (1 - animationProgress)
                    RoundedCornerShape(CornerSize(animatedRadius))
                }

                animationProgress == 1f -> defaultFullScreenShape
                else -> shape
            }
        }
    }

    val topInset = windowInsets.asPaddingValues().calculateTopPadding()
    val startInset =
        windowInsets.asPaddingValues().calculateStartPadding(LocalLayoutDirection.current)
    val endInset = windowInsets.asPaddingValues().calculateEndPadding(LocalLayoutDirection.current)

    val topPadding = SearchBarVerticalPadding + topInset
    val animatedSurfaceTopPadding = lerp(topPadding, 0.dp, animationProgress)
    val animatedInputFieldPadding by remember {
        derivedStateOf {
            PaddingValues(
                start = startInset * animationProgress,
                top = topPadding * animationProgress,
                end = endInset * animationProgress,
                bottom = SearchBarVerticalPadding * animationProgress,
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier.offset { IntOffset(x = 0, y = 0) },
        propagateMinConstraints = true,
    ) {
        val height: Dp
        val width: Dp
        val startPadding: Dp
        val endPadding: Dp
        with(LocalDensity.current) {
            val startWidth = constraints.maxWidth.toFloat()
            val startHeight = max(constraints.minHeight, InputFieldHeight.roundToPx())
                .coerceAtMost(constraints.maxHeight)
                .toFloat()
            val endWidth = constraints.maxWidth.toFloat()
            val endHeight = constraints.maxHeight.toFloat()

            height = lerp(startHeight, endHeight, animationProgress).toDp()
            width = lerp(startWidth, endWidth, animationProgress).toDp()
            startPadding = lerp(
                (SearchBarHorizontalPadding + startInset).roundToPx().toFloat(),
                0f,
                animationProgress
            ).toDp()
            endPadding = lerp(
                (SearchBarHorizontalPadding + endInset).roundToPx().toFloat(),
                0f,
                animationProgress
            ).toDp()
        }

        val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
        val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS
        val backdrop = if (isLiquidGlass) rememberLayerBackdrop() else null
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(topInset + AppBarHeight)
                .background(color = MaterialTheme.colorScheme.surface)
        )

        Surface(
            shape = animatedShape,
            color = if (isLiquidGlass) {
                // Transparent for liquid glass to show backdrop effect
                Color.Transparent
            } else {
                colors.containerColor
            },
            contentColor = contentColorFor(colors.containerColor),
            tonalElevation = tonalElevation,
            modifier = Modifier
                .padding(
                    top = animatedSurfaceTopPadding,
                    start = startPadding,
                    end = endPadding,
                )
                .size(width = width, height = height),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background with backdrop layer for liquid glass (like AndroidLiquidGlass)
                if (isLiquidGlass && backdrop != null) {
                    val isLightTheme = !isSystemInDarkTheme()
                    val gradientColors = if (isLightTheme) {
                        listOf(
                            Color(0xFFF5F5F5),
                            Color(0xFFE8E8E8),
                            Color(0xFFDADADA)
                        )
                    } else {
                        listOf(
                            Color(0xFF0A0A0A),
                            Color(0xFF121212),
                            Color(0xFF1A1A1A),
                            Color(0xFF0F0A1E)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop)
                            .background(
                                brush = Brush.verticalGradient(gradientColors)
                            )
                    )
                }
                
                Column {
                SearchBarInputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    active = active,
                    onActiveChange = onActiveChange,
                    modifier = Modifier.padding(animatedInputFieldPadding),
                    enabled = enabled,
                    placeholder = placeholder,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    interactionSource = interactionSource,
                    focusRequester = focusRequester,
                )

                if (animationProgress > 0) {
                    Column(
                        Modifier
                            .alpha(animationProgress)
                            .weight(1f)
                    ) {
                        HorizontalDivider(color = colors.dividerColor)
                        CompositionLocalProvider(LocalSearchBackdrop provides backdrop) {
                            content()
                        }
                    }
                }
                }
            }
        }
    }

    BackHandler(enabled = active) {
        onActiveChange(false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarInputField(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    colors: TextFieldColors,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val focused = interactionSource.collectIsFocusedAsState().value
    val textColor = LocalTextStyle.current.color.takeOrElse {
        if (focused) colors.focusedTextColor else colors.unfocusedTextColor
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(InputFieldHeight),
    ) {
        if (leadingIcon != null) {
            Spacer(Modifier.width(SearchBarIconOffsetX))
            leadingIcon()
        }

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .pointerInput(active) {
                    if (active) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        if (upEvent != null) {
                            onActiveChange(true)
                        }
                    }
                }
                .semantics {
                    contentDescription = "Search"
                    if (active) {
                        stateDescription = "Suggestions available"
                    }
                }
                .onKeyEvent {
                    if (it.key == Key.Enter) {
                        onSearch(query.text)
                        return@onKeyEvent true
                    }
                    false
                },
            enabled = enabled,
            singleLine = true,
            textStyle = LocalTextStyle.current.merge(TextStyle(color = textColor)),
            cursorBrush = SolidColor(colors.cursorColor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query.text) }),
            interactionSource = interactionSource,
            decorationBox = @Composable { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = query.text,
                    innerTextField = innerTextField,
                    enabled = enabled,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    placeholder = placeholder,
                    shape = SearchBarDefaults.inputFieldShape,
                    colors = colors,
                    contentPadding = PaddingValues(),
                    container = {},
                )
            },
        )

        if (trailingIcon != null) {
            trailingIcon()
            Spacer(Modifier.width(SearchBarIconOffsetX))
        }
    }
}

// Measurement specs
val InputFieldHeight = 48.dp
private val SearchBarCornerRadius: Dp = InputFieldHeight / 2
internal val SearchBarVerticalPadding: Dp = 8.dp
internal val SearchBarHorizontalPadding: Dp = 12.dp
val SearchBarIconOffsetX: Dp = 4.dp
private const val AnimationDurationMillis: Int = 300