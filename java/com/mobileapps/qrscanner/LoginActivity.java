package com.mobileapps.qrscanner;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mobileapps.qrscanner.RegisterActivity;

public class LoginActivity extends AppCompatActivity {

    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://bluetoothelectroniclocker-default-rtdb.firebaseio.com/");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final EditText txtuserid = findViewById(R.id.txtuserid);
        final EditText txtpassword = findViewById(R.id.txtpassword);
        final Button btnsignin = findViewById(R.id.btnsignin);
        final Button registerbtn = findViewById(R.id.registerbtn);

        // LoginActivity.java

        btnsignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String userid = txtuserid.getText().toString();
                final String userpwd = txtpassword.getText().toString();

                if (userid.isEmpty() || userpwd.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                } else {
                    DatabaseReference userRef = databaseReference.child("users").child(userid);
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                // User with the entered username exists in the "users" node

                                // Get the password from the user data
                                String getPassword = snapshot.child("password").getValue(String.class);
                                String userRole = snapshot.child("role").getValue(String.class);

                                // Check if the entered password matches the database password
                                if (getPassword.equals(userpwd)) {
                                    // Successful login
                                    Toast.makeText(LoginActivity.this, "Successful Login", Toast.LENGTH_SHORT).show();


                                    if (userRole.equals("2")) {
                                        startActivity(new Intent(LoginActivity.this, LecturerActivity.class));
                                    } else if (userRole.equals("1")) {
                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    } else {
                                        // Handle other roles here if needed
                                    }
                                } else {
                                    // Failed login
                                    Toast.makeText(LoginActivity.this, "Login Fail", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                // User with the entered username does not exist in the "users" node
                                Toast.makeText(LoginActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Handle cancelled event or error if needed
                        }
                    });
                }
            }
        });



        registerbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }
}
