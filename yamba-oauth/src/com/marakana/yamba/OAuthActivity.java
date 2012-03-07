package com.marakana.yamba;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import winterwell.jtwitter.OAuthSignpostClient;
import winterwell.jtwitter.Twitter;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class OAuthActivity extends Activity {
	static final String TAG = "OAuthActivity";
	static final String OAUTH_KEY = "1csHpu9jAh9XB41E210A";
	static final String OAUTH_SECRET = "7QuUrV43ULb4Pevtaly9RJqNKU6khLQpdtWGmT8c";
	static final String OAUTH_CALLBACK_SCHEME = "x-marakana-yamba-oauth-twitter";
	static final String OAUTH_CALLBACK_URL = OAUTH_CALLBACK_SCHEME
			+ "://callback";

	YambaApp yamba;
	private String username;
	private OAuthSignpostClient oauthClient;
	private OAuthConsumer consumer;
	private OAuthProvider provider;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		yamba = (YambaApp)getApplication();

		consumer = new CommonsHttpOAuthConsumer(OAUTH_KEY, OAUTH_SECRET);
		provider = new DefaultOAuthProvider(
				"https://api.twitter.com/oauth/request_token",
				"https://api.twitter.com/oauth/access_token",
				"https://api.twitter.com/oauth/authorize");

		// Read the prefs to see if we have token
		yamba.prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		String username = yamba.prefs.getString("username", "");
		String token = yamba.prefs.getString("token", null);
		String tokenSecret = yamba.prefs.getString("tokenSecret", null);
		if (token != null && tokenSecret != null) {
			// We have token, use it
			consumer.setTokenWithSecret(token, tokenSecret);
			// Make a Twitter object
			oauthClient = new OAuthSignpostClient(OAUTH_KEY, OAUTH_SECRET,
					token, tokenSecret);
			yamba.twitter = new Twitter(username, oauthClient);
		}
		
		setContentView(R.layout.oauth);
		TextView usernameText = (TextView) findViewById(R.id.username);
		usernameText.setText(yamba.prefs.getString("username", ""));

		Button authorizeButton = (Button) findViewById(R.id.authorize_button);
		authorizeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
//				username = ((EditText) layout.findViewById(R.id.username))
//						.getText().toString();
				new OAuthAuthorizeTask().execute();
			}
		});

	}

	
	/*
	 * Callback once we are done with the authorization of this app with
	 * Twitter.
	 */
	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.d(TAG, "intent: " + intent);

		// Check if this is a callback from OAuth
		Uri uri = intent.getData();
		if (uri != null && uri.getScheme().equals(OAUTH_CALLBACK_SCHEME)) {
			Log.d(TAG, "callback: " + uri.getPath());

			String verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);
			Log.d(TAG, "verifier: " + verifier);

			new RetrieveAccessTokenTask().execute(verifier);
		} else {
			Log.d(TAG, "NO callback nor verifier: " + uri.getPath());

		}
	}

	/* Responsible for starting the Twitter authorization */
	class OAuthAuthorizeTask extends AsyncTask<Void, Void, String> {

		@Override
		protected String doInBackground(Void... params) {
			String authUrl;
			String message = null;
			try {
				authUrl = provider.retrieveRequestToken(consumer,
						OAUTH_CALLBACK_URL);
				Intent intent = new Intent(Intent.ACTION_VIEW,
						Uri.parse(authUrl));
				startActivity(intent);
			} catch (OAuthMessageSignerException e) {
				message = "OAuthMessageSignerException";
				Log.e(TAG, message, e);
			} catch (OAuthNotAuthorizedException e) {
				message = "OAuthNotAuthorizedException";
				Log.e(TAG, message, e);
			} catch (OAuthExpectationFailedException e) {
				message = "OAuthExpectationFailedException";
				Log.e(TAG, message, e);
			} catch (OAuthCommunicationException e) {
				message = "OAuthCommunicationException";
				Log.e(TAG, message, e);
			}
			return message;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if (result != null) {
				Toast.makeText(OAuthActivity.this, result, Toast.LENGTH_LONG).show();
			}
		}
	}

	/* Responsible for retrieving access tokens from twitter */
	class RetrieveAccessTokenTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			String message = null;
			String verifier = params[0];
			try {
				// Get the token
				Log.d(TAG, "mConsumer: " + consumer);
				Log.d(TAG, "mProvider: " + provider);
				provider.retrieveAccessToken(consumer, verifier);
				String token = consumer.getToken();
				String tokenSecret = consumer.getTokenSecret();
				consumer.setTokenWithSecret(token, tokenSecret);

				Log.d(TAG, String.format(
						"verifier: %s, token: %s, tokenSecret: %s", verifier,
						token, tokenSecret));

				// Store token in prefs
				yamba.prefs.edit().putString("username", username)
						.putString("token", token)
						.putString("tokenSecret", tokenSecret).commit();

				// Make a Twitter object
				oauthClient = new OAuthSignpostClient(OAUTH_KEY, OAUTH_SECRET,
						token, tokenSecret);
				yamba.twitter = new Twitter(username, oauthClient);

				Log.d(TAG, "token: " + token);

				message = "Successfully authorized with Twitter";
				
				// Start main activity
				startActivity( new Intent(OAuthActivity.this, MainActivity.class));
			} catch (OAuthMessageSignerException e) {
				message = "OAuthMessageSignerException";
				e.printStackTrace();
			} catch (OAuthNotAuthorizedException e) {
				message = "OAuthNotAuthorizedException";
				e.printStackTrace();
			} catch (OAuthExpectationFailedException e) {
				message = "OAuthExpectationFailedException";
				e.printStackTrace();
			} catch (OAuthCommunicationException e) {
				message = "OAuthCommunicationException";
				e.printStackTrace();
			}
			return message;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if (result != null) {
				Toast.makeText(OAuthActivity.this, result, Toast.LENGTH_LONG).show();
			}
		}
	}

}
