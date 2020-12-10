package com.study.ipcamera;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class SocketClient extends Thread {
	private Socket mSocket;
	private CameraPreview mCameraPreview;
	private static final String TAG = "socket";
	private String mIP = "192.168.1.69";
	private int mPort = 8080;
	private String mUser;
	private String mPass;

	public SocketClient(CameraPreview preview, String ip, int port, String user, String pass) {
	    mCameraPreview = preview;
	    mIP = ip;
	    mPort = port;
	    mUser = user;
	    mPass = pass;
		start();
	}
	
	public SocketClient(CameraPreview preview, String user, String pass) {
	    mCameraPreview = preview;
		mUser = user;
		mPass = pass;
		start();
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		super.run();
		BufferedOutputStream outputStream = null;
		BufferedInputStream inputStream = null;
		try {
			mSocket = new Socket();
			mSocket.connect(new InetSocketAddress(mIP, mPort), 10000); // hard-code server address
			outputStream = new BufferedOutputStream(mSocket.getOutputStream());
			inputStream = new BufferedInputStream(mSocket.getInputStream());

			JsonObject jsonObj = new JsonObject();
            jsonObj.addProperty("type", "data");
            jsonObj.addProperty("length", mCameraPreview.getPreviewLength());
            jsonObj.addProperty("width", mCameraPreview.getPreviewWidth());
            jsonObj.addProperty("height", mCameraPreview.getPreviewHeight());
			jsonObj.addProperty("username", mUser);
			jsonObj.addProperty("password", mPass);

			byte[] buff = new byte[256];
			int len = 0;
            String msg = null;
            outputStream.write(jsonObj.toString().getBytes());
            outputStream.flush();

            while ((len = inputStream.read(buff)) != -1) {
                msg = new String(buff, 0, len);

                // JSON analysis
                JsonParser parser = new JsonParser();
                boolean isJSON = true;
                JsonElement element = null;
                try {
                    element =  parser.parse(msg);
                }
                catch (JsonParseException e) {
                    Log.e(TAG, "exception: " + e);
                    isJSON = false;
                }
                if (isJSON && element != null) {
                    JsonObject obj = element.getAsJsonObject();
                    element = obj.get("state");
                    if (element != null && element.getAsString().equals("ok")) {
                        // send data
                        while (true) {
                            outputStream.write(mCameraPreview.getImageBuffer());
                            outputStream.flush();

                            if (Thread.currentThread().isInterrupted())
                                break;
                        }

                        break;
                    }
                }
                else {
                    break;
                }
            }

			outputStream.close();
			inputStream.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
			Log.e(TAG, e.toString());
		}
		finally {
			try {

				if (outputStream != null) {
					outputStream.close();
					outputStream = null;
				}

				if (inputStream != null) {
					inputStream.close();
					inputStream = null;
				}

				if (mSocket != null) {
					mSocket.close();
					mSocket = null;
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


		}
	}
}
