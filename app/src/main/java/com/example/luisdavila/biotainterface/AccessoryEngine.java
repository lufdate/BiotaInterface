package com.example.luisdavila.biotainterface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AccessoryEngine {
    private static final int BUFFER_SIZE = 1024;
    // private static final int BUFFER_SIZE = 4;
    private final Context mContext;
    private final UsbManager mUsbManager;
    private final IEngineCallback mCallback;

    private final static String ACTION_ACCESSORY_DETACHED = "android.hardware.usb.action.USB_ACCESSORY_DETACHED";

    private volatile boolean mAccessoryConnected = false;
    private final AtomicBoolean mQuit = new AtomicBoolean(false);

    private UsbAccessory mAccessory = null;

    private ParcelFileDescriptor mParcelFileDescriptor = null;
    private FileDescriptor mFileDescriptor = null;
    private FileInputStream mInputStream = null;
    private FileOutputStream mOutputStream = null;

    public interface IEngineCallback {
        void onDeviceDisconnected();
        void onConnectionEstablished();
        void onConnectionClosed();
        void onDataReceived(byte[] data, int num);
    }

    public AccessoryEngine(Context applicationContext, IEngineCallback callback) {
        mContext = applicationContext;
        mCallback = callback;
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        mContext.registerReceiver(mDetachedReceiver, new IntentFilter(ACTION_ACCESSORY_DETACHED));
    }

    public void onNewIntent(Intent intent) {
        if (mUsbManager.getAccessoryList() != null) {
            mAccessory = mUsbManager.getAccessoryList()[0];
            connect();
        }
    }

    private static Thread sAccessoryThread;
    private void connect() {
        if (mAccessory != null){
            if (sAccessoryThread == null){
                sAccessoryThread = new Thread(mAccessoryRunnable, "Reader Thread");
                sAccessoryThread.start();
            }
            else{
                L.d("reader thread already started");
            }
        }
        else{
            L.d("accessory is null");
        }
    }

    public void onDestroy(){
        mQuit.set(true);
        mContext.unregisterReceiver(mDetachedReceiver);
    }

    private final BroadcastReceiver mDetachedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && ACTION_ACCESSORY_DETACHED.equals(intent.getAction())) {
                mCallback.onDeviceDisconnected();

            }
        }
    };

    public void write(byte[] data){
        if(mAccessoryConnected && mOutputStream != null){
            try {
                mOutputStream.write(data);
                mOutputStream.flush();
                Toast.makeText(mContext,"Valor enviado",Toast.LENGTH_SHORT).show();
            }catch (IOException e){
                L.e("could not send data");
                Toast.makeText(mContext,"No se pudo enviar dato!!!",Toast.LENGTH_SHORT).show();
            }
        } else{
            L.d("accessory not connected");
            Toast.makeText(mContext,"No se encontró accesorio..!!!",Toast.LENGTH_SHORT).show();
        }
    }

    private final Runnable mAccessoryRunnable = new Runnable() {
        @Override
        public void run() {
            L.d("open connection");
            mParcelFileDescriptor = mUsbManager.openAccessory(mAccessory);
            if (mParcelFileDescriptor == null){
                L.e("could not open accessory");
                mCallback.onConnectionClosed();
                return;
            }
            mFileDescriptor = mParcelFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(mFileDescriptor);
            mOutputStream = new FileOutputStream(mFileDescriptor);
            mCallback.onConnectionEstablished();
            mAccessoryConnected = true;

            byte[] buf = new byte[BUFFER_SIZE];
            while (!mQuit.get()){
                try {
                    int read = mInputStream.read(buf);
                    mCallback.onDataReceived(buf,read);
                }catch (Exception e){
                    L.e("ex" + e.getMessage());
                    break;
                }
            }
            L.d("exiting reader thread");
            mCallback.onConnectionClosed();

            if (mParcelFileDescriptor != null){
                try {
                    mParcelFileDescriptor.close();
                }catch (IOException e){
                    L.e("Unable to close InputStream");
                }
            }

            if (mInputStream != null){
                try {
                    mInputStream.close();
                }catch (IOException e){
                    L.e("Unable to close InputStream");
                }
            }

            if (mOutputStream != null){
                try {
                    mOutputStream.close();
                }catch (IOException e){
                    L.e("Unable to close OutputStream");
                }
            }

            mAccessoryConnected = false;
            mQuit.set(false);
            sAccessoryThread = null;
        }
    };
}
