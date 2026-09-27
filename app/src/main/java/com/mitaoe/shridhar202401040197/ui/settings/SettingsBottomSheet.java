package com.mitaoe.shridhar202401040197.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mitaoe.shridhar202401040197.R;
import com.mitaoe.shridhar202401040197.ai.AiService;
import com.mitaoe.shridhar202401040197.data.model.AiModel;
import com.mitaoe.shridhar202401040197.data.preference.PreferenceManager;
import com.mitaoe.shridhar202401040197.ui.chat.ModelLibraryBottomSheet;

import java.util.ArrayList;
import java.util.List;

public class SettingsBottomSheet extends BottomSheetDialogFragment {

    public interface OnSettingsSavedListener {
        void onSettingsSaved();
    }

    private PreferenceManager prefManager;
    private AiService aiService;
    private OnSettingsSavedListener listener;

    private TextView txtPinnedSummary;
    private MaterialCardView cardOpenModelLibrary;
    private MaterialButton btnOpenLibraryTop, btnDoneSettings;
    private ImageView btnCloseSettings;
    private RecyclerView recyclerProviders;
    private MaterialSwitch switchVoiceAutoSpeak;
    private ProviderAdapter providerAdapter;

    public void setOnSettingsSavedListener(OnSettingsSavedListener listener) {
        this.listener = listener;
    }

    @Override
    public int getTheme() {
        return R.style.Theme_AIPersonalAssistant_BottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_settings, container, false);

        prefManager = new PreferenceManager(requireContext());
        aiService = new AiService(requireContext());

        txtPinnedSummary = view.findViewById(R.id.txtPinnedSummary);
        cardOpenModelLibrary = view.findViewById(R.id.cardOpenModelLibrary);
        btnOpenLibraryTop = view.findViewById(R.id.btnOpenLibraryTop);
        btnDoneSettings = view.findViewById(R.id.btnDoneSettings);
        btnCloseSettings = view.findViewById(R.id.btnCloseSettings);
        recyclerProviders = view.findViewById(R.id.recyclerProviders);
        switchVoiceAutoSpeak = view.findViewById(R.id.switchVoiceAutoSpeak);

        updatePinnedSummary();

        // Providers list
        recyclerProviders.setLayoutManager(new LinearLayoutManager(requireContext()));
        providerAdapter = new ProviderAdapter(getProviderItems());
        recyclerProviders.setAdapter(providerAdapter);

        // Voice switch
        switchVoiceAutoSpeak.setChecked(prefManager.isVoiceAutoSpeakEnabled());
        switchVoiceAutoSpeak.setOnCheckedChangeListener((btn, isChecked) -> {
            prefManager.setVoiceAutoSpeakEnabled(isChecked);
        });

        View.OnClickListener openLibListener = v -> openModelLibrary(null);
        cardOpenModelLibrary.setOnClickListener(openLibListener);
        btnOpenLibraryTop.setOnClickListener(openLibListener);

        btnCloseSettings.setOnClickListener(v -> dismiss());
        btnDoneSettings.setOnClickListener(v -> {
            if (listener != null) listener.onSettingsSaved();
            dismiss();
        });

