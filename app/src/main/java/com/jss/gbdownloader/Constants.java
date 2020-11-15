/**
 *     Copyright 2020 Jacob Sommer
 *
 *     This file is part of gbdownloader.
 *
 *     gbdownloader is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     gbdownloader is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with gbdownloader.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.jss.gbdownloader;

public class Constants {

    public static final String DL_PROG_INTENT_ACTION = "com.jss.gbdownloader.DL_PROG_INTENT_ACTION";
    public static final String DL_PROG_URL_KEY = "URL_KEY";
    public static final String DL_PROG_PROGRESS_KEY = "PROGRESS_KEY";

    public static final String API_KEY = "API_KEY_KEY";
    public static final String PREFS_FILE = "PREFS_FILE";

    public enum DLButtonState {
        READY,
        DOWNLOADING,
        PARTIAL,
        DOWNLOADED
    }

}
