package com.app.totoambulance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    EditText mFullName, mEmail, mPassword, mPhone;
    Button mRegisterBtn;
    TextView mLoginBtn;
    FirebaseAuth fAuth;
    DatabaseReference userDatabaseRef;
    ProgressBar progressBar;
    //    String code="+91-";
    String email, password, phone, name;
    String onlineUserID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mFullName = findViewById(R.id.fullName);
        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mPhone = findViewById(R.id.mobile);
        mRegisterBtn = findViewById(R.id.registerBt);
        mLoginBtn = findViewById(R.id.createBtn);


        fAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progressBar);



        if (fAuth.getCurrentUser() != null) {
            //go to booking activity
//            finish();
        }

        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                email = mEmail.getText().toString().trim();
                password = mPassword.getText().toString().trim();
                phone = mPhone.getText().toString().trim();
//                String mobile=code + phone;
                name = mFullName.getText().toString().trim();

                if (TextUtils.isEmpty(name)) {
                    mFullName.setError("Enter Name");
                    return;
                }
                if (TextUtils.isEmpty(email)) {
                    mEmail.setError("Email is required");
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    mPassword.setError("Password is required");
                    return;
                }
                if (password.length() < 6) {
                    mPassword.setError("Password must be >= 6 Characters");
                    return;
                }
                if (TextUtils.isEmpty(phone)) {
                    mPhone.setError("Enter Mobile No.");
                    return;
                }
                if (phone.length() != 10) {
                    Toast.makeText(RegisterActivity.this, "Invalid Mobile No.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (phone.length() == 10) {
                    progressBar.setVisibility(View.VISIBLE);

                    //register the user in firebase

                    fAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {

                                onlineUserID=fAuth.getCurrentUser().getUid();
                                userDatabaseRef= FirebaseDatabase.getInstance().getReference().child("Users")
                                        .child("Customers").child(onlineUserID);
                                userDatabaseRef.setValue(true);
                                Toast.makeText(RegisterActivity.this, "Registration Successful!! Please verify Mobile No.", Toast.LENGTH_LONG).show();

                                // otp sent code
                                sendOtp();

                                //go to booking activity
                                progressBar.setVisibility(View.INVISIBLE);
                            } else {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(RegisterActivity.this, "Error ! " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                }

            }
        });
    }


    public void login(View view) {
        Intent i = new Intent(RegisterActivity.this, UserLoginActivity.class);
        startActivity(i);
    }

    private void sendOtp() {

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                "+91" + phone,
                60,
                TimeUnit.SECONDS,
                RegisterActivity.this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String backendotp, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        progressBar.setVisibility(View.GONE);
                        Intent i = new Intent(RegisterActivity.this, UserVerifyActivity.class);
                        i.putExtra("mobile", phone);
                        i.putExtra("backendotp", backendotp);
                        startActivity(i);
                    }
                }
        );

    }


}