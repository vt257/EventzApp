package com.eventzapp;

import java.util.Arrays;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.widget.LoginButton;

public class ConnectFragment extends Fragment {
	private static final List<String> READ_PERMISSIONS = Arrays.asList("user_location", "friends_location", "user_events", "user_likes", "friends_events", "offline_access");
	
	@Override
	public View onCreateView(LayoutInflater inflater, 
	        ViewGroup container, Bundle savedInstanceState) {
	    View view = inflater.inflate(R.layout.connect, 
	            container, false);
	    LoginButton authButton = (LoginButton) view.findViewById(R.id.fbAuthButton);
	    authButton.setReadPermissions(READ_PERMISSIONS);
	    return view;
	}
}
