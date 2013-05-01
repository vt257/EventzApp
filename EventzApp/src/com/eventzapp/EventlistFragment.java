package com.eventzapp;

import java.io.IOException;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eventzapp.userendpoint.Userendpoint;
import com.eventzapp.userendpoint.model.User;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson2.JacksonFactory;

public class EventlistFragment extends Fragment {
	private UiLifecycleHelper uiHelper;
	private static final int REAUTH_ACTIVITY_CODE = 100;

	// Some constants for the request to get basic user data
	// TODO consider using batchFB on the back-end after having
	// sent the offline_permissions access token and the user_id
	// to the backend
	// TODO need to get fiendids and likeids with this same request
	// batchFB also on the frontend?
	private static final String MEREQUESTPATH = "/me";
	// TODO need to get more fields, e.g. location
	// TODO will need to ask for the extra permissions right after
	// login required for getting all the data
	private static final String MEREQUESTFIELDS = "id,location";
	private static final String FIELDS_KEY = "fields";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		uiHelper = new UiLifecycleHelper(getActivity(), callback);
		uiHelper.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.eventlist, 
				container, false);

		Session session = Session.getActiveSession();
		if (session != null && session.isOpened()) {
			// TODO ask for the extra permissions, get the access token
			// Get the user's data
			getUserData(session);
		}
		return view;
	}

	/**
	 * Gets the user data and calls the asynctask to insert into the datastore
	 * @param session
	 */
	private void getUserData(final Session session) {
		// Make an API call to get user data and define a 
		// new callback to handle the response.
		Request request = Request.newGraphPathRequest(session, MEREQUESTPATH,
				new Request.Callback() {
			@Override
			public void onCompleted(Response response) {
				// If the response is successful
				if (session == Session.getActiveSession()) {
					Log.i("GraphUser", "user request completed");
					try {
						String uid = (String) response.getGraphObject().getInnerJSONObject().get("id");
						String location = null;
						if (response.getGraphObject().getInnerJSONObject().getJSONObject("location").has("name")) {
							location = response.getGraphObject().getInnerJSONObject().getJSONObject("location").getString("name");
						}
						new InsertUserEndpointsTask().execute(uid, location);
					} catch (Exception e) {
						// TODO handle the exception
					}
				}
				if (response.getError() != null) {
					// Handle errors, will do so later.
				}
			}
		});
		Bundle merequestparams = new Bundle();
		merequestparams.putString(FIELDS_KEY, MEREQUESTFIELDS);
		request.setParameters(merequestparams);
		request.executeAsync();
	} 

	@Override
	public void onResume() {
		super.onResume();
		uiHelper.onResume();
	}

	@Override
	public void onSaveInstanceState(Bundle bundle) {
		super.onSaveInstanceState(bundle);
		uiHelper.onSaveInstanceState(bundle);
	}

	@Override
	public void onPause() {
		super.onPause();
		uiHelper.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		uiHelper.onDestroy();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REAUTH_ACTIVITY_CODE) {
			uiHelper.onActivityResult(requestCode, resultCode, data);
		}
	}

	private Session.StatusCallback callback = new Session.StatusCallback() {
		@Override
		public void call(final Session session, final SessionState state, final Exception exception) {
			onSessionStateChange(session, state, exception);
		}
	};

	private void onSessionStateChange(final Session session, SessionState state, Exception exception) {
		if (session != null && session.isOpened()) {
			// Get the user's data.
			getUserData(session);
		}
	}

	/**
	 * The async task to ert the user into google datastore
	 */
	public class InsertUserEndpointsTask extends AsyncTask<String, Integer, Long> {
		protected Long doInBackground(String... params) {

			Userendpoint.Builder endpointBuilder = new Userendpoint.Builder(
					AndroidHttp.newCompatibleTransport(),
					new JacksonFactory(),
					new HttpRequestInitializer() {
						public void initialize(HttpRequest httpRequest) { }
					});
			Userendpoint endpoint = CloudEndpointUtils.updateBuilder(
					endpointBuilder).build();
			try {
				//TODO now only inserts the id, should include all the fields
				User user = new User().setUid(Long.parseLong(params[0]))
									  .setLocation(params[1])
									  .setEventfatchparamsId(Long.parseLong("0"))
									  .setOrderpreference(0)
									  .setTotalmatchmethodId(Long.parseLong("0"));
				User result = endpoint.insertUser(user).execute();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return (long) 0;
		}
	}

}
