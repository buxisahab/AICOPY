package com.buxi.music.ui.component

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buxi.music.R
import com.buxi.music.models.MediaMetadata
import com.buxi.music.ui.theme.ThemeAccent
import com.buxi.music.ui.theme.ThemeCard
import com.buxi.music.ui.theme.ThemeDark
import com.buxi.music.ui.theme.ThemeTextPrimary
import com.buxi.music.ui.theme.ThemeTextSecondary
import com.buxi.music.ui.theme.ThemeTextTertiary

/**
 * Data model for a single comment in Resso style
 */
data class RessoComment(
    val id: String,
    val author: String,
    val avatarColor: Color,
    val text: String,
    val timestamp: String,
    var likeCount: Int,
    var isLiked: Boolean = false,
    val isPinned: Boolean = false
)

/**
 * Resso-inspired interactive Song Comments Modal Bottom Sheet.
 * In Resso, community comments were a flagship social music listening feature.
 * Users can view listener feelings, like comments, and write their own comments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RessoCommentsSheet(
    mediaMetadata: MediaMetadata?,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    if (!isVisible) return

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val songTitle = mediaMetadata?.title ?: "Current Track"
    val songId = mediaMetadata?.id ?: "unknown"

    // SharedPreferences key to persist comments per song
    val prefs = remember { context.getSharedPreferences("resso_comments_$songId", Context.MODE_PRIVATE) }

    // Seed mock community comments for rich Resso social experience
    val comments = remember(songId) {
        mutableStateListOf(
            RessoComment(
                id = "c1",
                author = "MusicLover_99",
                avatarColor = Color(0xFFE50914),
                text = "This chorus gives me chills every single time! 🔥🎧",
                timestamp = "2h ago",
                likeCount = 342,
                isPinned = true
            ),
            RessoComment(
                id = "c2",
                author = "AestheticVibes",
                avatarColor = Color(0xFFFF2D55),
                text = "The bass drop and sync lyrics in Resso mode are unmatched.",
                timestamp = "5h ago",
                likeCount = 189
            ),
            RessoComment(
                id = "c3",
                author = "LateNightThinker",
                avatarColor = Color(0xFF8B5CF6),
                text = "Listening to this at 2 AM hits completely differently.",
                timestamp = "1d ago",
                likeCount = 95
            ),
            RessoComment(
                id = "c4",
                author = "SoundWave_Sonic",
                avatarColor = Color(0xFF00D4FF),
                text = "Production quality on this track is masterclass level! 🚀",
                timestamp = "2d ago",
                likeCount = 67
            )
        )
    }

    // Load any user-saved comments
    LaunchedEffect(songId) {
        val savedCount = prefs.getInt("saved_count", 0)
        for (i in 0 until savedCount) {
            val author = prefs.getString("comment_${i}_author", "You") ?: "You"
            val text = prefs.getString("comment_${i}_text", "") ?: ""
            val time = prefs.getString("comment_${i}_time", "Just now") ?: "Just now"
            val likes = prefs.getInt("comment_${i}_likes", 0)
            if (text.isNotBlank()) {
                comments.add(
                    0,
                    RessoComment(
                        id = "user_$i",
                        author = author,
                        avatarColor = ThemeAccent,
                        text = text,
                        timestamp = time,
                        likeCount = likes
                    )
                )
            }
        }
    }

    var commentInput by remember { mutableStateOf("") }
    val quickEmojis = remember { listOf("❤️", "🔥", "✨", "😭", "👏", "🎧", "🌙", "💯") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ThemeDark,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                .imePadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Comments (${comments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeTextPrimary
                    )
                    Text(
                        text = songTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeTextTertiary,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "Close",
                        tint = ThemeTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f),
                thickness = 1.dp
            )

            // Comments List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(comments, key = { it.id }) { comment ->
                    RessoCommentItem(
                        comment = comment,
                        onLikeClicked = {
                            comment.isLiked = !comment.isLiked
                            if (comment.isLiked) {
                                comment.likeCount += 1
                            } else {
                                comment.likeCount = maxOf(0, comment.likeCount - 1)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f),
                thickness = 1.dp
            )

            // Quick emoji bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                quickEmojis.forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 20.sp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                commentInput += emoji
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            // Input field & Send button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(ThemeCard)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (commentInput.isEmpty()) {
                        Text(
                            text = "Share your thoughts on this song...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThemeTextTertiary
                        )
                    }
                    BasicTextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        textStyle = TextStyle(
                            color = ThemeTextPrimary,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(ThemeAccent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (commentInput.isNotBlank()) {
                                val newComment = RessoComment(
                                    id = "user_${System.currentTimeMillis()}",
                                    author = "You",
                                    avatarColor = ThemeAccent,
                                    text = commentInput.trim(),
                                    timestamp = "Just now",
                                    likeCount = 0
                                )
                                comments.add(0, newComment)
                                // Persist
                                val nextIndex = prefs.getInt("saved_count", 0)
                                prefs.edit()
                                    .putString("comment_${nextIndex}_author", newComment.author)
                                    .putString("comment_${nextIndex}_text", newComment.text)
                                    .putString("comment_${nextIndex}_time", newComment.timestamp)
                                    .putInt("saved_count", nextIndex + 1)
                                    .apply()
                                commentInput = ""
                                focusManager.clearFocus()
                            }
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (commentInput.isNotBlank()) ThemeAccent else ThemeCard
                        )
                        .clickable(enabled = commentInput.isNotBlank()) {
                            val newComment = RessoComment(
                                id = "user_${System.currentTimeMillis()}",
                                author = "You",
                                avatarColor = ThemeAccent,
                                text = commentInput.trim(),
                                timestamp = "Just now",
                                likeCount = 0
                            )
                            comments.add(0, newComment)
                            val nextIndex = prefs.getInt("saved_count", 0)
                            prefs.edit()
                                .putString("comment_${nextIndex}_author", newComment.author)
                                .putString("comment_${nextIndex}_text", newComment.text)
                                .putString("comment_${nextIndex}_time", newComment.timestamp)
                                .putInt("saved_count", nextIndex + 1)
                                .apply()
                            commentInput = ""
                            focusManager.clearFocus()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward),
                        contentDescription = "Send",
                        tint = if (commentInput.isNotBlank()) Color.White else ThemeTextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RessoCommentItem(
    comment: RessoComment,
    onLikeClicked: () -> Unit
) {
    var isLiked by remember { mutableStateOf(comment.isLiked) }
    var likes by remember { mutableIntStateOf(comment.likeCount) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar circle with first letter of username
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(comment.avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = comment.author.firstOrNull()?.uppercase() ?: "U",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.author,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ThemeTextSecondary
                )
                if (comment.isPinned) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ThemeAccent.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Pinned",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeAccent
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = comment.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = ThemeTextTertiary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = ThemeTextPrimary,
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Heart like button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(CircleShape)
                .clickable {
                    isLiked = !isLiked
                    if (isLiked) likes += 1 else likes = maxOf(0, likes - 1)
                    onLikeClicked()
                }
                .padding(4.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (isLiked) R.drawable.favorite else R.drawable.favorite_border
                ),
                contentDescription = "Like comment",
                tint = if (isLiked) ThemeAccent else ThemeTextTertiary,
                modifier = Modifier.size(16.dp)
            )
            if (likes > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = likes.toString(),
                    fontSize = 11.sp,
                    color = if (isLiked) ThemeAccent else ThemeTextTertiary
                )
            }
        }
    }
}
