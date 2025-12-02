package com.example.patienttracker.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.LayoutDirection

/**
 * Supported languages for the Patient Tracker App
 * Supporting Pakistani language diversity
 */
enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val layoutDirection: LayoutDirection
) {
    ENGLISH("en", "English", "English", LayoutDirection.Ltr),
    URDU("ur", "Urdu", "اردو", LayoutDirection.Rtl),
    SINDHI("sd", "Sindhi", "سنڌي", LayoutDirection.Rtl),
    PUNJABI("pa", "Punjabi", "پنجابی", LayoutDirection.Rtl),
    PASHTO("ps", "Pashto", "پښتو", LayoutDirection.Rtl),
    BALOCHI("bal", "Balochi", "بلوچی", LayoutDirection.Rtl);
    
    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code == code } ?: ENGLISH
        }
    }
}

/**
 * Login screen translations for all supported languages
 */
data class LoginStrings(
    val signIn: String,
    val welcomeBack: String,
    val signInToContinue: String,
    val emailAddress: String,
    val password: String,
    val forgotPassword: String,
    val signInButton: String,
    val orContinueWith: String,
    val google: String,
    val dontHaveAccount: String,
    val signUp: String,
    val continueAsGuest: String,
    val enterEmail: String,
    val enterPassword: String,
    val selectLanguage: String,
    val passwordVisible: String,
    val passwordHidden: String,
    val signingIn: String,
    val errorInvalidEmail: String,
    val errorEmptyPassword: String
)

/**
 * Translations for all supported languages
 */
object LoginTranslations {
    
    val english = LoginStrings(
        signIn = "Sign In",
        welcomeBack = "Welcome Back",
        signInToContinue = "Sign in to continue to your account",
        emailAddress = "Email Address",
        password = "Password",
        forgotPassword = "Forgot Password?",
        signInButton = "Sign In",
        orContinueWith = "Or continue with",
        google = "Google",
        dontHaveAccount = "Don't have an account?",
        signUp = "Sign Up",
        continueAsGuest = "Continue as Guest",
        enterEmail = "Enter your email",
        enterPassword = "Enter your password",
        selectLanguage = "Language",
        passwordVisible = "Hide password",
        passwordHidden = "Show password",
        signingIn = "Signing in...",
        errorInvalidEmail = "Please enter a valid email",
        errorEmptyPassword = "Password cannot be empty"
    )
    
    val urdu = LoginStrings(
        signIn = "سائن ان",
        welcomeBack = "خوش آمدید",
        signInToContinue = "اپنے اکاؤنٹ میں جاری رکھنے کے لیے سائن ان کریں",
        emailAddress = "ای میل ایڈریس",
        password = "پاس ورڈ",
        forgotPassword = "پاس ورڈ بھول گئے؟",
        signInButton = "سائن ان کریں",
        orContinueWith = "یا اس کے ساتھ جاری رکھیں",
        google = "گوگل",
        dontHaveAccount = "کیا آپ کا اکاؤنٹ نہیں ہے؟",
        signUp = "سائن اپ کریں",
        continueAsGuest = "مہمان کے طور پر جاری رکھیں",
        enterEmail = "اپنا ای میل درج کریں",
        enterPassword = "اپنا پاس ورڈ درج کریں",
        selectLanguage = "زبان",
        passwordVisible = "پاس ورڈ چھپائیں",
        passwordHidden = "پاس ورڈ دکھائیں",
        signingIn = "سائن ان ہو رہا ہے...",
        errorInvalidEmail = "براہ کرم درست ای میل درج کریں",
        errorEmptyPassword = "پاس ورڈ خالی نہیں ہو سکتا"
    )
    
    val sindhi = LoginStrings(
        signIn = "سائن ان",
        welcomeBack = "ڀلي ڪري آيا",
        signInToContinue = "پنهنجي اڪائونٽ ۾ جاري رکڻ لاءِ سائن ان ڪريو",
        emailAddress = "اي ميل ايڊريس",
        password = "پاسورڊ",
        forgotPassword = "پاسورڊ وساري ويا؟",
        signInButton = "سائن ان ڪريو",
        orContinueWith = "يا ان سان جاري رکو",
        google = "گوگل",
        dontHaveAccount = "ڇا توهان وٽ اڪائونٽ ناهي؟",
        signUp = "سائن اپ ڪريو",
        continueAsGuest = "مهمان طور جاري رکو",
        enterEmail = "پنهنجو اي ميل داخل ڪريو",
        enterPassword = "پنهنجو پاسورڊ داخل ڪريو",
        selectLanguage = "ٻولي",
        passwordVisible = "پاسورڊ لڪايو",
        passwordHidden = "پاسورڊ ڏيکاريو",
        signingIn = "سائن ان ٿي رهيو آهي...",
        errorInvalidEmail = "مهرباني ڪري صحيح اي ميل داخل ڪريو",
        errorEmptyPassword = "پاسورڊ خالي نه ٿي سگهي"
    )
    
