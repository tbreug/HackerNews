package com.breug.hackerNews;

import android.webkit.URLUtil;

/**
 * Created by user on 29.07.2017.
 */

/**
 * Hacker news story properties
 * used by GET request in HackerNewsClient and stored from JSON response
 */
public class HackerNewsStory
{
    public Integer id;
    public String title;
    public String url;
    public boolean deleted;
    public boolean dead;

    /**
     * Indicates whether this story shall be added to the main's activity list
     * @return boolean
     */
    public boolean isApplicable() {
        return !(dead || deleted || url == null || url.isEmpty() || !URLUtil.isValidUrl(url));
    }
}
