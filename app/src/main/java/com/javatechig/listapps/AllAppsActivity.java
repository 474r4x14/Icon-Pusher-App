package com.javatechig.listapps;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import org.apache.http.NameValuePair;
import android.provider.Settings.Secure;


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
		builder.setMessage(getString(R.string.send_data));
		
		
		builder.setPositiveButton("Know More", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				//Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://javatechig.com"));
				//startActivity(browserIntent);


				//PushData pushData = new PushData();
				//pushData.postData();
				Log.w("myApp", "about to pushdata");
				new PushData().execute("temp");
				Log.w("myApp", "finished pushdata");


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


		String android_id = Secure.getString(this.getContentResolver(),
				Secure.ANDROID_ID);

		Log.w("android id?!",android_id);

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

	private class PushData extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... urls) {

			Log.w("myApp", "about to do http");




//do this wherever you are wanting to POST
			URL url;
			HttpURLConnection conn;

			try{
//if you are using https, make sure to import java.net.HttpsURLConnection
				url=new URL("https://defenestrate.me/icons.php");

//you need to encode ONLY the values of the parameters
				String param="param1=" + URLEncoder.encode("value1","UTF-8")+
				"&param2="+URLEncoder.encode("value2","UTF-8")+
				"&param3="+URLEncoder.encode("value3","UTF-8");

				conn=(HttpURLConnection)url.openConnection();
//set the output to true, indicating you are outputting(uploading) POST data
				conn.setDoOutput(true);
//once you set the output to true, you don’t really need to set the request method to post, but I’m doing it anyway
				conn.setRequestMethod("POST");

//Android documentation suggested that you set the length of the data you are sending to the server, BUT
// do NOT specify this length in the header by using conn.setRequestProperty(“Content-Length”, length);
//use this instead.
				conn.setFixedLengthStreamingMode(param.getBytes().length);
				conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//send the POST out

				PrintWriter out = new PrintWriter(conn.getOutputStream());
				out.print(param);
				out.close();

//build the string to store the response text from the server
				String response= "";

//start listening to the stream

				Scanner inStream = new Scanner(conn.getInputStream());

//process the stream and store it in StringBuilder
				while(inStream.hasNextLine())
					response+=(inStream.nextLine());


				Log.w("myApp","response was " + response);
			}
//catch some error
			catch(MalformedURLException ex){
				//Toast.makeText(MyActivity.this, ex.toString(), 1 ).show();
				ex.getStackTrace();

			}
// and some more
			catch(IOException ex){
ex.getStackTrace();
				//Toast.makeText(MyActivity.this, ex.toString(), 1 ).show();
			}






			return null;

		}


		private String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException
		{
			StringBuilder result = new StringBuilder();
			boolean first = true;

			for (NameValuePair pair : params)
			{
				if (first)
					first = false;
				else
					result.append("&");

				result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
				result.append("=");
				result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
			}

			return result.toString();
		}






		protected void onProgressUpdate(Integer... progress) {
			//setProgressPercent(progress[0]);
		}

		protected void onPostExecute(Long result) {
			//showDialog("Downloaded " + result + " bytes");
		}
	}


}


