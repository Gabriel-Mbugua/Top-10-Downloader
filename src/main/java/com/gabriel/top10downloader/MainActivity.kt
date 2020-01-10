package com.gabriel.top10downloader

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.FileNotFoundException
import java.net.URL
import kotlin.properties.Delegates

class FeedEntry {
    var name: String = ""
    var artist: String = ""
    var releaseDate: String = ""
    var summary: String = ""
    var imageURL: String = ""

    override fun toString(): String {
        return """
            title = $name
            artist = $artist
            releaseDate = $releaseDate
            name = $summary
            imageURL = $imageURL
            """.trimIndent()
    }
}

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private var feedUrl: String = "https://rss.itunes.apple.com/api/v1/ca/ios-apps/top-free/all/%d/explicit.atom"
    private var feedLimit = 10
    private var feedCachedURL = "INVALIDATED"
    private val STATE_URL = "feedUrl"
    private val STATE_LIMIT = "feedLimit"


    private var downloadData: DownloadData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate called")

        if (savedInstanceState != null){
            feedUrl = savedInstanceState.getString(STATE_URL)!!
            feedLimit = savedInstanceState.getInt(STATE_LIMIT)
        }

        downloadUrl(feedUrl.format(feedLimit)) //feedLimit will replace the %d in the original feedUrl
        Log.d(TAG, "==== On Create done ====")
    }

    private fun downloadUrl(feedUrl: String) {
        if(feedUrl != feedCachedURL) {
            Log.d(TAG, "=== downloadUrl starting AsyncTask ===")
            downloadData = DownloadData(this, xmlListView)
            downloadData?.execute(feedUrl)
            feedCachedURL = feedUrl
            Log.d(TAG, "=== downloadUrl done ===")
        }
        else{
            Log.d(TAG, "downloadUrl - Url not changed")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.feeds_menu, menu)

        if (feedLimit == 10) {
            menu?.findItem(R.id.mnu10)?.isChecked = true
        } else {
            menu?.findItem(R.id.mnu25)?.isChecked = true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        try {
            when (item.itemId) {
                R.id.mnuFree -> feedUrl = "https://rss.itunes.apple.com/api/v1/ca/ios-apps/top-free/all/%d/explicit.atom"
                R.id.mnuPaid -> feedUrl = "https://rss.itunes.apple.com/api/v1/ca/ios-apps/top-paid/all/%d/explicit.atom"
                R.id.mnuSongs -> feedUrl = "https://rss.itunes.apple.com/api/v1/ca/apple-music/top-songs/all/%d/explicit.atom"
                R.id.mnu10, R.id.mnu25 -> {
                    if (!item.isChecked) {
                        item.isChecked = true
                        feedLimit = 35 - feedLimit
                        Log.d(
                            TAG,
                            "onOptionsItemSelected: ${item.title} setting feedLimit to $feedLimit"
                        )
                    } else {
                        Log.d(
                            TAG,
                            "onOptionsItemSelected: ${item.title} setting feedLimit unchanged"
                        )
                    }
                }
                R.id.mnuRefresh -> feedCachedURL = "INVALIDATED"
                else -> return super.onOptionsItemSelected(item)
            }
            downloadUrl(feedUrl.format(feedLimit))
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "**** Check your URL! FileNotFoundException called: ${e.message} ****")
        } catch (e: Exception) {
            Log.e(TAG, "**** Unknown error: ${e.message} ****")
        }

        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_URL, feedUrl)
        outState.putInt(STATE_LIMIT, feedLimit)
    }

    override fun onDestroy() {
        super.onDestroy()

        downloadData?.cancel(true)
    }

    companion object {
        private class DownloadData(context: Context, listView: ListView) :
            AsyncTask<String, Void, String>() {
            private val TAG = "DownloadData"

            var propContext: Context by Delegates.notNull()
            var propListView: ListView by Delegates.notNull()

            init {
                propContext = context
                propListView = listView
            }

            override fun onPostExecute(result: String) {
                super.onPostExecute(result)
                val parseApplications = ParseApplications()
                parseApplications.parse(result)

                val feedAdapter =
                    FeedAdapter(propContext, R.layout.list_record, parseApplications.applications)
                propListView.adapter = feedAdapter
            }

            override fun doInBackground(vararg url: String?): String {
                Log.d(TAG, "doInBackground: starts with ${url[0]}")
                val rssFeed = downloadXML(url[0])
                if (rssFeed.isEmpty()) {
                    Log.e(TAG, "doInBackground: Error downloading")
                }
                return rssFeed
            }

            private fun downloadXML(urlPath: String?): String {
                return URL(urlPath).readText()
            }
        }
    }


}
