package com.walmartlabs.classwork.tweets.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Delete;
import com.codepath.apps.tweets.R;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.walmartlabs.classwork.tweets.adapters.TweetsAdapter;
import com.walmartlabs.classwork.tweets.fragments.ComposeTweetDialog;
import com.walmartlabs.classwork.tweets.main.TwitterApplication;
import com.walmartlabs.classwork.tweets.models.Tweet;
import com.walmartlabs.classwork.tweets.models.User;
import com.walmartlabs.classwork.tweets.net.TwitterClient;
import com.walmartlabs.classwork.tweets.util.EndlessScrollListener;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TimelineActivity extends AppCompatActivity implements ComposeTweetDialog.EditNameDialogListener {

    private TwitterClient client;
    private TweetsAdapter aTweets;
    private ArrayList<Tweet> tweets;
    private ListView lvTweets;
    public static long maxId;
    public static long sinceId;
    private ComposeTweetDialog composeTweetDialog;

    private SwipeRefreshLayout swipeContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActiveAndroid.setLoggingEnabled(true);
        setContentView(R.layout.activity_timeline);
        client = TwitterApplication.getRestClient();
        tweets = new ArrayList<Tweet>();
        aTweets = new TweetsAdapter(this, tweets);
        lvTweets = (ListView) findViewById(R.id.lvTweets);
        lvTweets.setAdapter(aTweets);
        setupListView();
        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                RequestParams params = new RequestParams();
                params.put("count", 25);
                params.put("since_id", 1);
                Log.i("sinceId", Long.toString(sinceId));
                fetchAndUpdateFeed(params);
            }
        });

        RequestParams params = new RequestParams();
        params.put("count", 25);
        params.put("since_id", 1);
        fetchAndPopulateTimeline(params, false);
    }

    private void setupListView() {
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#4FAAF1")));

        lvTweets.setOnScrollListener(new EndlessScrollListener() {
            @Override
            public boolean onLoadMore(int page, int totalItemsCount) {
                RequestParams params = new RequestParams();
                params.put("count", 25);
                params.put("max_id", TimelineActivity.maxId);
                fetchAndPopulateTimeline(params, true);
                return true;
            }
        });

        lvTweets.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(TimelineActivity.this, DetailedViewActivity.class);
                Tweet tweet = (Tweet) aTweets.getItem(position);
                i.putExtra("tweet", tweet);
                startActivity(i);
            }
        });
    }

    private void fetchAndUpdateFeed(RequestParams params) {
        if(isNetworkAvailable()) {
            clearCache();
            client.getTimeline(params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                    if (response.length() == 0) return;
                    ArrayList<Tweet> tweets = Tweet.fromJsonArray(response);
                    aTweets.clear();
                    aTweets.addAll(tweets);
                    Log.d("Success", response.toString());
                    swipeContainer.setRefreshing(false);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    Log.d("Failure", errorResponse.toString());
                    swipeContainer.setRefreshing(false);
                }
            });
        } else {
            getFromCache();
            swipeContainer.setRefreshing(false);
        }

    }

    private void fetchAndPopulateTimeline(RequestParams params, boolean isScroll) {
        if (isNetworkAvailable()) {
            if(! isScroll) clearCache();
            client.getTimeline(params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                    ArrayList<Tweet> tweets = Tweet.fromJsonArray(response);
                    aTweets.addAll(tweets);
                    Log.d("Success", response.toString());
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    Log.d("Failure", errorResponse.toString());
                }
            });
        } else {
            getFromCache();
        }
    }

    public void getFromCache() {
        List<Tweet> tweets  = Tweet.recentItems();
        aTweets.clear();
        aTweets.addAll(tweets);
        aTweets.notifyDataSetChanged();
    }

    public void clearCache() {
        new Delete().from(Tweet.class).execute();
        new Delete().from(User.class).execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_timeline, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.compose_tweet) {
            FragmentManager fm = getSupportFragmentManager();
            composeTweetDialog = ComposeTweetDialog.newInstance(null);
            composeTweetDialog.show(fm, "fragment_compose_tweet");
        }

        return super.onOptionsItemSelected(item);
    }

    private Boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    @Override
    public void onFinishEditDialog(Tweet tweet) {
        tweets.add(0, tweet);
        aTweets.notifyDataSetChanged();
    }
}
