package com.example.ui.profile

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.AppFontFamily
import com.example.domain.model.GridStyle
import com.example.domain.model.ThemeMode
import com.example.domain.model.UserProfileData
import com.example.ui.components.FlowBuilderBrandMarkF
import com.example.ui.theme.FlowEmerald500
import com.example.ui.theme.FlowTeal600
import com.example.ui.theme.toComposeFontFamily
import com.example.viewmodel.HomeUiState

private const val SUPPORT_WHATSAPP_NUMBER = "923429748731"
private const val SUPPORT_WHATSAPP_MESSAGE =
    "Assalam-o-Alaikum Muhammad Sufyan! Main FlowBuilder app ki taraf se rabta kar raha hoon. Mujhe app ke baray mein madad chahiye."

fun openWhatsAppSupport(context: Context) {
    val encodedMessage = Uri.encode(SUPPORT_WHATSAPP_MESSAGE)
    val uri = Uri.parse("https://wa.me/$SUPPORT_WHATSAPP_NUMBER?text=$encodedMessage")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "WhatsApp not found. Contact: 03429748731",
            Toast.LENGTH_LONG,
        ).show()
    }
}

@Composable
fun NeedMoreHelpWhatsAppCard(
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(FlowTeal600, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Forum,
                    contentDescription = "Chat on WhatsApp",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Need more help?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "Chat on WhatsApp • 03429748731",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Created by Muhammad Sufyan",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = FlowTeal600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun UserAvatarCircle(
    profile: UserProfileData,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    val initials = remember(profile.displayName) {
        profile.displayName
            .trim()
            .split(Regex("\\s+"))
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifBlank { "FB" }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(FlowEmerald500, FlowTeal600))
            )
            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.36f).sp,
        )
        if (!profile.photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = profile.photoUrl,
                contentDescription = "User profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        }
    }
}

/**
 * Hamburger Navigation Drawer matching Mockup 3 in FlowBuilder emerald/teal theme,
 * with Dark/Light Mode toggle and menu shortcuts.
 */
@Composable
fun FlowHamburgerDrawerContent(
    uiState: HomeUiState,
    onToggleDarkLight: () -> Unit,
    onNavigateHome: () -> Unit,
    onCreateFlow: () -> Unit,
    onCreateWithAi: () -> Unit,
    onOpenAiChat: () -> Unit,
    onNavigateMyFlows: () -> Unit,
    onNavigateTemplates: () -> Unit,
    onImportJson: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFaq: () -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val isDarkActive = uiState.settings.themeMode == ThemeMode.DARK

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(304.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Centered Brand Header matching Mockup 3
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FlowBuilderBrandMarkF(size = 54.dp)
                Text(
                    text = "FlowBuilder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Plan • Build • Organize • Grow",
                    style = MaterialTheme.typography.labelMedium,
                    color = FlowTeal600,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Quick Dark / Light Toggle inside Hamburger Drawer
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onToggleDarkLight)
                    .testTag("drawer_dark_mode_toggle"),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = if (isDarkActive) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = if (isDarkActive) "Dark Mode" else "Light Mode",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Switch(
                        checked = isDarkActive,
                        onCheckedChange = { onToggleDarkLight() },
                    )
                }
            }

            Spacer(Modifier.height(2.dp))
            Text(
                text = "Menu Shortcuts",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp),
            )

            DrawerShortcutRow(Icons.Filled.Home, "Home Dashboard", onClick = onNavigateHome)
            DrawerShortcutRow(Icons.Filled.AddCircleOutline, "New Flowchart", onClick = onCreateFlow)
            DrawerShortcutRow(Icons.Filled.AutoAwesome, "Make Flow by AI", onClick = onCreateWithAi)
            DrawerShortcutRow(Icons.Filled.SmartToy, "AI Chat Assistant", onClick = onOpenAiChat)
            DrawerShortcutRow(Icons.Filled.Folder, "My Flowcharts (${uiState.projects.size})", onClick = onNavigateMyFlows)
            DrawerShortcutRow(Icons.Filled.AutoAwesomeMosaic, "Templates Library", onClick = onNavigateTemplates)
            DrawerShortcutRow(Icons.Filled.UploadFile, "Import JSON", onClick = onImportJson)

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )

            DrawerShortcutRow(Icons.Filled.Person, "My Profile", onClick = onOpenProfile)
            DrawerShortcutRow(Icons.Filled.Settings, "Settings", onClick = onOpenSettings)
            DrawerShortcutRow(Icons.AutoMirrored.Filled.HelpOutline, "Help & FAQs", onClick = onOpenFaq)

            Spacer(Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onSignOut)
                    .testTag("drawer_logout_button"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Log Out",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Bottom Need More Help card opening WhatsApp (03429748731) + Created by Muhammad Sufyan
            NeedMoreHelpWhatsAppCard(
                onClick = { openWhatsAppSupport(context) },
                testTag = "drawer_need_more_help_whatsapp",
            )
        }
    }
}

