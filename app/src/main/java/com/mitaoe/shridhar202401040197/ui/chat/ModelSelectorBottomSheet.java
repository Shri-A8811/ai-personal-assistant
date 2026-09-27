package com.mitaoe.shridhar202401040197.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.mitaoe.shridhar202401040197.R;
import com.mitaoe.shridhar202401040197.data.model.AiModel;
import com.mitaoe.shridhar202401040197.data.preference.PreferenceManager;

import java.util.List;

public class ModelSelectorBottomSheet extends BottomSheetDialogFragment {

    public interface OnModelSelectedListener {
        void onModelSelected(AiModel model);
        void onOpenSettingsRequested();
        void onOpenLibraryRequested();
    }

    private PreferenceManager prefManager;
    private OnModelSelectedListener listener;

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

        RecyclerView recycler = view.findViewById(R.id.recyclerModelSelector);
        LinearLayout layoutNoPinned = view.findViewById(R.id.layoutNoPinnedModels);
        TextView btnManageModels = view.findViewById(R.id.btnManageModels);
        MaterialButton btnSearchAndPinMore = view.findViewById(R.id.btnSearchAndPinMore);

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        List<AiModel> pinned = prefManager.getPinnedModels();

        if (pinned.isEmpty()) {
            recycler.setVisibility(View.GONE);
            layoutNoPinned.setVisibility(View.VISIBLE);
        } else {
            recycler.setVisibility(View.VISIBLE);
            layoutNoPinned.setVisibility(View.GONE);

            ModelAdapter adapter = new ModelAdapter(pinned, prefManager.getActiveModelId(), model -> {
                prefManager.setActiveProvider(model.getProvider());
                prefManager.setActiveModelId(model.getId());
                prefManager.setActiveModelName(model.getDisplayName());
                if (listener != null) {
                    listener.onModelSelected(model);
                }
                dismiss();
            });
            recycler.setAdapter(adapter);
        }

        btnManageModels.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onOpenSettingsRequested();
            }
        });

        btnSearchAndPinMore.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onOpenLibraryRequested();
            }
        });

        return view;
    }

    static class ModelAdapter extends RecyclerView.Adapter<ModelAdapter.ViewHolder> {
        private final List<AiModel> models;
        private final String activeModelId;
        private final OnItemClickListener clickListener;

        interface OnItemClickListener {
            void onItemClick(AiModel model);
        }

        public ModelAdapter(List<AiModel> models, String activeModelId, OnItemClickListener clickListener) {
            this.models = models;
            this.activeModelId = activeModelId;
            this.clickListener = clickListener;
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
            String desc = model.getDescription() != null ? model.getDescription() : model.getId();
            holder.txtSubtitle.setText(model.getProvider().toUpperCase() + " • " + desc);

            if (model.isFree()) {
                holder.badgeFree.setVisibility(View.VISIBLE);
            } else {
                holder.badgeFree.setVisibility(View.GONE);
            }

            boolean isSelected = model.getId().equalsIgnoreCase(activeModelId);
            holder.imgSelectedCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            holder.itemView.setOnClickListener(v -> clickListener.onItemClick(model));
        }

        @Override
        public int getItemCount() {
            return models.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtTitle, txtSubtitle, badgeFree;
            ImageView imgSelectedCheck;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                txtTitle = itemView.findViewById(R.id.txtModelTitle);
                txtSubtitle = itemView.findViewById(R.id.txtModelSubtitle);
                badgeFree = itemView.findViewById(R.id.badgeFree);
                imgSelectedCheck = itemView.findViewById(R.id.imgSelectedCheck);
            }
        }
    }
}
