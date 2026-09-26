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
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mitaoe.shridhar202401040197.MainActivity;
import com.mitaoe.shridhar202401040197.R;
import com.mitaoe.shridhar202401040197.ai.AiService;
import com.mitaoe.shridhar202401040197.ai.NaturalLanguageActionParser;
import com.mitaoe.shridhar202401040197.data.database.AppDatabase;
import com.mitaoe.shridhar202401040197.data.model.ChatMessage;
import com.mitaoe.shridhar202401040197.data.model.Conversation;
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
    private LinearLayout previewContainer, layoutWelcome;
    private TextView chipSuggest1, chipSuggest2, chipSuggest3, chipSuggest4;

    private AppDatabase db;
    private PreferenceManager prefManager;
    private AiService aiService;
    private VoiceAssistantHelper voiceHelper;

    private Uri attachedImageUri = null;
    private List<ChatMessage> currentHistory = new ArrayList<>();
    private long currentConversationId = 0;
    private LiveData<List<ChatMessage>> currentMessagesLiveData = null;

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
        layoutWelcome = view.findViewById(R.id.layoutWelcome);

        chipSuggest1 = view.findViewById(R.id.chipSuggest1);
        chipSuggest2 = view.findViewById(R.id.chipSuggest2);
        chipSuggest3 = view.findViewById(R.id.chipSuggest3);
        chipSuggest4 = view.findViewById(R.id.chipSuggest4);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        recyclerChat.setLayoutManager(layoutManager);

        chatAdapter = new ChatAdapter(text -> voiceHelper.speak(text));
        recyclerChat.setAdapter(chatAdapter);

        setupStarterChips();

        // Load latest conversation or start fresh
        Executors.newSingleThreadExecutor().execute(() -> {
            Conversation latest = db.conversationDao().getLatestConversationSync();
            long initConvId = latest != null ? latest.getId() : 0;
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> loadConversation(initConvId));
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

    private void setupStarterChips() {
        View.OnClickListener clickListener = v -> {
            if (v instanceof TextView) {
                String text = ((TextView) v).getText().toString();
                // Strip emoji prefix
                String prompt = text.replaceFirst("^[💡⏰📝✅]\\s*", "").trim();
                editChatMessage.setText(prompt);
                sendMessage();
            }
        };

        chipSuggest1.setOnClickListener(clickListener);
        chipSuggest2.setOnClickListener(clickListener);
        chipSuggest3.setOnClickListener(clickListener);
        chipSuggest4.setOnClickListener(clickListener);
    }

    public void startNewChat() {
        currentConversationId = 0;
        currentHistory.clear();
        chatAdapter.setMessages(new ArrayList<>());
        if (currentMessagesLiveData != null) {
            currentMessagesLiveData.removeObservers(getViewLifecycleOwner());
            currentMessagesLiveData = null;
        }
        layoutWelcome.setVisibility(View.VISIBLE);
        recyclerChat.setVisibility(View.GONE);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateSelectedConversationInDrawer(0);
        }
    }

    public void loadConversation(long conversationId) {
        this.currentConversationId = conversationId;
        if (currentMessagesLiveData != null) {
            currentMessagesLiveData.removeObservers(getViewLifecycleOwner());
        }

        if (conversationId == 0) {
            startNewChat();
            return;
        }

        currentMessagesLiveData = db.chatDao().getMessagesForConversation(conversationId);
        currentMessagesLiveData.observe(getViewLifecycleOwner(), messages -> {
            if (messages != null && !messages.isEmpty()) {
                currentHistory = messages;
                chatAdapter.setMessages(messages);
                layoutWelcome.setVisibility(View.GONE);
                recyclerChat.setVisibility(View.VISIBLE);
                recyclerChat.scrollToPosition(messages.size() - 1);
            } else {
                currentHistory.clear();
                chatAdapter.setMessages(new ArrayList<>());
                layoutWelcome.setVisibility(View.VISIBLE);
                recyclerChat.setVisibility(View.GONE);
            }
        });

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateSelectedConversationInDrawer(conversationId);
        }
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
        layoutWelcome.setVisibility(View.GONE);
        recyclerChat.setVisibility(View.VISIBLE);

        Executors.newSingleThreadExecutor().execute(() -> {
            // If new conversation, create conversation record first
            if (currentConversationId == 0) {
                String title = input.length() > 32 ? input.substring(0, 32) + "..." : input;
                if (title.isEmpty()) title = "Image Query";
                Conversation conv = new Conversation(title, System.currentTimeMillis(), System.currentTimeMillis(), prefManager.getActiveModelName());
                currentConversationId = db.conversationDao().insert(conv);

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        loadConversation(currentConversationId);
                    });
                }
            } else {
                Conversation conv = db.conversationDao().getConversationById(currentConversationId);
                if (conv != null) {
                    conv.setUpdatedAt(System.currentTimeMillis());
                    db.conversationDao().update(conv);
                }
            }

            // 1. Insert User Message
            ChatMessage userMsg = new ChatMessage(input, true, System.currentTimeMillis(), "User");
            userMsg.setConversationId(currentConversationId);
            if (imageUriStr != null) userMsg.setImageUri(imageUriStr);
            db.chatDao().insert(userMsg);

            // 2. Check Natural Language Actions
            NaturalLanguageActionParser.ActionResult actionResult = NaturalLanguageActionParser.parseAndExecute(requireContext(), input);

            if (actionResult.type != NaturalLanguageActionParser.ActionType.NONE && actionResult.isSuccess) {
                ChatMessage assistantMsg = new ChatMessage(
                        actionResult.confirmationMessage,
                        false,
                        System.currentTimeMillis(),
                        prefManager.getActiveModelName()
                );
                assistantMsg.setConversationId(currentConversationId);
                assistantMsg.setHasAction(true);
                assistantMsg.setActionType(actionResult.type.name());
                assistantMsg.setActionTitle(actionResult.title);

                db.chatDao().insert(assistantMsg);

                if (prefManager.isVoiceAutoSpeakEnabled()) {
                    voiceHelper.speak(actionResult.confirmationMessage);
                }
                return;
            }

            // 3. Regular AI Query via Multi-Provider Streaming
            String activeModelName = prefManager.getActiveModelName();
            ChatMessage pendingMsg = new ChatMessage("Thinking...", false, System.currentTimeMillis(), activeModelName);
            pendingMsg.setConversationId(currentConversationId);

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    chatAdapter.appendMessage(pendingMsg);
                    recyclerChat.scrollToPosition(chatAdapter.getItemCount() - 1);
                });
            }

            aiService.generateResponse(currentHistory, input, new AiService.StreamCallback() {
                @Override public void onStart() {}

                @Override
                public void onToken(String token, String fullTextSoFar) {
                    if (isAdded()) {
                        chatAdapter.updateLastMessage(fullTextSoFar);
                        recyclerChat.scrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                }

                @Override
                public void onComplete(String fullResponse) {
                    if (isAdded()) {
                        chatAdapter.updateLastMessage(fullResponse);
                    }
                    pendingMsg.setText(fullResponse);
                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.chatDao().insert(pendingMsg);
                    });

                    if (prefManager.isVoiceAutoSpeakEnabled()) {
                        voiceHelper.speak(fullResponse);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    String errorNotice = "⚠️ " + errorMessage;
                    if (isAdded()) {
                        chatAdapter.updateLastMessage(errorNotice);
                    }
                    pendingMsg.setText(errorNotice);
                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.chatDao().insert(pendingMsg);
                    });
                }
            });
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
