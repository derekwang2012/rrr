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

package org.lol.reddit;

import android.os.Environment;
import android.util.Log;
import org.holoeverywhere.app.Application;
import org.holoeverywhere.preference.PreferenceManager;
import org.lol.reddit.cache.CacheManager;
import org.lol.reddit.common.Alarms;
import org.lol.reddit.common.PrefsUtility;
import org.lol.reddit.receivers.NewMessageChecker;
import org.lol.reddit.reddit.prepared.RedditChangeDataManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.UUID;

public class Reader extends Application {

    @Override
    public void onCreate() {

        super.onCreate();

        Log.i("RedReader", "Application created.");

        final Thread.UncaughtExceptionHandler androidHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread thread, Throwable t) {

                try {
                    t.printStackTrace();

                    File dir = Environment.getExternalStorageDirectory();

                    if (dir == null) {
                        dir = Environment.getDataDirectory();
                    }

                    final FileOutputStream fos = new FileOutputStream(new File(dir, "redreader_crash_log_" + UUID.randomUUID().toString() + ".txt"));
                    final PrintWriter pw = new PrintWriter(fos);
                    t.printStackTrace(pw);
                    pw.flush();
                    pw.close();

                } catch (Throwable t1) {
                }

                androidHandler.uncaughtException(thread, t);
            }
        });

        final CacheManager cm = CacheManager.getInstance(this);

        cm.pruneTemp();

        new Thread() {
            @Override
            public void run() {

                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

                cm.pruneCache(); // Hope for the best :)

                final RedditChangeDataManager cdm = RedditChangeDataManager.getInstance(Reader.this);
                cdm.prune(PrefsUtility.pref_cache_maxage(Reader.this, PreferenceManager.getDefaultSharedPreferences(Reader.this)));

            }
        }.start();

        Alarms.onBoot(this);

        NewMessageChecker.checkForNewMessages(this);
    }
}
