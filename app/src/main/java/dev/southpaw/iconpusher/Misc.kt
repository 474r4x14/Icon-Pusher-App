package dev.southpaw.iconpusher

import android.content.ContentResolver
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources.NotFoundException
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.provider.Settings.Secure
import android.util.DisplayMetrics
import android.util.Log

object Misc {

    fun drawableToBitmap(context:Context, drawable: Drawable?): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        } else if (drawable is AdaptiveIconDrawable) {
            val bg = drawable.background
            val fg = drawable.foreground
            val w = drawable.intrinsicWidth
            val h = drawable.intrinsicHeight
            val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            drawable.setBounds(0, 0, w, h)
            bg?.draw(canvas)
            fg?.draw(canvas)
            return result
        }
        val density: Float = context.resources.displayMetrics.density
        val defaultWidth = (48 * density).toInt()
        val defaultHeight = (48 * density).toInt()
        return Bitmap.createBitmap(defaultWidth, defaultHeight, Bitmap.Config.ARGB_8888)
    }

    // Try to get the largest icon possible
    fun getIconFromPackageName(packageName: String, context: Context): Drawable? {
        val pm = context.packageManager
        try {
            val pi = pm.getPackageInfo(packageName, 0)
            val otherAppCtx =
                context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)

            val displayMetrics = intArrayOf(
                DisplayMetrics.DENSITY_XXXHIGH,
                DisplayMetrics.DENSITY_XXHIGH,
                DisplayMetrics.DENSITY_XHIGH,
                DisplayMetrics.DENSITY_HIGH,
                DisplayMetrics.DENSITY_TV
            )

            for (displayMetric in displayMetrics) {
                try {
                    val d = otherAppCtx.resources.getDrawableForDensity(
                        pi.applicationInfo.icon,
                        displayMetric,
                        context.theme
                    )
                    if (d != null) {
                        return d
                    }
                } catch (e: NotFoundException) {
                    Log.d("TAG", "NameNotFound for$packageName @ density: $displayMetric")
                    continue
                }
            }
        } catch (e: Exception) {
            // Handle Error here
        }

        val appInfo: ApplicationInfo?
        try {
            appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }

        return appInfo.loadIcon(pm)
    }


    fun getPackageData(appInfo: ApplicationInfo, context: Context, contentResolver: ContentResolver): Map<String, String> {
        val theMap: MutableMap<String, String> = HashMap()
        val pm: PackageManager = context.packageManager
        val intent = pm.getLaunchIntentForPackage(appInfo.packageName)

        val tmp = intent!!.component
        val ai = try {
            pm.getApplicationInfo(appInfo.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        val applicationName =
            (if (ai != null) pm.getApplicationLabel(ai) else "(unknown)") as String

        theMap["appName"] = applicationName
        theMap["componentInfo"] = tmp!!.className

        theMap["packageName"] = appInfo.packageName

        val android_id = Secure.getString(
            contentResolver,
            Secure.ANDROID_ID
        )

        theMap["androidId"] = android_id
        theMap["iconPack"] = context.packageName
        return theMap
    }



}