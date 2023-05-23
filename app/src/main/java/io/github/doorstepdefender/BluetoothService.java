package io.github.doorstepdefender;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.io.IOException;
import java.io.InputStream;

public class BluetoothService extends Service {
    private static final String TAG = "BluetoothService";
    private static final String CHANNEL_ID = "BluetoothServiceChannel";

    private NotificationManager notificationManager;
    private static Thread thread;
    private static String deviceName = null;
    private static PackageStatus status = PackageStatus.NOT_CONNECTED;
    private static boolean alarmEnabled = false;
    private static final Lock lock = new ReentrantLock(true);

    private static boolean isRunning = false;

    public enum PackageStatus {
        NOT_CONNECTED,
        PRESENT,
        GONE,
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = getSystemService(NotificationManager.class);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
    }

    public static boolean getIsRunning() {
        return isRunning;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        startBluetoothThread();
        startForegroundService();

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static PackageStatus getPackageStatus() {
        lock.lock();
        PackageStatus s = status;
        lock.unlock();
        return s;
    }

    public static void setDeviceName(String name) {
        lock.lock();
        deviceName = name.isEmpty() ? null : name;
        lock.unlock();
    }

    public static void setAlarmEnabled(boolean enabled) {
        lock.lock();
        alarmEnabled = enabled;
        lock.unlock();
    }

    public static boolean getAlarmEnabled() {
        lock.lock();
        boolean enabled = alarmEnabled;
        lock.unlock();
        return enabled;
    }

    public static String getDeviceName() {
        lock.lock();
        String name = deviceName;
        lock.unlock();
        return name;
    }

    private void showNotification(String title, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Bluetooth Service",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .build();

        notificationManager.notify(1, notification);
    }

    private void startForegroundService() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_ID,
                NotificationManager.IMPORTANCE_HIGH
        );

        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Bluetooth Service")
                .setContentText("Receiving Bluetooth messages")
                .setSmallIcon(R.drawable.ic_launcher_background);

        Notification notification = notificationBuilder.build();
        startForeground(1, notification);
    }

    private void startBluetoothThread() {
        thread = new Thread(() -> {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                Log.e(TAG, "Problem getting device's Bluetooth Adapter");
            }
            if (!bluetoothAdapter.isEnabled()) {
                Log.e(TAG, "Bluetooth is not enabled");
                return;
            }

            BluetoothSocket socket = null;
            BluetoothDevice targetDevice = null;
            InputStream inputStream = null;

            String lastName = null;
            while (true) {
                Log.e("Service", "Service is running...");
                try {
                    String name = lastName;
                    lock.lock();
                    name = deviceName;
                    lock.unlock();

                    if (name == null) {
                        Thread.sleep(100);
                        continue;
                    }

                    if (!name.equals(lastName) || targetDevice == null) {
                        lastName = name;
                        if (socket != null) {
                            if (socket.isConnected()) {
                                try {
                                    socket.close();
                                }
                                catch (IOException e) {
                                    Log.e(TAG, "Failed to close socket: " + e.getMessage());
                                }
                            }
                        }
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            }
                            catch (IOException e) {
                                Log.e(TAG, "Failed to close input stream: " + e.getMessage());
                            }
                        }
                        socket = null;
                        targetDevice = null;
                        inputStream = null;
                        lock.lock();
                        status = PackageStatus.NOT_CONNECTED;
                        lock.unlock();

                        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                            Log.e(TAG, "Found: " + device.getName());
                            if (device.getName().equals(name) || device.getAddress().equals(name)) {
                                targetDevice = bluetoothAdapter.getRemoteDevice(device.getAddress());
                                Log.e(TAG, "Found Bluetooth device '" + name + "' address: '" + device.getAddress() + "'");
                                break;
                            }
                        }
                    }

                    if (targetDevice == null) {
                        Log.e(TAG, "Bluetooth device '" + name + "' not found");
                        Thread.sleep(100);
                        continue;
                    }


                    if (socket == null) {
                        try {
                            socket =(BluetoothSocket) targetDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(targetDevice,1);
                        }
                        catch (NoSuchMethodException|InvocationTargetException|IllegalAccessException e) {
                            Log.e(TAG, "Failed to create RFCOMM socket: " + e.getMessage());
                            Thread.sleep(100);
                            continue;
                        }
                    }

                    if (!socket.isConnected()) {
                        // Connect to the device
                        try {
                            socket.connect();
                            Log.i(TAG, "Bluetooth device connected successfully");
                        }
                        catch (IOException e) {
                            Log.e(TAG, "Failed to connect to Bluetooth device: " + e.getMessage());
                            try {
                                socket.close();
                            } catch (IOException ex) {
                                Log.e(TAG, "Failed to close socket: " + ex.getMessage());
                            }
                            socket = null;
                            Thread.sleep(100);
                            continue;
                        }
                    }

                    if (inputStream == null) {
                        try {
                            inputStream = socket.getInputStream();
                        }
                        catch (IOException e) {
                            Log.e(TAG, "Failed to get Input Stream from Bluetooth device: " + e.getMessage());
                            Thread.sleep(100);
                            inputStream = null;
                            continue;
                        }
                    }

                    try {
                        byte[] buffer = new byte[1024];
                        int bytes = inputStream.read(buffer);
                        if (bytes > 0) {
                            String message = new String(buffer, 0, bytes);
                            for (char c : message.toCharArray()) {
                                if (c == 'd') {
                                    showNotification("Package Delivered", "Package has been placed on sensor");
                                    Log.e(TAG, "Package Delivered");
                                    lock.lock();
                                    status = PackageStatus.PRESENT;
                                    lock.unlock();
                                }
                                else if (c == 'g') {
                                    showNotification("Package Removed", "Package has been removed from sensor");
                                    Log.e(TAG, "Package Removed");
                                    lock.lock();
                                    status = PackageStatus.GONE;
                                    lock.unlock();
                                }
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading/writing Bluetooth message: " + e.getMessage());
                        targetDevice = null;
                    }

                    Thread.sleep(2000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
}