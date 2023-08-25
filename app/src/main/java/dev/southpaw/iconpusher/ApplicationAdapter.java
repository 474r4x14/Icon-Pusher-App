package dev.southpaw.iconpusher;

import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

public class ApplicationAdapter extends ArrayAdapter<Request> {
	private List<Request> appsList = null;
	private final Context context;
	private final PackageManager packageManager;

	public ApplicationAdapter(Context context, int textViewResourceId,
			List<Request> appsList) {
		super(context, textViewResourceId, appsList);
		this.context = context;
		this.appsList = appsList;
		packageManager = context.getPackageManager();

	}

	@Override
	public int getCount() {
		return ((null != appsList) ? appsList.size() : 0);
	}

	@Override
	public Request getItem(int position) {
		return ((null != appsList) ? appsList.get(position) : null);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (null == view) {
			LayoutInflater layoutInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = layoutInflater.inflate(R.layout.snippet_list_row, null);
		}
		final Request request = appsList.get(position);
		ApplicationInfo data = request.info;
		if (null != data) {
			TextView appName = (TextView) view.findViewById(R.id.app_name);
			TextView packageName = (TextView) view.findViewById(R.id.app_paackage);
			ImageView iconview = (ImageView) view.findViewById(R.id.app_icon);
			CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
			AppListerActivity.checkboxes.add(checkBox);
			appName.setText(data.loadLabel(packageManager));
			packageName.setText(data.packageName);
			iconview.setImageDrawable(data.loadIcon(packageManager));
			checkBox.setChecked(request.selected);
		}
		return view;
	}
};