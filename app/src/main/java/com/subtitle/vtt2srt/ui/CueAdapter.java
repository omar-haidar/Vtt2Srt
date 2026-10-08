package com.subtitle.vtt2srt.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.subtitle.vtt2srt.databinding.ItemCueBinding;
import com.subtitle.vtt2srt.domain.model.SubtitleCue;
import com.subtitle.vtt2srt.util.TimeFormatter;

import java.util.Collections;
import java.util.List;

public class CueAdapter extends RecyclerView.Adapter<CueAdapter.Holder> {

    private List<SubtitleCue> items = Collections.emptyList();

    public void submit(List<SubtitleCue> newItems) {
        items = newItems != null ? newItems : Collections.<SubtitleCue>emptyList();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemCueBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SubtitleCue cue = items.get(position);
        holder.binding.tvTime.setText(position + 1 + "  |  "
                + TimeFormatter.toSrt(cue.getStartMs()) + " - " + TimeFormatter.toSrt(cue.getEndMs()));
        holder.binding.tvOriginal.setText(cue.getText());
        if (cue.isTranslated()) {
            holder.binding.tvTranslated.setVisibility(View.VISIBLE);
            holder.binding.tvTranslated.setText(cue.getTranslatedText());
        } else {
            holder.binding.tvTranslated.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemCueBinding binding;

        Holder(ItemCueBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
