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
    private MaterialButton btnSubmitAuth, btnGuestMode;
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
        btnGuestMode = findViewById(R.id.btnGuestMode);
        txtAuthSubtitle = findViewById(R.id.txtAuthSubtitle);

        // If an account already exists on device, select Sign In tab by default
        if (prefManager.hasAccount()) {
            TabLayout.Tab loginTab = tabAuthMode.getTabAt(1);
            if (loginTab != null) {
                loginTab.select();
                isSignUpMode = false;
            }
        }
        updateUiForMode();

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

        if (btnGuestMode != null) {
            // Guest mode is turned off once an account exists; otherwise it bypasses the password.
            btnGuestMode.setVisibility(prefManager.hasAccount() ? View.GONE : View.VISIBLE);
            btnGuestMode.setOnClickListener(v -> {
                if (!prefManager.setGuestSession()) {
                    Toast.makeText(this, "Guest mode is unavailable because an account exists. Please sign in.", Toast.LENGTH_LONG).show();
                    btnGuestMode.setVisibility(View.GONE);
                    return;
                }
                Toast.makeText(this, "Welcome! Using local Guest session.", Toast.LENGTH_SHORT).show();
                launchMainActivity();
            });
        }
    }

    private void updateUiForMode() {
        layoutEmail.setError(null);
        layoutPassword.setError(null);
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
            if (!prefManager.saveUser(name, email, password)) {
                Toast.makeText(this, "An account already exists on this device. Please sign in.", Toast.LENGTH_LONG).show();
                TabLayout.Tab loginTab = tabAuthMode.getTabAt(1);
                if (loginTab != null) {
                    loginTab.select();
                }
                return;
            }
            Toast.makeText(this, "Welcome, " + name + "! Account created.", Toast.LENGTH_SHORT).show();
            launchMainActivity();
        } else {
            if (!prefManager.hasAccount()) {
                Toast.makeText(this, "No saved account found. Please sign up first.", Toast.LENGTH_LONG).show();
                TabLayout.Tab signUpTab = tabAuthMode.getTabAt(0);
                if (signUpTab != null) {
                    signUpTab.select();
                }
                return;
            }

            if (!prefManager.getUserEmail().equalsIgnoreCase(email)) {
                layoutEmail.setError("No account matches this email address");
                return;
            }

            if (!prefManager.verifyCredentials(email, password)) {
                layoutPassword.setError("Incorrect password. Please try again.");
                return;
            }

            prefManager.login();
            Toast.makeText(this, "Welcome back, " + prefManager.getUserName() + "!", Toast.LENGTH_SHORT).show();
            launchMainActivity();
        }
    }

    private void launchMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
