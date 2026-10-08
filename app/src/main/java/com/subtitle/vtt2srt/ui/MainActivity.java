package com.subtitle.vtt2srt.ui;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.subtitle.vtt2srt.R;
import com.subtitle.vtt2srt.databinding.ActivityMainBinding;
import com.subtitle.vtt2srt.databinding.DialogTranslationSettingsBinding;
import com.subtitle.vtt2srt.domain.model.ConversionOptions;
import com.subtitle.vtt2srt.domain.model.SubtitleCue;
import com.subtitle.vtt2srt.domain.model.SubtitleLanguage;
import com.subtitle.vtt2srt.domain.translate.AiTranslatorConfig;
import com.subtitle.vtt2srt.domain.translate.TranslationEngineType;
import com.subtitle.vtt2srt.util.Event;
import com.subtitle.vtt2srt.util.UI;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private final CueAdapter adapter = new CueAdapter();
    private String selectedTargetLang = "ar";

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            } else {
                finish();
            }
        }
    };
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
        setupWindowInsets();
        getOnBackPressedDispatcher().addCallback(onBackPressedCallback);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        binding.rvCues.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCues.setAdapter(adapter);

        List<SubtitleLanguage> languages = SubtitleLanguage.getSupportedLanguages();
        ArrayAdapter<SubtitleLanguage> langAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, languages);
        binding.actvTargetLanguage.setAdapter(langAdapter);
        if (!languages.isEmpty()) {
            binding.actvTargetLanguage.setText(languages.get(0).getDisplayName(), false);
            selectedTargetLang = languages.get(0).getCode();
            binding.swRtl.setChecked(languages.get(0).isRtl());
        }
        binding.actvTargetLanguage.setOnItemClickListener((parent, view, position, id) ->

        {
            SubtitleLanguage lang = languages.get(position);
            selectedTargetLang = lang.getCode();
            binding.swRtl.setChecked(lang.isRtl());
        });

        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetPreview);
        binding.layoutSheetHeader.setOnClickListener(v ->

        {
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        binding.btnPick.setOnClickListener(v -> pickLauncher.launch(new String[]{"*/*"}));
        binding.btnConvert.setOnClickListener(v -> viewModel.convert(buildOptions()));
        binding.btnCancel.setOnClickListener(v -> viewModel.cancel());
        binding.btnSave.setOnClickListener(v -> saveLauncher.launch(viewModel.getSuggestedFileName()));
        binding.swTranslate.setOnCheckedChangeListener((button, checked) -> updateOptionDependencies());

        updateOptionDependencies();


        binding.toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_settings) {
                showSettingsDialog();
                return true;
            } else if (id == R.id.action_about) {
                showAboutDialog();
                return true;
            }
            return false;
        });

        viewModel.getFileName().observe(this, name -> binding.tvFileName.setText(name));
        viewModel.getCues().observe(this, this::renderCues);
        viewModel.getStatus().observe(this, this::renderStatus);
        viewModel.getProgress().observe(this, percent -> {
            int p = percent != null ? percent : 0;
            if (p <= 0) {
                binding.progressBar.setIndeterminate(true);
            } else {
                if (binding.progressBar.isIndeterminate()) {
                    binding.progressBar.setIndeterminate(false);
                }
                binding.progressBar.setProgressCompat(p, true);
            }
            binding.tvProgress.setText(getString(R.string.progress_fmt, p));
        });
        viewModel.getMessage().observe(this, this::showMessage);
    }

    private void setupWindowInsets() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        UI.addSystemWindowInsetToPadding(binding.appbar, true, true, true, false);
    }


    private ConversionOptions buildOptions() {
        boolean translate = binding.swTranslate.isChecked();
        return new ConversionOptions.Builder()
                .translate(translate)
                .targetLang(selectedTargetLang)
                .bilingual(translate && binding.swBilingual.isChecked())
                .keepSpeakers(binding.swSpeakers.isChecked())
                .rtlMarks(translate && binding.swRtl.isChecked())
                .build();
    }

    private void updateOptionDependencies() {
        boolean translate = binding.swTranslate.isChecked();
        binding.actvTargetLanguage.setEnabled(translate);
        binding.tlTargetLanguage.setEnabled(translate);
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

    private void showSettingsDialog() {
        DialogTranslationSettingsBinding dialogBinding =
                DialogTranslationSettingsBinding.inflate(getLayoutInflater());

        AiTranslatorConfig currentConfig = viewModel.getAiConfigValue();

        String[] engineOptions = new String[]{
                getString(R.string.engine_free),
                getString(R.string.engine_gemini),
                getString(R.string.engine_deepseek),
                getString(R.string.engine_claude),
                getString(R.string.engine_custom_openai)
        };

        TranslationEngineType[] engineTypes = new TranslationEngineType[]{
                TranslationEngineType.FREE,
                TranslationEngineType.GEMINI,
                TranslationEngineType.DEEPSEEK,
                TranslationEngineType.CLAUDE,
                TranslationEngineType.CUSTOM_OPENAI
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, engineOptions);
        dialogBinding.actvEngine.setAdapter(adapter);

        int selectedIndex = 0;
        for (int i = 0; i < engineTypes.length; i++) {
            if (engineTypes[i] == currentConfig.getEngineType()) {
                selectedIndex = i;
                break;
            }
        }

        dialogBinding.actvEngine.setText(engineOptions[selectedIndex], false);
        dialogBinding.etApiKey.setText(currentConfig.getApiKey());
        dialogBinding.etModelName.setText(currentConfig.getModelName());
        dialogBinding.etBaseUrl.setText(currentConfig.getBaseUrl());
        dialogBinding.etSystemPrompt.setText(currentConfig.getCustomSystemPrompt());

        Runnable updateFieldsVisibility = () -> {
            String selectedText = dialogBinding.actvEngine.getText().toString();
            boolean isFree = selectedText.equals(engineOptions[0]);
            boolean isCustom = selectedText.equals(engineOptions[4]);

            dialogBinding.tlApiKey.setVisibility(isFree ? View.GONE : View.VISIBLE);
            dialogBinding.tlModelName.setVisibility(isFree ? View.GONE : View.VISIBLE);
            dialogBinding.tlBaseUrl.setVisibility(isCustom ? View.VISIBLE : View.GONE);
            dialogBinding.tlSystemPrompt.setVisibility(isFree ? View.GONE : View.VISIBLE);
        };

        updateFieldsVisibility.run();
        dialogBinding.actvEngine.setOnItemClickListener((parent, view, position, id) -> updateFieldsVisibility.run());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.btn_save, (dialog, which) -> {
                    String selectedText = dialogBinding.actvEngine.getText().toString();
                    TranslationEngineType selectedType = TranslationEngineType.FREE;
                    for (int i = 0; i < engineOptions.length; i++) {
                        if (engineOptions[i].equals(selectedText)) {
                            selectedType = engineTypes[i];
                            break;
                        }
                    }

                    AiTranslatorConfig newConfig = new AiTranslatorConfig.Builder()
                            .engineType(selectedType)
                            .apiKey(dialogBinding.etApiKey.getText() != null ? dialogBinding.etApiKey.getText().toString() : "")
                            .modelName(dialogBinding.etModelName.getText() != null ? dialogBinding.etModelName.getText().toString() : "")
                            .baseUrl(dialogBinding.etBaseUrl.getText() != null ? dialogBinding.etBaseUrl.getText().toString() : "")
                            .customSystemPrompt(dialogBinding.etSystemPrompt.getText() != null ? dialogBinding.etSystemPrompt.getText().toString() : "")
                            .build();

                    viewModel.saveAiConfig(newConfig);
                    Snackbar.make(binding.getRoot(), R.string.msg_saved, Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_app_icon)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_message)
                .setPositiveButton(R.string.btn_close, null)
                .show();
    }

    private void showMessage(Event<String> event) {
        String text = event != null ? event.getContentIfNotHandled() : null;
        if (text != null) {
            Snackbar.make(binding.getRoot(), text, Snackbar.LENGTH_LONG).show();
        }
    }
}
