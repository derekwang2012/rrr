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

import android.graphics.Color;
import android.view.View;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.TextView;
import org.lol.reddit.common.RRError;
import org.lol.reddit.fragments.ErrorPropertiesDialog;

public final class ErrorView extends StatusListItemView {

    // TODO make XML
    // TODO show exception on long click
    public ErrorView(final Activity activity, final RRError error) {

        super(activity);

        final TextView textView = new TextView(activity);
        textView.setText(error.title);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(15.0f);
        textView.setPadding((int) (15 * dpScale), (int) (10 * dpScale), (int) (10 * dpScale), (int) (4 * dpScale));

        final TextView messageView = new TextView(activity);
        messageView.setText(error.message);

        messageView.setTextColor(Color.WHITE);
        messageView.setTextSize(12.0f);
        messageView.setPadding((int) (15 * dpScale), 0, (int) (10 * dpScale), (int) (10 * dpScale));

        final LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(textView);
        layout.addView(messageView);

        setContents(layout);

        setBackgroundColor(Color.rgb(0xCC, 0x00, 0x00));

        setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ErrorPropertiesDialog.newInstance(error).show(activity);
            }
        });
    }
}
