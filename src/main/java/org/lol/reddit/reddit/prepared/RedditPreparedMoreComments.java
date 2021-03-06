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

import org.lol.reddit.jsonwrap.JsonValue;
import org.lol.reddit.reddit.things.RedditMoreComments;
import org.lol.reddit.reddit.url.PostCommentListingURL;

import java.util.ArrayList;
import java.util.List;

public class RedditPreparedMoreComments {

    private final RedditMoreComments mSrc;
    private final PostCommentListingURL mPostUrl;

    public RedditPreparedMoreComments(final RedditMoreComments src, final PostCommentListingURL postUrl) {
        mSrc = src;
        mPostUrl = postUrl;
    }

    public List<PostCommentListingURL> getMoreUrls() {

        final ArrayList<PostCommentListingURL> urls = new ArrayList<PostCommentListingURL>(16);

        if (mSrc.count > 0) {
            for (JsonValue child : mSrc.children) {
                if (child.getType() == JsonValue.Type.STRING) {
                    urls.add(mPostUrl.commentId(child.asString()));
                }
            }

        } else {
            urls.add(mPostUrl.commentId(mSrc.parent_id));
        }

        return urls;
    }

    public int getCount() {
        return mSrc.count;
    }
}
