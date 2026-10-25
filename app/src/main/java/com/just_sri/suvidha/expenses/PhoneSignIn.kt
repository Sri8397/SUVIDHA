package com.just_sri.suvidha.expenses

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

// Brand color palette matching the UI design
private val BrandForest = Color(0xFF1E4D3E)
private val BrandTagline = Color(0xFF2E6F56)
private val BrandButton = Color(0xFF2E7253)
private val BrandFieldBorder = Color(0xFFE2E7E2)
private val BrandFieldBg = Color(0xFFFAFCFA)
private val BrandOtpBg = Color(0xFFF3F5F3)
private val BrandLabel = Color(0xFF4A5550)
private val BrandPlaceholder = Color(0xFF9EA7A2)
private val BrandDivider = Color(0xFFE5EAE6)
private val BrandDividerText = Color(0xFF7A8780)
private val BrandResend = Color(0xFF3D6855)

@Composable
fun PhoneSignInScreen(
    auth: FirebaseAuth,
    onContinueOffline: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val focusManager = LocalFocusManager.current
    val otpFocusRequester = remember { FocusRequester() }

    var phoneDigits by remember { mutableStateOf("") }
    var codeField by remember { mutableStateOf(TextFieldValue("")) }
    val code = codeField.text
    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var resendSeconds by remember { mutableIntStateOf(0) }
    var isOtpFocused by remember { mutableStateOf(false) }

    // Blinking cursor animation for the active OTP slot
    val cursorTransition = rememberInfiniteTransition(label = "otpCursor")
    val cursorAlpha by cursorTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    // Countdown timer for Resend OTP
    LaunchedEffect(resendSeconds) {
        if (resendSeconds > 0) {
            delay(1000L)
            resendSeconds -= 1
        }
    }

    fun finishSignIn(credential: PhoneAuthCredential) {
        busy = true
        message = null
        isError = false
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            busy = false
            if (!task.isSuccessful) {
                isError = true
                message = task.exception?.localizedMessage ?: "Invalid code or sign-in failed. Please try again."
            }
        }
    }

    fun sendVerification(token: PhoneAuthProvider.ForceResendingToken? = null) {
        val cleanNumber = phoneDigits.filter(Char::isDigit)
        if (cleanNumber.length < 10) {
            isError = true
            message = "Please enter a valid 10-digit mobile number."
            return
        }
        val e164 = if (phoneDigits.startsWith("+")) phoneDigits.trim() else "+91$cleanNumber"

        if (activity == null) {
            isError = true
            message = "Phone verification is unavailable on this device."
            return
        }

        busy = true
        message = null
        isError = false
        focusManager.clearFocus()

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                finishSignIn(credential)
            }

            override fun onVerificationFailed(error: FirebaseException) {
                busy = false
                isError = true
                message = error.localizedMessage ?: "Could not send verification code. Please try again."
            }

            override fun onCodeSent(id: String, resendingToken: PhoneAuthProvider.ForceResendingToken) {
                busy = false
                verificationId = id
                resendToken = resendingToken
                resendSeconds = 30
        codeField = TextFieldValue("")
                message = null
                try {
                    otpFocusRequester.requestFocus()
                } catch (_: Exception) {}
            }
        }

        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(e164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (token != null) {
            builder.setForceResendingToken(token)
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    // Auto-verify when 6 digits are typed
    LaunchedEffect(code) {
        val id = verificationId
        if (id != null && code.length == 6 && !busy) {
            finishSignIn(PhoneAuthProvider.getCredential(id, code))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(24.dp))

            // App Logo Badge
            SuvidhaLogoCard()

            Spacer(Modifier.height(24.dp))

            // App Name
            Text(
                text = "SUVIDHA",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = BrandForest,
                letterSpacing = 2.5.sp
            )

            Spacer(Modifier.height(6.dp))

            // Tagline
            Text(
                text = "India's split-second expense manager",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = BrandTagline,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(36.dp))

            // Mobile Number Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Mobile Number",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = BrandLabel,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Mobile Number Input Field
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = BrandFieldBg,
                    border = BorderStroke(1.dp, BrandFieldBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+91",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink
                        )

                        Spacer(Modifier.width(12.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (phoneDigits.isEmpty()) {
                                Text(
                                    text = "Enter mobile number",
                                    fontSize = 15.sp,
                                    color = BrandPlaceholder
                                )
                            }
                            BasicTextField(
                                value = phoneDigits,
                                onValueChange = { input ->
                                    val filtered = input.filter(Char::isDigit).take(10)
                                    phoneDigits = filtered
                                    if (verificationId != null) {
                                        verificationId = null
                                        codeField = TextFieldValue("")
                                    }
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Ink
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        if (phoneDigits.length >= 10 && !busy) sendVerification()
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !busy
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Primary Action Button (Send OTP / Verify OTP)
            Button(
                onClick = {
                    if (verificationId == null) {
                        sendVerification()
                    } else if (code.length == 6) {
                        finishSignIn(PhoneAuthProvider.getCredential(verificationId!!, code))
                    } else {
                        isError = true
                        message = "Please enter the 6-digit OTP."
                    }
                },
                enabled = !busy && (verificationId != null || phoneDigits.length >= 10),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandButton,
                    contentColor = Color.White,
                    disabledContainerColor = BrandButton.copy(alpha = 0.6f),
                    disabledContentColor = Color.White.copy(alpha = 0.8f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (verificationId == null) "Send OTP" else "Verify OTP",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Divider: ─── OTP ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = BrandDivider,
                    thickness = 1.dp
                )
                Text(
                    text = "OTP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = BrandDividerText,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = BrandDivider,
                    thickness = 1.dp
                )
            }

            Spacer(Modifier.height(20.dp))

            // OTP Input Box (Dashed Slots with Active Cursor)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandOtpBg)
                    .pointerInput(codeField.text, codeField.selection) {
                        detectTapGestures { tap ->
                            val rowWidth = size.width * 0.65f
                            val rowStart = (size.width - rowWidth) / 2f
                            val relativeX = (tap.x - rowStart).coerceIn(0f, rowWidth - 1f)
                            val slot = ((relativeX / rowWidth) * 6).toInt().coerceIn(0, 5)
                            val cursor = slot.coerceAtMost(codeField.text.length)
                            codeField = codeField.copy(selection = TextRange(cursor))
                            otpFocusRequester.requestFocus()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Invisible text field that captures keystrokes without drawing a misplaced cursor
                val hiddenSelectionColors = remember {
                    TextSelectionColors(handleColor = Color.Transparent, backgroundColor = Color.Transparent)
                }
                CompositionLocalProvider(LocalTextSelectionColors provides hiddenSelectionColors) {
                BasicTextField(
                    value = codeField,
                    onValueChange = { input ->
                        val digits = input.text.filter(Char::isDigit).take(6)
                        codeField = input.copy(
                            text = digits,
                            selection = TextRange(
                                input.selection.start.coerceIn(0, digits.length),
                                input.selection.end.coerceIn(0, digits.length)
                            )
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            val id = verificationId
                            if (id != null && code.length == 6) {
                                finishSignIn(PhoneAuthProvider.getCredential(id, code))
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(otpFocusRequester)
                        .onFocusChanged { isOtpFocused = it.isFocused },
                    textStyle = TextStyle(color = Color.Transparent),
                    cursorBrush = SolidColor(Color.Transparent)
                )
                }

                // Visual Slots: displays typed digits, blinking cursor in the active slot, or dashes
                Row(
                    modifier = Modifier.fillMaxWidth(0.65f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 6) {
                        val char = code.getOrNull(i)
                        val isCurrentSlot = isOtpFocused && codeField.selection.collapsed &&
                            i == codeField.selection.start && codeField.selection.start < 6

                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                char != null -> {
                                    Text(
                                        text = char.toString(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Ink,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                else -> {
                                    if (isCurrentSlot) {
                                    // Blinking cursor in the exact active slot
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(18.dp)
                                            .background(BrandButton.copy(alpha = cursorAlpha))
                                    )
                                    } else {
                                    Text(
                                        text = "-",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = BrandPlaceholder,
                                        textAlign = TextAlign.Center
                                    )
                                    }
                                }
                            }
                            if (char != null && isCurrentSlot) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 2.dp)
                                        .width(2.dp)
                                        .height(18.dp)
                                        .background(BrandButton.copy(alpha = cursorAlpha))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Resend OTP Countdown / Action
            val canResend = verificationId != null && resendSeconds == 0 && !busy
            Text(
                text = if (resendSeconds > 0) "Resend OTP in ${resendSeconds}s" else "Resend OTP",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline,
                color = if (canResend) BrandButton else BrandResend,
                modifier = Modifier.clickable(enabled = canResend) {
                    sendVerification(resendToken)
                }
            )

            // Status / Error message
            AnimatedVisibility(
                visible = message != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                message?.let {
                    Text(
                        text = it,
                        fontSize = 13.sp,
                        color = if (isError) Danger else BrandTagline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Continue Offline Option
            TextButton(
                onClick = onContinueOffline,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "Continue Offline",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = BrandTagline
                )
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

/**
 * Suvidha Brand Logo Card displaying the official [R.drawable.ic_logo] vector drawable.
 */
@Composable
private fun SuvidhaLogoCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(108.dp)
            .shadow(10.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = "Suvidha Logo",
            modifier = Modifier.fillMaxSize()
        )
    }
}

