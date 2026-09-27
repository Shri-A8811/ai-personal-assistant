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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.mitaoe.shridhar202401040197.data.database.AppDatabase;
import com.mitaoe.shridhar202401040197.data.model.AiModel;
import com.mitaoe.shridhar202401040197.data.model.Conversation;
import com.mitaoe.shridhar202401040197.data.preference.PreferenceManager;
import com.mitaoe.shridhar202401040197.service.NotificationHelper;
import com.mitaoe.shridhar202401040197.ui.chat.ChatFragment;
import com.mitaoe.shridhar202401040197.ui.chat.ConversationDrawerAdapter;
import com.mitaoe.shridhar202401040197.ui.chat.ModelLibraryBottomSheet;
import com.mitaoe.shridhar202401040197.ui.chat.ModelSelectorBottomSheet;
import com.mitaoe.shridhar202401040197.ui.dashboard.DashboardFragment;
import com.mitaoe.shridhar202401040197.ui.notes.NotesFragment;
import com.mitaoe.shridhar202401040197.ui.settings.SettingsBottomSheet;
import com.mitaoe.shridhar202401040197.ui.tasks.TasksFragment;

import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private PreferenceManager prefManager;
    private AppDatabase db;

    private DrawerLayout drawerLayout;
    private TextView txtCurrentModel;
    private LinearLayout btnModelPill;
    private ImageView btnDrawer, btnTopNewChat, btnSettings;
    private BottomNavigationView bottomNav;

    // Drawer Views
    private RecyclerView recyclerDrawerConversations;
    private TextView txtDrawerNoChats;
    private MaterialButton btnDrawerNewChat;
    private ImageView btnCloseDrawer;
    private LinearLayout btnDrawerSettings;
    private ConversationDrawerAdapter drawerAdapter;

    private String currentTab = "";

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
        db = AppDatabase.getInstance(this);
        NotificationHelper.createNotificationChannel(this);

        drawerLayout = findViewById(R.id.drawerLayout);
        txtCurrentModel = findViewById(R.id.txtCurrentModel);
        btnModelPill = findViewById(R.id.btnModelPill);
        btnDrawer = findViewById(R.id.btnDrawer);
        btnTopNewChat = findViewById(R.id.btnTopNewChat);
        btnSettings = findViewById(R.id.btnSettings);
        bottomNav = findViewById(R.id.bottom_navigation);

        setupDrawer();

        updateModelPillLabel();

        // Hamburger Menu click opens previous chats drawer
        btnDrawer.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Start New Chat from top icon
        btnTopNewChat.setOnClickListener(v -> handleStartNewChat());

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
                btnTopNewChat.setVisibility(View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_tasks) {
                switchTab("tasks");
                btnModelPill.setVisibility(View.GONE);
                btnTopNewChat.setVisibility(View.GONE);
                return true;
            } else if (itemId == R.id.nav_notes) {
                switchTab("notes");
                btnModelPill.setVisibility(View.GONE);
                btnTopNewChat.setVisibility(View.GONE);
                return true;
            } else if (itemId == R.id.nav_dashboard) {
                switchTab("dashboard");
                btnModelPill.setVisibility(View.GONE);
                btnTopNewChat.setVisibility(View.GONE);
                return true;
            }
            return false;
        });

        // Default screen is Chat
        if (savedInstanceState == null) {
            switchTab("chat");
        }

        checkNotificationPermission();
    }

    private void setupDrawer() {
        View drawerHeader = findViewById(R.id.drawerContent);
        if (drawerHeader == null) return;

        recyclerDrawerConversations = drawerHeader.findViewById(R.id.recyclerDrawerConversations);
        txtDrawerNoChats = drawerHeader.findViewById(R.id.txtDrawerNoChats);
        btnDrawerNewChat = drawerHeader.findViewById(R.id.btnDrawerNewChat);
        btnCloseDrawer = drawerHeader.findViewById(R.id.btnCloseDrawer);
        btnDrawerSettings = drawerHeader.findViewById(R.id.btnDrawerSettings);

        recyclerDrawerConversations.setLayoutManager(new LinearLayoutManager(this));
        drawerAdapter = new ConversationDrawerAdapter(new ConversationDrawerAdapter.OnConversationClickListener() {
            @Override
            public void onConversationClick(Conversation conversation) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!"chat".equals(currentTab)) {
                    bottomNav.setSelectedItemId(R.id.nav_chat);
                }
                Fragment frag = getSupportFragmentManager().findFragmentByTag("chat");
                if (frag instanceof ChatFragment) {
                    ((ChatFragment) frag).loadConversation(conversation.getId());
                }
            }

            @Override
            public void onConversationDelete(Conversation conversation) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Conversation")
                        .setMessage("Delete \"" + conversation.getTitle() + "\" and its chat history?")
                        .setPositiveButton("Delete", (d, w) -> {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                db.conversationDao().deleteMessagesForConversation(conversation.getId());
                                db.conversationDao().delete(conversation);
                            });
                            Fragment frag = getSupportFragmentManager().findFragmentByTag("chat");
                            if (frag instanceof ChatFragment) {
                                ((ChatFragment) frag).startNewChat();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        recyclerDrawerConversations.setAdapter(drawerAdapter);

        // Observe Conversations in Real Time
        db.conversationDao().getAllConversations().observe(this, conversations -> {
            if (conversations != null && !conversations.isEmpty()) {
                drawerAdapter.setConversations(conversations, -1);
                recyclerDrawerConversations.setVisibility(View.VISIBLE);
                txtDrawerNoChats.setVisibility(View.GONE);
            } else {
                drawerAdapter.setConversations(conversations, -1);
                recyclerDrawerConversations.setVisibility(View.GONE);
                txtDrawerNoChats.setVisibility(View.VISIBLE);
            }
        });

        btnDrawerNewChat.setOnClickListener(v -> handleStartNewChat());
        btnCloseDrawer.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        btnDrawerSettings.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            showSettingsBottomSheet();
        });
    }

    private void handleStartNewChat() {
        drawerLayout.closeDrawer(GravityCompat.START);
        if (!"chat".equals(currentTab)) {
            bottomNav.setSelectedItemId(R.id.nav_chat);
        }
        Fragment frag = getSupportFragmentManager().findFragmentByTag("chat");
        if (frag instanceof ChatFragment) {
            ((ChatFragment) frag).startNewChat();
        }
    }

    public void updateSelectedConversationInDrawer(long conversationId) {
        if (drawerAdapter != null) {
            drawerAdapter.setSelectedConversationId(conversationId);
        }
    }

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
        if (activeModelName != null && !activeModelName.isEmpty()) {
            txtCurrentModel.setText(activeModelName);
        } else {
            txtCurrentModel.setText("Select Model / Add API");
        }
    }

    public void showModelSelectorBottomSheet() {
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

            @Override
            public void onOpenLibraryRequested() {
                showModelLibraryBottomSheet();
            }
        });
        sheet.show(getSupportFragmentManager(), "ModelSelectorSheet");
    }

    public void showModelLibraryBottomSheet() {
        ModelLibraryBottomSheet sheet = new ModelLibraryBottomSheet();
        sheet.setOnModelLibraryListener(new ModelLibraryBottomSheet.OnModelLibraryListener() {
            @Override
            public void onModelSelected(AiModel model) {
                updateModelPillLabel();
                Toast.makeText(MainActivity.this, "Selected: " + model.getDisplayName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onOpenSettingsRequested() {
                showSettingsBottomSheet();
            }

            @Override
            public void onModelsUpdated() {
                updateModelPillLabel();
            }
        });
        sheet.show(getSupportFragmentManager(), "ModelLibrarySheet");
    }

    public void showSettingsBottomSheet() {
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
