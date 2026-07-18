package com.placer.firewatch.facebook

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.placer.firewatch.R

/**
 * "Start Monitoring" → Facebook Live, per Section 9 of the product spec.
 * Deliberately lightweight: no Facebook SDK, no Meta Developer App/App ID
 * (that would need you to register one — a real external-credential
 * setup step, same category as Firebase/Maps). This just checks whether
 * the Facebook app is installed and, if so, opens it via a deep link
 * toward composing a live video. Meta doesn't publish a guaranteed
 * direct-to-live-broadcast intent, so this is best-effort — it may land
 * on Facebook's main app instead of the live composer specifically,
 * depending on the installed version.
 */
object FacebookLive {

    private const val FACEBOOK_PACKAGE = "com.facebook.katana"
    private const val LIVE_DEEP_LINK = "fb://live_camera"

    fun isFacebookInstalled(context: Context): Boolean =
        try {
            context.packageManager.getPackageInfo(FACEBOOK_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    fun launch(context: Context) {
        if (!isFacebookInstalled(context)) {
            Toast.makeText(context, R.string.facebook_not_installed, Toast.LENGTH_LONG).show()
            return
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LIVE_DEEP_LINK)))
        } catch (e: ActivityNotFoundException) {
            // Deep link not handled by this Facebook app version — fall back
            // to just opening the app itself rather than doing nothing.
            val fallback = context.packageManager.getLaunchIntentForPackage(FACEBOOK_PACKAGE)
            if (fallback != null) {
                context.startActivity(fallback)
            } else {
                Toast.makeText(context, R.string.facebook_not_installed, Toast.LENGTH_LONG).show()
            }
        }
    }
}
