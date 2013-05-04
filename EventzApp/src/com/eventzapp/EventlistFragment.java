package com.eventzapp;

import java.io.IOException;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
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
import com.google.api.client.util.DateTime;

public class EventlistFragment extends Fragment {
	private UiLifecycleHelper uiHelper;
	private static final int REAUTH_ACTIVITY_CODE = 100;

	// Some constants for the request to get basic user data
	// TODO consider using batchFB on the back-end after having
	// sent the offline_permissions access token and the user_id
	// to the backend
	// TODO need to get fiendids and likeids with this same request
	// batchFB also on the frontend/backend?
	private static final String USERDATAREQUESTPATH = "/me";
	private static final String USERDATAREQUESTFIELDS = "id,location";
	private static final String FIELDS_KEY = "fields";
	private String accessToken = "";

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
		Request request = Request.newGraphPathRequest(session, USERDATAREQUESTPATH,
				new Request.Callback() {
			@Override
			public void onCompleted(Response response) {
				// If the response is successful
				if (session == Session.getActiveSession()) {
					accessToken = (String) session.getAccessToken();
					Log.i("GraphUser", "user request completed");
					try {
						String uid = response.getGraphObject().getInnerJSONObject().getString("id");
						// TODO handle the location better, it should probably be something like a 
						// HashMap with key->value pairs, or some sort of a List
						JSONObject locationJSON = response.getGraphObject().getInnerJSONObject().getJSONObject("location");
						String locationId = null;
						String locationName = null;
						String locationLongitude = null;
						String locationLatitude = null;
						if (locationJSON.has("id")) {
							locationId = locationJSON.getString("id");
							//TODO: Get the latitude and longitude from Facebook 
						}
						if (locationJSON.has("name")) {
							locationName = locationJSON.getString("name");
							if (locationJSON.has("longitude") && locationJSON.has("latitute")) {
								locationLongitude = locationJSON.getString("longitude");
								locationLatitude = locationJSON.getString("latitude");
							} else {
								int maxResults = 1; //TODO: It is assumed that only one address is returned for the location
								Geocoder gc = new Geocoder(getActivity());
								Address address = gc.getFromLocationName(locationName, maxResults).get(0);
								locationLongitude = Double.toString(address.getLongitude());
								locationLatitude = Double.toString(address.getLatitude());
							}
							Log.w("longtitue: ", locationLongitude);
							Log.w("latitude: ", locationLatitude);
						}
						new InsertUserEndpointsTask().execute(uid, locationName, locationLatitude, locationLongitude);
					} catch (Exception e) {
						// TODO handle the exception
					}
				}
				if (response.getError() != null) {
					// TODO Handle errors, will do so later.
				}
			}
		});
		
		Bundle userDataRequestParams = new Bundle();
		userDataRequestParams.putString(FIELDS_KEY, USERDATAREQUESTFIELDS);
		request.setParameters(userDataRequestParams);
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
	 * The async task to insert the user into google datastore
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
				// TODO now only inserts the id, should include all the fields
				User user = new User().setUid(Long.parseLong(params[0]))
									  .setLocation(params[1])
									  .setLocationLatitude(params[2])
									  .setLocationLongitude(params[3])
									  .setEventFatchParamsId(Long.parseLong("0"))
									  .setOrderPreference(0)
									  .setTotalMatchMethodId(Long.parseLong("0"))
									  .setModified(new DateTime(new Date()))
									  .setAccesToken(accessToken);
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
