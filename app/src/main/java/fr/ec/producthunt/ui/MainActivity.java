package fr.ec.producthunt.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ViewAnimator;
import fr.ec.producthunt.R;
import fr.ec.producthunt.data.DataProvider;
import fr.ec.producthunt.data.database.ProductHuntDbHelper;
import fr.ec.producthunt.data.model.Post;
import fr.ec.producthunt.ui.Adapter.PostAdapter;
import java.util.List;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = "MainActivity";
  public static final String ACTION = "fr.ec.producthunt.ui.MainActivity";

  public static final int NB_ITEM = 400;

  private PostAdapter adapter;
  private ListView listView;
  private SwipeRefreshLayout swipeRefreshPosts;
  private ProgressBar progressBar;
  private ViewAnimator viewAnimator;
  private ProductHuntDbHelper dbHelper;

  private AlarmManager alarmMgr;
  private PendingIntent alarmIntent;

  // Ce BroadcastReceiver sera appellé toutes les 2 heures.
  private BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      Log.d(TAG, "Refreshing listview");
      refreshPosts();
    }
  };

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    //Utilisé une toolbar au lieu d'un actionbar
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    setTitle(R.string.app_name);

    swipeRefreshPosts = (SwipeRefreshLayout) findViewById(R.id.swipe_to_refresh_posts);
    listView = (ListView) findViewById(R.id.listview);
    adapter = new PostAdapter();
    listView.setAdapter(adapter);
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Post post = (Post) adapter.getItem(position);
        //Toast.makeText(MainActivity.this,post.getTitle(),Toast.LENGTH_SHORT).show();

        //navigateToDetailActivity(post);
        navigateToCommentActivity(post);
      }
    });

    listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {

            Post post = (Post) adapter.getItem(position);
            navigateToDetailActivity(post);
            return true;
        }
    });
      
    progressBar = (ProgressBar) findViewById(R.id.progress);

    viewAnimator = (ViewAnimator) findViewById(R.id.viewAnimator);

    viewAnimator.setDisplayedChild(1);

    dbHelper = new ProductHuntDbHelper(this);

    swipeRefreshPosts.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override
      public void onRefresh() {
        refreshPosts();
        swipeRefreshPosts.setRefreshing(false);
      }
    });

    loadPosts();
    scheduleAlarm();

  }

  private void navigateToDetailActivity(Post post) {
    Intent intent = new Intent(MainActivity.this,DetailActivity.class);
    intent.putExtra(DetailActivity.POST_URL_KEY,post.getPostUrl());
    startActivity(intent);
  }

  private void navigateToCommentActivity(Post post) {
    Intent intent = new Intent(MainActivity.this,CommentActivity.class);
    intent.putExtra(CommentActivity.POST_ID_KEY,String.valueOf(post.getId()));
    intent.putExtra(CommentActivity.POST_TITLE_KEY,post.getTitle());
    startActivity(intent);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {

    //Attacher le main menu au menu de l'activity
    MenuInflater menuInflater = getMenuInflater();
    menuInflater.inflate(R.menu.main, menu);

    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {

      case R.id.refresh:
        refreshPosts();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  public void refreshPosts() {
    PostsAsyncTask postsAsyncTask = new PostsAsyncTask();
    postsAsyncTask.execute();
  }

  private class FetchPostsAsyncTask extends AsyncTask<Void,Void,List<Post>> {

    @Override protected void onPreExecute() {
      super.onPreExecute();
      Log.d(TAG, "onPreExecute: ");
      viewAnimator.setDisplayedChild(0);
    }


    @Override protected List<Post> doInBackground(Void... params) {
      return DataProvider.getPostsFromDatabase(dbHelper);
    }

    @Override protected void onPostExecute(List<Post> posts) {
      if (posts != null && !posts.isEmpty() ) {
        adapter.showListPost(posts);
      }
      viewAnimator.setDisplayedChild(1);

    }
  }

  private class PostsAsyncTask extends AsyncTask<Void, Integer, Boolean> {

    //Do on Main Thread
    @Override protected void onPreExecute() {
      super.onPreExecute();
      Log.d(TAG, "onPreExecute: ");
      viewAnimator.setDisplayedChild(0);
    }

    //Do on Background Thread
    @Override protected Boolean doInBackground(Void... params) {
      return DataProvider.syncPost(dbHelper);
    }

    //Do on Main Thread
    @Override protected void onPostExecute(Boolean result) {
      super.onPostExecute(result);
      Log.d(TAG, "onPostExecute() called with: " + "result = [" + result + "]");
      if (result ) {

        loadPosts();

      }else {
        viewAnimator.setDisplayedChild(1);

      }
    }
  }

  private void loadPosts() {
    FetchPostsAsyncTask fetchPstsAsyncTask = new FetchPostsAsyncTask();
    fetchPstsAsyncTask.execute();
  }

  public boolean isOnline() {

    ConnectivityManager connectivityManager =
            (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
    if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
      return true;
    }

    return false;
  }

  public void scheduleAlarm(){

    IntentFilter intentFilter = new IntentFilter(ACTION + ".refreshReceiver");
    registerReceiver(refreshReceiver, intentFilter);

    Intent intent = new Intent(ACTION + ".refreshReceiver");
    alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

    alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 2 * 60 * 60 * 1000,
            2 * 60 * 60 * 1000,
            alarmIntent);
  }

  @Override
  protected void onDestroy() {
    unregisterReceiver(refreshReceiver);
    super.onDestroy();
  }


}
