package com.mitaoe.shridhar202401040197.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputLayout;
import com.mitaoe.shridhar202401040197.R;
import com.mitaoe.shridhar202401040197.ai.AiService;
import com.mitaoe.shridhar202401040197.data.model.AiModel;
import com.mitaoe.shridhar202401040197.data.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class SettingsBottomSheet extends BottomSheetDialogFragment {

    public interface OnSettingsSavedListener {
        void onSettingsSaved();
    }

    private PreferenceManager prefManager;
    private AiService aiService;
    private OnSettingsSavedListener listener;

    private Spinner spinnerProvider;
    private EditText editApiKey, editCustomUrl, editCustomModelId;
    private TextInputLayout layoutCustomUrl;
    private Button btnFetchLiveModels, btnAddCustomModel, btnSaveSettings, btnCancelSettings;
    private ProgressBar progressFetchModels;
    private RecyclerView recyclerSettingModels;
    private MaterialSwitch switchVoiceAutoSpeak;

    private final String[] providers = {"OpenRouter", "Google Gemini", "Groq", "NVIDIA NIM", "OpenAI", "Custom"};
    private final String[] providerKeys = {"openrouter", "gemini", "groq", "nim", "openai", "custom"};

    private List<AiModel> modelsList = new ArrayList<>();
    private ModelSettingAdapter modelAdapter;

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

        spinnerProvider = view.findViewById(R.id.spinnerProvider);
        editApiKey = view.findViewById(R.id.editApiKey);
        editCustomUrl = view.findViewById(R.id.editCustomUrl);
        editCustomModelId = view.findViewById(R.id.editCustomModelId);
        layoutCustomUrl = view.findViewById(R.id.layoutCustomUrl);
        btnFetchLiveModels = view.findViewById(R.id.btnFetchLiveModels);
        btnAddCustomModel = view.findViewById(R.id.btnAddCustomModel);
        btnSaveSettings = view.findViewById(R.id.btnSaveSettings);
        btnCancelSettings = view.findViewById(R.id.btnCancelSettings);
        progressFetchModels = view.findViewById(R.id.progressFetchModels);
        recyclerSettingModels = view.findViewById(R.id.recyclerSettingModels);
        switchVoiceAutoSpeak = view.findViewById(R.id.switchVoiceAutoSpeak);

        // Providers spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, providers);
        spinnerProvider.setAdapter(spinnerAdapter);

        int currentIdx = 0;
        String active = prefManager.getActiveProvider();
        for (int i = 0; i < providerKeys.length; i++) {
            if (providerKeys[i].equalsIgnoreCase(active)) {
                currentIdx = i;
                break;
            }
        }
        spinnerProvider.setSelection(currentIdx);
        loadProviderKey(providerKeys[currentIdx]);

        spinnerProvider.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String pKey = providerKeys[position];
                loadProviderKey(pKey);
                layoutCustomUrl.setVisibility("custom".equalsIgnoreCase(pKey) ? View.VISIBLE : View.GONE);
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Voice switch
        switchVoiceAutoSpeak.setChecked(prefManager.isVoiceAutoSpeakEnabled());

        // Models RecyclerView
        recyclerSettingModels.setLayoutManager(new LinearLayoutManager(requireContext()));
        modelsList = prefManager.getPinnedModels();
        modelAdapter = new ModelSettingAdapter(modelsList);
        recyclerSettingModels.setAdapter(modelAdapter);

        // Fetch Live Models Button
        btnFetchLiveModels.setOnClickListener(v -> {
            int pos = spinnerProvider.getSelectedItemPosition();
            String pKey = providerKeys[pos];
            String key = editApiKey.getText().toString().trim();

            if (key.isEmpty() && !"custom".equalsIgnoreCase(pKey)) {
                Toast.makeText(requireContext(), "Please enter an API key first to fetch models.", Toast.LENGTH_SHORT).show();
                return;
            }

            progressFetchModels.setVisibility(View.VISIBLE);
            aiService.fetchAvailableModels(pKey, key, new AiService.ModelFetchCallback() {
                @Override
                public void onSuccess(List<AiModel> fetched) {
                    progressFetchModels.setVisibility(View.GONE);
                    if (fetched.isEmpty()) {
                        Toast.makeText(requireContext(), "No models returned.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Merge into modelsList without duplicate IDs
                    for (AiModel f : fetched) {
                        boolean exists = false;
                        for (AiModel m : modelsList) {
                            if (m.getId().equalsIgnoreCase(f.getId())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            modelsList.add(f);
                        }
                    }
                    modelAdapter.notifyDataSetChanged();
                    Toast.makeText(requireContext(), "Fetched " + fetched.size() + " models!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    progressFetchModels.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Add custom model ID
        btnAddCustomModel.setOnClickListener(v -> {
            String customId = editCustomModelId.getText().toString().trim();
            if (customId.isEmpty()) return;

            int pos = spinnerProvider.getSelectedItemPosition();
            String pKey = providerKeys[pos];

            AiModel custom = new AiModel(customId, customId, pKey, false, true, "Custom added model");
            modelsList.add(0, custom);
            modelAdapter.notifyItemInserted(0);
            recyclerSettingModels.scrollToPosition(0);
            editCustomModelId.setText("");
            Toast.makeText(requireContext(), "Custom model added & pinned!", Toast.LENGTH_SHORT).show();
        });

        btnCancelSettings.setOnClickListener(v -> dismiss());

        btnSaveSettings.setOnClickListener(v -> saveAllSettings());

        return view;
    }

    private void loadProviderKey(String pKey) {
        switch (pKey) {
            case "openrouter": editApiKey.setText(prefManager.getOpenRouterApiKey()); break;
            case "gemini": editApiKey.setText(prefManager.getGeminiApiKey()); break;
            case "groq": editApiKey.setText(prefManager.getGroqApiKey()); break;
            case "nim": editApiKey.setText(prefManager.getNimApiKey()); break;
            case "openai": editApiKey.setText(prefManager.getOpenAiApiKey()); break;
            case "custom":
                editApiKey.setText(prefManager.getCustomApiKey());
                editCustomUrl.setText(prefManager.getCustomBaseUrl());
                break;
        }
    }

    private void saveAllSettings() {
        int pos = spinnerProvider.getSelectedItemPosition();
        String pKey = providerKeys[pos];
        String key = editApiKey.getText().toString().trim();

        // Save selected provider key
        switch (pKey) {
            case "openrouter": prefManager.setOpenRouterApiKey(key); break;
            case "gemini": prefManager.setGeminiApiKey(key); break;
            case "groq": prefManager.setGroqApiKey(key); break;
            case "nim": prefManager.setNimApiKey(key); break;
            case "openai": prefManager.setOpenAiApiKey(key); break;
            case "custom":
                prefManager.setCustomApiKey(key);
                prefManager.setCustomBaseUrl(editCustomUrl.getText().toString().trim());
                break;
        }

        prefManager.setActiveProvider(pKey);
        prefManager.setVoiceAutoSpeakEnabled(switchVoiceAutoSpeak.isChecked());
        prefManager.savePinnedModels(modelsList);

        Toast.makeText(requireContext(), "Settings saved successfully!", Toast.LENGTH_SHORT).show();

        if (listener != null) {
            listener.onSettingsSaved();
        }
        dismiss();
    }

    static class ModelSettingAdapter extends RecyclerView.Adapter<ModelSettingAdapter.ViewHolder> {
        private final List<AiModel> models;

        public ModelSettingAdapter(List<AiModel> models) {
            this.models = models;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pinned_model_setting, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AiModel m = models.get(position);
            holder.txtName.setText(m.getDisplayName());
            holder.txtId.setText(m.getId() + " (" + m.getProvider() + ")");
            holder.badgeFree.setVisibility(m.isFree() ? View.VISIBLE : View.GONE);

            holder.checkPinned.setOnCheckedChangeListener(null);
            holder.checkPinned.setChecked(m.isPinned());
            holder.checkPinned.setOnCheckedChangeListener((btn, isChecked) -> {
                m.setPinned(isChecked);
            });
        }

        @Override
        public int getItemCount() {
            return models.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            CheckBox checkPinned;
            TextView txtName, txtId, badgeFree;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                checkPinned = itemView.findViewById(R.id.checkModelPinned);
                txtName = itemView.findViewById(R.id.txtSettingModelName);
                txtId = itemView.findViewById(R.id.txtSettingModelId);
                badgeFree = itemView.findViewById(R.id.badgeSettingFree);
            }
        }
    }
}
