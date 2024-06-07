package dev.southpaw.iconpusher;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings.Secure;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


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

		//new LoadApplications().execute();

		ExecutorService executor = Executors.newSingleThreadExecutor();
		Handler handler = new Handler(Looper.getMainLooper());


		progress = ProgressDialog.show(AppListerActivity.this, null,
				"Loading application info...");

		executor.execute(() -> {

            //Background work here
            List<ApplicationInfo> tmpList = checkForLaunchIntent(packageManager.getInstalledApplications(PackageManager.GET_META_DATA));
            //List<ApplicationInfo> tmpList = checkForLaunchIntent(packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA)));


            applist = new ArrayList<>();
            for (int i = 0; i < tmpList.size(); i++) {
                Request request = new Request();
                request.info = tmpList.get(i);
                applist.add(request);
            }

            listadaptor = new ApplicationAdapter(
                    AppListerActivity.this,
                    R.layout.snippet_list_row, applist
            );

            handler.post(() -> {
				//UI Thread work here
				mainListView = findViewById(R.id.mainListView);
				mainListView.setAdapter(listadaptor);

				mainListView.setClickable(true);

				mainListView.setOnItemClickListener((parent, view, position, id) -> {
					Log.w("GOT pos",""+position);
					Request request = applist.get(position);
					request.selected = !request.selected;
					listadaptor.notifyDataSetChanged();
				});
				progress.dismiss();
			});
        });
	}


	public void idClick(View view)
	{
		Log.w("doird","meh");
		String android_id = Secure.getString(this.getContentResolver(),
			Secure.ANDROID_ID);
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://iconpusher.com/device/"+android_id));
		startActivity(browserIntent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}


	public boolean onOptionsItemSelected(MenuItem item) {
		boolean result = true;

		switch (item.getItemId()) {
			case R.id.menu_info: {
				displayAboutDialog();
				break;
			}
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


	private void displayAboutDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.info_title));
		builder.setMessage(getString(R.string.info_data));

		builder.setNeutralButton("Privacy Policy", (dialog, id) -> displayPrivacyDialog());

		builder.setNegativeButton("Close", (dialog, id) -> dialog.cancel());
		builder.show();
	}


	private void displayPrivacyDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.privacy_title));
		builder.setMessage(getString(R.string.info_data));
		builder.setMessage(Html.fromHtml(getString(R.string.privacy_data_1)+"<br/><br/><b>Data collected</b><br/>"+ getString(R.string.privacy_data_2)+"<br/><br/>" +
				"<b>What happens to the data</b><br/>"+getString(R.string.privacy_data_3)+"<br/><br/>" +
				getString(R.string.privacy_data_4)));

		builder.setNegativeButton("Close", (dialog, id) -> dialog.cancel());
		builder.show();
	}




	public void sendTest(View view)
	{
		displaySendDialog();
	}


	static Boolean isRunning = true;


	private void displaySendDialog() {

		progressDone = 0;
		// Let's build the request list
		pushList.clear();
		for (int i=0; i < applist.size(); i++) {
			Request request = applist.get(i);
			if (request.selected) {
				pushList.add(request);
			}
		}

		if (pushList.isEmpty()) {
			Toast.makeText(this, "Please select at least one app", Toast.LENGTH_SHORT ).show();

			return;
		}

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.send_title));
		builder.setMessage("Do you want to push these "+pushList.size()+" apps?");
		builder.setCancelable(false);

		progress=new ProgressDialog(this);
		builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {

				Log.w("myApp", "about to pushdata");

				progress.setCancelable(false);

				progress.setMessage("Sending App Details");
				progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				progress.setProgress(0);
				progress.setMax(pushList.size());

				isRunning = true;


				progress.setButton("Cancel", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						isRunning = false;
						return;
					}
				});

				final Thread mThread = new Thread(() -> {
                    for (int i = 0; i < pushList.size(); i++) {
                        if (!isRunning) {
                            break;
                        }
                        ApplicationInfo info = pushList.get(i).info;
//                        new PushData().execute(info);
						ExecutorService executor = Executors.newSingleThreadExecutor();
						Handler handler = new Handler(Looper.getMainLooper());

						if (info == null) {
							continue;
						}
						executor.execute(() -> {

                            //Background work here
							Misc.INSTANCE.postData(info, getApplicationContext(), getContentResolver());

                            handler.post(() -> {
								//UI Thread work here
								if (progress != null) {
									progressDone++;
									progress.setProgress(progressDone);
									if (progressDone == pushList.size()) {
										progress.dismiss();
									}
								}
							});
                        });
                    }
                });

				progress.show();
				mThread.start();

				Log.w("myApp", "finished pushdata");


				for (int i=0; i < applist.size(); i++) {
					applist.get(i).selected = false;
				}
				listadaptor.notifyDataSetChanged();

				dialog.cancel();
			}
		});
		builder.setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());
		 
		builder.show();
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


		applist.sort(new Comparator<ApplicationInfo>() {
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
}
