package com.plinio.skoobunofficial;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class LoginActivity extends Activity {

	static final private String LOG_TAG = "LoginActivity";
	static final private String LOGIN_URL = "http://www.skoob.com.br/login";
	static final private String LOGOUT_URL = "http://www.skoob.com.br/login/sair/";
	private int TIMEOUT_CONNECTION = 5000;
	private int TIMEOUT_REQUEST = 10000;

	private boolean mStop = false;
	private HttpClient mHttpClient;
	private ProgressDialog mProgressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_activity);

		// set http connection parameters
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams,
				TIMEOUT_CONNECTION);
		HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_REQUEST);

		// create HTTP request
		mHttpClient = new DefaultHttpClient();
		
		// create progress dialog
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setCanceledOnTouchOutside(false);
	}

	/*@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}*/

	@Override
	protected void onResume() {
		super.onResume();

		mStop = false;
	}

	@Override
	protected void onPause() {
		super.onPause();

		mStop = true;
	}

	public void onClickLogin(View v) {
		mProgressDialog.setMessage(getString(R.string.logging_in));
		mProgressDialog.show();

		String login = ((EditText) findViewById(R.id.editTextLogin)).getText()
				.toString();
		String password = ((EditText) findViewById(R.id.editTextPassword))
				.getText().toString();

		try {
			login(login, password);
		} catch (Exception ex) {
			mProgressDialog.cancel();
			ex.printStackTrace();
		}
	}

	public void onClickLogout(View v) {
		mProgressDialog.setMessage(getString(R.string.logging_out));
		mProgressDialog.show();
		try {
			logout();
		} catch (Exception ex) {
			mProgressDialog.cancel();
			ex.printStackTrace();
		}
	}

	private void login(String login, String password)
			throws UnsupportedEncodingException {

		HttpPost httpPost = new HttpPost(LOGIN_URL);
		// Add post data
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs
				.add(new BasicNameValuePair("data[Usuario][email]", login));
		nameValuePairs.add(new BasicNameValuePair("data[Usuario][senha]",
				password));
		nameValuePairs.add(new BasicNameValuePair("data[Login][automatico]",
				"true"));
		httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

		new HttpGuy().execute(httpPost);
	}

	private void logout() throws UnsupportedEncodingException {
		HttpGet httpGet = new HttpGet(LOGOUT_URL);
		new HttpGuy().execute(httpGet);
	}

	class HttpGuy extends AsyncTask<HttpUriRequest, Integer, Integer> {

		private String mContent;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Integer doInBackground(HttpUriRequest... params) {
			int error = 0;
			if (params.length != 1)
				return -1;

			HttpUriRequest httpRequest = params[0];

			try {
				// execute HTTP request
				Log.d(LOG_TAG, "requesting:");
				Log.d(LOG_TAG, httpRequest.getURI().toString());
				HttpResponse response = mHttpClient.execute(httpRequest);

				// get response
				StatusLine statusLine = response.getStatusLine();
				HttpEntity entity = response.getEntity();

				// check response's status
				if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
					// convert content from stream to string
					mContent = streamToString(entity.getContent());
				}

				return statusLine.getStatusCode();

			} catch (SocketTimeoutException toEx) {
				Log.e(LOG_TAG, "TIMEOUT");
				toEx.printStackTrace();
				error = 1;
			} catch (IOException ioEx) {
				if (mStop) {
					error = 0;
				} else {
					Log.e(LOG_TAG, "IO ERROR");
					ioEx.printStackTrace();
					error = 2;
				}
			} catch (Exception ex) {
				Log.e(LOG_TAG, "GENERIC ERROR");
				ex.printStackTrace();
				error = 2;
			}

			return error;
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			Log.d(LOG_TAG, "result = " + result);

			String name = extractName(mContent);
			Log.i(LOG_TAG, "Name: " + name);

			TextView tv = (TextView) findViewById(R.id.textViewInstructions);
			if (name.trim().length() != 0) {
				tv.setText(getString(R.string.loged_as) + " " + name);
			} else {
				tv.setText(getString(R.string.instructions_login));
			}

			// Log.i(LOG_TAG, "--- START CONTENT ---");
			// while (mContent.length() > 50) {
			// Log.i(LOG_TAG, mContent.substring(0, 50));
			// mContent = mContent.substring(50);
			// }
			// Log.i(LOG_TAG, mContent);
			// Log.i(LOG_TAG, "--- END CONTENT ---");

			mProgressDialog.cancel();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
		}

		// Função auxiliar para passar o conteúdo de um InputStream para String

		private String streamToString(InputStream inputStream)
				throws IOException {
			byte[] bytes = new byte[512];
			int count;
			long countTotal = 0;
			int MAX_PAGE_SIZE = 100000; // entity.getContentLength();
			// int total = 20000; // entity.getContentLength();
			String endTag = "configurações</a>";
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			while ((count = inputStream.read(bytes)) > 0) {
				countTotal += count;
				baos.write(bytes, 0, count);
				// set progress
				// refresher.updateProgress((int) (10000 * countTotal / total));
				// Log.d(LOG_TAG, "read: " + countTotal);
				if (countTotal > MAX_PAGE_SIZE) {
					break;
				}
				if (mStop) {
					break;
				}
				if (new String(baos.toByteArray()).contains(endTag)) {
					break;
				}
			}
			// return new String(baos.toByteArray(), "iso-8859-1");
			return new String(baos.toByteArray());
		}
	}

	public String extractName(String mContent) {

		String tagPerfil = "<div id=\"meu_perfil\"";

		if (mContent.contains(tagPerfil)) {
			int cutIndex = mContent.indexOf(tagPerfil) + tagPerfil.length();
			mContent = mContent.substring(cutIndex);
		} else {
			return "";
		}

		if (mContent.contains("<div")) {
			int cutIndex = mContent.indexOf("<div") + 4;
			mContent = mContent.substring(cutIndex);
		} else {
			return "";
		}

		if (mContent.contains("<div")) {
			int cutIndex = mContent.indexOf("<div") + 4;
			mContent = mContent.substring(cutIndex);
		} else {
			return "";
		}

		if (mContent.contains(">")) {
			int cutIndex = mContent.indexOf(">") + 1;
			mContent = mContent.substring(cutIndex);
		} else {
			return "";
		}

		if (mContent.contains("<")) {
			int cutIndex = mContent.indexOf("<");
			return mContent.substring(0, cutIndex);
		} else {
			return "";
		}
	}
}