    val punjabi = LoginStrings(
        signIn = "سائن ان",
        welcomeBack = "جی آیاں نوں",
        signInToContinue = "اپنے اکاؤنٹ وچ جاری رکھن لئی سائن ان کرو",
        emailAddress = "ای میل پتہ",
        password = "پاسورڈ",
        forgotPassword = "پاسورڈ بھل گئے؟",
        signInButton = "سائن ان کرو",
        orContinueWith = "یا ایہدے نال جاری رکھو",
        google = "گوگل",
        dontHaveAccount = "کی تہاڈا اکاؤنٹ نہیں؟",
        signUp = "سائن اپ کرو",
        continueAsGuest = "مہمان دے طور تے جاری رکھو",
        enterEmail = "اپنا ای میل پاؤ",
        enterPassword = "اپنا پاسورڈ پاؤ",
        selectLanguage = "بولی",
        passwordVisible = "پاسورڈ لکاؤ",
        passwordHidden = "پاسورڈ دکھاؤ",
        signingIn = "سائن ان ہو رہیا اے...",
        errorInvalidEmail = "مہربانی نال صحیح ای میل پاؤ",
        errorEmptyPassword = "پاسورڈ خالی نہیں ہو سکدا"
    )
    
    val pashto = LoginStrings(
        signIn = "ننوتل",
        welcomeBack = "بیرته ښه راغلاست",
        signInToContinue = "خپل حساب ته دوام ورکولو لپاره ننوځئ",
        emailAddress = "بریښنالیک پته",
        password = "پټ نوم",
        forgotPassword = "پټ نوم مو هیر شو؟",
        signInButton = "ننوځئ",
        orContinueWith = "یا له دې سره دوام ورکړئ",
        google = "ګوګل",
        dontHaveAccount = "ایا حساب نلرئ؟",
        signUp = "نوم لیکنه وکړئ",
        continueAsGuest = "د میلمه په توګه دوام ورکړئ",
        enterEmail = "خپل بریښنالیک ولیکئ",
        enterPassword = "خپل پټ نوم ولیکئ",
        selectLanguage = "ژبه",
        passwordVisible = "پټ نوم پټ کړئ",
        passwordHidden = "پټ نوم ښکاره کړئ",
        signingIn = "ننوتل روان دي...",
        errorInvalidEmail = "مهرباني وکړئ سم بریښنالیک ولیکئ",
        errorEmptyPassword = "پټ نوم نشي کولی خالي وي"
    )
    
    val balochi = LoginStrings(
        signIn = "سائن ان",
        welcomeBack = "پدا شمارا دیدگ بوت",
        signInToContinue = "وتی اکاؤنٹ ءِ تها پیش برئیں سائن ان کنیت",
        emailAddress = "ای میل پته",
        password = "پاسورڈ",
        forgotPassword = "پاسورڈ پہ یات نہ انت؟",
        signInButton = "سائن ان کنیت",
        orContinueWith = "یا اے گوں پیش برئیت",
        google = "گوگل",
        dontHaveAccount = "شمارا اکاؤنٹ نیست؟",
        signUp = "سائن اپ کنیت",
        continueAsGuest = "مہمان ءِ پیما پیش برئیت",
        enterEmail = "وتی ای میل بنویسیت",
        enterPassword = "وتی پاسورڈ بنویسیت",
        selectLanguage = "زبان",
        passwordVisible = "پاسورڈ پناہ کنیت",
        passwordHidden = "پاسورڈ پیش کنیت",
        signingIn = "سائن ان بیت گوں...",
        errorInvalidEmail = "مہربانی کنیت درستیں ای میل بنویسیت",
        errorEmptyPassword = "پاسورڈ ہالیگ نہ بیت کنت"
    )
    
    fun getStrings(language: AppLanguage): LoginStrings {
        return when (language) {
            AppLanguage.ENGLISH -> english
            AppLanguage.URDU -> urdu
            AppLanguage.SINDHI -> sindhi
            AppLanguage.PUNJABI -> punjabi
            AppLanguage.PASHTO -> pashto
            AppLanguage.BALOCHI -> balochi
        }
    }
}

/**
 * Language preference manager using SharedPreferences
 */
class LanguagePreferenceManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    fun saveLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }
    
    fun getLanguage(): AppLanguage {
        val code = prefs.getString(KEY_LANGUAGE, AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code
        return AppLanguage.fromCode(code)
    }
    
    companion object {
        private const val PREFS_NAME = "language_prefs"
        private const val KEY_LANGUAGE = "selected_language"
    }
}

/**
 * CompositionLocal for providing language state across composables
 */
val LocalAppLanguage = compositionLocalOf { AppLanguage.ENGLISH }
val LocalLoginStrings = compositionLocalOf { LoginTranslations.english }
