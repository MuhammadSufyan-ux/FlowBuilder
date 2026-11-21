package com.example.ui.auth

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.ui.components.FlowBuilderBrandMarkF
import com.example.ui.theme.FlowEmerald500
import com.example.ui.theme.FlowTeal600
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object GoogleAuthHelper {

    suspend fun attemptAutoSignIn(context: Context): Boolean {
        val webClientId = context.getString(R.string.default_web_client_id)
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .setServerClientId(webClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        return try {
            val response = credentialManager.getCredential(context = context, request = request)
            val googleCred = GoogleIdTokenCredential.createFrom(response.credential.data)
            val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
            Firebase.auth.signInWithCredential(firebaseCred).await()
            true
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Auto sign-in cancelled by user")
            false
        } catch (_: NoCredentialException) {
            false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("Auth", "Auto sign-in skipped: ${e.message}")
            false
        }
    }

    suspend fun signInWithGoogle(context: Context): Result<Unit> {
        val webClientId = context.getString(R.string.default_web_client_id)
        val credentialManager = CredentialManager.create(context)
        val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()
        return try {
            val response = credentialManager.getCredential(context = context, request = request)
            val googleCred = GoogleIdTokenCredential.createFrom(response.credential.data)
            val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
            Firebase.auth.signInWithCredential(firebaseCred).await()
            Result.success(Unit)
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google sign-in cancelled by user")
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e("Auth", "Google sign-in failed", e)
            Result.failure(IllegalStateException("Google Sign-In failed. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Auth", "Firebase sign-in failed", e)
            Result.failure(IllegalStateException(e.message ?: "Sign-in failed. Check your connection."))
        }
    }

    suspend fun signOutUser(context: Context) {
        withContext(NonCancellable) {
            runCatching {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            }
            Firebase.auth.signOut()
        }
    }
}

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
) {
    LaunchedEffect(Unit) {
        delay(1150L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF064E3B),
                        Color(0xFF0D9488),
                        Color(0xFF10B981),
                    )
                )
            ),
    ) {
        // Subtle translucent decorative wave at bottom (matching Splash mockup in signature emerald green)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val wave1 = Path().apply {
                moveTo(0f, h * 0.72f)
                cubicTo(w * 0.28f, h * 0.62f, w * 0.68f, h * 0.80f, w, h * 0.68f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(wave1, Color.White.copy(alpha = 0.08f))

            val wave2 = Path().apply {
                moveTo(0f, h * 0.82f)
                cubicTo(w * 0.35f, h * 0.90f, w * 0.65f, h * 0.74f, w, h * 0.84f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(wave2, Color.White.copy(alpha = 0.07f))
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FlowBuilderBrandMarkF(size = 86.dp, tintLight = true)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "FlowBuilder",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-0.5).sp,
            )
            Text(
                text = "Plan • Build • Organize • Grow",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD1FAE5),
                letterSpacing = 0.6.sp,
            )
        }

        CircularProgressIndicator(
            color = Color.White.copy(alpha = 0.9f),
            strokeWidth = 3.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 52.dp)
                .size(34.dp),
        )
    }
}

private data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val imageRes: Int?,
)

