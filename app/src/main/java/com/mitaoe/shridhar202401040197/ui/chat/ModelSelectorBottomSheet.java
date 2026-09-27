package com.mitaoe.shridhar202401040197.ui.chat;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.mitaoe.shridhar202401040197.R;
import com.mitaoe.shridhar202401040197.data.model.AiModel;
import com.mitaoe.shridhar202401040197.data.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class ModelSelectorBottomSheet extends BottomSheetDialogFragment {

    public interface OnModelSelectedListener {
        void onModelSelected(@Nullable AiModel model);
        void onOpenSettingsRequested();
        void onOpenLibraryRequested();
    }

    private PreferenceManager prefManager;
    private OnModelSelectedListener listener;

    private RecyclerView recycler;
    private LinearLayout layoutNoPinned;
    private TextView btnManageModels, txtSelectorSubtitle;
    private MaterialButton btnSearchAndPinMore, btnEmptySearchModels, btnEmptyOpenSettings;
    private ModelAdapter adapter;
    private List<AiModel> pinnedList = new ArrayList<>();

    public void setOnModelSelectedListener(OnModelSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public int getTheme() {
        return R.style.Theme_AIPersonalAssistant_BottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_model_selector, container, false);
        prefManager = new PreferenceManager(requireContext());

        recycler = view.findViewById(R.id.recyclerModelSelector);
        layoutNoPinned = view.findViewById(R.id.layoutNoPinnedModels);
        txtSelectorSubtitle = view.findViewById(R.id.txtSelectorSubtitle);
        btnManageModels = view.findViewById(R.id.btnManageModels);
        btnSearchAndPinMore = view.findViewById(R.id.btnSearchAndPinMore);
        btnEmptySearchModels = view.findViewById(R.id.btnEmptySearchModels);
        btnEmptyOpenSettings = view.findViewById(R.id.btnEmptyOpenSettings);

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadPinnedModels();

        btnManageModels.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onOpenSettingsRequested();
        });

        btnSearchAndPinMore.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onOpenLibraryRequested();
        });

        btnEmptySearchModels.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onOpenLibraryRequested();
        });

        btnEmptyOpenSettings.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onOpenSettingsRequested();
        });

        return view;
    }

    private void loadPinnedModels() {
        pinnedList = prefManager.getPinnedModels();

        if (pinnedList.isEmpty()) {
            recycler.setVisibility(View.GONE);
            btnSearchAndPinMore.setVisibility(View.GONE);
            txtSelectorSubtitle.setVisibility(View.GONE);
            layoutNoPinned.setVisibility(View.VISIBLE);
        } else {
            recycler.setVisibility(View.VISIBLE);
            btnSearchAndPinMore.setVisibility(View.VISIBLE);
            txtSelectorSubtitle.setVisibility(View.VISIBLE);
            layoutNoPinned.setVisibility(View.GONE);

            adapter = new ModelAdapter(pinnedList);
            recycler.setAdapter(adapter);
        }
    }

    class ModelAdapter extends RecyclerView.Adapter<ModelAdapter.ViewHolder> {
        private final List<AiModel> models;

        public ModelAdapter(List<AiModel> models) {
            this.models = models;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_model_selector, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AiModel model = models.get(position);
            holder.txtTitle.setText(model.getDisplayName());
            holder.txtProviderBadge.setText(model.getProvider().toUpperCase());
            holder.txtSubtitle.setText(model.getId());

            // Provider Icon / Emoji
            holder.txtProviderIcon.setText(getProviderEmoji(model.getProvider()));

            // Free badge
            holder.badgeFree.setVisibility(model.isFree() ? View.VISIBLE : View.GONE);

            // Active model highlight (electric blue glow border + checkmark)
            boolean isSelected = model.getId().equalsIgnoreCase(prefManager.getActiveModelId());
            if (isSelected) {
                int strokeColor = ContextCompat.getColor(requireContext(), R.color.accent_gemini_blue);
                int strokeWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
                holder.card.setStrokeColor(strokeColor);
                holder.card.setStrokeWidth(strokeWidth);
                holder.imgSelectedCheck.setVisibility(View.VISIBLE);
            } else {
                int strokeColor = ContextCompat.getColor(requireContext(), R.color.card_stroke_dark);
                int strokeWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
                holder.card.setStrokeColor(strokeColor);
                holder.card.setStrokeWidth(strokeWidth);
                holder.imgSelectedCheck.setVisibility(View.GONE);
            }

            // Click to select as active model
            holder.itemView.setOnClickListener(v -> {
                prefManager.setActiveProvider(model.getProvider());
                prefManager.setActiveModelId(model.getId());
                prefManager.setActiveModelName(model.getDisplayName());
                Toast.makeText(requireContext(), "Switched to " + model.getDisplayName(), Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onModelSelected(model);
                }
                dismiss();
            });

            // One-tap remove / unpin button (✕)
            holder.btnRemoveModel.setOnClickListener(v -> {
                int currentPos = holder.getBindingAdapterPosition();
                if (currentPos == RecyclerView.NO_POSITION) return;

                AiModel removed = models.remove(currentPos);
                prefManager.unpinModel(removed.getId());
                notifyItemRemoved(currentPos);

                // If removed model was the active one, clear or switch
                if (removed.getId().equalsIgnoreCase(prefManager.getActiveModelId())) {
                    if (!models.isEmpty()) {
                        AiModel next = models.get(0);
                        prefManager.setActiveProvider(next.getProvider());
                        prefManager.setActiveModelId(next.getId());
                        prefManager.setActiveModelName(next.getDisplayName());
                        if (listener != null) listener.onModelSelected(next);
                    } else {
                        prefManager.setActiveProvider("");
                        prefManager.setActiveModelId("");
                        prefManager.setActiveModelName("");
                        if (listener != null) listener.onModelSelected(null);
                    }
                }

                Toast.makeText(requireContext(), "Removed " + removed.getDisplayName(), Toast.LENGTH_SHORT).show();

                if (models.isEmpty()) {
                    recycler.setVisibility(View.GONE);
                    btnSearchAndPinMore.setVisibility(View.GONE);
                    txtSelectorSubtitle.setVisibility(View.GONE);
                    layoutNoPinned.setVisibility(View.VISIBLE);
                }
            });
        }

        private String getProviderEmoji(String provider) {
            if (provider == null) return "✨";
            switch (provider.toLowerCase()) {
                case "openrouter": return "🌐";
                case "groq": return "⚡";
                case "gemini": return "✨";
                case "deepseek": return "🧠";
                case "xai": return "🚀";
                case "anthropic": return "🎭";
                case "openai": return "🤖";
                case "fireworks": return "🎆";
                case "custom": return "⚙️";
                default: return "🤖";
            }
        }

        @Override
        public int getItemCount() {
            return models.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView card;
            TextView txtProviderIcon, txtTitle, txtProviderBadge, txtSubtitle, badgeFree;
            ImageView imgSelectedCheck, btnRemoveModel;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.cardModelSelector);
                txtProviderIcon = itemView.findViewById(R.id.txtProviderIcon);
                txtTitle = itemView.findViewById(R.id.txtModelTitle);
                txtProviderBadge = itemView.findViewById(R.id.txtProviderBadge);
                txtSubtitle = itemView.findViewById(R.id.txtModelSubtitle);
                badgeFree = itemView.findViewById(R.id.badgeFree);
                imgSelectedCheck = itemView.findViewById(R.id.imgSelectedCheck);
                btnRemoveModel = itemView.findViewById(R.id.btnRemoveModel);
            }
        }
    }
}
