package com.defenestrateme.pusher;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class AppListerActivity extends AppCompatActivity {
	private PackageManager packageManager = null;
	private List<Request> applist = null;
	private ApplicationAdapter listadaptor = null;
	private ProgressDialog progress;
	private Integer progressDone = 0;
	public ListView mainListView;
	public static ArrayList<CheckBox> checkboxes = new ArrayList<>();

	private Boolean checkAll = false;
	private ArrayList<Request> pushList = new ArrayList<>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

		packageManager = getPackageManager();


		String android_id = Secure.getString(this.getContentResolver(),
			Secure.ANDROID_ID);

		TextView androidIdTxt = (TextView) findViewById(R.id.android_id_txt);
		androidIdTxt.setText("Android ID: "+android_id+"\nClick here to view on site");


//		mainListView = (ListView) findViewById(R.id.mainListView);
//		mainListView.setClickable(true);






		new LoadApplications().execute();
	}



	public void idClick(View view)
	{
		Log.w("doird","meh");
		String android_id = Secure.getString(this.getContentResolver(),
			Secure.ANDROID_ID);
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://icons.defenestrate.me/device/"+android_id));
		startActivity(browserIntent);
	}
/*
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);

		return true;
	}
*/

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}


	public boolean onOptionsItemSelected(MenuItem item) {
		boolean result = true;

		switch (item.getItemId()) {
			case R.id.menu_select_all: {
				checkAll = !checkAll;
				for (int i = 0; i < applist.size(); i++) {
					applist.get(i).selected = checkAll;
				}
				listadaptor.notifyDataSetChanged();
				break;
			}
			default: {
				result = super.onOptionsItemSelected(item);

				break;
			}
		}

		return result;
	}


	public void sendTest(View view)
	{
		displaySendDialog();
	}

	private void displaySendDialog() {

		// Let's build the request list
		pushList.clear();
		for (int i=0; i < applist.size(); i++) {
			Request request = applist.get(i);
			if (request.selected) {
				pushList.add(request);
			}
		}

		if (pushList.size() == 0) {
			Toast.makeText(this, "Please select at least one app", Toast.LENGTH_SHORT ).show();

			return;
		}

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.send_title));
		builder.setMessage("Do you want to push these "+pushList.size()+" apps?");

		progress=new ProgressDialog(this);
		builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				//Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://javatechig.com"));
				//startActivity(browserIntent);


				//PushData pushData = new PushData();
				//pushData.postData();
				Log.w("myApp", "about to pushdata");

				//Map<String, String> map = new HashMap<>();


				progress.setMessage("Sending App Details");
				progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				progress.setProgress(0);
				progress.setMax(pushList.size());
				progress.show();

				Thread mThread = new Thread() {
					@Override
					public void run() {
						for (int i = 0; i < pushList.size(); i++) {
							ApplicationInfo info = pushList.get(i).info;
							//System.out.println(applist.get(i));
							Map appData = getPackageData(info);
							new PushData().execute(appData);
							//progress.setProgress(i);
						}
						//progress.dismiss();
					}
				};
				mThread.start();

				//map.put("name", "demo");
				//map.put("fname", "fdemo");



				//new PushData().execute(map);
				Log.w("myApp", "finished pushdata");


				dialog.cancel();
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		 
		builder.show();
	}


	private Map getPackageData(ApplicationInfo appInfo) {
		Map theMap = new HashMap<>();
		final PackageManager pm = getApplicationContext().getPackageManager();
		Intent intent=pm.getLaunchIntentForPackage(appInfo.packageName);

		ComponentName tmp = intent.getComponent();

		ApplicationInfo ai;
		try {
			ai = pm.getApplicationInfo( appInfo.packageName, 0);
		} catch (final PackageManager.NameNotFoundException e) {
			ai = null;
		}
		final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");

		theMap.put("appName", applicationName);
		theMap.put("componentInfo", tmp.getClassName());

		theMap.put("packageName", appInfo.packageName);

		String android_id = Secure.getString(this.getContentResolver(),
				Secure.ANDROID_ID);

		theMap.put("android_id", android_id);
		return theMap;
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


	public void itemClicked(View view)
	{
		Log.w("clkd","??");
	}


	private class LoadApplications extends AsyncTask<Void, Void, Void> {
		private ProgressDialog progress = null;

		@Override
		protected Void doInBackground(Void... params) {
			List<ApplicationInfo> tmpList = checkForLaunchIntent(packageManager.getInstalledApplications(PackageManager.GET_META_DATA));


			applist = new ArrayList<>();
			for (Integer i = 0; i < tmpList.size(); i++) {
				Request request = new Request();
				request.info = tmpList.get(i);
				applist.add(request);

			}

			listadaptor = new ApplicationAdapter(AppListerActivity.this,
					R.layout.snippet_list_row, applist);

			return null;
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(Void result) {
//			setListAdapter(listadaptor);
			mainListView = (ListView) findViewById(R.id.mainListView);
			mainListView.setAdapter(listadaptor);

			mainListView.setClickable(true);
//			mainListView.setItemsCanFocus(false);



			mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				public void onItemClick (final AdapterView<?> parent, View view, int position, long id) {
//					final String item = (String) parent.getItemAtPosition(position);
					Log.w("GOT pos",""+position);
					Request request = applist.get(position);
					request.selected = !request.selected;
					listadaptor.notifyDataSetChanged();
//					Log.w("GOT",request.info.name);
				} });




			progress.dismiss();
			super.onPostExecute(result);
		}

		@Override
		protected void onPreExecute() {
			progress = ProgressDialog.show(AppListerActivity.this, null,
					"Loading application info...");
			super.onPreExecute();
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}
	}




	private class PushData extends AsyncTask<Map<String,String>, Void, Boolean> {
		@Override
		//protected Void doInBackground(Map... mapVals) {
		protected Boolean doInBackground(Map<String,String>... maps) {
			/*
			//int count = urls.length;
			long totalSize = 0;
			for (int i = 0; i < count; i++) {
				totalSize += Downloader.downloadFile(urls[i]);
				publishProgress((int) ((i / (float) count) * 100));
				// Escape early if cancel() is called
				if (isCancelled()) break;
			}
			return totalSize;
			*/



			int count = maps.length;
			//long totalSize = 0;
			for (int i = 0; i < count; i++) {
				//totalSize += Downloader.downloadFile(mapVals[i]);
				//publishProgress((int) ((i / (float) count) * 100));
				// Escape early if cancel() is called
				//if (isCancelled()) break;










				Log.w("myApp", "about to do http");










	//do this wherever you are wanting to POST
				URL url;
				HttpURLConnection conn;

				try{
	//if you are using https, make sure to import java.net.HttpsURLConnection
					url=new URL("https://icons.defenestrate.me/push");

	//you need to encode ONLY the values of the parameters
					/*
					String param="param1=" + URLEncoder.encode("value1","UTF-8")+
					"&param2="+URLEncoder.encode("value2","UTF-8")+
					"&param3="+URLEncoder.encode("value3","UTF-8");
	*/
					String param = "";
					for (Map.Entry<String, String> entry : maps[i].entrySet())
					{
						//System.out.println(entry.getKey() + "/" + entry.getValue());

						if (param != "") {
							param += "&";
						}

						param += entry.getKey()+"="+URLEncoder.encode(entry.getValue(),"UTF-8");
					}







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




			}























			return null;

		}

		/*
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
		*/





		protected void onProgressUpdate(Integer... progress) {
			//setProgressPercent(progress[0]);
		}

		//@Override
		protected void onPostExecute(Boolean result) {
			//showDialog("Downloaded " + result + " bytes");
			if (progress != null) {
				progressDone++;
				progress.setProgress(progressDone);
				if (progressDone == pushList.size()) {
					progress.dismiss();
				}
			}
		}
	}


}


