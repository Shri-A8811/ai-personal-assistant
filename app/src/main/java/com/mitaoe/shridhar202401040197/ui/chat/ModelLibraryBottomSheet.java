package com.mitaoe.shridhar202401040197.ui.chat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.google.android.material.chip.ChipGroup;
import com.mitaoe.shridhar202401040197.R;
import com.mitaoe.shridhar202401040197.ai.AiService;
import com.mitaoe.shridhar202401040197.data.model.AiModel;
import com.mitaoe.shridhar202401040197.data.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class ModelLibraryBottomSheet extends BottomSheetDialogFragment {

    public interface OnModelLibraryListener {
        void onModelSelected(AiModel model);
        void onOpenSettingsRequested();
        void onModelsUpdated();
    }

    private PreferenceManager prefManager;
    private AiService aiService;
    private OnModelLibraryListener listener;

    private EditText editLibSearch;
    private ImageView btnLibClearSearch;
    private TextView txtLibModelCount;
    private MaterialButton btnLibRefresh, btnLibDone, btnLibGoToSettings;
    private ChipGroup chipGroupFilters;
    private ProgressBar progressLibLoading;
    private RecyclerView recyclerLibModels;
    private LinearLayout layoutLibEmpty;

    private List<AiModel> allModels = new ArrayList<>();
    private List<AiModel> filteredModels = new ArrayList<>();
    private ModelLibraryAdapter adapter;

    private String currentSearchQuery = "";
    private String currentFilterMode = "all"; // "all", "free", "pinned", or provider name

    public void setOnModelLibraryListener(OnModelLibraryListener listener) {
        this.listener = listener;
    }

    @Override
    public int getTheme() {
        return R.style.Theme_AIPersonalAssistant_BottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_model_library, container, false);

        prefManager = new PreferenceManager(requireContext());
        aiService = new AiService(requireContext());

        editLibSearch = view.findViewById(R.id.editLibSearch);
        btnLibClearSearch = view.findViewById(R.id.btnLibClearSearch);
        txtLibModelCount = view.findViewById(R.id.txtLibModelCount);
        btnLibRefresh = view.findViewById(R.id.btnLibRefresh);
        btnLibDone = view.findViewById(R.id.btnLibDone);
        btnLibGoToSettings = view.findViewById(R.id.btnLibGoToSettings);
        chipGroupFilters = view.findViewById(R.id.chipGroupFilters);
        progressLibLoading = view.findViewById(R.id.progressLibLoading);
        recyclerLibModels = view.findViewById(R.id.recyclerLibModels);
        layoutLibEmpty = view.findViewById(R.id.layoutLibEmpty);

        recyclerLibModels.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ModelLibraryAdapter();
        recyclerLibModels.setAdapter(adapter);

        loadModels();

        // Search text watcher for real-time filtering
        editLibSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim().toLowerCase() : "";
                btnLibClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnLibClearSearch.setOnClickListener(v -> editLibSearch.setText(""));

        // Filter chips
        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentFilterMode = "all";
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chipAll) currentFilterMode = "all";
                else if (id == R.id.chipFreeOnly) currentFilterMode = "free";
                else if (id == R.id.chipPinnedOnly) currentFilterMode = "pinned";
                else if (id == R.id.chipOpenRouter) currentFilterMode = "openrouter";
                else if (id == R.id.chipGroq) currentFilterMode = "groq";
                else if (id == R.id.chipGemini) currentFilterMode = "gemini";
                else if (id == R.id.chipDeepSeek) currentFilterMode = "deepseek";
                else if (id == R.id.chipXai) currentFilterMode = "xai";
                else if (id == R.id.chipAnthropic) currentFilterMode = "anthropic";
                else if (id == R.id.chipOpenAi) currentFilterMode = "openai";
                else if (id == R.id.chipFireworks) currentFilterMode = "fireworks";
                else if (id == R.id.chipCustom) currentFilterMode = "custom";
            }
            applyFilter();
        });

        btnLibRefresh.setOnClickListener(v -> refreshLiveModels());

        btnLibGoToSettings.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onOpenSettingsRequested();
            }
        });

        btnLibDone.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onModelsUpdated();
            }
        });

        return view;
    }

    private void loadModels() {
        allModels = prefManager.getCachedFetchedModels();

        // Also ensure any pinned models are present in the library
        List<AiModel> pinned = prefManager.getPinnedModels();
        for (AiModel p : pinned) {
            boolean found = false;
            for (AiModel m : allModels) {
                if (m.getId().equalsIgnoreCase(p.getId())) {
                    found = true;
                    m.setPinned(true);
                    break;
                }
            }
            if (!found) {
                p.setPinned(true);
                allModels.add(p);
            }
        }

        if (allModels.isEmpty()) {
            refreshLiveModels();
        } else {
            applyFilter();
        }
    }

    public void refreshLiveModels() {
        progressLibLoading.setVisibility(View.VISIBLE);

        String[] providers = {"openrouter", "groq", "gemini", "deepseek", "xai", "anthropic", "openai", "fireworks", "custom"};
        int[] pendingCount = {0};

        for (String p : providers) {
            String key = prefManager.getApiKey(p);
            // OpenRouter can fetch even with empty key
            if (!key.isEmpty() || "openrouter".equalsIgnoreCase(p)) {
                pendingCount[0]++;
                aiService.fetchAvailableModels(p, key, new AiService.ModelFetchCallback() {
                    @Override
                    public void onSuccess(List<AiModel> fetched) {
                        pendingCount[0]--;
                        if (isAdded()) {
                            prefManager.mergeCachedFetchedModels(fetched);
                            if (pendingCount[0] <= 0) {
                                progressLibLoading.setVisibility(View.GONE);
                                loadModels();
                            }
                        }
                    }

                    @Override
                    public void onError(String error) {
                        pendingCount[0]--;
                        if (isAdded() && pendingCount[0] <= 0) {
                            progressLibLoading.setVisibility(View.GONE);
                            loadModels();
                        }
                    }
                });
            }
        }

        if (pendingCount[0] == 0) {
            progressLibLoading.setVisibility(View.GONE);
            applyFilter();
        }
    }

    private void applyFilter() {
        filteredModels.clear();

        for (AiModel m : allModels) {
            // 1. Check Category/Chip Filter
            boolean matchesChip = true;
            if ("free".equals(currentFilterMode)) {
                matchesChip = m.isFree();
            } else if ("pinned".equals(currentFilterMode)) {
                matchesChip = prefManager.isModelPinned(m.getId());
            } else if (!"all".equals(currentFilterMode)) {
                matchesChip = currentFilterMode.equalsIgnoreCase(m.getProvider());
            }

            if (!matchesChip) continue;

            // 2. Check Search Query
            if (!currentSearchQuery.isEmpty()) {
                String name = m.getDisplayName() != null ? m.getDisplayName().toLowerCase() : "";
                String id = m.getId() != null ? m.getId().toLowerCase() : "";
                String provider = m.getProvider() != null ? m.getProvider().toLowerCase() : "";
                String desc = m.getDescription() != null ? m.getDescription().toLowerCase() : "";

                boolean matchesQuery = name.contains(currentSearchQuery)
                        || id.contains(currentSearchQuery)
                        || provider.contains(currentSearchQuery)
                        || desc.contains(currentSearchQuery);

                if (!matchesQuery) continue;
            }

            filteredModels.add(m);
        }

        txtLibModelCount.setText(filteredModels.size() + " models available");
        adapter.notifyDataSetChanged();

        if (filteredModels.isEmpty()) {
            recyclerLibModels.setVisibility(View.GONE);
            layoutLibEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerLibModels.setVisibility(View.VISIBLE);
            layoutLibEmpty.setVisibility(View.GONE);
        }
    }

    class ModelLibraryAdapter extends RecyclerView.Adapter<ModelLibraryAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_model_library, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AiModel model = filteredModels.get(position);
            holder.txtName.setText(model.getDisplayName());
            holder.txtId.setText(model.getId());
            holder.txtProvider.setText(model.getProvider().toUpperCase());
            holder.txtDesc.setText(model.getDescription() != null ? model.getDescription() : "AI Assistant Model");

            holder.badgeFree.setVisibility(model.isFree() ? View.VISIBLE : View.GONE);

            boolean isPinned = prefManager.isModelPinned(model.getId());
            if (isPinned) {
                holder.btnPin.setImageResource(R.drawable.ic_pin_filled);
                holder.btnPin.setColorFilter(ContextCompat.getColor(requireContext(), R.color.accent_gemini_blue));
            } else {
                holder.btnPin.setImageResource(R.drawable.ic_pin);
                holder.btnPin.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_muted));
            }

            boolean isActive = model.getId().equalsIgnoreCase(prefManager.getActiveModelId());
            if (isActive) {
                holder.btnSelectActive.setText("✓ In Use");
                holder.btnSelectActive.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_gemini_blue));
            } else {
                holder.btnSelectActive.setText("Use in Chat");
                holder.btnSelectActive.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            }

            // Pin / Unpin click
            holder.btnPin.setOnClickListener(v -> {
                boolean currentlyPinned = prefManager.isModelPinned(model.getId());
                if (currentlyPinned) {
                    prefManager.unpinModel(model.getId());
                    model.setPinned(false);
                    Toast.makeText(requireContext(), "Unpinned " + model.getDisplayName(), Toast.LENGTH_SHORT).show();
                } else {
                    prefManager.pinModel(model);
                    model.setPinned(true);
                    Toast.makeText(requireContext(), "📌 Pinned " + model.getDisplayName() + " to chat", Toast.LENGTH_SHORT).show();
                }
                notifyItemChanged(position);
                if (listener != null) {
                    listener.onModelsUpdated();
                }
            });

            // Select as Active click
            View.OnClickListener selectListener = v -> {
                prefManager.setActiveProvider(model.getProvider());
                prefManager.setActiveModelId(model.getId());
                prefManager.setActiveModelName(model.getDisplayName());
                // Auto pin if not yet pinned
                if (!prefManager.isModelPinned(model.getId())) {
                    prefManager.pinModel(model);
                }
                Toast.makeText(requireContext(), "Selected: " + model.getDisplayName(), Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onModelSelected(model);
                    listener.onModelsUpdated();
                }
                dismiss();
            };

            holder.btnSelectActive.setOnClickListener(selectListener);
            holder.itemView.setOnClickListener(selectListener);
        }

        @Override
        public int getItemCount() {
            return filteredModels.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtName, txtId, txtProvider, txtDesc, badgeFree;
            ImageView btnPin;
            MaterialButton btnSelectActive;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                txtName = itemView.findViewById(R.id.txtLibModelName);
                txtId = itemView.findViewById(R.id.txtLibModelId);
                txtProvider = itemView.findViewById(R.id.txtLibProvider);
                txtDesc = itemView.findViewById(R.id.txtLibDescription);
                badgeFree = itemView.findViewById(R.id.badgeLibFree);
                btnPin = itemView.findViewById(R.id.btnLibPin);
                btnSelectActive = itemView.findViewById(R.id.btnLibSelectActive);
            }
        }
    }
}
