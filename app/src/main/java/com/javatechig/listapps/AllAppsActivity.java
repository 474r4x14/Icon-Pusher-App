package com.javatechig.listapps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class AllAppsActivity extends ListActivity {
	private PackageManager packageManager = null;
	private List<ApplicationInfo> applist = null;
	private ApplicationAdapter listadaptor = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		packageManager = getPackageManager();

		new LoadApplications().execute();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		boolean result = true;

		switch (item.getItemId()) {
		case R.id.menu_about: {
			displayAboutDialog();

			break;
		}
		default: {
			result = super.onOptionsItemSelected(item);

			break;
		}
		}

		return result;
	}

	private void displayAboutDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.about_title));
		builder.setMessage(getString(R.string.about_desc));
		
		
		builder.setPositiveButton("Know More", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://javatechig.com"));
				startActivity(browserIntent);
				dialog.cancel();
			}
		});
		builder.setNegativeButton("No Thanks!", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		 
		builder.show();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		ApplicationInfo app = applist.get(position);


		final PackageManager pm = getApplicationContext().getPackageManager();
		Intent intent=pm.getLaunchIntentForPackage(app.packageName);

		ComponentName tmp = intent.getComponent();
		String tmp10 = tmp.getClassName();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String tmp1 = app.packageName + "!!!";

		builder.setMessage(tmp1 + "\n\n" + tmp10 + "\n")
				.setPositiveButton(R.string.fire, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// FIRE ZE MISSILES!
					}
				})
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// User cancelled the dialog
					}
				});
		// Create the AlertDialog object and return it
		//return builder.create();
		builder.create().show();


		/*
		ApplicationInfo app = applist.get(position);
		try {
			Intent intent = packageManager
					.getLaunchIntentForPackage(app.packageName);

			if (null != intent) {
				startActivity(intent);
			}
		} catch (ActivityNotFoundException e) {
			Toast.makeText(AllAppsActivity.this, e.getMessage(),
					Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			Toast.makeText(AllAppsActivity.this, e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
		*/

	}

	private List<ApplicationInfo> checkForLaunchIntent(List<ApplicationInfo> list) {
		ArrayList<ApplicationInfo> applist = new ArrayList<ApplicationInfo>();
		for (ApplicationInfo info : list) {
			try {
				if (null != packageManager.getLaunchIntentForPackage(info.packageName)) {
					applist.add(info);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}


		Collections.sort(applist, new Comparator<ApplicationInfo>()
		{
			@Override
			public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {

				ApplicationInfo ai1;
				ApplicationInfo ai2;
				try {
					ai1 = packageManager.getApplicationInfo(lhs.packageName, 0);
				} catch (final PackageManager.NameNotFoundException e) {
					ai1 = null;
				}
				try {
					ai2 = packageManager.getApplicationInfo(rhs.packageName, 0);
				} catch (final PackageManager.NameNotFoundException e) {
					ai2 = null;
				}
				final String lhsName = (String) (ai1 != null ? packageManager.getApplicationLabel(ai1) : "(unknown)");
				final String rhsName = (String) (ai2 != null ? packageManager.getApplicationLabel(ai2) : "(unknown)");

				return lhsName.compareToIgnoreCase(rhsName);
			}
		});


		return applist;
	}

	private class LoadApplications extends AsyncTask<Void, Void, Void> {
		private ProgressDialog progress = null;

		@Override
		protected Void doInBackground(Void... params) {
			applist = checkForLaunchIntent(packageManager.getInstalledApplications(PackageManager.GET_META_DATA));
			listadaptor = new ApplicationAdapter(AllAppsActivity.this,
					R.layout.snippet_list_row, applist);

			return null;
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(Void result) {
			setListAdapter(listadaptor);
			progress.dismiss();
			super.onPostExecute(result);
		}

		@Override
		protected void onPreExecute() {
			progress = ProgressDialog.show(AllAppsActivity.this, null,
					"Loading application info...");
			super.onPreExecute();
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}
	}
}