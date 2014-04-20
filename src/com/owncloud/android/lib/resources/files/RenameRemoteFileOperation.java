/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud Inc.
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.resources.files;

import java.io.File;

import android.util.Log;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;

/**
 * Remote operation performing the rename of a remote file or folder in the
 * ownCloud server.
 * 
 * @author David A. Velasco
 * @author masensio
 */
public class RenameRemoteFileOperation extends MoveRemoteFileOperation {

	private static final String TAG = RenameRemoteFileOperation.class.getSimpleName();

	private static final int RENAME_READ_TIMEOUT = 10000;
	private static final int RENAME_CONNECTION_TIMEOUT = 5000;

	private String mOldName;
	private String mNewName;

	/**
	 * Constructor
	 * 
	 * @param oldName
	 *            Old name of the file.
	 * @param oldRemotePath
	 *            Old remote path of the file.
	 * @param newName
	 *            New name to set as the name of file.
	 * @param isFolder
	 *            'true' for folder and 'false' for files
	 */
	public RenameRemoteFileOperation(String oldName, String oldRemotePath, String newName, boolean isFolder) {
		super(oldRemotePath, getNewRemotePath(oldRemotePath, newName, isFolder), isFolder);
		mOldName = oldName;
		mNewName = newName;
	}

	/**
	 * Calculates the new remote path to rename to
	 * 
	 * @param oldRemotePath
	 *            Old remote path of the file
	 * @param newName
	 *            New name to set as file name
	 * @param isFolder
	 *            If renaming a folder
	 * @return the new remote file path to rename to
	 */
	private static String getNewRemotePath(String oldRemotePath, String newName, boolean isFolder) {
		String parent = (new File(oldRemotePath)).getParent();
		parent = (parent.endsWith(FileUtils.PATH_SEPARATOR)) ? parent : parent + FileUtils.PATH_SEPARATOR;
		String resultPath = parent + newName;
		if (isFolder) {
			resultPath += FileUtils.PATH_SEPARATOR;
		}

		return resultPath;
	}

	/**
	 * Performs the rename operation.
	 * 
	 * @param client
	 *            Client object to communicate with the remote ownCloud server.
	 */
	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {

		if (mNewName.equals(mOldName)) {
			return new RemoteOperationResult(ResultCode.OK);
		}

		RemoteOperationResult result = null;

		try {
			result = runNoLog(client, RENAME_READ_TIMEOUT, RENAME_CONNECTION_TIMEOUT);
			Log.i(TAG, "Rename " + mSrcRemotePath + " to " + mDestRemotePath + ": " + result.getLogMessage());
		} catch (Exception e) {
			result = new RemoteOperationResult(e);
			Log.e(TAG, "Rename " + mSrcRemotePath + " to " + ((mDestRemotePath == null) ? mNewName : mDestRemotePath) + ": " + result.getLogMessage(), e);
		}

		return result;
	}

}
