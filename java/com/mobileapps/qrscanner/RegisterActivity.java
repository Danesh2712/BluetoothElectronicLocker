package com.mobileapps.qrscanner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://bluetoothelectroniclocker-default-rtdb.firebaseio.com/");
    private Spinner courseSpinner;
    private ArrayAdapter<String> courseAdapter;
    private String selectedRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg);

        courseSpinner = findViewById(R.id.courseSpinner);
        final EditText txtuserid = findViewById(R.id.txtuserid);
        final EditText txtpassword = findViewById(R.id.txtpassword);
        final Button registerbtn = findViewById(R.id.registerbtn);
        final Button loginnow = findViewById(R.id.loginnow);

        // Initialize the spinner and set the adapter
        List<String> courseOptions = new ArrayList<>();
        courseOptions.add("Student");
        courseOptions.add("Lecturer");

        courseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courseOptions);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        courseSpinner.setAdapter(courseAdapter);

        courseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRole = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        registerbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String userid = txtuserid.getText().toString();
                final String userpwd = txtpassword.getText().toString();

                if (userid.isEmpty() || userpwd.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Please enter all details", Toast.LENGTH_SHORT).show();
                } else {
                    databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.hasChild(userid)) {
                                Toast.makeText(RegisterActivity.this, "Username already registered", Toast.LENGTH_SHORT).show();
                            } else {
                                DatabaseReference newUserRef = databaseReference.child("users").child(userid);
                                newUserRef.child("username").setValue(userid);
                                newUserRef.child("password").setValue(userpwd);

                                String role ="";
                                if (selectedRole.equals("Student")) {
                                    role = "1";
                                } else if(selectedRole.equals("Lecturer")){
                                    role = "2";
                                }
                                newUserRef.child("role").setValue(role);

                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                startActivity(intent);

                                Toast.makeText(RegisterActivity.this, "User registered successfully", Toast.LENGTH_SHORT).show();
                                finish();
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

        loginnow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
