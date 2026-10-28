package com.buxi.music.ui.screens.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.buxi.music.ui.theme.ThemeDark
import com.buxi.music.ui.theme.ThemeAccent
import com.buxi.music.ui.theme.ThemeAccentBright
import com.buxi.music.ui.theme.ThemeTextPrimary
import com.buxi.music.ui.theme.ThemeTextTertiary
import com.buxi.music.ui.theme.ThemeCard
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.buxi.music.BuildConfig
import com.buxi.music.LocalPlayerAwareWindowInsets
import com.buxi.music.R
import com.buxi.music.ui.component.IconButton
import com.buxi.music.ui.utils.backToMain

@Composable
fun shimmerEffect(): Brush {
    val shimmerColors = listOf(
        ThemeCard.copy(alpha = 0.6f),
        ThemeCard.copy(alpha = 0.2f),
        ThemeCard.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "shimmerEffect")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "shimmerEffect"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
}

@Composable
fun UserCard(
    imageUrl: String,
    name: String,
    role: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .height(140.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemeDark,
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = imageUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    ThemeAccent.copy(alpha = 0.1f),
                                    ThemeAccent.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .border(1.dp, ThemeAccent, CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeTextPrimary.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = role,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeTextTertiary.copy(alpha = 0.6f)
                    )
                }
            }

            // Decorative element
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp)
                    .offset(x = 20.dp, y = (-20).dp)
                    .background(
                        ThemeAccent.copy(alpha = 0.1f).copy(alpha = 0.1f),
                        CircleShape
                    )
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,

    ) {
    val uriHandler = LocalUriHandler.current
    val shimmerBrush = shimmerEffect()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))

        // Image with shimmer effect
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(ThemeCard)
        ) {
            Image(
                painter = painterResource(R.drawable.opentune_monochrome),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    ThemeTextPrimary,
                    BlendMode.SrcIn
                ),
                modifier = Modifier
                    .matchParentSize()
                    .clickable { }
            )

            // Shimmer effect overlay only for the image
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(shimmerBrush)
            )
        }


        Row(
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = "Ai Music",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }


        Row(verticalAlignment = Alignment.CenterVertically) {
//

            Spacer(Modifier.width(10.dp))

            Text(
                text = BuildConfig.VERSION_NAME.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = ThemeAccentBright,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = ThemeAccentBright,
                        shape = CircleShape
                    )
                    .padding(
                        horizontal = 6.dp,
                        vertical = 2.dp
                    )
            )

            if (BuildConfig.DEBUG) {
                Spacer(Modifier.width(4.dp))

                Text(
                    text = "DEBUG",
                    style = MaterialTheme.typography.labelSmall,
                    color = ThemeAccentBright,
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = ThemeAccentBright,
                            shape = CircleShape
                        )
                        .padding(
                            horizontal = 6.dp,
                            vertical = 2.dp
                        )
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = " Dev By Arturo Cervantes 亗",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            fontWeight = FontWeight.Bold,
            color = ThemeAccentBright
        )

        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp), // Bordes muy redondeados estilo MD3
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(
                containerColor = ThemeCard, // Color de fondo MD3
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { uriHandler.openUri("https://github.com/Arturo254/AiMusic") }
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.github),
                        contentDescription = null
                    )
                }
                IconButton(
                    onClick = { uriHandler.openUri("https://www.paypal.me/AiMusic") }
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        contentDescription = null,
                        painter = painterResource(R.drawable.paypal)
                    )
                }

                IconButton(
                    onClick = { uriHandler.openUri("https://g.dev/Arturo254") }
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        contentDescription = null,
                        painter = painterResource(R.drawable.google)
                    )
                }


                IconButton(
                    onClick = { uriHandler.openUri("https://opentune.netlify.app/") }
                ) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        contentDescription = null,
                        painter = painterResource(R.drawable.resource_public)
                    )
                }
                
                // Crash logging removed for stability
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.group),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.contributors),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }


        }

        UserCards(uriHandler)
        Spacer(modifier = Modifier.width(12.dp))
        
        // Copyright text
        Spacer(modifier = Modifier.height(20.dp))
        var showCopyrightDialog by remember { mutableStateOf(false) }
        
        Text(
            text = "All rights reserved buxi.sahab",
            style = MaterialTheme.typography.bodySmall,
            color = ThemeTextTertiary.copy(alpha = 0.6f),
            modifier = Modifier
                .clickable { showCopyrightDialog = true }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        if (showCopyrightDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showCopyrightDialog = false },
                title = {
                    Text("Copyright Notice")
                },
                text = {
                    Text("All music and content is protected by copyright. All rights reserved. Unauthorized copying, redistribution, or public performance is prohibited.")
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { showCopyrightDialog = false }
                    ) {
                        Text("OK")
                    }
                }
            )
        }

    }

    TopAppBar(
        title = {
            Text(
                stringResource(R.string.about),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = ThemeDark,
            titleContentColor = ThemeTextPrimary,
        )
    )
}

@Composable
fun UserCards(uriHandler: UriHandler) {
    Column {
        UserCard(
            imageUrl = "https://avatars.githubusercontent.com/u/87346871?v=4",
            name = "亗 Arturo254",
            role = "Lead Developer ",
            onClick = { uriHandler.openUri("https://github.com/Arturo254") }
        )

        UserCard(
            imageUrl = "https://avatars.githubusercontent.com/u/138934847?v=4",
            name = "\uD81A\uDD10 Fabito02",
            role = "Traductor (PR_BR)  Icon designer",
            onClick = { uriHandler.openUri("https://github.com/Fabito02/") }
        )

        UserCard(
            imageUrl = "https://avatars.githubusercontent.com/u/205341163?v=4",
            name = "ϟ Xamax-code",
            role = "Code Refactor",
            onClick = { uriHandler.openUri("https://github.com/xamax-code") }
        )
        UserCard(
            imageUrl = "https://avatars.githubusercontent.com/u/106829560?v=4",
            name = "ϟ Derpachi",
            role = "Traductor (ru_RU)",
            onClick = { uriHandler.openUri("https://github.com/Derpachi") }
        )

        UserCard(
            imageUrl = "https://avatars.githubusercontent.com/u/147309938?v=4",
            name = "「★」 RightSideUpCak3",
            role = "Language selector",
            onClick = { uriHandler.openUri("https://github.com/RightSideUpCak3") }
        )
    }
}


@Composable
fun CardItem(
    icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
//            .shadow(8.dp, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemeCard,
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = ThemeAccent
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ThemeTextTertiary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeTextTertiary.copy(alpha = 0.7f)
                )

            }

        }
    }

}
