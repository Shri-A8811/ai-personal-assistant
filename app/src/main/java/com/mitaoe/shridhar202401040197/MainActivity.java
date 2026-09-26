package com.mitaoe.shridhar202401040197;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mitaoe.shridhar202401040197.data.model.AiModel;
import com.mitaoe.shridhar202401040197.data.preference.PreferenceManager;
import com.mitaoe.shridhar202401040197.service.NotificationHelper;
import com.mitaoe.shridhar202401040197.ui.chat.ChatFragment;
import com.mitaoe.shridhar202401040197.ui.chat.ModelSelectorBottomSheet;
import com.mitaoe.shridhar202401040197.ui.dashboard.DashboardFragment;
import com.mitaoe.shridhar202401040197.ui.notes.NotesFragment;
import com.mitaoe.shridhar202401040197.ui.settings.SettingsBottomSheet;
import com.mitaoe.shridhar202401040197.ui.tasks.TasksFragment;

public class MainActivity extends AppCompatActivity {

    private PreferenceManager prefManager;
    private TextView txtCurrentModel;
    private LinearLayout btnModelPill;
    private ImageView btnSettings;
    private BottomNavigationView bottomNav;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Notification permission is needed for reminder alarms.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefManager = new PreferenceManager(this);
        NotificationHelper.createNotificationChannel(this);

        txtCurrentModel = findViewById(R.id.txtCurrentModel);
        btnModelPill = findViewById(R.id.btnModelPill);
        btnSettings = findViewById(R.id.btnSettings);
        bottomNav = findViewById(R.id.bottom_navigation);

        updateModelPillLabel();

        // Model Switcher Pill click
        btnModelPill.setOnClickListener(v -> showModelSelectorBottomSheet());

        // Settings Gear click
        btnSettings.setOnClickListener(v -> showSettingsBottomSheet());

        // Setup Bottom Navigation
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_chat) {
                switchTab("chat");
                btnModelPill.setVisibility(View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_tasks) {
                switchTab("tasks");
                btnModelPill.setVisibility(View.GONE);
                return true;
            } else if (itemId == R.id.nav_notes) {
                switchTab("notes");
                btnModelPill.setVisibility(View.GONE);
                return true;
            } else if (itemId == R.id.nav_dashboard) {
                switchTab("dashboard");
                btnModelPill.setVisibility(View.GONE);
                return true;
            }
            return false;
        });

        // Default screen is Chat
        if (savedInstanceState == null) {
            switchTab("chat");
        }

        // Request notification permission on Android 13+
        checkNotificationPermission();
    }

    private String currentTab = "";

    private void switchTab(String tag) {
        if (tag.equals(currentTab)) return;

        androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
        androidx.fragment.app.FragmentTransaction transaction = fm.beginTransaction();

        Fragment currentFrag = fm.findFragmentByTag(currentTab);
        if (currentFrag != null) {
            transaction.hide(currentFrag);
        }

        Fragment targetFrag = fm.findFragmentByTag(tag);
        if (targetFrag == null) {
            switch (tag) {
                case "chat": targetFrag = new ChatFragment(); break;
                case "tasks": targetFrag = new TasksFragment(); break;
                case "notes": targetFrag = new NotesFragment(); break;
                case "dashboard": targetFrag = new DashboardFragment(); break;
                default: targetFrag = new ChatFragment(); break;
            }
            transaction.add(R.id.fragment_container, targetFrag, tag);
        } else {
            transaction.show(targetFrag);
        }

        transaction.commit();
        currentTab = tag;
    }

    public void updateModelPillLabel() {
        String activeModelName = prefManager.getActiveModelName();
        txtCurrentModel.setText(activeModelName != null && !activeModelName.isEmpty() ? activeModelName : "Select Model");
    }

    private void showModelSelectorBottomSheet() {
        ModelSelectorBottomSheet sheet = new ModelSelectorBottomSheet();
        sheet.setOnModelSelectedListener(new ModelSelectorBottomSheet.OnModelSelectedListener() {
            @Override
            public void onModelSelected(AiModel model) {
                updateModelPillLabel();
                Toast.makeText(MainActivity.this, "Switched to " + model.getDisplayName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onOpenSettingsRequested() {
                showSettingsBottomSheet();
            }
        });
        sheet.show(getSupportFragmentManager(), "ModelSelectorSheet");
    }

    private void showSettingsBottomSheet() {
        SettingsBottomSheet sheet = new SettingsBottomSheet();
        sheet.setOnSettingsSavedListener(() -> {
            updateModelPillLabel();
        });
        sheet.show(getSupportFragmentManager(), "SettingsSheet");
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}
