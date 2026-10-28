package com.musifly.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.musifly.android.R
import com.musifly.android.ui.component.IconButton

private val BeatsThemeColor = Color(0xFF000000)
private val BeatsAccentColor = Color(0xFF1DB954)
private val BeatsPrimaryText = Color(0xFFFFFFFF)
private val BeatsSecondaryText = Color(0xFFB3B3B3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy", color = BeatsPrimaryText) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Back",
                            tint = BeatsPrimaryText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BeatsThemeColor
                )
            )
        },
        containerColor = BeatsThemeColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "PRIVACY POLICY FOR MUSSIFLY",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = BeatsPrimaryText
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Last Updated: February 18, 2026",
                fontSize = 14.sp,
                color = BeatsSecondaryText
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            PrivacySection(
                title = "1. INFORMATION WE COLLECT",
                content = "To provide you with the best music streaming experience, we collect certain information:\n\n" +
                        "• Personal Information: When you create an account, we may collect your name, email address, and profile photo.\n" +
                        "• User-Generated Content: We collect information you provide through social features, such as your profile bio and followers/following lists.\n" +
                        "• App Activity: We collect data about how you interact with the app (e.g., songs played, search history) to improve performance.\n" +
                        "• Device or other IDs: We collect basic information like device model, OS version, and unique identifiers required for app security and licensing.\n" +
                        "• Local Cached Media: Cached songs are stored on your device's local storage for offline playback and are managed exclusively by you."
            )
            
            PrivacySection(
                title = "2. INFORMATION WE DO NOT COLLECT",
                content = "Maintaining your privacy is our priority. We explicitly DO NOT access or collect:\n\n" +
                        "• Your contacts or call logs.\n" +
                        "• Your SMS messages.\n" +
                        "• Your precise GPS location tracking.\n" +
                        "• Any unnecessary personal data not required for the app's functionality."
            )
            
            PrivacySection(
                title = "3. THIRD-PARTY SERVICES",
                content = "Mussifly utilizes reputable third-party services to provide core functionalities:\n\n" +
                        "• Authentication & Database: We use services like Supabase and Firebase for user authentication, database management, and secure cloud storage.\n" +
                        "• Media Sources: Mussifly streams music from publicly available or third-party sources, such as YouTube. Your interactions with these sources may be subject to their own privacy policies.\n" +
                        "• Analytics: We may use Google Analytics or Firebase Crashlytics to monitor app stability and performance."
            )
            
            PrivacySection(
                title = "4. DATA USAGE",
                content = "We use the collected data solely for the following purposes:\n\n" +
                        "• To provide and maintain our music streaming services.\n" +
                        "• To manage your account and authenticate your identity.\n" +
                        "• To enable social features like following other users.\n" +
                        "• To monitor app usage and improve overall performance and user experience.\n" +
                        "• To provide customer support and respond to your inquiries."
            )

            PrivacySection(
                title = "5. CONTENT DISCLAIMER",
                content = "IMPORTANT NOTICE: Mussifly does not host or store any copyrighted music content on its own servers. All content is streamed from publicly available or third-party sources. We respect intellectual property rights and provide an interface to access publicly available media."
            )

            PrivacySection(
                title = "6. DATA STORAGE & SECURITY",
                content = "• Security: All personal data is transmitted over secure, encrypted connections (HTTPS) and stored on secure cloud servers.\n" +
                        "• No Third-Party Sharing: We do not sell or share your personal information with third parties for marketing purposes.\n" +
                        "• Local Storage: Cached media files are stored only on your device. Clearing the app cache will remove this data."
            )

            PrivacySection(
                title = "7. CHILDREN'S PRIVACY",
                content = "Mussifly is not intended for children under the age of 13. We do not knowingly collect personal information from children under 13. If we become aware that we have collected info from a child under 13, we will take steps to delete that data immediately."
            )

            PrivacySection(
                title = "8. PERMISSIONS EXPLAINED",
                content = "To function correctly, Mussifly requires the following permissions:\n\n" +
                        "• INTERNET: Required to stream music and sync your profile data.\n" +
                        "• STORAGE / MEDIA ACCESS: Required to cache songs for offline use and to upload/save profile images.\n" +
                        "• NOTIFICATIONS: Required to send you updates regarding your account or social interactions (if enabled)."
            )

            PrivacySection(
                title = "9. YOUR RIGHTS",
                content = "You have full control over your data within Mussifly:\n\n" +
                        "• You can update your profile information at any time within the app settings.\n" +
                        "• You can clear the app's local cache at any time to free up space.\n" +
                        "• ACCOUNT DELETION: You can permanently delete your account and all associated personal data from our servers through the account settings menu."
            )

            PrivacySection(
                title = "10. POLICY UPDATES",
                content = "We may update this Privacy Policy from time to time. We will notify you of any changes by posting the new policy on this page and updating the \"Last Updated\" date. We recommend checking the policy periodically inside the app or on the Play Store."
            )

            PrivacySection(
                title = "11. CONTACT US",
                content = "If you have any questions or concerns about this Privacy Policy, please contact us."
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun PrivacySection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = BeatsAccentColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            fontSize = 15.sp,
            color = BeatsSecondaryText,
            lineHeight = 22.sp
        )
    }
}
