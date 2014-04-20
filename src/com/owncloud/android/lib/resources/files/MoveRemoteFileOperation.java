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

import java.io.IOException;

import org.apache.commons.httpclient.HttpException;
import org.apache.jackrabbit.webdav.client.methods.DavMethodBase;

import android.util.Log;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;

/**
 * Remote operation performing the move of a remote file or folder in the
 * ownCloud server.
 * 
 * @author Kaloyan Kolev
 */
public class MoveRemoteFileOperation extends RemoteOperation {

	private static final String TAG = MoveRemoteFileOperation.class.getSimpleName();

	private static final int MOVE_READ_TIMEOUT = 10000;
	private static final int MOVE_CONNECTION_TIMEOUT = 5000;

	protected String mSrcRemotePath;
	protected String mDestRemotePath;

	/**
	 * Constructor
	 * 
	 * @param srcRemotePath
	 *            The source remote file path.
	 * @param destRemotePath
	 *            The destination (to be) remote file
	 * @param isFolder
	 *            <code>true</code> for folder and <code>false</code> for files
	 */
	public MoveRemoteFileOperation(String srcRemotePath, String destRemotePath, boolean isFolder) {
		if (isFolder) {
			mSrcRemotePath = srcRemotePath.endsWith(FileUtils.PATH_SEPARATOR) ? srcRemotePath : srcRemotePath + FileUtils.PATH_SEPARATOR;
			mDestRemotePath = destRemotePath.endsWith(FileUtils.PATH_SEPARATOR) ? destRemotePath : destRemotePath + FileUtils.PATH_SEPARATOR;
		} else {
			mSrcRemotePath = srcRemotePath.endsWith(FileUtils.PATH_SEPARATOR) ? srcRemotePath.substring(0, srcRemotePath.length() - 1) : srcRemotePath;
			mDestRemotePath = destRemotePath.endsWith(FileUtils.PATH_SEPARATOR) ? destRemotePath.substring(0, destRemotePath.length() - 1) : destRemotePath;
		}
	}

	/**
	 * Performs the move operation.
	 * 
	 * @param client
	 *            Client object to communicate with the remote ownCloud server.
	 */
	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {

		RemoteOperationResult result = null;

		try {
			result = runNoLog(client, MOVE_READ_TIMEOUT, MOVE_CONNECTION_TIMEOUT);
			Log.i(TAG, "Move " + mSrcRemotePath + " to " + mDestRemotePath + ": " + result.getLogMessage());
		} catch (Exception e) {
			result = new RemoteOperationResult(e);
			Log.e(TAG, "Move " + mSrcRemotePath + " to " + mDestRemotePath + ": " + result.getLogMessage(), e);
		}

		return result;
	}

	protected RemoteOperationResult runNoLog(OwnCloudClient client, int moveReadTimeout, int moveConnectionTimeout) throws HttpException, IOException {

		RemoteOperationResult result = null;
		boolean noInvalidChars = FileUtils.isValidPath(mDestRemotePath);

		if (noInvalidChars) {

			LocalMoveMethod move = null;
			try {
				if (mSrcRemotePath.equals(mDestRemotePath)) {
					return new RemoteOperationResult(ResultCode.OK);
				}

				// check if a file with the new name already exists
				if (client.existsFile(mDestRemotePath)) {
					return new RemoteOperationResult(ResultCode.INVALID_OVERWRITE);
				}

				// e.g. "/" -> "/a/" or "/file" -> "/file/file"
				if (mSrcRemotePath.startsWith(mDestRemotePath)) {
					return new RemoteOperationResult(ResultCode.INVALID_DESTINATION_FILE);
				}

				move = new LocalMoveMethod(client.getWebdavUri() + WebdavUtils.encodePath(mSrcRemotePath), client.getWebdavUri()
						+ WebdavUtils.encodePath(mDestRemotePath));
				int status = client.executeMethod(move, moveReadTimeout, moveConnectionTimeout);

				move.getResponseBodyAsString(); // exhaust response, although
												// not interesting
				result = new RemoteOperationResult(move.succeeded(), status, move.getResponseHeaders());
			} finally {
				if (move != null)
					move.releaseConnection();
			}
		} else {
			result = new RemoteOperationResult(ResultCode.INVALID_CHARACTER_IN_NAME);
		}
		return result;
	}

	/**
	 * Move operation
	 */
	private class LocalMoveMethod extends DavMethodBase {

		public LocalMoveMethod(String uri, String dest) {
			super(uri);
			addRequestHeader(new org.apache.commons.httpclient.Header("Destination", dest));
		}

		@Override
		public String getName() {
			return "MOVE";
		}

		@Override
		protected boolean isSuccess(int status) {
			return status == 201 || status == 204;
		}

	}

}
