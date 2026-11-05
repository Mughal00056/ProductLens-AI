package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

// Provisioned Firebase configuration constants from google-services.json
private const val FIREBASE_APP_ID = "1:641594230599:android:c851e0a75e90f87a4c6e62"
private const val FIREBASE_API_KEY = "AIzaSyBPu7AmrgUHQr4sQ3D6U6BRLekyLSL-bAo"
private const val FIREBASE_PROJECT_ID = "gen-lang-client-0662873984"
private const val FIREBASE_STORAGE_BUCKET = "gen-lang-client-0662873984.firebasestorage.app"

fun ensureFirebaseInitialized(context: Context): FirebaseApp {
    if (FirebaseApp.getApps(context).isNotEmpty()) {
        return FirebaseApp.getInstance()
    }
    return try {
        val app = FirebaseApp.initializeApp(context)
        if (app != null) {
            Log.d(TAG, "FirebaseApp initialized from resources.")
            app
        } else {
            initializeExplicitFirebase(context)
        }
    } catch (e: Exception) {
        Log.w(TAG, "Default FirebaseApp initialization from resources failed, using explicit options", e)
        initializeExplicitFirebase(context)
    }
}

private fun initializeExplicitFirebase(context: Context): FirebaseApp {
    val options = FirebaseOptions.Builder()
        .setApplicationId(FIREBASE_APP_ID)
        .setApiKey(FIREBASE_API_KEY)
        .setProjectId(FIREBASE_PROJECT_ID)
        .setStorageBucket(FIREBASE_STORAGE_BUCKET)
        .build()
    val app = FirebaseApp.initializeApp(context.applicationContext, options)
    Log.d(TAG, "FirebaseApp initialized with explicit FirebaseOptions.")
    return app
}

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        ensureFirebaseInitialized(context)

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured successfully.")
            } else {
                Log.w(TAG, "No debug token found in Intent extra.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                } else {
                    Log.e(TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}
