package com.eventzapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;

/**
 * The Main Activity.
 * 
 * Incorporates 3 fragments:
 * The ConnectFragment which is displayed when the user is not logged in
 * The EventlistFragment which is the main fragment responsible for showing the list of events
 * The SettingsFragment which is a standard facabook fragment, currently responsible for logout
 */
public class MainActivity extends FragmentActivity {
	// setting the constant fragment indexes and count
	private static final int CONNECT = 0;
	private static final int EVENTLIST = 1;
	private static final int SETTINGS = 2;
	private static final int FRAGMENT_COUNT = SETTINGS +1;
	
	// the variable to keep track if the activity is resumed or not
	private boolean isResumed = false;

	// the settings menu item which currently points to the facebook logout screen
	private MenuItem settings;
	
	// the array which will store all the fragments handled by this activity
	private Fragment[] fragments = new Fragment[FRAGMENT_COUNT];

	// the lifecycle helper
	private UiLifecycleHelper uiHelper;

	// setting the session change status callback
	private Session.StatusCallback callback = 
			new Session.StatusCallback() {
		@Override
		public void call(Session session, 
				SessionState state, Exception exception) {
			onSessionStateChange(session, state, exception);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		uiHelper = new UiLifecycleHelper(this, callback);
		uiHelper.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		FragmentManager fm = getSupportFragmentManager();
		fragments[CONNECT] = fm.findFragmentById(R.id.connectFragment);
		fragments[EVENTLIST] = fm.findFragmentById(R.id.eventlistFragment);
		fragments[SETTINGS] = fm.findFragmentById(R.id.userSettingsFragment);
		
		FragmentTransaction transaction = fm.beginTransaction();
		for(int i = 0; i < fragments.length; i++) {
			transaction.hide(fragments[i]);
		}
		transaction.commit();
	}
	
	@Override
	protected void onResumeFragments() {
		super.onResumeFragments();
		Session session = Session.getActiveSession();
		if (session != null && session.isOpened()) {
			// if the session is already open,
			// try to show the selection fragment
			showFragment(EVENTLIST, false);
		} else {
			// otherwise present the splash screen
			// and ask the user to login.
			showFragment(CONNECT, false);
		}
	}

	/**
	 * determines what to do when the facebook session state is changed
	 * e.g when somebody logs in or logs out
	 * @param session the current facebook session
	 * @param state the state of the current facebook session
	 * @param exception the exception, if thrown
	 */
	private void onSessionStateChange(Session session, SessionState state, Exception exception) {
		// Only make changes if the activity is visible
		if (isResumed) {
			FragmentManager manager = getSupportFragmentManager();
			// Get the number of entries in the back stack
			int backStackSize = manager.getBackStackEntryCount();
			// Clear the back stack
			for (int i = 0; i < backStackSize; i++) {
				manager.popBackStack();
			}
			if (state.isOpened()) {
				// If the session state is open:
				// Show the authenticated fragment
				showFragment(EVENTLIST, false);
			} else if (state.isClosed()) {
				// If the session state is closed:
				// Show the login fragment
				showFragment(CONNECT, false);
			}
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
	    // only add the menu when the selection fragment is showing
	    if (fragments[EVENTLIST].isVisible()) {
	        if (menu.size() == 0) {
	            settings = menu.add(R.string.settings);
	        }
	        return true;
	    } else {
	        menu.clear();
	        settings = null;
	    }
	    return false;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    if (item.equals(settings)) {
	        showFragment(SETTINGS, true);
	        return true;
	    }
	    return false;
	}
	
	@Override
	public void onResume() {
	    super.onResume();
	    uiHelper.onResume();
	    isResumed = true;
	}

	@Override
	public void onPause() {
	    super.onPause();
	    uiHelper.onPause();
	    isResumed = false;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    uiHelper.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onDestroy() {
	    super.onDestroy();
	    uiHelper.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	    uiHelper.onSaveInstanceState(outState);
	}
	
	/**
	 * function that shows the specified fragment and hides the rest
	 * @param fragmentIndex the index of the fragment to show
	 * @param addToBackStack if the fragment should be added to the backstack
	 */
	private void showFragment(int fragmentIndex, boolean addToBackStack) {
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction transaction = fm.beginTransaction();
		for (int i = 0; i < fragments.length; i++) {
			if (i == fragmentIndex) {
				transaction.show(fragments[i]);
			} else {
				transaction.hide(fragments[i]);
			}
		}
		if (addToBackStack) {
			transaction.addToBackStack(null);
		}
		transaction.commit();
	}
}
