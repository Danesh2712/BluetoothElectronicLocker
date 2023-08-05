package com.mobileapps.qrscanner;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.bluetooth.BluetoothProfile;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BLE_Page extends AppCompatActivity {

    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    private BluetoothGatt mBluetoothGatt;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int BLUETOOTH = 2;
    private static final int REQUEST_ENABLE_BT = 1;

    ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<>();
    BluetoothDevice selectedDevice; // Store the selected device

    private String userRole;

    private Handler mHandler = new Handler();
    private static final long SCAN_PERIOD = 5000;

//    private String keyToSend = "a672c327b80194dc06cf2e7fd0b98863";


    private void disconnectFromDevice() {

        if (mBluetoothGatt != null) {

            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_page);
        checkPermission(Manifest.permission.BLUETOOTH, BLUETOOTH);
        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PERMISSION_REQUEST_COARSE_LOCATION);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

        retrieveUserRoleFromFirebase();

        startScanning();
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceName = device.getName();

            if (deviceName != null && !devicesDiscovered.contains(device)) {
                devicesDiscovered.add(device);

                String qrCodeResult = getIntent().getStringExtra("key_ble_name");
                if (qrCodeResult != null && qrCodeResult.equals(deviceName)) {
                    stopScanning();
                    showConnectionConfirmationDialog(deviceName);
                    return;
                }
            }

            // Check if the devicesDiscovered ArrayList is empty
            if (devicesDiscovered.size() == 0) {
                showNoDeviceFoundDialog();
            }
        }
    };

    private void startScanning() {
        devicesDiscovered.clear();
        btScanner.startScan(leScanCallback);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScanning();
                showNoDeviceFoundDialog();
            }
        }, SCAN_PERIOD);
    }

    private void stopScanning() {
        btScanner.stopScan(leScanCallback);
    }

    private void retrieveUserRoleFromFirebase() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReferenceFromUrl("https://bluetoothelectroniclocker-default-rtdb.firebaseio.com/").child("users").child("Nesh");
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userRole = snapshot.child("role").getValue(String.class);
                    Log.d("UserRoleDebug", "Retrieved user role: " + userRole);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle cancelled event or error if needed
                Log.e("FirebaseError", "Firebase data retrieval error: " + error.getMessage());
            }
        });
    }

    private void showConnectionConfirmationDialog(String deviceName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Connect to " + deviceName + "?")
                .setMessage("Do you want to connect to the selected device?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Connect to the device
                        for (BluetoothDevice device : devicesDiscovered) {
                            if (device.getName() != null && device.getName().equals(deviceName)) {
                                selectedDevice = device;

                                connectToDevice();

                                // Schedule disconnection after 10 seconds
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        disconnectFromDevice();
                                    }
                                }, 10000); // 10 seconds

                                break;
                            }
                        }
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Go back to scanning QR code page
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }


    private void connectToDevice() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it from the user
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            return;
        }

        try {
            BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        // Device connected
                        // Perform necessary operations after connection
                        // For example, discover services and characteristics
                        gatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        // Device disconnected
                        // Perform necessary operations after disconnection
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        // Perform necessary operations with the discovered services and characteristics
                        Log.d("UserRoleDebug", "gatt success");
                        // Get the service and characteristic for role
                        BluetoothGattService service = gatt.getService(UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b"));
                        if (service != null) {
                            Log.d("UserRoleDebug", "service success" + service);
                            BluetoothGattCharacteristic roleCharacteristic = service.getCharacteristic(UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8"));
                            Log.d("UserRoleDebug", "roleCharacteristic " + roleCharacteristic);
                            if (roleCharacteristic != null) {
                                Log.d("UserRoleDebug", "role_sucess " + roleCharacteristic);
                                // Set the role value (1 for Student, 2 for Lecturer)
                                byte[] roleBytes = userRole.getBytes(); // Convert role string to bytes
                                Log.d("UserRoleDebug", "Sending role: " + Arrays.toString(roleBytes)); // Debug print statement
                                roleCharacteristic.setValue(roleBytes);
                                gatt.writeCharacteristic(roleCharacteristic);
                            }
                        }

                        // ... (other operations)
                    }
                }

                // ... (other callback methods)
            };

            mBluetoothGatt = selectedDevice.connectGatt(this, false, gattCallback);
            // Save the BluetoothGatt object for later use

            // Perform necessary operations with the BluetoothGatt object
            // For example, discover services and characteristics, enable notifications, etc.
            // Refer to the BluetoothGatt documentation for more details on available operations.

            // Show connection confirmation message
            Toast.makeText(this, "Connected to: " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();

            // Show door unlocked message for 10 seconds
            Toast.makeText(this, "Door unlocked for 10 seconds", Toast.LENGTH_SHORT).show();

            // Schedule disconnection after 10 seconds
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    disconnectFromDevice();
                }
            }, 10000); // 10 seconds

            // Commented out the following line to prevent immediate disconnection
            // disconnectFromDevice(); // Disconnect from the device after the desired operations
        } catch (SecurityException e) {
            // Handle SecurityException
            e.printStackTrace();
            Toast.makeText(this, "Permission denied. Please grant location permission.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showNoDeviceFoundDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No device found")
                .setMessage("No device matching the scanned QR code was found.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish(); // Finish the activity and go back to scanning QR code page
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, connect to the device
                connectToDevice();
            } else {
                // Permission denied, show a message or take appropriate action
                Toast.makeText(this, "Location permission denied. Cannot connect to the device.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(BLE_Page.this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(BLE_Page.this, new String[]{permission}, requestCode);
        }
    }
}