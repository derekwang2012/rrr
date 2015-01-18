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

package org.lol.reddit.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.*;
import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Fragment;
import org.holoeverywhere.widget.FrameLayout;
import org.holoeverywhere.widget.ProgressBar;
import org.holoeverywhere.widget.Toast;
import org.lol.reddit.R;
import org.lol.reddit.account.RedditAccountManager;
import org.lol.reddit.cache.CacheManager;
import org.lol.reddit.common.AndroidApi;
import org.lol.reddit.common.General;
import org.lol.reddit.common.LinkHandler;
import org.lol.reddit.reddit.prepared.RedditPreparedPost;
import org.lol.reddit.reddit.things.RedditPost;
import org.lol.reddit.reddit.url.RedditURLParser;
import org.lol.reddit.views.RedditPostView;
import org.lol.reddit.views.WebViewFixed;
import org.lol.reddit.views.bezelmenu.BezelSwipeOverlay;
import org.lol.reddit.views.bezelmenu.SideToolbarOverlay;

import java.util.Timer;
import java.util.TimerTask;

public class WebViewFragment extends Fragment implements RedditPostView.PostSelectionListener {

    private String url, html;
    private volatile String currentUrl;
    private volatile boolean goingBack;
    private volatile int lastBackDepthAttempt;

    private WebViewFixed webView;
    private ProgressBar progressView;
    private FrameLayout outer;

    public static WebViewFragment newInstance(final String url, final RedditPost post) {

        final WebViewFragment f = new WebViewFragment();

        final Bundle bundle = new Bundle(1);
        bundle.putString("url", url);
        if (post != null) bundle.putParcelable("post", post);
        f.setArguments(bundle);

        return f;
    }

    public static WebViewFragment newInstanceHtml(final String html) {

        final WebViewFragment f = new WebViewFragment();

        final Bundle bundle = new Bundle(1);
        bundle.putString("html", html);
        f.setArguments(bundle);

        return f;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        // TODO load position/etc?
        super.onCreate(savedInstanceState);
        url = getArguments().getString("url");
        html = getArguments().getString("html");
    }

