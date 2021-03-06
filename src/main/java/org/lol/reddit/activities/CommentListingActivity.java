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

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import org.holoeverywhere.preference.PreferenceManager;
import org.holoeverywhere.preference.SharedPreferences;
import org.lol.reddit.R;
import org.lol.reddit.account.RedditAccountChangeListener;
import org.lol.reddit.account.RedditAccountManager;
import org.lol.reddit.common.General;
import org.lol.reddit.common.LinkHandler;
import org.lol.reddit.common.PrefsUtility;
import org.lol.reddit.fragments.CommentListingFragment;
import org.lol.reddit.fragments.SessionListDialog;
import org.lol.reddit.listingcontrollers.CommentListingController;
import org.lol.reddit.reddit.prepared.RedditPreparedPost;
import org.lol.reddit.reddit.url.PostCommentListingURL;
import org.lol.reddit.reddit.url.RedditURLParser;
import org.lol.reddit.views.RedditPostView;

import java.util.UUID;

public class CommentListingActivity extends RefreshableActivity
        implements RedditAccountChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener,
        OptionsMenuUtility.OptionsMenuCommentsListener,
        RedditPostView.PostSelectionListener,
        SessionChangeListener {

    private CommentListingController controller;

    private SharedPreferences sharedPreferences;

    public void onCreate(final Bundle savedInstanceState) {

        PrefsUtility.applyTheme(this);

        super.onCreate(savedInstanceState);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        OptionsMenuUtility.fixActionBar(this, getString(R.string.app_name));

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        final boolean solidblack = PrefsUtility.appearance_solidblack(this, sharedPreferences)
                && PrefsUtility.appearance_theme(this, sharedPreferences) == PrefsUtility.AppearanceTheme.NIGHT;

        // TODO load from savedInstanceState

        final View layout = getLayoutInflater().inflate(R.layout.main_single);
        if (solidblack) layout.setBackgroundColor(Color.BLACK);
        setContentView(layout);

        RedditAccountManager.getInstance(this).addUpdateListener(this);

        if (getIntent() != null) {

            final Intent intent = getIntent();

            final String url = intent.getDataString();
            controller = new CommentListingController(RedditURLParser.parseProbableCommentListing(Uri.parse(url)), this);

            doRefresh(RefreshableFragment.COMMENTS, false);

        } else {
            throw new RuntimeException("Nothing to show! (should load from bundle)"); // TODO
        }
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        // TODO save instance state
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        OptionsMenuUtility.prepare(this, menu, false, false, true, false, controller.isSortable(), null, false, true);
        return true;
    }

    public void onRedditAccountChanged() {
        requestRefresh(RefreshableFragment.ALL, false);
    }

    @Override
    protected void doRefresh(final RefreshableFragment which, final boolean force) {
        final CommentListingFragment fragment = controller.get(force);
        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_single_frame, fragment, "comment_listing_fragment");
        transaction.commit();
        OptionsMenuUtility.fixActionBar(this, controller.getCommentListingUrl().humanReadableName(this, false));
    }

    public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {

        if (PrefsUtility.isRestartRequired(this, key)) {
            requestRefresh(RefreshableFragment.RESTART, false);
        }

        if (PrefsUtility.isRefreshRequired(this, key)) {
            requestRefresh(RefreshableFragment.ALL, false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onRefreshComments() {
        controller.setSession(null);
        requestRefresh(RefreshableFragment.COMMENTS, true);
    }

    public void onPastComments() {
        final SessionListDialog sessionListDialog = SessionListDialog.newInstance(controller.getUri(), controller.getSession(), SessionChangeListener.SessionChangeType.COMMENTS);
        sessionListDialog.show(this);
    }

    public void onSortSelected(final PostCommentListingURL.Sort order) {
        controller.setSort(order);
        requestRefresh(RefreshableFragment.COMMENTS, false);
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

    public void onSessionRefreshSelected(SessionChangeType type) {
        onRefreshComments();
    }

    public void onSessionSelected(UUID session, SessionChangeType type) {
        controller.setSession(session);
        requestRefresh(RefreshableFragment.COMMENTS, false);
    }

    public void onSessionChanged(UUID session, SessionChangeType type, long timestamp) {
        controller.setSession(session);
    }

    public void onPostSelected(final RedditPreparedPost post) {
        LinkHandler.onLinkClicked(this, post.url, false, post.src);
    }

    public void onPostCommentsSelected(final RedditPreparedPost post) {
        LinkHandler.onLinkClicked(this, PostCommentListingURL.forPostId(post.idAlone).toString(), false);
    }

    @Override
    public void onBackPressed() {
        if (General.onBackPressed()) super.onBackPressed();
    }
}