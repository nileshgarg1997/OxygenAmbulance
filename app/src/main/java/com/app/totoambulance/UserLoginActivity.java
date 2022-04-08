package com.app.totoambulance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class UserLoginActivity extends AppCompatActivity {

    EditText mEmail, mPassword;
    Button mLoginBtn;
    TextView mCreateBtn;
    FirebaseAuth fAuth;
    private ProgressDialog loadingBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);

        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mLoginBtn = findViewById(R.id.loginBtn);
        mCreateBtn = findViewById(R.id.createBtn);
        fAuth = FirebaseAuth.getInstance();
        loadingBar = new ProgressDialog(this);


        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = mEmail.getText().toString().trim();
                String password = mPassword.getText().toString().trim();

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


                //authenticate the user

                loadingBar.setTitle("Customer Login");
                loadingBar.setMessage("Please wait, while we are checking your credentials...");
                loadingBar.show();


                fAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            if (email.equals("totodriver1@gmail.com") && password.equals("totoseva@123")) {
                                Toast.makeText(getApplicationContext(), "Please enter correct credentials...", Toast.LENGTH_LONG).show();
                                loadingBar.dismiss();
                            }else {

                                //go to Patient map activity
                                Intent i = new Intent(getApplicationContext(), PatientMapActivity.class);
                                startActivity(i);

                                Toast.makeText(UserLoginActivity.this, "Logged In", Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();

                            }
                        } else {
                            Toast.makeText(UserLoginActivity.this, "Error ! " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            loadingBar.dismiss();
                        }
                    }
                });
            }
        });


    }

    public void createAccount(View view) {
        Intent i = new Intent(UserLoginActivity.this, RegisterActivity.class);
        startActivity(i);
    }
}