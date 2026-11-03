package com.example.pillzilla;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterScreen extends AppCompatActivity implements View.OnClickListener {

    TextInputEditText txtName, txtEmail, txtPassword;
    Button btnRegister, btnAlreadyHaveAccount;
    CircularProgressIndicator progressRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.linearLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        txtName = findViewById(R.id.txtName);

        txtEmail = findViewById(R.id.txtEmail);

        txtPassword = findViewById(R.id.txtPassword);

        btnRegister = findViewById(R.id.btnRegister);
        btnAlreadyHaveAccount = findViewById(R.id.btnAlreadyHaveAccount);
        progressRegister = findViewById(R.id.progressRegister);

        btnRegister.setOnClickListener(this);
        btnAlreadyHaveAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterScreen.this, LoginScreen.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onClick(View v) {

        String email = txtEmail.getText().toString().trim();
        String password = txtPassword.getText().toString().trim();
        String name = txtName.getText().toString().trim();

        if (name.isEmpty()) {
            txtName.setError("Please enter your name");
            txtName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            txtEmail.setError("Please enter your email");
            txtEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            txtPassword.setError("Please enter your password");
            txtPassword.requestFocus();
            return;
        }

        progressRegister.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        FirebaseAuth auth = FirebaseAuth.getInstance();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    progressRegister.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);

                    if (task.isSuccessful()) {

                        String uid = auth.getCurrentUser().getUid();

                        FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(uid)
                                .setValue(new User(name, email))
                                .addOnFailureListener(e ->
                                        Toast.makeText(
                                                RegisterScreen.this,
                                                "Failed to save user data",
                                                Toast.LENGTH_SHORT
                                        ).show()
                                );

                        Toast.makeText(
                                RegisterScreen.this,
                                "Registration successful!",
                                Toast.LENGTH_SHORT
                        ).show();

                        Intent intent = new Intent(RegisterScreen.this, HomeActivity.class);
                        startActivity(intent);
                        finish();

                    } else {

                        Toast.makeText(
                                RegisterScreen.this,
                                task.getException().getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}