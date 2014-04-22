package com.example.musicrec;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.facebook.Request;
import com.facebook.RequestAsyncTask;
import com.facebook.Response;
import com.facebook.model.GraphUser;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

//THIS CLASS SHOULD YOU FB FRIENDS FEED - MOVE THE UPLOAD PART TO WELCOME ACTIVITY

public class FragmentTab1 extends SherlockFragment {

  public static final String SERVICECMD = "com.android.music.musicservicecommand";
  public static final String CMDNAME = "command";
  public static final String CMDTOGGLEPAUSE = "togglepause";
  public static final String CMDSTOP = "stop";
  public static final String CMDPAUSE = "pause";
  public static final String CMDPREVIOUS = "previous";
  public static final String CMDNEXT = "next";

  Song currSong;
  private List<Song> songArrayList = null;
  int i = 0;
  Thread a, b, c;

  @SuppressWarnings("deprecation")
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.tab_feed, container, false);
    songArrayList = new ArrayList<Song>();

    getFacebookIdInBackground();

    com.beardedhen.androidbootstrap.BootstrapButton btn = (com.beardedhen.androidbootstrap.BootstrapButton) rootView
        .findViewById(R.id.logout_btn);
    btn.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        ParseUser.getCurrentUser();
        ParseUser.logOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);

      }
    });

    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    RequestAsyncTask r = Request.executeMyFriendsRequestAsync(
        ParseFacebookUtils.getSession(), new Request.GraphUserListCallback() {

          @SuppressWarnings("unchecked")
          @Override
          public void onCompleted(List<GraphUser> users, Response response) {
            if (users != null) {

              List<String> friendsList = new ArrayList<String>();
              for (GraphUser user : users) {
                friendsList.add(user.getId());
              }

              @SuppressWarnings("rawtypes")
              final ParseQuery friendQuery = ParseQuery.getUserQuery();
              friendQuery.whereContainedIn("fbId", friendsList);

              friendQuery.findInBackground(new FindCallback<ParseObject>() {

                @Override
                public void done(List<ParseObject> friendUsers,
                    ParseException e2) {

                  for (ParseObject obj : friendUsers) {
                    // wtf
                    ParseUser currUser = (ParseUser) obj;
                    Log.d("User", "name is " + currUser.get("fbId"));

                    try {
                      currUser.fetchIfNeeded();
                    } catch (ParseException e1) {
                      e1.printStackTrace();
                    }

                    ParseQuery<ParseObject> query = ParseQuery.getQuery("Song");
                    query.whereEqualTo("author", currUser);

                    query.findInBackground(new FindCallback<ParseObject>() {

                      @Override
                      public void done(List<ParseObject> songListForSingleUser,
                          ParseException e) {
                        
                        if (e == null && songListForSingleUser != null) {

                          for (i = 0; i < songListForSingleUser.size(); i++) {

                            songArrayList.add((Song) songListForSingleUser
                                .get(i));
                            Log.d("User", "Sizes are WHOLE + CURRENT "
                                + songArrayList.size() + " "
                                + songListForSingleUser.size());

                          }
                          //
                        }
                        try {
                          Thread.sleep(4000);
                        } catch (InterruptedException e2) {
                          e2.printStackTrace();
                        }
                        
                        Log.d("User", "Whole size is + " + songArrayList.size());
                      } // here were done getting the songList for each
                        // followed.
                      
                    });
                    
                  }
                  
                }
              });
           
              
            }

          }

        });
    
   
    
    IntentFilter iF = new IntentFilter();
    iF.addAction("com.android.music.metachanged");
    getActivity().registerReceiver(mReceiver, iF);
    return rootView;
  }

  private BroadcastReceiver mReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {

      String action = intent.getAction();
      String cmd = intent.getStringExtra("command");
      Log.d("mIntentReceiver.onReceive", action + "/" + cmd);

      String artist = intent.getStringExtra("artist");
      String album = intent.getStringExtra("album");
      String track = intent.getStringExtra("track");
      Log.d("Music", artist + ":" + album + ":" + track);

      // everytime theres an update, push it to Parse track.
      currSong = new Song();
      currSong.setAuthor(ParseUser.getCurrentUser());
      currSong.setTitle(track);
      currSong.setArtist(artist);
      currSong.saveInBackground();

    }
  };

  @SuppressWarnings("deprecation")
  private static void getFacebookIdInBackground() {
    Request.executeMeRequestAsync(ParseFacebookUtils.getSession(),
        new Request.GraphUserCallback() {
          @Override
          public void onCompleted(GraphUser user, Response response) {
            if (user != null) {
              ParseUser.getCurrentUser().put("fbId", user.getId());
              ParseUser.getCurrentUser().saveInBackground();
              // use this part to get details from the logged in
              // user
            }
          }
        });
  }

}