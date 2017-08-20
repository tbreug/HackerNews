package com.breug.hackerNews;

/**
 * Created by user on 29.07.2017.
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Main activity
 */
@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity
{
    @ViewById
    ListView storiesList; // list view of stories

    @ViewById(R.id.progressBar)
    ProgressBar progress; // circular progress bar shown while loading stories

    private boolean loading = false;
    private HackerNewsClient client; // HTTP REST client
    private Button btnLoad; // button for loading more stories
    private int lastEntry = 0; // last loaded entry in list 'ids'
    private List<Integer> ids = null; // list of top story ids returned by response of HackerNewsClient::getTopStories
    private List<HackerNewsStory> stories = Collections.synchronizedList(new LinkedList<HackerNewsStory>()); // synchronized list of stories being shown in 'storiesList'

    private synchronized void setLoadState(boolean state)
    {
        loading = state;
        invalidateOptionsMenu();
    }

    /**
     * List adapter for 'storiesList'
     */
    private class StoryAdapter extends ArrayAdapter<HackerNewsStory>
    {
        private StoryAdapter(Context context, List<HackerNewsStory> list)
        {
            super(context, R.layout.story, list);
        }

        @Override
        public View getView(int position, View rowView, ViewGroup parent)
        {
            if (rowView == null)
                rowView = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.story, parent, false);
            final HackerNewsStory story = getItem(position);
            assert story != null;
            ((TextView) rowView.findViewById(R.id.title)).setText(story.title);
            ((TextView) rowView.findViewById(R.id.url)).setText(story.url);
            return rowView;
        }
    }

    /**
     * Asynchronous invocation of the HackerNewsClient REST API 'getStoryItem' for MAX_ENTRIES stories
     * called once on activity creation and whenever button 'btnLoad' was pressed
     */
    private class Load extends AsyncTask<Void, Void, List<HackerNewsStory>>
    {
        // @todo constant should be moved into settings that may be shown on a menu
        // I omitted the implementation of a settings activity and menu
        private final int MAX_ENTRIES = 30; // number of entries to load for each load step

        @Override
        protected List<HackerNewsStory> doInBackground(Void... params)
        {
            assert client != null;
            assert ids != null;

            final List<HackerNewsStory> list = new LinkedList<HackerNewsStory>();
            try
            {
                // load up to 'MAX_ENTRIES' stories from REST interface 'getStoryItem'
                int count = MAX_ENTRIES;
                while (count > 0 && lastEntry < ids.size()) {
                    final HackerNewsStory story = client.getStoryItem(ids.get(lastEntry++)).execute().body();
                    assert story != null;
                    if (story.isApplicable()) { // add story to list
                        list.add(story);
                        --count;
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            return list;
        }

        @Override
        protected void onPostExecute(List<HackerNewsStory> result)
        {
            final Context context = MainActivity.this;
            final int scroll = storiesList.getFirstVisiblePosition(); // remember top scroll item position
            stories.addAll(result);
            storiesList.setAdapter(new StoryAdapter(context, stories));
            if (lastEntry < ids.size()) {
                btnLoad.setVisibility(View.VISIBLE); // enable load of more items
            } else {
                storiesList.removeFooterView(btnLoad); // no more items to load
            }
            progress.setVisibility(View.INVISIBLE); // hide progress bar
            storiesList.setSelection(scroll); // scroll to previous scroll item position
            setLoadState(false);
        }
    }

    /**
     * Called when activity is being created
     * Subclassing of control elements and initialization
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState); // invoke parent's func
/*      setContentView(R.layout.activity_main); // set main layout

        // subclass control elements
        storiesList = (ListView) findViewById(R.id.stories);
        progress = (ProgressBar) findViewById(R.id.progressBar);
*/  }
    
    @AfterViews
    void Init()
    {
        // create button for loading more stories
        btnLoad = new Button(this);
        btnLoad.setText(R.string.strLoad);

        storiesList.addFooterView(btnLoad); // Adding Load More button to lisview at bottom
        storiesList.setEmptyView(progress); // force progrssbar being shown while loading ids
        storiesList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            /**
             * event listener for click on an item in list view 'storiesList'
             */
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id)
            {
                // start generic action activity for selected story URL
                final HackerNewsStory story = (HackerNewsStory) parent.getItemAtPosition(pos);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(story.url)));
            }
        });

        // set event listener for button 'btnLoad'
        btnLoad.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View arg0) {
                // load more stories asynchronously
                btnLoad.setVisibility(View.INVISIBLE); // hide button to avoid thread concurrency
                progress.setVisibility(View.VISIBLE); // show progress spinner
                new Load().execute(); // run async task for more calls REST API 'getStoryItem'
            }
        });

        // initialize Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(HackerNewsClient.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        client = retrofit.create(HackerNewsClient.class);
        load(); // load ids of top stories in REST API 'getTopStories'
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.Refresh).setVisible(!loading);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.Refresh:
                if (!loading)
                    load();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Load ids of top stories in REST API 'getTopStories' and store into list 'ids'
     * Runs async task 'Load' for loading stories after successful completion
     */
    protected synchronized void load()
    {
        assert client != null;

        final Context context = this;

        setLoadState(true);

        storiesList.setAdapter(null);
        stories.clear();
        lastEntry = 0;

        client.getTopStories().enqueue(new Callback<List<Integer>>()
        {
            @Override
            public void onResponse(Call<List<Integer>> call, Response<List<Integer>> response)
            {
                // set list of ids and invoke load of story properties
                ids = response.body();
                new Load().execute(); // run async task for initial calls REST API 'getStoryItem'
            }

            @Override
            public void onFailure(Call<List<Integer>> call, Throwable t)
            {
                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                alert.setPositiveButton("Try again", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        load(); // reload
                    }
                });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        if (ids == null)
                            finish(); // exit application
                        setLoadState(false);
                    }
                });
                alert.setTitle(getString(R.string.app_name));
                alert.setMessage("Error on loading: '" + t.getMessage() + "'");
                alert.create().show();
            }
        });
    }
}