@Composable
private fun DrawerShortcutRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * Profile Screen matching Mockup 4 in FlowBuilder green palette.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: HomeUiState,
    onBack: () -> Unit,
    onOpenMyFlows: () -> Unit,
    onOpenSettings: () -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onFontFamilyChange: (AppFontFamily) -> Unit,
    onSignOut: () -> Unit,
) {
    var showAppearanceSheet by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Top Row: Back + Settings Gear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("profile_back_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("profile_settings_gear"),
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }

                // Centered Avatar + Name + Tagline matching Mockup 4
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    UserAvatarCircle(profile = uiState.userProfile, size = 96.dp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = uiState.userProfile.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = uiState.userProfile.subtitle.ifBlank { "Flowchart Creator" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    if (uiState.userProfile.email.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        ) {
                            Text(
                                text = uiState.userProfile.email,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                // Grouped Profile Menu Card matching Mockup 4
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        GreenMenuRow(
                            icon = Icons.Filled.Folder,
                            title = "My Flows",
                            trailingText = "${uiState.projects.size}",
                            onClick = onOpenMyFlows,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        GreenMenuRow(
                            icon = Icons.Filled.Settings,
                            title = "Settings",
                            onClick = onOpenSettings,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        GreenToggleMenuRow(
                            icon = Icons.Filled.Notifications,
                            title = "Notifications",
                            checked = uiState.settings.notificationsEnabled,
                            onCheckedChange = onNotificationsChange,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        GreenMenuRow(
                            icon = Icons.Filled.DarkMode,
                            title = "Appearance",
                            trailingText = uiState.settings.themeMode.label,
                            onClick = { showAppearanceSheet = true },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        GreenMenuRow(
                            icon = Icons.AutoMirrored.Filled.HelpOutline,
                            title = "Help & Support",
                            onClick = { showHelpDialog = true },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        GreenMenuRow(
                            icon = Icons.Filled.Info,
                            title = "About App",
                            onClick = { showAboutDialog = true },
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Soft Emerald Full-Width Log Out Button matching Mockup 4
                Button(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    contentPadding = PaddingValues(vertical = 15.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_logout_button"),
                ) {
                    Text(
                        text = "Log Out",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showAppearanceSheet) {
        ModalBottomSheet(onDismissRequest = { showAppearanceSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Appearance & Typography", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Theme Mode", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = uiState.settings.themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            label = { Text(mode.label) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Text("App Font Family", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppFontFamily.entries.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            pair.forEach { font ->
                                FilterChip(
                                    selected = uiState.settings.appFontFamily == font,
                                    onClick = { onFontFamilyChange(font) },
                                    label = {
                                        Text(
                                            text = font.label,
                                            fontFamily = font.toComposeFontFamily(),
                                            maxLines = 1,
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showHelpDialog) {
        FlowFaqDialog(onDismiss = { showHelpDialog = false })
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = { FlowBuilderBrandMarkF(size = 44.dp) },
            title = { Text("FlowBuilder") },
            text = {
                Text(
                    "Version 1.0.0\nCreated by Muhammad Sufyan\nPlan • Build • Organize • Grow\n\nDesign algorithms with 24+ flowchart symbols, AI flow builder & assistant, live Pseudocode & Flow DSL, realtime cloud sync, and PNG/PDF/HTML/JSON export.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) { Text("Close") }
            },
        )
    }
}

/**
 * Redesigned Settings Screen matching Mockups 1 & 2 in FlowBuilder's signature green theme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: HomeUiState,
    showBackArrow: Boolean,
    onBack: () -> Unit,
    onUpdateProfile: (displayName: String, subtitle: String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onFontFamilyChange: (AppFontFamily) -> Unit,
    onAutoSaveChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onSaveAiApiKey: (String) -> Unit,
    onTestAiConnection: (String) -> Unit,
    onRemoveAiApiKey: () -> Unit,
    onGridStyleChange: (GridStyle) -> Unit,
    onSnapToGridChange: (Boolean) -> Unit,
    onAlignmentGuidesChange: (Boolean) -> Unit,
    onValidationBadgesChange: (Boolean) -> Unit,
    onImportJsonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showChangeNameDialog by remember { mutableStateOf(false) }
    var showChangeTaglineDialog by remember { mutableStateOf(false) }
    var showFontPickerSheet by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    var apiKeyDraft by remember { mutableStateOf("") }
    var showApiKeyText by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header Row matching Mockup 1
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showBackArrow) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        // --- Account Settings ---
        SettingsSectionLabel("Account Settings")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCardRow(
                icon = Icons.Filled.Person,
                title = "Change Name",
                trailingText = uiState.userProfile.displayName,
                onClick = { showChangeNameDialog = true },
                testTag = "settings_change_name",
            )
            SettingsCardRow(
                icon = Icons.Filled.Badge,
                title = "Change Tagline",
                trailingText = uiState.userProfile.subtitle.ifBlank { "Creator" },
                onClick = { showChangeTaglineDialog = true },
                testTag = "settings_change_tagline",
            )
            SettingsCardRow(
                icon = Icons.Filled.Email,
                title = "Google Account",
                trailingText = uiState.userProfile.email.ifBlank { "Connected" },
                onClick = {},
                showChevron = false,
            )
        }

        // --- App Settings ---
        SettingsSectionLabel("App Settings")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCardToggleRow(
                icon = Icons.Filled.Notifications,
                title = "Notifications",
                checked = uiState.settings.notificationsEnabled,
                onCheckedChange = onNotificationsChange,
                testTag = "settings_notifications_toggle",
            )
            SettingsCardToggleRow(
                icon = Icons.Filled.DarkMode,
                title = "Dark Mode",
                checked = uiState.settings.themeMode == ThemeMode.DARK,
                onCheckedChange = { dark ->
                    onThemeModeChange(if (dark) ThemeMode.DARK else ThemeMode.LIGHT)
                },
                testTag = "settings_dark_mode_toggle",
            )
            SettingsCardToggleRow(
                icon = Icons.Filled.CloudSync,
                title = "Auto-Save Realtime",
                subtitle = if (uiState.settings.autoSaveEnabled) "Syncs edits automatically" else "Manual save button in editor",
                checked = uiState.settings.autoSaveEnabled,
                onCheckedChange = onAutoSaveChange,
                testTag = "settings_autosave_toggle",
            )
            SettingsCardRow(
                icon = Icons.Filled.FontDownload,
                title = "Font Family",
                trailingText = uiState.settings.appFontFamily.label,
                onClick = { showFontPickerSheet = true },
                testTag = "settings_font_family",
            )
        }

        // --- AI Configuration (Universal Groq / OpenRouter / OpenAI Key) ---
        val aiConfig = uiState.aiConfig
        val (statusBg, statusFg, statusIcon) = when (aiConfig.status) {
            AiConnectionStatus.CONNECTED -> Triple(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
                MaterialTheme.colorScheme.onPrimaryContainer,
                Icons.Filled.CheckCircle,
            )
            AiConnectionStatus.INVALID_API_KEY -> Triple(
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                MaterialTheme.colorScheme.onErrorContainer,
                Icons.Filled.ErrorOutline,
            )
            AiConnectionStatus.CONNECTION_FAILED -> Triple(
                Color(0xFFFFF4E5),
                Color(0xFF9A5B13),
                Icons.Filled.Warning,
            )
            AiConnectionStatus.NOT_CONFIGURED -> Triple(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant,
                Icons.Filled.Key,
            )
        }

        SettingsSectionLabel("AI Configuration")
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        GreenCircularBadge(Icons.Filled.AutoAwesome)
                        Column {
                            Text(
                                text = "Universal AI Key",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = when {
                                    aiConfig.status == AiConnectionStatus.CONNECTED && aiConfig.provider != null ->
                                        "${aiConfig.provider.displayName} detected automatically"
                                    aiConfig.hasApiKey ->
                                        "Encrypted key saved securely on device"
                                    else ->
                                        "Supports Groq & OpenRouter automatically"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusBg,
                        modifier = Modifier.testTag("settings_ai_connection_status"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusFg,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = aiConfig.status.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusFg,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = apiKeyDraft,
                    onValueChange = { apiKeyDraft = it },
                    singleLine = true,
                    enabled = !uiState.isConfiguringAi,
                    placeholder = {
                        Text(
                            text = if (aiConfig.hasApiKey) {
                                "Key saved securely — paste to replace"
                            } else {
                                "Paste your AI API key here"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    visualTransformation = if (showApiKeyText) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { showApiKeyText = !showApiKeyText },
                            modifier = Modifier.testTag("settings_toggle_api_key_visibility"),
                        ) {
                            Icon(
                                imageVector = if (showApiKeyText) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showApiKeyText) "Hide API key" else "Show API key",
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_ai_api_key_input"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            val keyToSave = apiKeyDraft
                            onSaveAiApiKey(keyToSave)
                            apiKeyDraft = ""
                            showApiKeyText = false
                        },
                        enabled = !uiState.isConfiguringAi && apiKeyDraft.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_save_ai_key_button"),
                    ) {
                        if (uiState.isConfiguringAi) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("Save API Key", maxLines = 1, softWrap = false, fontSize = 13.sp)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val keyToTest = apiKeyDraft
                            onTestAiConnection(keyToTest)
                            if (keyToTest.isNotBlank()) {
                                apiKeyDraft = ""
                                showApiKeyText = false
                            }
                        },
                        enabled = !uiState.isConfiguringAi && (apiKeyDraft.isNotBlank() || aiConfig.hasApiKey),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_test_ai_connection_button"),
                    ) {
                        Text("Test Connection", maxLines = 1, softWrap = false, fontSize = 13.sp)
                    }
                }

                if (aiConfig.hasApiKey) {
                    TextButton(
                        onClick = {
                            apiKeyDraft = ""
                            showApiKeyText = false
                            onRemoveAiApiKey()
                        },
                        enabled = !uiState.isConfiguringAi,
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("settings_remove_ai_key_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Remove API Key",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                if (!aiConfig.statusDetail.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusBg.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = aiConfig.statusDetail,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusFg,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }

        // --- Canvas & Editor Preferences ---
        SettingsSectionLabel("Canvas & Studio")
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Default Grid Style",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GridStyle.entries.forEach { style ->
                        FilterChip(
                            selected = uiState.settings.defaultGridStyle == style,
                            onClick = { onGridStyleChange(style) },
                            label = {
                                Text(
                                    style.label.replace(" Grid", ""),
                                    maxLines = 1,
                                    softWrap = false,
                                    fontSize = 12.sp,
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                CompactToggleRow("Snap Shapes to Grid", uiState.settings.defaultSnapToGrid, onSnapToGridChange)
                CompactToggleRow("Smart Alignment Guides", uiState.settings.showAlignmentGuides, onAlignmentGuidesChange)
                CompactToggleRow("Validation Issue Badges", uiState.settings.showLiveValidationBadges, onValidationBadgesChange)
            }
        }

        // --- Support & Legal (matching Mockup 2) ---
        SettingsSectionLabel("Support & Legal")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCardRow(
                icon = Icons.Filled.UploadFile,
                title = "Import Flowchart JSON",
                onClick = onImportJsonClick,
            )
            SettingsCardRow(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = "FAQs",
                onClick = { showFaqDialog = true },
            )
            SettingsCardRow(
                icon = Icons.Filled.Policy,
                title = "Privacy Policy",
                onClick = { showPrivacyDialog = true },
            )
            SettingsCardRow(
                icon = Icons.Filled.Gavel,
                title = "Terms of Service",
                onClick = { showTermsDialog = true },
            )
        }

        // Bottom Tinted Emerald Help Card -> WhatsApp 03429748731 + Created by Muhammad Sufyan
        NeedMoreHelpWhatsAppCard(
            onClick = { openWhatsAppSupport(context) },
            testTag = "settings_need_more_help_whatsapp",
        )

        Spacer(Modifier.height(24.dp))
    }

    // --- Dialogs & Bottom Sheets ---

    if (showChangeNameDialog) {
        var nameInput by remember(uiState.userProfile.displayName) { mutableStateOf(uiState.userProfile.displayName) }
        AlertDialog(
            onDismissRequest = { showChangeNameDialog = false },
            title = { Text("Change Display Name") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateProfile(nameInput, uiState.userProfile.subtitle)
                    showChangeNameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showChangeNameDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showChangeTaglineDialog) {
        var tagInput by remember(uiState.userProfile.subtitle) { mutableStateOf(uiState.userProfile.subtitle) }
        AlertDialog(
            onDismissRequest = { showChangeTaglineDialog = false },
            title = { Text("Change Tagline") },
            text = {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    singleLine = true,
                    label = { Text("Bio or Role") },
                    placeholder = { Text("e.g., CS Student • Algorithm Designer") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateProfile(uiState.userProfile.displayName, tagInput)
                    showChangeTaglineDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showChangeTaglineDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showFontPickerSheet) {
        ModalBottomSheet(onDismissRequest = { showFontPickerSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Choose App Font Family", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Applies across menus, cards, and new canvas shapes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                AppFontFamily.entries.forEach { font ->
                    val selected = uiState.settings.appFontFamily == font
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onFontFamilyChange(font)
                                showFontPickerSheet = false
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = font.label,
                                fontFamily = font.toComposeFontFamily(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            )
                            if (selected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showFaqDialog) {
        FlowFaqDialog(onDismiss = { showFaqDialog = false })
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            icon = { Icon(Icons.Filled.Policy, contentDescription = null) },
            title = { Text("Privacy Policy") },
            text = {
                Text(
                    "Your flowcharts and profile are securely stored in your personal Firebase Firestore account and cached locally on your device. Your optional OpenAI API key is stored strictly on-device in encrypted Android DataStore and is only sent directly to OpenAI when you generate a flowchart.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = { Button(onClick = { showPrivacyDialog = false }) { Text("Got it") } },
        )
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            icon = { Icon(Icons.Filled.Description, contentDescription = null) },
            title = { Text("Terms of Service") },
            text = {
                Text(
                    "You retain full ownership of all flowcharts, pseudocode, HTML, PDF, PNG, and JSON exports created with FlowBuilder. When using Make by AI with your own OpenAI API key, usage is subject to your OpenAI account plan.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = { Button(onClick = { showTermsDialog = false }) { Text("Close") } },
        )
    }
}

@Composable
fun FlowFaqDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = FlowTeal600) },
        title = { Text("FlowBuilder Quick Guide & FAQs") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FaqItem(
                    q = "How do I connect two shapes?",
                    a = "Tap a shape and drag from any of its 4 anchor ports to another shape, or switch to the Connect tool and tap two shapes.",
                )
                FaqItem(
                    q = "How do I add text outside shapes?",
                    a = "Tap the Text (T) tool in the floating editor bar or pick Text Box from the Shapes bottom sheet.",
                )
                FaqItem(
                    q = "How does Make by AI work?",
                    a = "Add your OpenAI API key in Settings, tap New Flow, switch on 'Make by AI', and describe any algorithm to generate a complete arranged flowchart.",
                )
                FaqItem(
                    q = "Where can I see Pseudocode?",
                    a = "Tap the Code (<>) icon in the top bar of the Editor to view live Pseudocode, Flow DSL, and JSON.",
                )
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun FaqItem(q: String, a: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(q, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(a, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@Composable
private fun GreenCircularBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun SettingsCardRow(
    icon: ImageVector,
    title: String,
    trailingText: String? = null,
    showChevron: Boolean = true,
    testTag: String? = null,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = showChevron, onClick = onClick)
            .let { if (testTag != null) it.testTag(testTag) else it },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GreenCircularBadge(icon)
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            if (!trailingText.isNullOrBlank()) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 132.dp),
                )
            }
            if (showChevron) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsCardToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .let { if (testTag != null) it.testTag(testTag) else it },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GreenCircularBadge(icon)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = FlowTeal600,
                ),
            )
        }
    }
}

@Composable
private fun GreenMenuRow(
    icon: ImageVector,
    title: String,
    trailingText: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        GreenCircularBadge(icon)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (!trailingText.isNullOrBlank()) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GreenToggleMenuRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        GreenCircularBadge(icon)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = FlowTeal600,
            ),
        )
    }
}

@Composable
private fun CompactToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
