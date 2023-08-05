package com.mobileapps.qrscanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class LecturerActivity extends AppCompatActivity {

    BluetoothAdapter btAdapter;
    private BluetoothGatt mBluetoothGatt;
    ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<>();
    private Handler mHandler = new Handler();
    private static final long SCAN_PERIOD = 5000;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lect);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter != null && !btAdapter.isEnabled()) {
            // Request user to enable Bluetooth here (not shown in this code)
        }

        Button btnOpenDoor = findViewById(R.id.openbtn);
        Button btnLockDoor = findViewById(R.id.closebtn);

        retrieveUserRoleFromFirebase();

        btnOpenDoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("2".equals(userRole)) {
                    startScanning();
                } else {
                    Toast.makeText(LecturerActivity.this, "You do not have permission to unlock the door.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnLockDoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectFromDevice();
            }
        });
    }

    private void retrieveUserRoleFromFirebase() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReferenceFromUrl("https://bluetoothelectroniclocker-default-rtdb.firebaseio.com/").child("users").child("Dr chung");
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
            }
        });
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceName = device.getName();

            String esp32BleName = "999"; // Replace with the BLE name of your ESP32
            if (deviceName != null && deviceName.equals(esp32BleName)) {
                stopScanning();
                connectToDevice(device);
            }
        }
    };

    private boolean scanning = false;
    private void startScanning() {
        scanning = true;
        btAdapter.getBluetoothLeScanner().startScan(leScanCallback);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (scanning) {
                    scanning = false;
                    stopScanning();
                    showNoDeviceFoundDialog();
                }
            }
        }, SCAN_PERIOD);
    }

    private void stopScanning() {
        scanning = false;
        btAdapter.getBluetoothLeScanner().stopScan(leScanCallback);
    }

    private void connectToDevice(BluetoothDevice device) {
        if (device != null) {
            mBluetoothGatt = device.connectGatt(this, false, gattCallback);
            Log.d("Bluetooth", "Connecting to device: " + device.getName());
        } else {
            Log.e("Bluetooth", "Device is null. Cannot establish connection.");
        }
    }

    private void disconnectFromDevice() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            Toast.makeText(this, "Door is Locked.", Toast.LENGTH_SHORT).show();
        }
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                // Handle disconnection
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

        // Implement other necessary callback methods here
    };

    private void showNoDeviceFoundDialog() {
        Toast.makeText(this, "ESP32 BLE device not found.", Toast.LENGTH_SHORT).show();
    }
}