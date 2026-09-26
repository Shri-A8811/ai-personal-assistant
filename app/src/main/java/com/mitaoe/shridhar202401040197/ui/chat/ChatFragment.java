package com.mitaoe.shridhar202401040197.ui.chat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mitaoe.shridhar202401040197.R;
import com.mitaoe.shridhar202401040197.ai.AiService;
import com.mitaoe.shridhar202401040197.ai.NaturalLanguageActionParser;
import com.mitaoe.shridhar202401040197.data.database.AppDatabase;
import com.mitaoe.shridhar202401040197.data.model.ChatMessage;
import com.mitaoe.shridhar202401040197.data.preference.PreferenceManager;
import com.mitaoe.shridhar202401040197.util.VoiceAssistantHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerChat;
    private ChatAdapter chatAdapter;
    private EditText editChatMessage;
    private FrameLayout btnSend;
    private ImageView btnVoiceMic, btnAttachImage, btnRemoveImage, imgAttachedPreview;
    private LinearLayout previewContainer;

    private AppDatabase db;
    private PreferenceManager prefManager;
    private AiService aiService;
    private VoiceAssistantHelper voiceHelper;

    private Uri attachedImageUri = null;
    private List<ChatMessage> currentHistory = new ArrayList<>();

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    attachedImageUri = uri;
                    previewContainer.setVisibility(View.VISIBLE);
                    imgAttachedPreview.setImageURI(uri);
                }
            });

    private final ActivityResultLauncher<String> recordAudioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startVoiceInput();
                } else {
                    Toast.makeText(requireContext(), "Microphone permission is required for voice commands.", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        db = AppDatabase.getInstance(requireContext());
        prefManager = new PreferenceManager(requireContext());
        aiService = new AiService(requireContext());
        voiceHelper = new VoiceAssistantHelper(requireContext());

        recyclerChat = view.findViewById(R.id.recyclerChat);
        editChatMessage = view.findViewById(R.id.editChatMessage);
        btnSend = view.findViewById(R.id.btnSend);
        btnVoiceMic = view.findViewById(R.id.btnVoiceMic);
        btnAttachImage = view.findViewById(R.id.btnAttachImage);
        btnRemoveImage = view.findViewById(R.id.btnRemoveImage);
        imgAttachedPreview = view.findViewById(R.id.imgAttachedPreview);
        previewContainer = view.findViewById(R.id.previewContainer);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        recyclerChat.setLayoutManager(layoutManager);

        chatAdapter = new ChatAdapter(text -> voiceHelper.speak(text));
        recyclerChat.setAdapter(chatAdapter);

        db.chatDao().getAllMessages().observe(getViewLifecycleOwner(), messages -> {
            if (messages != null) {
                currentHistory = messages;
                chatAdapter.setMessages(messages);
                if (!messages.isEmpty()) {
                    recyclerChat.scrollToPosition(messages.size() - 1);
                }
            }
        });

        btnSend.setOnClickListener(v -> sendMessage());

        btnAttachImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnRemoveImage.setOnClickListener(v -> {
            attachedImageUri = null;
            previewContainer.setVisibility(View.GONE);
        });

        btnVoiceMic.setOnClickListener(v -> checkVoicePermissionAndListen());

        return view;
    }

    private void checkVoicePermissionAndListen() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoiceInput();
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startVoiceInput() {
        btnVoiceMic.setColorFilter(ContextCompat.getColor(requireContext(), R.color.accent_gemini_blue));
        Toast.makeText(requireContext(), "Listening...", Toast.LENGTH_SHORT).show();

        voiceHelper.startListening(new VoiceAssistantHelper.SpeechResultCallback() {
            @Override
            public void onSpeechResult(String recognizedText) {
                btnVoiceMic.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                editChatMessage.setText(recognizedText);
                sendMessage();
            }

            @Override
            public void onSpeechError(String errorMessage) {
                btnVoiceMic.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSpeechRmsChanged(float rmsdB) {}
        });
    }

    private void sendMessage() {
        String input = editChatMessage.getText().toString().trim();
        if (input.isEmpty() && attachedImageUri == null) return;

        editChatMessage.setText("");
        String imageUriStr = attachedImageUri != null ? attachedImageUri.toString() : null;
        attachedImageUri = null;
        previewContainer.setVisibility(View.GONE);

        // 1. User Message
        ChatMessage userMsg = new ChatMessage(input, true, System.currentTimeMillis(), "User");
        if (imageUriStr != null) userMsg.setImageUri(imageUriStr);

        Executors.newSingleThreadExecutor().execute(() -> {
            db.chatDao().insert(userMsg);
        });

        // 2. Check Natural Language Actions (Create task / reminder / note directly)
        NaturalLanguageActionParser.ActionResult actionResult = NaturalLanguageActionParser.parseAndExecute(requireContext(), input);

        if (actionResult.type != NaturalLanguageActionParser.ActionType.NONE && actionResult.isSuccess) {
            ChatMessage assistantMsg = new ChatMessage(
                    actionResult.confirmationMessage,
                    false,
                    System.currentTimeMillis(),
                    prefManager.getActiveModelName()
            );
            assistantMsg.setHasAction(true);
            assistantMsg.setActionType(actionResult.type.name());
            assistantMsg.setActionTitle(actionResult.title);

            Executors.newSingleThreadExecutor().execute(() -> {
                db.chatDao().insert(assistantMsg);
            });

            if (prefManager.isVoiceAutoSpeakEnabled()) {
                voiceHelper.speak(actionResult.confirmationMessage);
            }
            return;
        }

        // 3. Regular AI Query via Multi-Provider Streaming
        String activeModelName = prefManager.getActiveModelName();
        ChatMessage pendingMsg = new ChatMessage("Thinking...", false, System.currentTimeMillis(), activeModelName);
        chatAdapter.appendMessage(pendingMsg);
        recyclerChat.scrollToPosition(chatAdapter.getItemCount() - 1);

        aiService.generateResponse(currentHistory, input, new AiService.StreamCallback() {
            @Override
            public void onStart() {}

            @Override
            public void onToken(String token, String fullTextSoFar) {
                chatAdapter.updateLastMessage(fullTextSoFar);
                recyclerChat.scrollToPosition(chatAdapter.getItemCount() - 1);
            }

            @Override
            public void onComplete(String fullResponse) {
                chatAdapter.updateLastMessage(fullResponse);
                ChatMessage completedMsg = new ChatMessage(fullResponse, false, System.currentTimeMillis(), activeModelName);
                Executors.newSingleThreadExecutor().execute(() -> {
                    db.chatDao().insert(completedMsg);
                });

                if (prefManager.isVoiceAutoSpeakEnabled()) {
                    voiceHelper.speak(fullResponse);
                }
            }

            @Override
            public void onError(String errorMessage) {
                String errorNotice = "⚠️ " + errorMessage;
                chatAdapter.updateLastMessage(errorNotice);
                ChatMessage errMsg = new ChatMessage(errorNotice, false, System.currentTimeMillis(), activeModelName);
                Executors.newSingleThreadExecutor().execute(() -> {
                    db.chatDao().insert(errMsg);
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (voiceHelper != null) {
            voiceHelper.destroy();
        }
    }
}
