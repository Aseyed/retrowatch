/*
 * Copyright (C) 2014 The Retro Watch - Open source smart watch project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hardcopy.retrowatch.connectivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.hardcopy.retrowatch.utils.Logs;
import com.hardcopy.retrowatch.utils.Utils;

import android.os.Handler;
import android.os.Looper;


public class HttpFileAsyncTask implements HttpInterface {
	// Global variables
	public static final String tag = "HttpFileAsyncTask";
	private static final Executor executor = Executors.newSingleThreadExecutor();
	private static final Handler handler = new Handler(Looper.getMainLooper());
	
	private int mType;
	private String mID = null;
	private String mURL = null;
	private String mDir = null;
	private String mFileName = null;
	private int mResultStatus = MSG_HTTP_RESULT_CODE_OK;
	
	private static final int CONNECTION_TIMEOUT = 5000;
	
	// Context, system
	private HttpListener mListener;
	

	// Constructor
	public HttpFileAsyncTask(HttpListener l, int type, String id, String url, String directory, String filename) {
		mListener = l;
		mType = type;			// Not used in async task. will be used in callback
		mID = id;				// Not used in async task. will be used in callback
		mURL = url;
		mDir = directory;
		mFileName = filename;
	}

	/**
	 * Execute the HTTP file download asynchronously
	 */
	public void execute() {
		executor.execute(this::doInBackground);
	}
	
	private void doInBackground() 
	{
		if(mListener==null || mID==null || mURL==null || mDir==null || mFileName==null) { 
			//Logs.d(tag, "###### Error!!! : Parameter is null. Check parameter");
			onPostExecute("");
			return; 
		}
					
		URL url = null;
		try {
			url = new URL(mURL);
		} 
		catch (MalformedURLException e1) { 
			e1.printStackTrace();
			mResultStatus = MSG_HTTP_RESULT_CODE_ERROR_REQUEST_EXCEPTION;
			Logs.d(tag, "# URL = "+url);
			//Logs.d(tag, "###### Error!!! : MalformedURLException ");
			onPostExecute(null);
			return;
		}
		
		if(Utils.checkFileExists(mDir, mFileName)) {
			onPostExecute(null);
			return;
		}
		String filePathAndName = new String(mDir+"/"+mFileName);

		try {
			// Set up connection
			HttpURLConnection conn= (HttpURLConnection)url.openConnection(); 
			conn.setConnectTimeout(CONNECTION_TIMEOUT);
			conn.setReadTimeout(CONNECTION_TIMEOUT);
			conn.connect(); 
			
			// Copy input stream (one is for calculate bitmap size and another is for decode bitmap)
			InputStream inputStream = conn.getInputStream();
			
			// Make file and output stream
			File file = new File(filePathAndName);
			OutputStream outStream = new FileOutputStream(file);
			
			byte[] buf = new byte[1024];
			int len = 0;
			
			while ((len = inputStream.read(buf)) > 0) {
				outStream.write(buf, 0, len);
			}
			outStream.close();
			inputStream.close();
 			
			mResultStatus = MSG_HTTP_RESULT_CODE_OK;
 			
		} catch (Exception e) {
			mResultStatus = MSG_HTTP_RESULT_CODE_ERROR_UNKNOWN;
			//Logs.d(tag, "###### Error!!! : Cannot download file... ");
			e.printStackTrace();
			onPostExecute(null);
			return; 
		}

		onPostExecute(filePathAndName);
	}

	private void onPostExecute(String filename) {
		// Post to UI thread using handler
		final String finalFilename = filename;
		final int finalStatus = mResultStatus;
		handler.post(() -> {
			if(mListener != null) {
				mListener.OnReceiveFileResponse(mType, mID, finalFilename, mURL, finalStatus);
			}
		});
	}
	
}
