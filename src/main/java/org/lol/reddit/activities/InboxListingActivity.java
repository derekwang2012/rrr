/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.lol.reddit.activities;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import com.actionbarsherlock.view.MenuItem;
import org.apache.http.StatusLine;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.preference.PreferenceManager;
import org.holoeverywhere.preference.SharedPreferences;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.TextView;
import org.lol.reddit.R;
import org.lol.reddit.account.RedditAccount;
import org.lol.reddit.account.RedditAccountManager;
import org.lol.reddit.adapters.InboxListingAdapter;
import org.lol.reddit.cache.CacheManager;
import org.lol.reddit.cache.CacheRequest;
import org.lol.reddit.cache.RequestFailureType;
import org.lol.reddit.common.*;
import org.lol.reddit.jsonwrap.JsonBufferedArray;
import org.lol.reddit.jsonwrap.JsonBufferedObject;
import org.lol.reddit.jsonwrap.JsonValue;
import org.lol.reddit.reddit.RedditPreparedInboxItem;
import org.lol.reddit.reddit.prepared.RedditPreparedComment;
import org.lol.reddit.reddit.prepared.RedditPreparedMessage;
import org.lol.reddit.reddit.things.RedditMessage;
import org.lol.reddit.reddit.things.RedditThing;
import org.lol.reddit.views.liststatus.ErrorView;
import org.lol.reddit.views.liststatus.LoadingView;

import java.net.URI;
import java.util.EnumSet;
import java.util.UUID;

public final class InboxListingActivity extends Activity {

    private InboxListingAdapter adapter;

    private LoadingView loadingView;
    private LinearLayout notifications;

    private CacheRequest request;

    private EnumSet<PrefsUtility.AppearanceCommentHeaderItems> headerItems;

    private boolean isModmail = false;

