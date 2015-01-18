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

package org.lol.reddit.reddit.prepared;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.SpannableStringBuilder;
import android.view.ViewGroup;
import org.apache.commons.lang3.StringEscapeUtils;
import org.holoeverywhere.app.Activity;
import org.lol.reddit.R;
import org.lol.reddit.common.BetterSSB;
import org.lol.reddit.common.LinkHandler;
import org.lol.reddit.common.RRTime;
import org.lol.reddit.reddit.RedditPreparedInboxItem;
import org.lol.reddit.reddit.prepared.markdown.MarkdownParagraphGroup;
import org.lol.reddit.reddit.prepared.markdown.MarkdownParser;
import org.lol.reddit.reddit.things.RedditMessage;

import java.util.HashSet;

public final class RedditPreparedMessage implements RedditPreparedInboxItem {

    public SpannableStringBuilder header;
    public final MarkdownParagraphGroup body;
    public final String idAndType;
    public final RedditMessage src;

    public RedditPreparedMessage(final Context context, final RedditMessage message, final long timestamp) {

        this.src = message;

        // TODO custom time

        final TypedArray appearance = context.obtainStyledAttributes(new int[]{
                R.attr.rrCommentHeaderBoldCol,
                R.attr.rrCommentHeaderAuthorCol,
        });

        int rrCommentHeaderBoldCol = appearance.getColor(0, 255);
        int rrCommentHeaderAuthorCol = appearance.getColor(1, 255);

        body = MarkdownParser.parse(StringEscapeUtils.unescapeHtml4(message.body).toCharArray());

        idAndType = message.name;

        final BetterSSB sb = new BetterSSB();

        if (src.author == null) {
            sb.append("[" + context.getString(R.string.general_unknown) + "]", BetterSSB.FOREGROUND_COLOR | BetterSSB.BOLD, rrCommentHeaderAuthorCol, 0, 1f);
        } else {
            sb.append(src.author, BetterSSB.FOREGROUND_COLOR | BetterSSB.BOLD, rrCommentHeaderAuthorCol, 0, 1f);
        }

        sb.append("   ", 0);
        sb.append(RRTime.formatDurationFrom(context, src.created_utc * 1000L), BetterSSB.FOREGROUND_COLOR | BetterSSB.BOLD, rrCommentHeaderBoldCol, 0, 1f);

        header = sb.get();
    }

    public HashSet<String> computeAllLinks() {
        return LinkHandler.computeAllLinks(StringEscapeUtils.unescapeHtml4(src.body_html));
    }

    public SpannableStringBuilder getHeader() {
        return header;
    }

    public ViewGroup getBody(Activity context, float textSize, Integer textCol, boolean showLinkButtons) {
        return body.buildView(context, textCol, textSize, showLinkButtons);
    }

    public void handleInboxClick(Activity activity) {
        // Do nothing
    }
}
