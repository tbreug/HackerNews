package com.breug.hackerNews;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by user on 29.07.2017.
 */

/**
 * interface for Retrofit REST client
 */
public interface HackerNewsClient
{
    /**
     * Base URL for Hacker news
     */
    String BASE_URL = "https://hacker-news.firebaseio.com/v0/";

    /**
     * @return REST interface for top stories. Response is an array of ids of type Integer
     */
    @GET("topstories.json")
    Call<List<Integer>> getTopStories();

    /**
     * @return REST interface for story properties with specified id. Response is of type HackerNewsStory
     */
    @GET("item/{id}.json")
    Call<HackerNewsStory> getStoryItem(@Path("id") Integer id);
}
