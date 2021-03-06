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

package org.lol.reddit.views.liststatus;

import android.content.res.TypedArray;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.TextView;
import org.lol.reddit.R;
import org.lol.reddit.reddit.url.PostCommentListingURL;

public final class SpecificCommentThreadView extends StatusListItemView {

    private final PostCommentListingURL mUrl;

    public SpecificCommentThreadView(final Activity activity, final PostCommentListingURL url) {

        super(activity);

        mUrl = url;

        final TypedArray attr = activity.obtainStyledAttributes(new int[]{
                R.attr.rrCommentSpecificThreadHeaderBackCol,
                R.attr.rrCommentSpecificThreadHeaderTextCol
        });

        final int rrCommentSpecificThreadHeaderBackCol = attr.getColor(0, 0);
        final int rrCommentSpecificThreadHeaderTextCol = attr.getColor(1, 0);

        final TextView textView = new TextView(activity);
        textView.setText(R.string.comment_header_specific_thread_title);
        textView.setTextColor(rrCommentSpecificThreadHeaderTextCol);
        textView.setTextSize(15.0f);
        textView.setPadding((int) (15 * dpScale), (int) (10 * dpScale), (int) (10 * dpScale), (int) (4 * dpScale));

        final TextView messageView = new TextView(activity);
        messageView.setText(R.string.comment_header_specific_thread_message);
        messageView.setTextColor(rrCommentSpecificThreadHeaderTextCol);
        messageView.setTextSize(12.0f);
        messageView.setPadding((int) (15 * dpScale), 0, (int) (10 * dpScale), (int) (10 * dpScale));

        final LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(textView);
        layout.addView(messageView);

        setContents(layout);
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);

        setBackgroundColor(rrCommentSpecificThreadHeaderBackCol);
    }

    public PostCommentListingURL getUrl() {
        return mUrl;
    }
}
