package com.svape.qr.coorapp.ui.main;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.svape.qr.coorapp.databinding.ItemBackupBinding;
import com.svape.qr.coorapp.model.BackupItem;
import java.util.ArrayList;
import java.util.List;

public class BackupAdapter extends RecyclerView.Adapter<BackupAdapter.BackupViewHolder> {
    private List<BackupItem> items = new ArrayList<>();
    private final OnMapClickListener mapClickListener;

    public interface OnMapClickListener {
        void onMapClick(BackupItem item);
    }

    public BackupAdapter(OnMapClickListener mapClickListener) {
        this.mapClickListener = mapClickListener;
    }

    @NonNull
    @Override
    public BackupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBackupBinding binding = ItemBackupBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BackupViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BackupViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<BackupItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    class BackupViewHolder extends RecyclerView.ViewHolder {
        private final ItemBackupBinding binding;

        BackupViewHolder(ItemBackupBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(BackupItem item) {
            binding.etiquetaTextView.setText("etiqueta1d: " + item.getEtiqueta1d());
            binding.observacionTextView.setText(item.getObservacion());

            binding.mapButton.setOnClickListener(v ->
                    mapClickListener.onMapClick(item));
        }
    }
}