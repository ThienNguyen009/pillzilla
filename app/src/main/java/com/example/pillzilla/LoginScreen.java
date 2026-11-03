package com.example.pillzilla;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class LoginScreen extends AppCompatActivity implements View.OnClickListener {

   TextView txtEmail,txtPassword;

    Button btnLogin, btnCreateAccount;
    CircularProgressIndicator progressLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.linearLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtEmail = findViewById(R.id.txtEmail);
        txtPassword = findViewById(R.id.txtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        progressLogin = findViewById(R.id.progressLogin);

        btnLogin.setOnClickListener(this);
        btnCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginScreen.this, RegisterScreen.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onClick(View v) {

        String email = txtEmail.getText().toString();
        String password = txtPassword.getText().toString();

        progressLogin.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        FirebaseAuth auth = FirebaseAuth.getInstance();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {


                        Intent intent = new Intent(LoginScreen.this, HomeActivity.class);
                        startActivity(intent);
                        finish();

                    } else {
                        progressLogin.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        Log.e("LOGIN", "Login failed", task.getException());
                        Toast.makeText(LoginScreen.this, "Login failed. Check your email and password.", Toast.LENGTH_SHORT).show();
                    }
                });


    }
}