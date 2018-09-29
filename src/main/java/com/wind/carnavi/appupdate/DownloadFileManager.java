package com.wind.carnavi.appupdate;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadFileManager {
	
	public interface DownloadListener{
		public void loading(int point);
		public void success(File file);
		public void failed();
	}
	
	public static DownloadFileManager mManager;

	private DownloadFileManager() {
	}

	public synchronized static DownloadFileManager getInstance() {
		if (mManager == null) {
			mManager = new DownloadFileManager();
		}
		return mManager;
	}
	
	/**
	 * @param httpUrl download
	 * */
	public File downLoadFile(int type , String httpUrl, DownloadListener listener, String filePath, String fileName, Handler handler) {
		int mLength = 0;
		File tmpFile = new File(filePath);
		if (!tmpFile.exists()) {
			tmpFile.mkdir();
		}
		FileOutputStream fos = null;
		InputStream is = null;
		HttpURLConnection conn = null;
		final File file = new File(filePath +"/"+ fileName);
		try {
			URL url = new URL(httpUrl);
			try {
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestProperty("Accept-Encoding", "identity");
				mLength = conn.getContentLength();
				is = conn.getInputStream();
				fos = new FileOutputStream(file);
				byte[] buf = new byte[1024];
				conn.connect();
				int mReceiver = 0;
				double count = 0;
				if (conn.getResponseCode() >= 400) {
				} else {
					while (count <= 100) {
						if (is != null) {
							int numRead = is.read(buf);
							if (numRead <= 0) {
								break;
							} else {
								mReceiver += numRead;
								fos.write(buf, 0, numRead);
								if (mLength != 0) {
									float temp = (float) ((float) mReceiver / (float) mLength);
									listener.loading((int) (temp * 100));
									if (type != 2){
										Message message = new Message();
										message.what = Constants.UPDATE_APP_DOWNLOAD_LOADING;
										message.obj = (int) (temp * 100);
										handler.sendMessage(message);
									}
								}
								try {
									Thread.sleep(2);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						} else {
							break;
						}
					}
				}
				conn.disconnect();
				fos.close();
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
				listener.failed();
				if (conn != null){
					conn.disconnect();
				}
				try {
					if (is != null){
						is.close();
					}
					if (fos != null){
						fos.close();
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				return tmpFile;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			listener.failed();
			return tmpFile;
		}
		listener.success(file);
		return file;
	}
	
	public void openFile(Context context, File file) {

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		if (Build.VERSION.SDK_INT >= 24) { //判读版本是否在7.0以上
			Uri apkUri =
					FileProvider.getUriForFile(context, "com.wind.carnavi.provider", file);
			//添加这一句表示对目标应用临时授权该Uri所代表的文件
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
		}else {
			intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
		}
		context.startActivity(intent);
	}

}
