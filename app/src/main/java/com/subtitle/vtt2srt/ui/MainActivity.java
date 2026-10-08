package com.subtitle.vtt2srt.ui;

import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.subtitle.vtt2srt.R;
import com.subtitle.vtt2srt.databinding.ActivityMainBinding;
import com.subtitle.vtt2srt.domain.model.ConversionOptions;
import com.subtitle.vtt2srt.domain.model.SubtitleCue;
import com.subtitle.vtt2srt.util.Event;
import com.subtitle.vtt2srt.util.UI;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private final CueAdapter adapter = new CueAdapter();

    private final ActivityResultLauncher<String[]> pickLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) viewModel.loadFile(uri);
            });

    private final ActivityResultLauncher<String> saveLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/x-subrip"), uri -> {
                if (uri != null) viewModel.saveTo(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
       UI.addSystemWindowInsetToPadding(binding.appbar,true,true,true,false);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        binding.rvCues.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCues.setAdapter(adapter);

        binding.btnPick.setOnClickListener(v -> pickLauncher.launch(new String[]{"*/*"}));
        binding.btnConvert.setOnClickListener(v -> viewModel.convert(buildOptions()));
        binding.btnCancel.setOnClickListener(v -> viewModel.cancel());
        binding.btnSave.setOnClickListener(v -> saveLauncher.launch(viewModel.getSuggestedFileName()));
        binding.swTranslate.setOnCheckedChangeListener((button, checked) -> updateOptionDependencies());
        updateOptionDependencies();

        viewModel.getFileName().observe(this, name -> binding.tvFileName.setText(name));
        viewModel.getCues().observe(this, this::renderCues);
        viewModel.getStatus().observe(this, this::renderStatus);
        viewModel.getProgress().observe(this, percent -> {
            binding.progressBar.setProgressCompat(percent, true);
            binding.tvProgress.setText(getString(R.string.progress_fmt, percent));
        });
        viewModel.getMessage().observe(this, this::showMessage);
    }

    private ConversionOptions buildOptions() {
        boolean translate = binding.swTranslate.isChecked();
        return new ConversionOptions.Builder()
                .translate(translate)
                .bilingual(translate && binding.swBilingual.isChecked())
                .keepSpeakers(binding.swSpeakers.isChecked())
                .rtlMarks(translate && binding.swRtl.isChecked())
                .build();
    }

    private void updateOptionDependencies() {
        boolean translate = binding.swTranslate.isChecked();
        binding.swBilingual.setEnabled(translate);
        binding.swRtl.setEnabled(translate);
    }

    private void renderCues(List<SubtitleCue> list) {
        adapter.submit(list);
        boolean empty = list == null || list.isEmpty();
        binding.rvCues.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.tvCueCount.setText(empty ? "" : getString(R.string.cues_count, list.size()));
    }

    private void renderStatus(MainViewModel.Status status) {
        boolean working = status == MainViewModel.Status.WORKING;
        boolean hasFile = status != MainViewModel.Status.IDLE;
        binding.btnPick.setEnabled(!working);
        binding.btnConvert.setEnabled(hasFile && !working);
        binding.btnSave.setEnabled(status == MainViewModel.Status.DONE);
        binding.btnCancel.setVisibility(working ? View.VISIBLE : View.GONE);
        binding.progressGroup.setVisibility(working ? View.VISIBLE : View.GONE);
    }

    private void showMessage(Event<String> event) {
        String text = event != null ? event.getContentIfNotHandled() : null;
        if (text != null) {
            Snackbar.make(binding.getRoot(), text, Snackbar.LENGTH_LONG).show();
        }
    }
}