    private final Handler itemHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(final Message msg) {
            adapter.addItem((RedditPreparedInboxItem) msg.obj);
        }
    };

    // TODO load more on scroll to bottom?

    @Override
    public void onCreate(Bundle savedInstanceState) {

        PrefsUtility.applyTheme(this);
        super.onCreate(savedInstanceState);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        final boolean solidblack = PrefsUtility.appearance_solidblack(this, sharedPreferences)
                && PrefsUtility.appearance_theme(this, sharedPreferences) == PrefsUtility.AppearanceTheme.NIGHT;

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final String title;

        isModmail = getIntent() != null && getIntent().getBooleanExtra("modmail", false);

        if (!isModmail) {
            title = getString(R.string.mainmenu_inbox);
        } else {
            title = getString(R.string.mainmenu_modmail);
        }

        OptionsMenuUtility.fixActionBar(this, title);

        headerItems = PrefsUtility.appearance_comment_header_items(this, sharedPreferences);
        headerItems.remove(PrefsUtility.AppearanceCommentHeaderItems.SCORE);

        final LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(android.widget.LinearLayout.VERTICAL);

        if (solidblack) {
            outer.setBackgroundColor(Color.BLACK);
        }

        loadingView = new LoadingView(this, getString(R.string.download_waiting), true, true);

        notifications = new LinearLayout(this);
        notifications.setOrientation(android.widget.LinearLayout.VERTICAL);
        notifications.addView(loadingView);

        final ListView lv = new ListView(this);

        lv.setSmoothScrollbarEnabled(false);
        lv.setVerticalFadingEdgeEnabled(false);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final Object item = lv.getAdapter().getItem(position);

                if (item != null && item instanceof RedditPreparedInboxItem) {
                    ((RedditPreparedInboxItem) item).handleInboxClick(InboxListingActivity.this);
                }
            }
        });

        adapter = new InboxListingAdapter(this, this);
        lv.setAdapter(adapter);

        registerForContextMenu(lv);

        outer.addView(notifications);
        outer.addView(lv);

        makeFirstRequest(this);

        setContentView(outer);
    }

    public void cancel() {
        if (request != null) request.cancel();
    }

    private void makeFirstRequest(final Context context) {

        final RedditAccount user = RedditAccountManager.getInstance(context).getDefaultAccount();
        final CacheManager cm = CacheManager.getInstance(context);

        final URI url;

        if (!isModmail) {
            url = Constants.Reddit.getUri("/message/inbox.json?mark=true&limit=100");
        } else {
            url = Constants.Reddit.getUri("/message/moderator.json?limit=100");
        }

        // TODO parameterise limit
        request = new CacheRequest(url, user, null, Constants.Priority.API_INBOX_LIST, 0, CacheRequest.DownloadType.FORCE, Constants.FileType.INBOX_LIST, true, true, true, context) {

            @Override
            protected void onDownloadNecessary() {
            }

            @Override
            protected void onDownloadStarted() {
            }

            @Override
            protected void onCallbackException(final Throwable t) {
                request = null;
                BugReportActivity.handleGlobalError(context, t);
            }

            @Override
            protected void onFailure(final RequestFailureType type, final Throwable t, final StatusLine status, final String readableMessage) {

                request = null;

                if (loadingView != null) loadingView.setDone(R.string.download_failed);

                final RRError error = General.getGeneralErrorForFailure(context, type, t, status, url.toString());
                General.UI_THREAD_HANDLER.post(new Runnable() {
                    public void run() {
                        notifications.addView(new ErrorView(InboxListingActivity.this, error));
                    }
                });

                if (t != null) t.printStackTrace();
            }

            @Override
            protected void onProgress(final long bytesRead, final long totalBytes) {
            }

            @Override
            protected void onSuccess(final CacheManager.ReadableCacheFile cacheFile, final long timestamp, final UUID session, final boolean fromCache, final String mimetype) {
                request = null;
            }

            @Override
            public void onJsonParseStarted(final JsonValue value, final long timestamp, final UUID session, final boolean fromCache) {

                if (loadingView != null) loadingView.setIndeterminate(R.string.download_downloading);

                // TODO pref (currently 10 mins)
                // TODO xml
                if (fromCache && RRTime.since(timestamp) > 10 * 60 * 1000) {
                    General.UI_THREAD_HANDLER.post(new Runnable() {
                        public void run() {
                            final TextView cacheNotif = new TextView(context);
                            cacheNotif.setText(context.getString(R.string.listing_cached) + RRTime.formatDateTime(timestamp, context));
                            final int paddingPx = General.dpToPixels(context, 6);
                            final int sidePaddingPx = General.dpToPixels(context, 10);
                            cacheNotif.setPadding(sidePaddingPx, paddingPx, sidePaddingPx, paddingPx);
                            cacheNotif.setTextSize(13f);
                            notifications.addView(cacheNotif);
                            adapter.notifyDataSetChanged();
                        }
                    });
                }

                // TODO {"error": 403} is received for unauthorized subreddits

                try {

                    final JsonBufferedObject root = value.asObject();
                    final JsonBufferedObject data = root.getObject("data");
                    final JsonBufferedArray children = data.getArray("children");

                    for (JsonValue child : children) {

                        final RedditThing thing = child.asObject(RedditThing.class);

                        switch (thing.getKind()) {
                            case COMMENT:
                                final RedditPreparedComment comment = new RedditPreparedComment(
                                        InboxListingActivity.this, thing.asComment(), timestamp, false, null, user, headerItems);
                                itemHandler.sendMessage(General.handlerMessage(0, comment));

                                break;

                            case MESSAGE:
                                final RedditPreparedMessage message = new RedditPreparedMessage(
                                        InboxListingActivity.this, thing.asMessage(), timestamp);
                                itemHandler.sendMessage(General.handlerMessage(0, message));

                                if (message.src.replies != null && message.src.replies.getType() == JsonValue.Type.OBJECT) {

                                    final JsonBufferedArray replies = message.src.replies.asObject().getObject("data").getArray("children");

                                    for (JsonValue childMsgValue : replies) {
                                        final RedditMessage childMsgRaw = childMsgValue.asObject(RedditThing.class).asMessage();
                                        final RedditPreparedMessage childMsg = new RedditPreparedMessage(InboxListingActivity.this, childMsgRaw, timestamp);
                                        itemHandler.sendMessage(General.handlerMessage(0, childMsg));
                                    }
                                }

                                break;

                            default:
                                throw new RuntimeException("Unknown item in list.");
                        }
                    }

                } catch (Throwable t) {
                    notifyFailure(RequestFailureType.PARSE, t, null, "Parse failure");
                    return;
                }

                if (loadingView != null) loadingView.setDone(R.string.download_done);
            }
        };

        cm.makeRequest(request);
    }

    @Override
    public void onBackPressed() {
        if (General.onBackPressed()) super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