@Composable
fun OnboardingScreen(
    onCompleteToAuth: (startOnSignUp: Boolean) -> Unit,
    onContinueWithGoogle: () -> Unit,
    isSigningIn: Boolean,
    authError: String?,
) {
    val pages = remember {
        listOf(
            OnboardingPageData(
                title = "Plan Your Flows",
                subtitle = "Turn complex algorithms and ideas into clean, structured visual flowcharts effortlessly.",
                imageRes = R.drawable.img_onboard_plan,
            ),
            OnboardingPageData(
                title = "24+ Flowchart Shapes",
                subtitle = "Access every standard symbol — decisions, subroutines, databases, loops, and canvas text notes.",
                imageRes = R.drawable.img_onboard_shapes,
            ),
            OnboardingPageData(
                title = "Build Faster with AI",
                subtitle = "Describe any algorithm in plain words and watch FlowBuilder generate a complete diagram.",
                imageRes = R.drawable.img_onboard_ai,
            ),
            OnboardingPageData(
                title = "Export Anywhere",
                subtitle = "Export crisp HD PNG, print-ready PDF, interactive HTML pages, JSON, and Pseudocode.",
                imageRes = R.drawable.img_onboard_export,
            ),
            OnboardingPageData(
                title = "Sync & Get Started",
                subtitle = "Save your work automatically in realtime across sessions with your Google account.",
                imageRes = null,
            ),
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FlowBuilderBrandMarkF(size = 28.dp)
                    Text(
                        text = "FlowBuilder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (!isLastPage) {
                    TextButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pages.lastIndex) }
                        },
                        modifier = Modifier.testTag("onboarding_skip_button"),
                    ) {
                        Text("Skip", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 440.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Clean borderless modern illustration container (NO thick stroke borders!)
                    if (page.imageRes != null) {
                        Image(
                            painter = painterResource(id = page.imageRes),
                            contentDescription = page.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth(0.84f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(32.dp)),
                        )
                    } else {
                        OnboardingCloudSyncArtwork(
                            modifier = Modifier
                                .fillMaxWidth(0.84f)
                                .aspectRatio(1f),
                        )
                    }

                    Spacer(Modifier.height(28.dp))

                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = page.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }

            // Animated Emerald Page Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 16.dp),
            ) {
                repeat(pages.size) { index ->
                    val selected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (selected) 26.dp else 8.dp,
                        label = "dot_width",
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (selected) FlowTeal600
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            AnimatedVisibility(visible = !authError.isNullOrBlank()) {
                Text(
                    text = authError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            if (!isLastPage) {
                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FlowTeal600,
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(vertical = 15.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .testTag("onboarding_next_button"),
                ) {
                    Text(
                        text = "Next",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GoogleSignInPillButton(
                        label = "Continue with Google",
                        isLoading = isSigningIn,
                        isPrimary = true,
                        onClick = onContinueWithGoogle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_google_button"),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { onCompleteToAuth(true) },
                            enabled = !isSigningIn,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(vertical = 13.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_create_account_button"),
                        ) {
                            Text("Create Account", maxLines = 1, softWrap = false, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = { onCompleteToAuth(false) },
                            enabled = !isSigningIn,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(vertical = 13.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_sign_in_button"),
                        ) {
                            Text("Sign In", maxLines = 1, softWrap = false, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun OnboardingCloudSyncArtwork(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFCCFBF1),
                        Color(0xFFE6FAF5),
                        Color(0xFFF6FDFB),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(28.dp)) {
            val w = size.width
            val h = size.height

            // Soft concentric glow rings
            drawCircle(Color(0x2210B981), radius = w * 0.44f, center = Offset(w * 0.5f, h * 0.5f))
            drawCircle(Color(0x280D9488), radius = w * 0.30f, center = Offset(w * 0.5f, h * 0.5f))

            // Connecting flow lines
            drawLine(
                color = Color(0xFF0D9488),
                start = Offset(w * 0.50f, h * 0.25f),
                end = Offset(w * 0.50f, h * 0.42f),
                strokeWidth = 6f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color(0xFF0D9488),
                start = Offset(w * 0.30f, h * 0.68f),
                end = Offset(w * 0.50f, h * 0.54f),
                strokeWidth = 6f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color(0xFF0D9488),
                start = Offset(w * 0.70f, h * 0.68f),
                end = Offset(w * 0.50f, h * 0.54f),
                strokeWidth = 6f,
                cap = StrokeCap.Round,
            )

            // Top Start Pill
            drawRoundRect(
                color = Color(0xFF10B981),
                topLeft = Offset(w * 0.30f, h * 0.14f),
                size = Size(w * 0.40f, h * 0.13f),
                cornerRadius = CornerRadius(h * 0.065f, h * 0.065f),
            )
            // Center Emerald Circle Badge
            drawCircle(Color(0xFF065F46), radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.50f))
            drawCircle(Color.White, radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.50f), style = Stroke(width = 5f))

            // Bottom cards
            drawRoundRect(
                color = Color(0xFFA7F3D0),
                topLeft = Offset(w * 0.14f, h * 0.68f),
                size = Size(w * 0.30f, h * 0.16f),
                cornerRadius = CornerRadius(18f, 18f),
            )
            drawRoundRect(
                color = Color(0xFF99F6E4),
                topLeft = Offset(w * 0.56f, h * 0.68f),
                size = Size(w * 0.30f, h * 0.16f),
                cornerRadius = CornerRadius(18f, 18f),
            )
        }
        Icon(
            imageVector = Icons.Filled.CloudDone,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(34.dp),
        )
    }
}

@Composable
fun AuthScreen(
    initialSignUpMode: Boolean,
    isSigningIn: Boolean,
    errorMessage: String?,
    onGoogleAuthClick: (customName: String, customSubtitle: String) -> Unit,
) {
    var isSignUp by remember(initialSignUpMode) { mutableStateOf(initialSignUpMode) }
    var fullName by remember { mutableStateOf("") }
    var tagline by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF064E3B),
                        Color(0xFF0D9488),
                    )
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Deep Emerald Brand Header matching Mockup 5
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FlowBuilderBrandMarkF(size = 56.dp, tintLight = true)
                Text(
                    text = "FlowBuilder",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Text(
                    text = "Plan • Build • Organize • Grow",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFD1FAE5),
                )
            }

            // Rounded Crisp White/Surface Auth Card matching Mockup 5
            Surface(
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 420.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = if (isSignUp) "Create Account" else "Welcome Back!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = if (isSignUp) {
                                "Start your flowchart journey with realtime cloud sync"
                            } else {
                                "Sign in with Google to access your synced flowcharts"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(6.dp))

                        // Mode Switcher Pills
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                AuthTabChip(
                                    label = "Sign In",
                                    selected = !isSignUp,
                                    onClick = { isSignUp = false },
                                    modifier = Modifier.weight(1f).testTag("auth_tab_sign_in"),
                                )
                                AuthTabChip(
                                    label = "Create Account",
                                    selected = isSignUp,
                                    onClick = { isSignUp = true },
                                    modifier = Modifier.weight(1f).testTag("auth_tab_sign_up"),
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isSignUp,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    singleLine = true,
                                    label = { Text("Display Name (optional)") },
                                    placeholder = { Text("e.g., Alex Morgan") },
                                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_full_name_input"),
                                )
                                OutlinedTextField(
                                    value = tagline,
                                    onValueChange = { tagline = it },
                                    singleLine = true,
                                    label = { Text("Role or Tagline (optional)") },
                                    placeholder = { Text("e.g., CS Student • Algorithm Designer") },
                                    leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_tagline_input"),
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        GoogleSignInPillButton(
                            label = if (isSignUp) "Create Account with Google" else "Continue with Google",
                            isLoading = isSigningIn,
                            isPrimary = true,
                            onClick = { onGoogleAuthClick(fullName.trim(), tagline.trim()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_google_submit_button"),
                        )

                        AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        // Feature Trust Highlights
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                AuthFeatureRow(
                                    icon = Icons.Filled.Sync,
                                    text = "Realtime cloud backup & offline cache for all flowcharts",
                                )
                                AuthFeatureRow(
                                    icon = Icons.Filled.CheckCircle,
                                    text = "Secure one-tap Google authentication — zero passwords to remember",
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = if (isSignUp) "Already have an account? " else "Don't have an account? ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = if (isSignUp) "Sign In" else "Sign Up",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = FlowTeal600,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isSignUp = !isSignUp }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 10.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) FlowTeal600 else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AuthFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FlowTeal600,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun GoogleSignInPillButton(
    label: String,
    isLoading: Boolean,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FlowTeal600,
                contentColor = Color.White,
            ),
            contentPadding = PaddingValues(vertical = 14.dp, horizontal = 18.dp),
            modifier = modifier,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.4.dp,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text("Signing in…", fontWeight = FontWeight.Bold)
            } else {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "G",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = FlowTeal600,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            contentPadding = PaddingValues(vertical = 14.dp, horizontal = 18.dp),
            modifier = modifier,
        ) {
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    }
}