        return view;
    }

    private void updatePinnedSummary() {
        List<AiModel> pinned = prefManager.getPinnedModels();
        if (pinned.isEmpty()) {
            txtPinnedSummary.setText("0 models pinned — tap to search & pin free models");
        } else {
            txtPinnedSummary.setText(pinned.size() + " models pinned for chat switcher");
        }
    }

    private void openModelLibrary(@Nullable String initialProviderFilter) {
        ModelLibraryBottomSheet librarySheet = new ModelLibraryBottomSheet();
        librarySheet.setOnModelLibraryListener(new ModelLibraryBottomSheet.OnModelLibraryListener() {
            @Override
            public void onModelSelected(AiModel model) {
                updatePinnedSummary();
                if (listener != null) listener.onSettingsSaved();
                dismiss();
            }

            @Override
            public void onOpenSettingsRequested() {}

            @Override
            public void onModelsUpdated() {
                updatePinnedSummary();
                if (listener != null) listener.onSettingsSaved();
            }
        });
        librarySheet.show(getParentFragmentManager(), "ModelLibrarySheet");
    }

    private List<ProviderItem> getProviderItems() {
        List<ProviderItem> items = new ArrayList<>();
        items.add(new ProviderItem("openrouter", "OpenRouter", "Free & paid models: Llama, DeepSeek, Qwen"));
        items.add(new ProviderItem("groq", "Groq", "Ultra-fast free inference: Llama 3.3, 3.1"));
        items.add(new ProviderItem("gemini", "Google Gemini", "Direct Google AI: Gemini 2.0 Flash, 1.5 Pro"));
        items.add(new ProviderItem("deepseek", "DeepSeek", "DeepSeek V3 (Chat) & R1 (Reasoner)"));
        items.add(new ProviderItem("xai", "xAI (Grok)", "Grok 2, Grok Vision"));
        items.add(new ProviderItem("anthropic", "Anthropic", "Claude 3.5 Sonnet, Claude 3.5 Haiku"));
        items.add(new ProviderItem("openai", "OpenAI", "ChatGPT GPT-4o, GPT-4o Mini"));
        items.add(new ProviderItem("fireworks", "Fireworks AI", "High-speed open model inference"));
        items.add(new ProviderItem("custom", "Custom Endpoint", "Local AI: Ollama, LM Studio, vLLM"));
        return items;
    }

    static class ProviderItem {
        String key;
        String title;
        String subtitle;
        boolean isExpanded = false;

        public ProviderItem(String key, String title, String subtitle) {
            this.key = key;
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    class ProviderAdapter extends RecyclerView.Adapter<ProviderAdapter.ViewHolder> {
        private final List<ProviderItem> items;

        public ProviderAdapter(List<ProviderItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_provider_setting, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProviderItem item = items.get(position);
            holder.txtTitle.setText(item.title);

            String savedKey = prefManager.getApiKey(item.key);
            boolean hasKey = !savedKey.isEmpty();

            // Status dot
            if (hasKey) {
                holder.viewStatusDot.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.status_active));
                holder.txtKeyPreview.setText(PreferenceManager.maskApiKey(savedKey));
                holder.txtKeyPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            } else {
                holder.viewStatusDot.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.status_inactive));
                holder.txtKeyPreview.setText("Paste " + item.title + " key");
                holder.txtKeyPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            }

            // Expanded state
            holder.layoutExpanded.setVisibility(item.isExpanded ? View.VISIBLE : View.GONE);
            holder.imgChevron.setRotation(item.isExpanded ? 180f : 0f);

            // Pre-fill inputs
            holder.editKey.setText(savedKey);
            if ("custom".equalsIgnoreCase(item.key)) {
                holder.layoutCustomBaseUrl.setVisibility(View.VISIBLE);
                holder.editCustomBaseUrl.setText(prefManager.getCustomBaseUrl());
            } else {
                holder.layoutCustomBaseUrl.setVisibility(View.GONE);
            }

            // Expand/collapse toggle
            holder.layoutRowHeader.setOnClickListener(v -> {
                item.isExpanded = !item.isExpanded;
                notifyItemChanged(position);
            });

            // Save & Fetch Button
            holder.btnSaveProviderKey.setOnClickListener(v -> {
                String inputKey = holder.editKey.getText() != null ? holder.editKey.getText().toString().trim() : "";
                prefManager.setApiKey(item.key, inputKey);

                if ("custom".equalsIgnoreCase(item.key) && holder.editCustomBaseUrl.getText() != null) {
                    prefManager.setCustomBaseUrl(holder.editCustomBaseUrl.getText().toString().trim());
                }

                // If no active provider set, make this provider active
                if (prefManager.getActiveProvider().isEmpty()) {
                    prefManager.setActiveProvider(item.key);
                }

                notifyItemChanged(position);

                holder.progressFetch.setVisibility(View.VISIBLE);
                aiService.fetchAvailableModels(item.key, inputKey, new AiService.ModelFetchCallback() {
                    @Override
                    public void onSuccess(List<AiModel> models) {
                        if (isAdded()) {
                            holder.progressFetch.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Saved! Fetched " + models.size() + " models.", Toast.LENGTH_SHORT).show();
                            openModelLibrary(item.key);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (isAdded()) {
                            holder.progressFetch.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Key saved! (" + error + ")", Toast.LENGTH_SHORT).show();
                            openModelLibrary(item.key);
                        }
                    }
                });
            });

            // Search Models for this Provider Button
            holder.btnSearchModelsForProvider.setOnClickListener(v -> {
                openModelLibrary(item.key);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View viewStatusDot;
            TextView txtTitle, txtKeyPreview;
            ImageView imgChevron;
            LinearLayout layoutRowHeader, layoutExpanded;
            TextInputLayout layoutCustomBaseUrl, layoutKeyInput;
            TextInputEditText editCustomBaseUrl, editKey;
            ProgressBar progressFetch;
            MaterialButton btnSearchModelsForProvider, btnSaveProviderKey;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                viewStatusDot = itemView.findViewById(R.id.viewStatusDot);
                txtTitle = itemView.findViewById(R.id.txtProviderTitle);
                txtKeyPreview = itemView.findViewById(R.id.txtKeyPreview);
                imgChevron = itemView.findViewById(R.id.imgChevron);
                layoutRowHeader = itemView.findViewById(R.id.layoutRowHeader);
                layoutExpanded = itemView.findViewById(R.id.layoutExpanded);
                layoutCustomBaseUrl = itemView.findViewById(R.id.layoutCustomBaseUrl);
                layoutKeyInput = itemView.findViewById(R.id.layoutKeyInput);
                editCustomBaseUrl = itemView.findViewById(R.id.editCustomBaseUrl);
                editKey = itemView.findViewById(R.id.editProviderKey);
                progressFetch = itemView.findViewById(R.id.progressProviderFetch);
                btnSearchModelsForProvider = itemView.findViewById(R.id.btnSearchModelsForProvider);
                btnSaveProviderKey = itemView.findViewById(R.id.btnSaveProviderKey);
            }
        }
    }
}