    @SuppressLint("NewApi")
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {

        final Context context = inflater.getContext();

        CookieSyncManager.createInstance(getSupportActivity());

        outer = (FrameLayout) inflater.inflate(R.layout.web_view_fragment);

        final RedditPost src_post = getArguments().getParcelable("post");
        final RedditPreparedPost post = src_post == null ? null
                : new RedditPreparedPost(context, CacheManager.getInstance(context), 0, src_post, -1, false,
                false, false, false, RedditAccountManager.getInstance(context).getDefaultAccount(), false);

        webView = (WebViewFixed) outer.findViewById(R.id.web_view_fragment_webviewfixed);
        final FrameLayout loadingViewFrame = (FrameLayout) outer.findViewById(R.id.web_view_fragment_loadingview_frame);

        progressView = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        loadingViewFrame.addView(progressView);
        loadingViewFrame.setPadding(General.dpToPixels(context, 10), 0, General.dpToPixels(context, 10), 0);

        final WebSettings settings = webView.getSettings();

        settings.setBuiltInZoomControls(true);
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setDomStorageEnabled(true);

        if (AndroidApi.isHoneyCombOrLater()) {
            settings.setDisplayZoomControls(false);
        }

        // TODO handle long clicks

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, final int newProgress) {

                super.onProgressChanged(view, newProgress);

                General.UI_THREAD_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        progressView.setProgress(newProgress);
                        progressView.setVisibility(newProgress == 100 ? View.GONE : View.VISIBLE);
                    }
                });
            }
        });


        if (url != null) {

            webView.loadUrl(url);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                    // Go back if loading same page to prevent redirect loops.
                    if (goingBack && currentUrl != null && url != null && url.equals(currentUrl)) {

                        General.quickToast(context,
                                String.format("Handling redirect loop (level %d)", -lastBackDepthAttempt), Toast.LENGTH_SHORT);

                        lastBackDepthAttempt--;

                        if (webView.canGoBackOrForward(lastBackDepthAttempt)) {
                            webView.goBackOrForward(lastBackDepthAttempt);
                        } else {
                            getSupportActivity().finish();
                        }
                    } else {

                        if (RedditURLParser.parse(Uri.parse(url)) != null) {
                            LinkHandler.onLinkClicked(getSupportActivity(), url, false);
                        } else {
                            webView.loadUrl(url);
                            currentUrl = url;
                        }
                    }

                    return true;
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    getSupportActivity().setTitle(url);
                }

                @Override
                public void onPageFinished(final WebView view, final String url) {
                    super.onPageFinished(view, url);

                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {

                            General.UI_THREAD_HANDLER.post(new Runnable() {
                                public void run() {

                                    if (currentUrl == null || url == null) return;

                                    if (!url.equals(view.getUrl())) return;

                                    if (goingBack && url.equals(currentUrl)) {

                                        General.quickToast(context,
                                                String.format("Handling redirect loop (level %d)", -lastBackDepthAttempt));

                                        lastBackDepthAttempt--;

                                        if (webView.canGoBackOrForward(lastBackDepthAttempt)) {
                                            webView.goBackOrForward(lastBackDepthAttempt);
                                        } else {
                                            getSupportActivity().finish();
                                        }

                                    } else {
                                        goingBack = false;
                                    }
                                }
                            });
                        }
                    }, 1000);
                }

                @Override
                public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                    super.doUpdateVisitedHistory(view, url, isReload);
                }
            });

        } else {
            webView.loadData(html, "text/html", "UTF-8");
        }

        final FrameLayout outerFrame = new FrameLayout(context);
        outerFrame.addView(outer);

        if (post != null) {

            final SideToolbarOverlay toolbarOverlay = new SideToolbarOverlay(context);

            final BezelSwipeOverlay bezelOverlay = new BezelSwipeOverlay(context, new BezelSwipeOverlay.BezelSwipeListener() {

                public boolean onSwipe(BezelSwipeOverlay.SwipeEdge edge) {

                    toolbarOverlay.setContents(post.generateToolbar(getSupportActivity(), false, toolbarOverlay));
                    toolbarOverlay.show(edge == BezelSwipeOverlay.SwipeEdge.LEFT ?
                            SideToolbarOverlay.SideToolbarPosition.LEFT : SideToolbarOverlay.SideToolbarPosition.RIGHT);
                    return true;
                }

                public boolean onTap() {

                    if (toolbarOverlay.isShown()) {
                        toolbarOverlay.hide();
                        return true;
                    }

                    return false;
                }
            });

            outerFrame.addView(bezelOverlay);
            outerFrame.addView(toolbarOverlay);

            bezelOverlay.getLayoutParams().width = android.widget.FrameLayout.LayoutParams.MATCH_PARENT;
            bezelOverlay.getLayoutParams().height = android.widget.FrameLayout.LayoutParams.MATCH_PARENT;

            toolbarOverlay.getLayoutParams().width = android.widget.FrameLayout.LayoutParams.MATCH_PARENT;
            toolbarOverlay.getLayoutParams().height = android.widget.FrameLayout.LayoutParams.MATCH_PARENT;
        }

        return outerFrame;
    }

    @Override
    public void onDestroyView() {

        webView.stopLoading();
        webView.loadData("<html></html>", "text/plain", "UTF-8");
        webView.reload();
        webView.loadUrl("about:blank");
        outer.removeAllViews();
        webView.destroy();

        final CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        super.onDestroyView();
    }

    public boolean onBackButtonPressed() {

        if (webView.canGoBack()) {
            goingBack = true;
            lastBackDepthAttempt = -1;
            webView.goBack();
            return true;
        }

        return false;
    }

    public void onPostSelected(final RedditPreparedPost post) {
        ((RedditPostView.PostSelectionListener) getSupportActivity()).onPostSelected(post);
    }

    public void onPostCommentsSelected(final RedditPreparedPost post) {
        ((RedditPostView.PostSelectionListener) getSupportActivity()).onPostCommentsSelected(post);
    }

    public String getCurrentUrl() {
        return (currentUrl != null) ? currentUrl : url;
    }

    @Override
    @SuppressLint("NewApi")
    public void onPause() {
        super.onPause();

        if (AndroidApi.isHoneyCombOrLater()) {
            webView.onPause();
        }

        webView.pauseTimers();
    }

    @Override
    @SuppressLint("NewApi")
    public void onResume() {
        super.onResume();
        webView.resumeTimers();

        if (AndroidApi.isHoneyCombOrLater()) {
            webView.onResume();
        }
    }

    public void clearCache() {
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();

        final CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
    }
}