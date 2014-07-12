package com.example.musicrec;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.echonest.api.v4.Artist;
import com.echonest.api.v4.EchoNestAPI;
import com.echonest.api.v4.EchoNestException;
import com.echonest.api.v4.Song;
import com.echonest.api.v4.SongParams;
import com.facebook.android.Facebook;
import com.google.android.youtube.player.YouTubeIntents;

public class CurrSongWindow extends Activity {

  private EchoNestAPI en;

  String artist, title;
  String url;
  Artist currEchoArtist;
  Song song;

  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.curr_song_window);

    final ImageView songView = (ImageView) findViewById(R.id.songImage);

    /* TEMP SOL */
    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
        .permitAll().build();
    StrictMode.setThreadPolicy(policy);
    en = new EchoNestAPI("FUS98WPLXFNIHZHHG");

    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      artist = extras.getString("ARTIST");
      title = extras.getString("TITLE");
    }
    
    
    
    String jsonData = extras.getString("com.parse.Data");
    
    if(jsonData != null) {
      Log.d("JSON DATA",jsonData);
      JSONObject json;
      try {
        json = new JSONObject(jsonData);
        artist = json.getString("songartist");
        title = json.getString("songtitle");
        
      } catch (JSONException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
    }

    
    
    
    SongParams p1 = new SongParams();
    p1.setArtist(artist);
    p1.setTitle(title);
    p1.includeTracks(); // the album art is in the track data
    p1.setLimit(true); // only return songs that have track data
    p1.addIDSpace("7digital-US");

    try {

      List<com.echonest.api.v4.Song> songs = en.searchSongs(p1);
      if (songs.size() != 0) {
        song = songs.get(0);
        try {
          url = song.getString("tracks[0].release_image");
        } catch (IndexOutOfBoundsException e) {

        }
      } else {
        SongParams p2 = new SongParams();
        p2.setTitle(title);
        p2.includeTracks(); // the album art is in the track data
        p2.setLimit(true); // only return songs that have track data
        p2.addIDSpace("7digital-US");

        List<com.echonest.api.v4.Song> songsWithoutArtists = en
            .searchSongs(p2);
        if (songsWithoutArtists.size() != 0) {
          song = songsWithoutArtists.get(0);
          try {
            url = song.getString("tracks[0].release_image");
          } catch (IndexOutOfBoundsException e) {

          }
        }
      }

      /* for SONG image */
      AsyncTask<Void, Void, Bitmap> songImageTask = new AsyncTask<Void, Void, Bitmap>() {
        protected Bitmap doInBackground(Void... p) {
          Bitmap bm = null;
          try {

            InputStream is = new URL(url).openStream();

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            BitmapFactory.decodeStream(is, null, options);

            options.inSampleSize = calculateInSampleSize(options, 200, 200);
            options.inJustDecodeBounds = false;
            is.close();

            is = new URL(url).openStream();
            bm = BitmapFactory.decodeStream(is, null, options);

            int width = bm.getWidth();
            int height = bm.getHeight();
            int newWidth = 200;
            int newHeight = 200;

            float scaleWidth = ((float) newWidth) / width;
            float scaleHeight = ((float) newHeight) / height;

            Matrix matrix = new Matrix();
            // resize the bit map
            matrix.postScale(scaleWidth, scaleHeight);

            bm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
            bm = CustomArrayAdapter.getRoundedCornerBitmap(bm, 12f);

          } catch (IOException e) {
            e.printStackTrace();
          }
          return bm;
        }

        protected void onPostExecute(Bitmap bm) {

          /* SET THE SONG IMAAGE VIEW */
          songView.setImageBitmap(bm);

        }
      };
      songImageTask.execute();

    } catch (EchoNestException e) {
      e.printStackTrace();
    }

  }

  private static int calculateInSampleSize(BitmapFactory.Options options,
      int reqWidth, int reqHeight) {

    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {

      final int heightRatio = Math.round((float) height / (float) reqHeight);
      final int widthRatio = Math.round((float) width / (float) reqWidth);

      inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
    }
    return inSampleSize;
  }

  public void openYoutubeSearch(View v) {
    Intent intent = YouTubeIntents.createSearchIntent(this, title + " by "
        + artist);
    startActivity(intent);
  }

  public boolean isRdioAvailable(Context context) {
    PackageManager packageManager = context.getPackageManager();
    Intent intent = packageManager
        .getLaunchIntentForPackage("com.rdio.android.ui");

    if (intent == null) {
      // Check if the Brazil version of Rdio is installed
      intent = packageManager
          .getLaunchIntentForPackage("com.rdio.oi.android.ui");
    }

    return intent != null;
  }

  public void openRdioSearch(View v) {
    boolean isRdioAvail = isRdioAvailable(this);
    if (isRdioAvail) {
      url = new String("rdio://search/" + title + "%20" + artist);
    } else {
      url = "https://play.google.com/store/apps/details?id=com.rdio.android.ui";
    }

    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(url));
    startActivity(intent);
  }

  public void openSpotifySearch(View v) {

    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.setAction(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
    intent.setComponent(new ComponentName("com.spotify.mobile.android.ui",
        "com.spotify.mobile.android.ui.Launcher"));
    intent.putExtra(SearchManager.QUERY, title + " " + artist);
    try {
      startActivity(intent);
    } catch (ActivityNotFoundException e) {
      Toast.makeText(this, "You must first install Spotify", Toast.LENGTH_LONG)
          .show();
      Intent i = new Intent(Intent.ACTION_VIEW,
          Uri.parse("market://details?id=com.spotify.mobile.android.ui"));
      startActivity(i);
    }
    

  }

}
