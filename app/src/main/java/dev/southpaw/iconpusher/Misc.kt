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
import com.google.gson.Gson
import org.apache.commons.io.FileUtils
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

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

    fun postData(info: ApplicationInfo, context: Context, contentResolver: ContentResolver)
    {


        Log.w("myApp", "about to do http")


        val url: URL


        var conn: HttpURLConnection? = null
        var dos: DataOutputStream? = null
        var inStream: DataInputStream? = null

        try {
            val appInfo: ApplicationInfo = info
            val map = HashMap<String,String>()
            map.putAll(getPackageData(appInfo, context, contentResolver))

            val pm: PackageManager = context.getPackageManager()
            val pi = pm.getPackageInfo(appInfo.packageName, 0)

            //if you are using https, make sure to import java.net.HttpsURLConnection
            url = URL("https://api.iconpusher.com/package/" + appInfo.packageName)


            //you need to encode ONLY the values of the parameters
            map["version"] = pi.versionName

            val icon = getIconFromPackageName(appInfo.packageName, context)
            val bitmap = drawableToBitmap(context, icon)


            //create a file to write bitmap data
            val file: File = File(context.getCacheDir(), "icon")
            file.createNewFile()

            //Convert bitmap to byte array
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 0,  /*ignored for PNG*/bos)
            val bitmapdata = bos.toByteArray()

            //write the bytes in file
            val fos = FileOutputStream(file)
            fos.write(bitmapdata)
            fos.flush()
            fos.close()


            //------------------ CLIENT REQUEST
//                    FileInputStream fileInputStream = new FileInputStream(file);
            // open a URL connection to the Servlet
            // Open a HTTP connection to the URL
            conn = url.openConnection() as HttpURLConnection
            // Allow Inputs
            conn!!.doInput = true
            // Allow Outputs
            conn!!.doOutput = true
            // Don't use a cached copy.
            conn!!.useCaches = false
            // Use a post method.
            conn!!.requestMethod = "POST"
            conn!!.setRequestProperty("Connection", "Keep-Alive")
            conn!!.setRequestProperty("Content-Type", "application/json")

            // convert the image to base64
            val fileContent = FileUtils.readFileToByteArray(file)
            val encodedString = Base64.getEncoder().encodeToString(fileContent)

            map["icon"] = encodedString

            dos = DataOutputStream(conn!!.outputStream)
            val gson = Gson()
            dos.writeBytes(gson.toJson(map))


            //                    fileInputStream.close();
            dos.flush()
            dos.close()
        } catch (ex: IOException) {
            Log.e("Debug", "error: " + ex.message, ex)
        } catch (ex: PackageManager.NameNotFoundException) {
            Log.e("Debug", "error: " + ex.message, ex)
        } catch (ex: NullPointerException) {
            Log.e("Debug", "error: " + ex.message, ex)
        }

        //------------------ read the SERVER RESPONSE
        try {
            inStream = DataInputStream(conn!!.inputStream)
            var str: String
            while ((inStream.readLine().also { str = it }) != null) {
                Log.e("Debug", "Server Response $str")
            }
            inStream.close()
        } catch (ioex: IOException) {
            Log.e("Debug", "error: " + ioex.message, ioex)
        }
    }

}