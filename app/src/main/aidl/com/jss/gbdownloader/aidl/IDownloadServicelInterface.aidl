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

// IDownloadServicelInterface.aidl
package com.jss.gbdownloader.aidl;

// Declare any non-default types here with import statements

interface IDownloadServicelInterface {

    boolean enqueue(String uri);
    boolean cancel(String uri);
    boolean isQueued(String uri);

}
