package com.mitaoe.shridhar202401040197.ui.chat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
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
import com.mitaoe.shridhar202401040197.util.AppExecutors;
import com.mitaoe.shridhar202401040197.util.ImageUtil;
import com.mitaoe.shridhar202401040197.util.VoiceAssistantHelper;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerChat;
    private ChatAdapter chatAdapter;
    private EditText editChatMessage;
    private FrameLayout btnSend;
    private ImageView btnVoiceMic, btnAttachImage, btnRemoveImage, imgAttachedPreview, imgSendIcon;
    private LinearLayout previewContainer;
    private View layoutWelcome;
    private TextView chipSuggest1, chipSuggest2, chipSuggest3, chipSuggest4;

    private AppDatabase db;
    private PreferenceManager prefManager;
    private AiService aiService;
    private VoiceAssistantHelper voiceHelper;

    private Uri attachedImageUri = null;
    private List<ChatMessage> currentHistory = new ArrayList<>();
    private volatile long currentConversationId = 0;
    private LiveData<List<ChatMessage>> currentMessagesLiveData = null;

    private volatile AiService.RequestHandle activeStreamHandle = null;
    private volatile ChatMessage activeStreamingMsg = null;
    private volatile long activeStreamingConvId = -1;

    public long getCurrentConversationId() {
        return currentConversationId;
    }

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (!isAdded() || getView() == null || getContext() == null) return;
                if (uri != null) {
                    attachedImageUri = uri;
                    if (previewContainer != null) previewContainer.setVisibility(View.VISIBLE);
                    if (imgAttachedPreview != null) imgAttachedPreview.setImageURI(uri);
                }
            });

    private final ActivityResultLauncher<String> recordAudioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isAdded() || getContext() == null) return;
                if (isGranted) {
                    startVoiceInput();
                } else {
                    Toast.makeText(getContext(), "Microphone permission is required for voice commands.", Toast.LENGTH_SHORT).show();
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
        imgSendIcon = view.findViewById(R.id.imgSendIcon);
        btnVoiceMic = view.findViewById(R.id.btnVoiceMic);
        btnAttachImage = view.findViewById(R.id.btnAttachImage);
        btnRemoveImage = view.findViewById(R.id.btnRemoveImage);
        imgAttachedPreview = view.findViewById(R.id.imgAttachedPreview);
        previewContainer = view.findViewById(R.id.previewContainer);
        layoutWelcome = view.findViewById(R.id.layoutWelcome);
        TextView txtWelcomeTitle = view.findViewById(R.id.txtWelcomeTitle);
        if (txtWelcomeTitle != null) {
            String userName = prefManager.getUserName();
            txtWelcomeTitle.setText("Hello, " + userName + "\nHow can I assist you today?");
        }

        chipSuggest1 = view.findViewById(R.id.chipSuggest1);
        chipSuggest2 = view.findViewById(R.id.chipSuggest2);
        chipSuggest3 = view.findViewById(R.id.chipSuggest3);
        chipSuggest4 = view.findViewById(R.id.chipSuggest4);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        recyclerChat.setLayoutManager(layoutManager);

        chatAdapter = new ChatAdapter(text -> {
            if (voiceHelper != null) voiceHelper.speak(text);
        });
        recyclerChat.setAdapter(chatAdapter);

        setupStarterChips();

        btnSend.setOnClickListener(v -> {
            if (activeStreamHandle != null && !activeStreamHandle.isCancelled()) {
                stopActiveStream();
            } else {
                sendMessage();
            }
        });
        btnAttachImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnRemoveImage.setOnClickListener(v -> {
            attachedImageUri = null;
            previewContainer.setVisibility(View.GONE);
        });
        btnVoiceMic.setOnClickListener(v -> checkVoicePermissionAndListen());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load latest conversation or start fresh once the view is ready
        AppExecutors.getInstance().diskIO().execute(() -> {
            Conversation latest = db.conversationDao().getLatestConversationSync();
            long initConvId = latest != null ? latest.getId() : 0;
            Activity act = getActivity();
            if (isAdded() && act != null && !act.isFinishing() && !act.isDestroyed()) {
                act.runOnUiThread(() -> {
                    if (isAdded() && getView() != null) {
                        loadConversation(initConvId);
                    }
                });
            }
        });
    }

    private void setStreamingUiState(boolean streaming) {
        if (!isAdded() || getView() == null) return;
        if (imgSendIcon != null) {
            imgSendIcon.setImageResource(streaming ? R.drawable.ic_stop : R.drawable.ic_send);
        }
        if (btnSend != null) {
            btnSend.setContentDescription(streaming ? "Stop generating" : "Send message");
        }
    }

    private void stopActiveStream() {
        if (activeStreamHandle != null) {
            activeStreamHandle.cancel();
            activeStreamHandle = null;
        }
        activeStreamingMsg = null;
        activeStreamingConvId = -1;
        setStreamingUiState(false);
    }

    private void safeScrollToBottom() {
        if (recyclerChat != null && chatAdapter != null) {
            int count = chatAdapter.getItemCount();
            if (count > 0) {
                recyclerChat.scrollToPosition(count - 1);
            }
        }
    }

    private void setupStarterChips() {
        View.OnClickListener clickListener = v -> {
            if (v instanceof TextView) {
                String text = ((TextView) v).getText().toString();
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
        stopActiveStream();
        currentConversationId = 0;
        currentHistory = new ArrayList<>();
        if (chatAdapter != null) {
            chatAdapter.setMessages(new ArrayList<>());
        }
        if (currentMessagesLiveData != null) {
            try {
                if (isAdded() && getView() != null) {
                    currentMessagesLiveData.removeObservers(getViewLifecycleOwner());
                }
            } catch (Exception ignored) {}
            currentMessagesLiveData = null;
        }
        if (layoutWelcome != null) layoutWelcome.setVisibility(View.VISIBLE);
        if (recyclerChat != null) recyclerChat.setVisibility(View.GONE);
        if (getView() != null) {
            TextView txtWelcomeTitle = getView().findViewById(R.id.txtWelcomeTitle);
            if (txtWelcomeTitle != null) {
                String userName = prefManager.getUserName();
                txtWelcomeTitle.setText("Hello, " + userName + "\nHow can I assist you today?");
            }
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateSelectedConversationInDrawer(0);
        }
    }

    public void loadConversation(long conversationId) {
        if (activeStreamingConvId != -1 && activeStreamingConvId != conversationId) {
            stopActiveStream();
        }
        this.currentConversationId = conversationId;
        if (!isAdded() || getView() == null) {
            return;
        }

        if (currentMessagesLiveData != null) {
            try {
                currentMessagesLiveData.removeObservers(getViewLifecycleOwner());
            } catch (Exception ignored) {}
            currentMessagesLiveData = null;
        }

        if (conversationId == 0) {
            startNewChat();
            return;
        }

        currentMessagesLiveData = db.chatDao().getMessagesForConversation(conversationId);
        try {
            currentMessagesLiveData.observe(getViewLifecycleOwner(), messages -> {
                if (!isAdded() || getView() == null) return;
                List<ChatMessage> safeMessages = messages != null ? messages : new ArrayList<>();
                currentHistory = new ArrayList<>(safeMessages);

                List<ChatMessage> displayList = new ArrayList<>(safeMessages);
                if (activeStreamingMsg != null && activeStreamingConvId == conversationId) {
                    displayList.add(activeStreamingMsg);
                }

                if (!displayList.isEmpty()) {
                    if (chatAdapter != null) chatAdapter.setMessages(displayList);
                    if (layoutWelcome != null) layoutWelcome.setVisibility(View.GONE);
                    if (recyclerChat != null) {
                        recyclerChat.setVisibility(View.VISIBLE);
                        safeScrollToBottom();
                    }
                } else {
                    if (chatAdapter != null) chatAdapter.setMessages(new ArrayList<>());
                    if (layoutWelcome != null) layoutWelcome.setVisibility(View.VISIBLE);
                    if (recyclerChat != null) recyclerChat.setVisibility(View.GONE);
                }
            });
        } catch (Exception ignored) {}

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
        Context ctx = getContext();
        if (ctx == null || !isAdded()) return;
        if (btnVoiceMic != null) {
            btnVoiceMic.setColorFilter(ContextCompat.getColor(ctx, R.color.accent_gemini_blue));
        }
        Toast.makeText(ctx, "Listening...", Toast.LENGTH_SHORT).show();

        voiceHelper.startListening(new VoiceAssistantHelper.SpeechResultCallback() {
            @Override
            public void onSpeechResult(String recognizedText) {
                Context innerCtx = getContext();
                if (!isAdded() || getView() == null || innerCtx == null) return;
                if (btnVoiceMic != null) {
                    btnVoiceMic.setColorFilter(ContextCompat.getColor(innerCtx, R.color.text_secondary));
                }
                if (editChatMessage != null) {
                    editChatMessage.setText(recognizedText);
                    sendMessage();
                }
            }

            @Override
            public void onSpeechError(String errorMessage) {
                Context innerCtx = getContext();
                if (!isAdded() || getView() == null || innerCtx == null) return;
                if (btnVoiceMic != null) {
                    btnVoiceMic.setColorFilter(ContextCompat.getColor(innerCtx, R.color.text_secondary));
                }
                Toast.makeText(innerCtx, errorMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSpeechRmsChanged(float rmsdB) {}
        });
    }

    private void sendMessage() {
        if (editChatMessage == null) return;
        String input = editChatMessage.getText().toString().trim();
        if (input.isEmpty() && attachedImageUri == null) return;

        Context ctx = getContext();
        if (ctx == null) return;
        final Context appContext = ctx.getApplicationContext();

        editChatMessage.setText("");
        final Uri pickedUri = attachedImageUri;
        attachedImageUri = null;
        if (previewContainer != null) previewContainer.setVisibility(View.GONE);
        if (layoutWelcome != null) layoutWelcome.setVisibility(View.GONE);
        if (recyclerChat != null) recyclerChat.setVisibility(View.VISIBLE);

        final List<ChatMessage> historySnapshot = new ArrayList<>(currentHistory);

        AppExecutors.getInstance().diskIO().execute(() -> {
            String persistedImageUri = pickedUri != null
                    ? ImageUtil.copyImageToInternalStorage(appContext, pickedUri)
                    : null;
            String promptText = input.isEmpty() && persistedImageUri != null ? "Analyze this image" : input;

            boolean createdNewConv = false;
            if (currentConversationId == 0) {
                String title = promptText.length() > 32 ? promptText.substring(0, 32) + "..." : promptText;
                Conversation conv = new Conversation(title, System.currentTimeMillis(), System.currentTimeMillis(), prefManager.getActiveModelName());
                currentConversationId = db.conversationDao().insert(conv);
                createdNewConv = true;
            } else {
                Conversation conv = db.conversationDao().getConversationById(currentConversationId);
                if (conv != null) {
                    conv.setUpdatedAt(System.currentTimeMillis());
                    db.conversationDao().update(conv);
                }
            }
            final long targetConvId = currentConversationId;
            final boolean shouldAttachObserver = createdNewConv;

            // 1. Insert User Message
            ChatMessage userMsg = new ChatMessage(promptText, true, System.currentTimeMillis(), "User");
            userMsg.setConversationId(targetConvId);
            if (persistedImageUri != null) userMsg.setImageUri(persistedImageUri);
            db.chatDao().insert(userMsg);

            // 2. Check Natural Language Actions (only if no image attached)
            if (persistedImageUri == null) {
                NaturalLanguageActionParser.ActionResult actionResult = NaturalLanguageActionParser.parseAndExecute(appContext, input);

                if (actionResult.type != NaturalLanguageActionParser.ActionType.NONE && actionResult.isSuccess) {
                    ChatMessage assistantMsg = new ChatMessage(
                            actionResult.confirmationMessage,
                            false,
                            System.currentTimeMillis(),
                            prefManager.getActiveModelName()
                    );
                    assistantMsg.setConversationId(targetConvId);
                    assistantMsg.setHasAction(true);
                    assistantMsg.setActionType(actionResult.type.name());
                    assistantMsg.setActionTitle(actionResult.title);

                    db.chatDao().insert(assistantMsg);

                    if (shouldAttachObserver) {
                        Activity act = getActivity();
                        if (isAdded() && act != null && !act.isFinishing() && !act.isDestroyed()) {
                            act.runOnUiThread(() -> {
                                if (isAdded() && getView() != null) {
                                    loadConversation(targetConvId);
                                }
                            });
                        }
                    }

                    if (prefManager.isVoiceAutoSpeakEnabled() && voiceHelper != null) {
                        voiceHelper.speak(actionResult.confirmationMessage);
                    }
                    return;
                }
            }

            // 3. Regular AI Query via Multi-Provider Streaming (with vision if image attached)
            String activeModelName = prefManager.getActiveModelName();
            ChatMessage pendingMsg = new ChatMessage("Thinking...", false, System.currentTimeMillis(), activeModelName);
            pendingMsg.setConversationId(targetConvId);
            activeStreamingMsg = pendingMsg;
            activeStreamingConvId = targetConvId;

            Activity act = getActivity();
            if (isAdded() && act != null && !act.isFinishing() && !act.isDestroyed()) {
                act.runOnUiThread(() -> {
                    if (!isAdded() || getView() == null) return;
                    setStreamingUiState(true);
                    if (shouldAttachObserver) {
                        loadConversation(targetConvId);
                    } else if (currentConversationId == targetConvId) {
                        chatAdapter.appendMessage(pendingMsg);
                        safeScrollToBottom();
                    }
                });
            }

            activeStreamHandle = aiService.generateResponse(historySnapshot, promptText, persistedImageUri, new AiService.StreamCallback() {
                @Override public void onStart() {}

                @Override
                public void onToken(String token, String fullTextSoFar) {
                    if (!isAdded() || getView() == null) return;
                    if (currentConversationId == targetConvId) {
                        pendingMsg.setText(fullTextSoFar);
                        chatAdapter.updateLastMessage(fullTextSoFar);
                        safeScrollToBottom();
                    }
                }

                @Override
                public void onComplete(String fullResponse) {
                    activeStreamHandle = null;
                    activeStreamingMsg = null;
                    activeStreamingConvId = -1;
                    if (isAdded() && getView() != null) {
                        setStreamingUiState(false);
                        if (currentConversationId == targetConvId) {
                            chatAdapter.updateLastMessage(fullResponse);
                        }
                    }
                    pendingMsg.setText(fullResponse);
                    AppExecutors.getInstance().diskIO().execute(() -> {
                        db.chatDao().insert(pendingMsg);
                    });

                    if (prefManager.isVoiceAutoSpeakEnabled() && voiceHelper != null) {
                        voiceHelper.speak(fullResponse);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    activeStreamHandle = null;
                    activeStreamingMsg = null;
                    activeStreamingConvId = -1;
                    String errorNotice = "⚠️ " + errorMessage;
                    if (isAdded() && getView() != null) {
                        setStreamingUiState(false);
                        if (currentConversationId == targetConvId) {
                            chatAdapter.updateLastMessage(errorNotice);
                        }
                    }
                    pendingMsg.setText(errorNotice);
                    AppExecutors.getInstance().diskIO().execute(() -> {
                        db.chatDao().insert(pendingMsg);
                    });
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopActiveStream();
        if (currentMessagesLiveData != null) {
            try {
                currentMessagesLiveData.removeObservers(getViewLifecycleOwner());
            } catch (Exception ignored) {}
            currentMessagesLiveData = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (voiceHelper != null) {
            voiceHelper.destroy();
        }
    }
}
