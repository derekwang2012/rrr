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

package org.lol.reddit.reddit.api;

import android.content.Context;
import org.apache.http.StatusLine;
import org.lol.reddit.cache.RequestFailureType;
import org.lol.reddit.common.General;
import org.lol.reddit.common.RRError;

import java.net.URI;

public class SubredditRequestFailure {
    public final RequestFailureType requestFailureType;
    public final Throwable t;
    public final StatusLine statusLine;
    public final String readableMessage;
    public final String url;

    public SubredditRequestFailure(RequestFailureType requestFailureType, Throwable t,
                                   StatusLine statusLine, String readableMessage, String url) {
        this.requestFailureType = requestFailureType;
        this.t = t;
        this.statusLine = statusLine;
        this.readableMessage = readableMessage;
        this.url = url;
    }

    public SubredditRequestFailure(RequestFailureType requestFailureType, Throwable t,
                                   StatusLine statusLine, String readableMessage, URI url) {
        this(requestFailureType, t, statusLine, readableMessage, url != null ? url.toString() : null);
    }

    public RRError asError(Context context) {
        return General.getGeneralErrorForFailure(context, requestFailureType, t, statusLine, url);
    }
}
