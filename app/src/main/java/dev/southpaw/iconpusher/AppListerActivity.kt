package dev.southpaw.iconpusher

import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings.Secure
import android.text.Html
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.CheckBox
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import dev.southpaw.iconpusher.Misc.postData
import java.util.concurrent.Executors

class AppListerActivity : AppCompatActivity() {
    lateinit var pm: PackageManager
    private var applist = ArrayList<Request>()
    private var listadaptor: ApplicationAdapter? = null
    lateinit var progress: ProgressDialog
    private var progressDone = 0
    lateinit var mainListView: ListView
    private var checkAll = false
    private val pushList = ArrayList<Request>()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById<View>(R.id.toolbar) as Toolbar)

        pm = getPackageManager()


        val android_id = Secure.getString(
            this.contentResolver,
            Secure.ANDROID_ID
        )

        val androidIdTxt = findViewById<View>(R.id.android_id_txt) as TextView
        androidIdTxt.text = "Android ID: $android_id\nClick here to view on site"

        //new LoadApplications().execute();
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())


        progress = ProgressDialog.show(
            this@AppListerActivity, null,
            "Loading application info..."
        )

        executor.execute {
            //Background work here
            val tmpList =
                checkForLaunchIntent(pm.getInstalledApplications(PackageManager.GET_META_DATA))


            //List<ApplicationInfo> tmpList = checkForLaunchIntent(pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA)));
//            applist = ArrayList()
            for (i in tmpList.indices) {
                val request = Request()
                request.info = tmpList[i]
                applist.add(request)
            }

            listadaptor = ApplicationAdapter(
                this@AppListerActivity,
                R.layout.snippet_list_row, applist
            )
            handler.post {
                //UI Thread work here
                mainListView = findViewById(R.id.mainListView)
                mainListView.setAdapter(listadaptor)

                mainListView.setClickable(true)

                mainListView.setOnItemClickListener(OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
                    Log.w("GOT pos", "" + position)
                    val request = applist.get(position)
                    request.selected = !request.selected
                    listadaptor!!.notifyDataSetChanged()
                })
                progress.dismiss()
            }
        }
    }


    fun idClick(view: View?) {
        Log.w("doird", "meh")
        val android_id = Secure.getString(
            this.contentResolver,
            Secure.ANDROID_ID
        )
        val browserIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse("https://iconpusher.com/device/$android_id"))
        startActivity(browserIntent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var result = true

        when (item.itemId) {
            R.id.menu_info -> {
                displayAboutDialog()
            }

            R.id.menu_select_all -> {
                checkAll = !checkAll
                var i = 0
                while (i < applist!!.size) {
                    applist!![i].selected = checkAll
                    i++
                }
                listadaptor!!.notifyDataSetChanged()
            }

            else -> {
                result = super.onOptionsItemSelected(item)
            }
        }
        return result
    }


    private fun displayAboutDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.info_title))
        builder.setMessage(getString(R.string.info_data))

        builder.setNeutralButton("Privacy Policy") { dialog: DialogInterface?, id: Int -> displayPrivacyDialog() }

        builder.setNegativeButton("Close") { dialog: DialogInterface, id: Int -> dialog.cancel() }
        builder.show()
    }


    private fun displayPrivacyDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.privacy_title))
        builder.setMessage(getString(R.string.info_data))
        builder.setMessage(
            Html.fromHtml(
                getString(R.string.privacy_data_1) + "<br/><br/><b>Data collected</b><br/>" + getString(
                    R.string.privacy_data_2
                ) + "<br/><br/>" +
                        "<b>What happens to the data</b><br/>" + getString(R.string.privacy_data_3) + "<br/><br/>" +
                        getString(R.string.privacy_data_4)
            )
        )

        builder.setNegativeButton("Close") { dialog: DialogInterface, id: Int -> dialog.cancel() }
        builder.show()
    }


    fun sendTest(view: View?) {
        displaySendDialog()
    }


    private fun displaySendDialog() {
        progressDone = 0
        // Let's build the request list
        pushList.clear()
        for (i in applist!!.indices) {
            val request = applist!![i]
            if (request.selected) {
                pushList.add(request)
            }
        }

        if (pushList.isEmpty()) {
            Toast.makeText(this, "Please select at least one app", Toast.LENGTH_SHORT).show()

            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.send_title))
        builder.setMessage("Do you want to push these " + pushList.size + " apps?")
        builder.setCancelable(false)

        progress = ProgressDialog(this)
        builder.setPositiveButton("Submit", DialogInterface.OnClickListener { dialog, id ->
            Log.w("myApp", "about to pushdata")
            progress!!.setCancelable(false)

            progress!!.setMessage("Sending App Details")
            progress!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progress!!.progress = 0
            progress!!.max = pushList.size

            isRunning = true


            progress!!.setButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
                isRunning = false
                return@OnClickListener
            })

            val mThread = Thread {
                for (i in pushList.indices) {
                    if (!isRunning) {
                        break
                    }
                    val info = pushList[i].info
                    //                        new PushData().execute(info);
                    val executor = Executors.newSingleThreadExecutor()
                    val handler = Handler(Looper.getMainLooper())

                    if (info == null) {
                        continue
                    }
                    executor.execute {
                        //Background work here
                        postData(info, applicationContext, contentResolver)
                        handler.post {
                            //UI Thread work here
                            if (progress != null) {
                                progressDone++
                                progress!!.progress = progressDone
                                if (progressDone == pushList.size) {
                                    progress!!.dismiss()
                                }
                            }
                        }
                    }
                }
            }

            progress!!.show()
            mThread.start()

            Log.w("myApp", "finished pushdata")


            for (i in applist!!.indices) {
                applist!![i].selected = false
            }
            listadaptor!!.notifyDataSetChanged()
            dialog.cancel()
        })
        builder.setNegativeButton("Cancel") { dialog: DialogInterface, id: Int -> dialog.cancel() }

        builder.show()
    }


    private fun checkForLaunchIntent(list: List<ApplicationInfo>): List<ApplicationInfo> {
        val applist = ArrayList<ApplicationInfo>()
        for (info in list) {
            try {
                if (null != pm!!.getLaunchIntentForPackage(info.packageName)) {
                    applist.add(info)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        applist.sortWith(java.util.Comparator { lhs, rhs ->
            var ai1 = try {
                pm!!.getApplicationInfo(lhs.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
            var ai2 = try {
                pm!!.getApplicationInfo(rhs.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
            val lhsName =
                (if (ai1 != null) pm!!.getApplicationLabel(ai1) else "(unknown)") as String
            val rhsName =
                (if (ai2 != null) pm!!.getApplicationLabel(ai2) else "(unknown)") as String
            lhsName.compareTo(rhsName, ignoreCase = true)
        })


        return applist
    }

    companion object {
        @JvmField
		var checkboxes: ArrayList<CheckBox> = ArrayList()

        var isRunning: Boolean = true
    }
}
