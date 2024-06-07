package dev.southpaw.iconpusher

import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView

class ApplicationAdapter(
    private val context: Context, textViewResourceId: Int,
    val appsList: List<Request>
) : ArrayAdapter<Request?>(
    context, textViewResourceId, appsList!!
) {
//    private var appsList: List<Request>? = null
    private val packageManager: PackageManager

    init {
//        this.appsList = appsList
        packageManager = context.packageManager
    }

    override fun getCount(): Int {
        return (if ((null != appsList)) appsList.size else 0)
    }

    override fun getItem(position: Int): Request? {
        return (if ((null != appsList)) appsList.get(position) else null)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (null == view) {
            val layoutInflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = layoutInflater.inflate(R.layout.snippet_list_row, null)
        }
        val request = appsList!![position]
        val data = request.info
        if (null != data) {
            val appName = view!!.findViewById<View>(R.id.app_name) as TextView
            val packageName = view.findViewById<View>(R.id.app_paackage) as TextView
            val iconview = view.findViewById<View>(R.id.app_icon) as ImageView
            val checkBox = view.findViewById<View>(R.id.checkbox) as CheckBox
            AppListerActivity.checkboxes.add(checkBox)
            appName.text = data.loadLabel(packageManager)
            packageName.text = data.packageName
            iconview.setImageDrawable(data.loadIcon(packageManager))
            checkBox.isChecked = request.selected
        }
        return view!!
    }
}