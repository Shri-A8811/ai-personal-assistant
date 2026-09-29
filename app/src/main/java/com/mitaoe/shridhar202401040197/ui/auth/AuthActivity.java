package com.mitaoe.shridhar202401040197.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mitaoe.shridhar202401040197.MainActivity;
import com.mitaoe.shridhar202401040197.R;
import com.mitaoe.shridhar202401040197.data.preference.PreferenceManager;

public class AuthActivity extends AppCompatActivity {

    private PreferenceManager prefManager;
    private TabLayout tabAuthMode;
    private TextInputLayout layoutName, layoutEmail, layoutPassword;
    private TextInputEditText edtName, edtEmail, edtPassword;
    private MaterialButton btnSubmitAuth;
    private TextView txtAuthSubtitle;

    private boolean isSignUpMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefManager = new PreferenceManager(this);

        // If user is already logged in, navigate straight to MainActivity
        if (prefManager.isLoggedIn()) {
            launchMainActivity();
            return;
        }

        setContentView(R.layout.activity_auth);

        tabAuthMode = findViewById(R.id.tabAuthMode);
        layoutName = findViewById(R.id.layoutName);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnSubmitAuth = findViewById(R.id.btnSubmitAuth);
        txtAuthSubtitle = findViewById(R.id.txtAuthSubtitle);

        tabAuthMode.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isSignUpMode = (tab.getPosition() == 0);
                updateUiForMode();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnSubmitAuth.setOnClickListener(v -> handleSubmit());
    }

    private void updateUiForMode() {
        if (isSignUpMode) {
            layoutName.setVisibility(View.VISIBLE);
            btnSubmitAuth.setText("Create Account");
            txtAuthSubtitle.setText("Create your account to personalize your AI workspace");
        } else {
            layoutName.setVisibility(View.GONE);
            btnSubmitAuth.setText("Sign In");
            txtAuthSubtitle.setText("Welcome back! Enter your email and password");
        }
    }

    private void handleSubmit() {
        String name = edtName.getText() != null ? edtName.getText().toString().trim() : "";
        String email = edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";
        String password = edtPassword.getText() != null ? edtPassword.getText().toString() : "";

        if (isSignUpMode) {
            if (TextUtils.isEmpty(name)) {
                layoutName.setError("Please enter your name");
                return;
            } else {
                layoutName.setError(null);
            }
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError("Please enter a valid email address");
            return;
        } else {
            layoutEmail.setError(null);
        }

        if (TextUtils.isEmpty(password) || password.length() < 4) {
            layoutPassword.setError("Password must be at least 4 characters");
            return;
        } else {
            layoutPassword.setError(null);
        }

        if (isSignUpMode) {
            prefManager.saveUser(name, email, password);
            Toast.makeText(this, "Welcome, " + name + "! Account created.", Toast.LENGTH_SHORT).show();
        } else {
            String savedName = prefManager.getUserName();
            if ("User".equalsIgnoreCase(savedName)) {
                // Infer name from email username if not previously saved
                String inferredName = email.split("@")[0];
                if (inferredName.length() > 0) {
                    inferredName = Character.toUpperCase(inferredName.charAt(0)) + inferredName.substring(1);
                }
                savedName = inferredName;
            }
            prefManager.saveUser(savedName, email, password);
            Toast.makeText(this, "Welcome back, " + savedName + "!", Toast.LENGTH_SHORT).show();
        }

        launchMainActivity();
    }

    private void launchMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
