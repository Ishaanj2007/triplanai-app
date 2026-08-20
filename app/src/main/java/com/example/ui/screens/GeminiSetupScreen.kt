package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.viewmodel.TripViewModel

enum class SetupViewMode {
    INTRO,
    WIZARD,
    TESTING,
    SUCCESS,
    ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiSetupDialog(
    viewModel: TripViewModel,
    initialStep: Int = 0, // 0 = Intro, 1..4 = Wizard steps
    onDismiss: () -> Unit,
    onConnectedSuccess: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ArtBackground)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            GeminiSetupContent(
                viewModel = viewModel,
                initialStep = initialStep,
                onClose = onDismiss,
                onConnectedSuccess = onConnectedSuccess
            )
        }
    }
}

@Composable
fun GeminiSetupContent(
    viewModel: TripViewModel,
    initialStep: Int = 0,
    onClose: () -> Unit,
    onConnectedSuccess: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var viewMode by remember {
        mutableStateOf(if (initialStep > 0) SetupViewMode.WIZARD else SetupViewMode.INTRO)
    }
    var currentWizardStep by remember {
        mutableStateOf(if (initialStep in 1..4) initialStep else 1)
    }
    var inputApiKey by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showHowItWorksDialog by remember { mutableStateOf(false) }

    val userApiKeyVal by viewModel.userApiKey.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (viewMode == SetupViewMode.WIZARD && currentWizardStep > 1) {
                IconButton(
                    onClick = { currentWizardStep-- },
                    modifier = Modifier
                        .size(40.dp)
                        .background(ArtCardBackground, CircleShape)
                        .border(1.5.dp, ArtBorderDark, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ArtTextDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (viewMode == SetupViewMode.WIZARD && currentWizardStep == 1) {
                IconButton(
                    onClick = { viewMode = SetupViewMode.INTRO },
                    modifier = Modifier
                        .size(40.dp)
                        .background(ArtCardBackground, CircleShape)
                        .border(1.5.dp, ArtBorderDark, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Intro",
                        tint = ArtTextDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(40.dp))
            }

            Text(
                text = "GEMINI SETUP",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = ArtPrimaryPurple,
                letterSpacing = 1.sp
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .background(ArtCardBackground, CircleShape)
                    .border(1.5.dp, ArtBorderDark, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = ArtTextDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content Body
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (viewMode) {
                SetupViewMode.INTRO -> {
                    GeminiIntroView(
                        onStartSetup = {
                            viewMode = SetupViewMode.WIZARD
                            currentWizardStep = 1
                        },
                        onHowItWorks = { showHowItWorksDialog = true }
                    )
                }
                SetupViewMode.WIZARD -> {
                    GeminiWizardView(
                        currentStep = currentWizardStep,
                        onStepChange = { currentWizardStep = it },
                        apiKey = inputApiKey,
                        onApiKeyChange = {
                            inputApiKey = it
                            errorMessage = null
                        },
                        isPasswordVisible = isPasswordVisible,
                        onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                        onPaste = {
                            val clip = clipboardManager.getText()?.text?.trim()
                            if (!clip.isNullOrBlank()) {
                                inputApiKey = clip
                            }
                        },
                        onTestAndConnect = {
                            if (inputApiKey.isBlank()) {
                                errorMessage = "Please enter or paste your Gemini API key."
                                return@GeminiWizardView
                            }
                            viewMode = SetupViewMode.TESTING
                            viewModel.validateAndSetUserApiKey(inputApiKey.trim()) { success, errorMsg ->
                                if (success) {
                                    viewMode = SetupViewMode.SUCCESS
                                } else {
                                    errorMessage = errorMsg ?: "Couldn't connect to Gemini. Please check your key."
                                    viewMode = SetupViewMode.ERROR
                                }
                            }
                        }
                    )
                }
                SetupViewMode.TESTING -> {
                    GeminiTestingView()
                }
                SetupViewMode.SUCCESS -> {
                    GeminiSuccessView(
                        onStartPlanning = {
                            onConnectedSuccess()
                            onClose()
                        }
                    )
                }
                SetupViewMode.ERROR -> {
                    GeminiErrorView(
                        errorMessage = errorMessage ?: "Couldn't connect to Gemini",
                        onTryAgain = {
                            viewMode = SetupViewMode.WIZARD
                            currentWizardStep = 4
                        },
                        onSetupGuide = {
                            viewMode = SetupViewMode.WIZARD
                            currentWizardStep = 1
                        }
                    )
                }
            }
        }
    }

    if (showHowItWorksDialog) {
        HowItWorksModal(onDismiss = { showHowItWorksDialog = false })
    }
}

@Composable
private fun GeminiIntroView(
    onStartSetup: () -> Unit,
    onHowItWorks: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .padding(vertical = 12.dp)
        ) {
            // Decorative Hero Icon Card
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(ArtSecondaryPurple, ArtSoftLavender)
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .border(2.dp, ArtBorderDark, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ArtPrimaryPurple,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Connect Gemini",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = ArtTextDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Connect your own Gemini API key to generate personalized travel itineraries.",
                fontSize = 14.sp,
                color = ArtGrayMuted,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Non-technical Benefit Cards
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
                border = BorderStroke(1.5.dp, ArtBorderDark)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FeatureHighlightRow(
                        icon = Icons.Default.Bolt,
                        title = "Fast Itinerary Generation",
                        description = "Direct AI connection creates complete day-by-day travel plans in seconds.",
                        accentColor = ArtPeachGold
                    )
                    Divider(color = ArtBorderDark.copy(alpha = 0.5f), thickness = 1.dp)
                    FeatureHighlightRow(
                        icon = Icons.Default.Savings,
                        title = "Free to Use",
                        description = "Google AI Studio offers free API keys for personal use with zero hidden fees.",
                        accentColor = ArtMintGreen
                    )
                    Divider(color = ArtBorderDark.copy(alpha = 0.5f), thickness = 1.dp)
                    FeatureHighlightRow(
                        icon = Icons.Default.Shield,
                        title = "Private & Secure",
                        description = "Your key is saved only on your device and never uploaded to any third party.",
                        accentColor = ArtSoftLavender
                    )
                }
            }
        }

        // Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onStartSetup,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ArtPrimaryPurple,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Set Up Gemini",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            OutlinedButton(
                onClick = onHowItWorks,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, ArtBorderDark),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ArtTextDark,
                    containerColor = Color.Transparent
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = ArtGrayMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "How does this work?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun GeminiWizardView(
    currentStep: Int,
    onStepChange: (Int) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    onPaste: () -> Unit,
    onTestAndConnect: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
        ) {
            // Step Progress Indicator (1..4)
            WizardStepIndicator(currentStep = currentStep, onSelectStep = onStepChange)

            Spacer(modifier = Modifier.height(20.dp))

            when (currentStep) {
                1 -> {
                    // STEP 1: OPEN GOOGLE AI STUDIO
                    WizardStepContent(
                        stepNumber = "01",
                        title = "OPEN GOOGLE AI STUDIO",
                        subtitle = "Google AI Studio is where you create your Gemini API key. Sign in with your Google account to get started.",
                        visualContent = {
                            StepVisualMockup(
                                badgeText = "Official Portal",
                                icon = Icons.AutoMirrored.Filled.OpenInNew,
                                title = "Google AI Studio",
                                subtitle = "aistudio.google.com/app/apikey",
                                description = "Sign in with your Google account to access Gemini models.",
                                accentColor = ArtSoftLavender
                            )
                        }
                    )
                }
                2 -> {
                    // STEP 2: CREATE AN API KEY
                    WizardStepContent(
                        stepNumber = "02",
                        title = "CREATE AN API KEY",
                        subtitle = "Tap 'Create API key' in Google AI Studio and choose or create a project.",
                        visualContent = {
                            StepVisualMockup(
                                badgeText = "Key Generator",
                                icon = Icons.Default.AddCircleOutline,
                                title = "+ Create API key",
                                subtitle = "Select 'Create in new project'",
                                description = "A new personal Gemini API key will be generated instantly.",
                                accentColor = ArtPeachGold
                            )
                        }
                    )
                }
                3 -> {
                    // STEP 3: COPY YOUR KEY
                    WizardStepContent(
                        stepNumber = "03",
                        title = "COPY YOUR KEY",
                        subtitle = "Copy the key generated by Google AI Studio and return to TriplanAI.",
                        visualContent = {
                            StepVisualMockup(
                                badgeText = "Ready to Copy",
                                icon = Icons.Default.ContentCopy,
                                title = "AIzaSy••••••••••••••••",
                                subtitle = "Tap 'Copy' next to your key",
                                description = "Your key begins with 'AIzaSy' and is ready to connect.",
                                accentColor = ArtMintGreen
                            )
                        }
                    )
                }
                4 -> {
                    // STEP 4: CONNECT YOUR KEY
                    WizardStepContent(
                        stepNumber = "04",
                        title = "CONNECT YOUR KEY",
                        subtitle = "Paste your Gemini API key below to link it to TriplanAI.",
                        visualContent = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ArtCardBackground, RoundedCornerShape(18.dp))
                                    .border(1.5.dp, ArtBorderDark, RoundedCornerShape(18.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "GEMINI API KEY",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = ArtPrimaryPurple,
                                        letterSpacing = 0.5.sp
                                    )

                                    TextButton(
                                        onClick = onPaste,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = null,
                                            tint = ArtPrimaryPurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Paste from clipboard",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = ArtPrimaryPurple
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = apiKey,
                                    onValueChange = onApiKeyChange,
                                    placeholder = { Text("Paste AIzaSy... key here", fontSize = 13.sp, color = ArtGrayMuted) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = onTogglePasswordVisibility) {
                                            Icon(
                                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (isPasswordVisible) "Hide Key" else "Show Key",
                                                tint = ArtGrayMuted,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ArtPrimaryPurple,
                                        unfocusedBorderColor = ArtBorderDark,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Text(
                                    text = "Supported Gemini models will be automatically configured for you.",
                                    fontSize = 11.sp,
                                    color = ArtGrayMuted,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    )
                }
            }
        }

        // Action Buttons Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (currentStep) {
                1 -> {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ArtPrimaryPurple,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Google AI Studio", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    OutlinedButton(
                        onClick = { onStepChange(2) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, ArtBorderDark),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ArtTextDark)
                    ) {
                        Text("Next: Create Key →", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                2 -> {
                    Button(
                        onClick = { onStepChange(3) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ArtPrimaryPurple,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Next: Copy Key →", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                3 -> {
                    Button(
                        onClick = { onStepChange(4) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ArtPrimaryPurple,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Next: Connect Key →", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                4 -> {
                    Button(
                        onClick = onTestAndConnect,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ArtPrimaryPurple,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test & Connect", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun WizardStepIndicator(
    currentStep: Int,
    onSelectStep: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val steps = listOf("Open AI Studio", "Create key", "Copy key", "Connect")
        steps.forEachIndexed { index, label ->
            val stepNum = index + 1
            val isCompleted = stepNum < currentStep
            val isCurrent = stepNum == currentStep

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectStep(stepNum) }
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            when {
                                isCurrent -> ArtPrimaryPurple
                                isCompleted -> ArtMintGreen
                                else -> ArtCardBackground
                            },
                            CircleShape
                        )
                        .border(
                            1.5.dp,
                            if (isCurrent || isCompleted) ArtBorderDark else ArtBorderDark.copy(alpha = 0.5f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF166534),
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "$stepNum",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isCurrent) Color.White else ArtGrayMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCurrent) ArtPrimaryPurple else ArtGrayMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun WizardStepContent(
    stepNumber: String,
    title: String,
    subtitle: String,
    visualContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ArtSecondaryPurple,
                border = BorderStroke(1.dp, ArtPrimaryPurple.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "STEP $stepNumber",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = ArtPrimaryPurple,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = ArtTextDark
            )
        }

        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = ArtGrayMuted,
            lineHeight = 18.sp
        )

        visualContent()
    }
}

@Composable
private fun StepVisualMockup(
    badgeText: String,
    icon: ImageVector,
    title: String,
    subtitle: String,
    description: String,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
        border = BorderStroke(1.5.dp, ArtBorderDark)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArtTextDark,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFE5E7EB), CircleShape))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFE5E7EB), CircleShape))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFE5E7EB), CircleShape))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ArtBackground, RoundedCornerShape(12.dp))
                    .border(1.dp, ArtBorderDark.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(accentColor, CircleShape)
                            .border(1.dp, ArtBorderDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = ArtTextDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ArtTextDark
                        )
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = ArtGrayMuted
                        )
                    }
                }
            }

            Text(
                text = description,
                fontSize = 12.sp,
                color = ArtGrayMuted,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun GeminiTestingView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )

        Box(
            modifier = Modifier
                .size((80 * scale).dp)
                .background(ArtSoftLavender, CircleShape)
                .border(2.dp, ArtBorderDark, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = ArtPrimaryPurple,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Connecting to Gemini...",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = ArtTextDark,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Validating your API key and preparing AI itinerary planner.",
            fontSize = 13.sp,
            color = ArtGrayMuted,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        LinearProgressIndicator(
            modifier = Modifier
                .width(180.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = ArtPrimaryPurple,
            trackColor = ArtSecondaryPurple
        )
    }
}

@Composable
private fun GeminiSuccessView(
    onStartPlanning: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .padding(top = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(Color(0xFFDCFCE7), CircleShape)
                    .border(2.dp, ArtBorderDark, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF166534),
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Gemini connected",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = ArtTextDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your itinerary generation is ready.",
                fontSize = 14.sp,
                color = ArtGrayMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
                border = BorderStroke(1.dp, ArtBorderDark)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ArtMintGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = Color(0xFF166534),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Key Verified & Saved",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ArtTextDark
                        )
                        Text(
                            text = "You won't be asked to enter it again.",
                            fontSize = 11.sp,
                            color = ArtGrayMuted
                        )
                    }
                }
            }
        }

        Button(
            onClick = onStartPlanning,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .height(52.dp)
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ArtPrimaryPurple,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Start Planning",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun GeminiErrorView(
    errorMessage: String,
    onTryAgain: () -> Unit,
    onSetupGuide: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .padding(top = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFFFEE2E2), CircleShape)
                    .border(2.dp, ArtBorderDark, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Couldn't connect to Gemini",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = ArtTextDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = errorMessage,
                fontSize = 13.sp,
                color = ArtGrayMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
                border = BorderStroke(1.dp, ArtBorderDark)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "POSSIBLE REASONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = ArtPrimaryPurple,
                        letterSpacing = 0.5.sp
                    )
                    Text("• Your API key may have a typo or extra spaces.", fontSize = 12.sp, color = ArtTextDark)
                    Text("• Your Gemini project may have reached its usage limit.", fontSize = 12.sp, color = ArtTextDark)
                    Text("• Check your internet connection.", fontSize = 12.sp, color = ArtTextDark)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onTryAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ArtPrimaryPurple,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            OutlinedButton(
                onClick = onSetupGuide,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, ArtBorderDark),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ArtTextDark)
            ) {
                Text("Setup Guide", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(accentColor, CircleShape)
                .border(1.dp, ArtBorderDark, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ArtTextDark,
                modifier = Modifier.size(20.dp)
            )
        }

        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ArtTextDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = ArtGrayMuted,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun HowItWorksModal(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = ArtPrimaryPurple
                )
                Text(
                    text = "How Gemini Keys Work",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = ArtTextDark
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Why do I need my own Gemini API key?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ArtTextDark
                )
                Text(
                    text = "Connecting your own key ensures fast, reliable, and unlimited travel generation without shared server quotas or interruptions.",
                    fontSize = 12.sp,
                    color = ArtGrayMuted,
                    lineHeight = 16.sp
                )

                Divider(color = ArtBorderDark.copy(alpha = 0.5f), thickness = 1.dp)

                Text(
                    text = "Is it completely free?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ArtTextDark
                )
                Text(
                    text = "Yes! Google AI Studio provides a free tier with generous limits for personal accounts.",
                    fontSize = 12.sp,
                    color = ArtGrayMuted,
                    lineHeight = 16.sp
                )

                Divider(color = ArtBorderDark.copy(alpha = 0.5f), thickness = 1.dp)

                Text(
                    text = "Is my key kept private?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ArtTextDark
                )
                Text(
                    text = "Yes. Your key is stored securely in on-device storage. It is used exclusively to make direct travel itinerary requests.",
                    fontSize = 12.sp,
                    color = ArtGrayMuted,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ArtPrimaryPurple)
            ) {
                Text("Got it", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = ArtCardBackground
    )
}
