package com.redmi.universalremote;

import android.content.Context;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ConsumerIrManager irManager;
    private boolean hasIr = false;

    // Pattern indices per button (cycles each tap)
    private final Map<String, Integer> patternIndex = new HashMap<>();

    // Auto-scan state
    private boolean scanning = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable scanRunnable;

    // Current device
    private String currentDevice = "tv";

    // UI references
    private TextView tvStatus;
    private TextView tvCode;
    private TextView tvProto;
    private LinearLayout layoutButtons;

    // Devices
    private static final String[] DEVICES = {"tv", "ac", "fan", "projector", "dvd", "audio"};
    private static final String[] DEVICE_LABELS = {"📺 TV", "❄ A/C", "🌀 Fan", "📽 Proj", "💿 DVD", "🔊 Audio"};

    // AC temp
    private int acTemp = 24;
    private TextView tvTemp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        irManager = (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);
        hasIr = irManager != null && irManager.hasIrEmitter();

        tvStatus = findViewById(R.id.tv_status);
        tvCode   = findViewById(R.id.tv_code);
        tvProto  = findViewById(R.id.tv_proto);
        layoutButtons = findViewById(R.id.layout_buttons);

        if (!hasIr) {
            tvStatus.setText("⚠ No IR blaster detected on this device");
        } else {
            tvStatus.setText("IR blaster ready — Redmi 12");
        }

        setupDeviceTabs();
        buildButtons("tv");
    }

    private void setupDeviceTabs() {
        LinearLayout tabRow = findViewById(R.id.tab_row);
        tabRow.removeAllViews();
        for (int i = 0; i < DEVICES.length; i++) {
            final String dev = DEVICES[i];
            Button btn = new Button(this);
            btn.setText(DEVICE_LABELS[i]);
            btn.setTextSize(11f);
            btn.setPadding(16, 8, 16, 8);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(4, 0, 4, 0);
            btn.setLayoutParams(lp);
            btn.setOnClickListener(v -> {
                currentDevice = dev;
                stopScan();
                buildButtons(dev);
                updateTabHighlight(tabRow, dev);
            });
            btn.setTag(dev);
            tabRow.addView(btn);
        }
    }

    private void updateTabHighlight(LinearLayout row, String active) {
        for (int i = 0; i < row.getChildCount(); i++) {
            View v = row.getChildAt(i);
            if (v instanceof Button) {
                Button b = (Button) v;
                if (active.equals(b.getTag())) {
                    b.setBackgroundColor(0xFF534AB7);
                    b.setTextColor(0xFFEEEDFE);
                } else {
                    b.setBackgroundColor(0xFFE8E8E8);
                    b.setTextColor(0xFF333333);
                }
            }
        }
    }

    private void buildButtons(String device) {
        layoutButtons.removeAllViews();
        tvTemp = null;

        // Auto-scan button
        Button scanBtn = makeButton("↺  Auto-scan all patterns", 0xFF534AB7, 0xFFFFFFFF);
        scanBtn.setOnClickListener(v -> {
            if (scanning) stopScan();
            else startAutoScan(device);
        });
        layoutButtons.addView(scanBtn);

        addDivider("tap buttons — cycles to next protocol on each press");

        switch (device) {
            case "tv":   buildTvButtons();   break;
            case "ac":   buildAcButtons();   break;
            case "fan":  buildFanButtons();  break;
            case "projector": buildProjButtons(); break;
            case "dvd":  buildDvdButtons();  break;
            case "audio": buildAudioButtons(); break;
        }
    }

    private void buildTvButtons() {
        addRow(
            makeIrBtn("⏻ Power", "tv_power", IrPatterns.TV_POWER_ALL, IrPatterns.TV_POWER_NAMES, IrPatterns.FREQ_38K),
            makeIrBtn("🔇 Mute",  "tv_mute",  IrPatterns.TV_MUTE_ALL,  new String[]{"NEC","Samsung"}, IrPatterns.FREQ_38K),
            makeIrBtn("↔ Input", "tv_input", IrPatterns.TV_INPUT_ALL, new String[]{"NEC"}, IrPatterns.FREQ_38K)
        );
        addRow(
            makeIrBtn("CH ▲",   "tv_chup",   IrPatterns.TV_CHUP_ALL,   new String[]{"NEC","Samsung"}, IrPatterns.FREQ_38K),
            makeIrBtn("OK",      "tv_ok",     IrPatterns.TV_OK_ALL,     new String[]{"NEC"}, IrPatterns.FREQ_38K),
            makeIrBtn("CH ▼",   "tv_chdown", IrPatterns.TV_CHDOWN_ALL, new String[]{"NEC"}, IrPatterns.FREQ_38K)
        );
        addRow(
            makeIrBtn("VOL ▲",  "tv_volup",   IrPatterns.TV_VOLUP_ALL,   new String[]{"NEC","Samsung","LG","Sony"}, IrPatterns.FREQ_38K),
            null,
            makeIrBtn("VOL ▼",  "tv_voldown", IrPatterns.TV_VOLDOWN_ALL, new String[]{"NEC","Samsung","LG"}, IrPatterns.FREQ_38K)
        );
    }

    private void buildAcButtons() {
        // Temperature display
        LinearLayout tempRow = new LinearLayout(this);
        tempRow.setOrientation(LinearLayout.HORIZONTAL);
        tempRow.setGravity(android.view.Gravity.CENTER);
        tempRow.setPadding(0, 12, 0, 12);

        Button btnMinus = makeButton("  −  ", 0xFFE0E0E0, 0xFF333333);
        tvTemp = new TextView(this);
        tvTemp.setText(acTemp + "°C");
        tvTemp.setTextSize(26f);
        tvTemp.setTextColor(0xFF222222);
        tvTemp.setPadding(32, 0, 32, 0);
        tvTemp.setGravity(android.view.Gravity.CENTER);
        Button btnPlus = makeButton("  +  ", 0xFFE0E0E0, 0xFF333333);

        btnMinus.setOnClickListener(v -> {
            acTemp = Math.max(16, acTemp - 1);
            tvTemp.setText(acTemp + "°C");
            fireIr("ac_tmpdown", IrPatterns.AC_TMPDOWN_ALL, new String[]{"Daikin/NEC"}, IrPatterns.FREQ_38K);
        });
        btnPlus.setOnClickListener(v -> {
            acTemp = Math.min(30, acTemp + 1);
            tvTemp.setText(acTemp + "°C");
            fireIr("ac_tmpup", IrPatterns.AC_TMPUP_ALL, new String[]{"Daikin/NEC"}, IrPatterns.FREQ_38K);
        });

        tempRow.addView(btnMinus);
        tempRow.addView(tvTemp);
        tempRow.addView(btnPlus);
        layoutButtons.addView(tempRow);

        addRow(
            makeIrBtn("⏻ Power", "ac_power", IrPatterns.AC_POWER_ALL, IrPatterns.AC_POWER_NAMES, IrPatterns.FREQ_38K),
            makeIrBtn("❄ Cool",  "ac_cool",  IrPatterns.AC_COOL_ALL,  new String[]{"Daikin"}, IrPatterns.FREQ_38K),
            makeIrBtn("☀ Heat",  "ac_heat",  IrPatterns.AC_HEAT_ALL,  new String[]{"Daikin"}, IrPatterns.FREQ_38K)
        );
        addRow(
            makeIrBtn("💧 Dry",   "ac_cool",  IrPatterns.AC_COOL_ALL,  new String[]{"Daikin"}, IrPatterns.FREQ_38K),
            makeIrBtn("Fan",      "ac_cool",  IrPatterns.AC_COOL_ALL,  new String[]{"Daikin"}, IrPatterns.FREQ_38K),
            makeIrBtn("↕ Swing",  "ac_cool",  IrPatterns.AC_COOL_ALL,  new String[]{"Daikin"}, IrPatterns.FREQ_38K)
        );
    }

    private void buildFanButtons() {
        addRow(
            makeIrBtn("⏻ Power",  "fan_power", IrPatterns.FAN_POWER_ALL, IrPatterns.FAN_POWER_NAMES, IrPatterns.FREQ_38K),
            makeIrBtn("💡 Light", "fan_light", IrPatterns.FAN_SPD1_ALL, new String[]{"NEC"}, IrPatterns.FREQ_38K),
            makeIrBtn("↺ Reverse","fan_rev",   IrPatterns.FAN_SPD1_ALL, new String[]{"NEC"}, IrPatterns.FREQ_38K)
        );
        addRow(
            makeIrBtn("Low",   "fan_spd1", IrPatterns.FAN_SPD1_ALL, new String[]{"NEC"}, IrPatterns.FREQ_38K),
            makeIrBtn("Med",   "fan_spd2", IrPatterns.FAN_SPD2_ALL, new String[]{"NEC"}, IrPatterns.FREQ_38K),
            makeIrBtn("High",  "fan_spd3", IrPatterns.FAN_SPD3_ALL, new String[]{"NEC"}, IrPatterns.FREQ_38K)
        );
    }

    private void buildProjButtons() {
        addRow(
            makeIrBtn("⏻ Power",  "proj_power",  IrPatterns.PROJ_POWER_ALL, IrPatterns.PROJ_POWER_NAMES, IrPatterns.FREQ_38K),
            makeIrBtn("↔ Source", "proj_source", IrPatterns.PROJ_POWER_ALL, IrPatterns.PROJ_POWER_NAMES, IrPatterns.FREQ_38K),
            makeIrBtn("☰ Menu",   "proj_menu",   IrPatterns.PROJ_POWER_ALL, IrPatterns.PROJ_POWER_NAMES, IrPatterns.FREQ_38K)
        );
        addRow(
            makeIrBtn("VOL ▲",   "proj_volup",   IrPatterns.PROJ_POWER_ALL, IrPatterns.PROJ_POWER_NAMES, IrPatterns.FREQ_38K),
            makeIrBtn("OK",       "proj_ok",      IrPatterns.PROJ_POWER_ALL, IrPatterns.PROJ_POWER_NAMES, IrPatterns.FREQ_38K),
            makeIrBtn("VOL ▼",   "proj_voldown", IrPatterns.PROJ_POWER_ALL, IrPatterns.PROJ_POWER_NAMES, IrPatterns.FREQ_38K)
        );
        addRow(
            makeIrBtn("⊕ Zoom",  "proj_zoom",  IrPatterns.PROJ_POWER_ALL, IrPatterns.PROJ_POWER_NAMES, IrPatterns.FREQ_38K),
            makeIrBtn("⬛ Blank","proj_blank", IrPatterns.PROJ_POWER_ALL, IrPatterns.PROJ_POWER_NAMES, IrPatterns.FREQ_38K),
            null
        );
    }

    private void buildDvdButtons() {
        addRow(
            makeIrBtn("⏻ Power", "dvd_power", IrPatterns.DVD_POWER_ALL, IrPatterns.DVD_POWER_NAMES, IrPatterns.FREQ_38K),
            makeIrBtn("⏏ Eject", "dvd_eject", IrPatterns.DVD_POWER_ALL, IrPatterns.DVD_POWER_NAMES, IrPatterns.FREQ_38K),
            makeIrBtn("☰ Menu",  "dvd_menu",  IrPatterns.DVD_POWER_ALL, IrPatterns.DVD_POWER_NAMES, IrPatterns.FREQ_38K)
        );
        addRow(
            makeIrBtn("⏮ Prev",  "dvd_prev",  IrPatterns.DVD_POWER_ALL, IrPatterns.DVD_POWER_NAMES, IrPatterns.FREQ_38K),
            makeIrBtn("▶ Play",  "dvd_play",  IrPatterns.DVD_PLAY_ALL,  IrPatterns.DVD_POWER_NAMES, IrPatterns.FREQ_38K),
            makeIrBtn("⏭ Next",  "dvd_next",  IrPatterns.DVD_POWER_ALL, IrPatterns.DVD_POWER_NAMES, IrPatterns.FREQ_38K)
        );
        addRow(
            makeIrBtn("◀◀ RWD", "dvd_rwd",   IrPatterns.DVD_POWER_ALL,  IrPatterns.DVD_POWER_NAMES, IrPatterns.FREQ_38K),
            makeIrBtn("⏸ Pause","dvd_pause", IrPatterns.DVD_PAUSE_ALL,  IrPatterns.DVD_POWER_NAMES, IrPatterns.FREQ_38K),
            makeIrBtn("▶▶ FWD", "dvd_fwd",   IrPatterns.DVD_POWER_ALL,  IrPatterns.DVD_POWER_NAMES, IrPatterns.FREQ_38K)
        );
        addRow(
            makeIrBtn("⏹ Stop","dvd_stop", IrPatterns.DVD_STOP_ALL, IrPatterns.DVD_POWER_NAMES, IrPatterns.FREQ_38K),
            null, null
        );
    }

    private void buildAudioButtons() {
        addRow(
            makeIrBtn("⏻ Power",  "audio_power",   IrPatterns.AUDIO_POWER_ALL, IrPatterns.AUDIO_POWER_NAMES, IrPatterns.FREQ_38K),
            makeIrBtn("🔇 Mute",  "audio_mute",    IrPatterns.AUDIO_POWER_ALL, IrPatterns.AUDIO_POWER_NAMES, IrPatterns.FREQ_38K),
            makeIrBtn("↔ Input",  "audio_input",   IrPatterns.AUDIO_POWER_ALL, IrPatterns.AUDIO_POWER_NAMES, IrPatterns.FREQ_38K)
        );
        addRow(
            makeIrBtn("VOL ▲",    "audio_volup",   IrPatterns.AUDIO_VOLUP_ALL, IrPatterns.AUDIO_POWER_NAMES, IrPatterns.FREQ_38K),
            makeIrBtn("▶ Play",   "audio_play",    IrPatterns.AUDIO_POWER_ALL, IrPatterns.AUDIO_POWER_NAMES, IrPatterns.FREQ_38K),
            makeIrBtn("⏭ Skip",   "audio_skip",    IrPatterns.AUDIO_POWER_ALL, IrPatterns.AUDIO_POWER_NAMES, IrPatterns.FREQ_38K)
        );
        addRow(
            makeIrBtn("VOL ▼",    "audio_voldown", IrPatterns.AUDIO_VOLDOWN_ALL, IrPatterns.AUDIO_POWER_NAMES, IrPatterns.FREQ_38K),
            makeIrBtn("Bass+",    "audio_bass",    IrPatterns.AUDIO_POWER_ALL, IrPatterns.AUDIO_POWER_NAMES, IrPatterns.FREQ_38K),
            makeIrBtn("Treble+",  "audio_treble",  IrPatterns.AUDIO_POWER_ALL, IrPatterns.AUDIO_POWER_NAMES, IrPatterns.FREQ_38K)
        );
    }

    // ─── IR firing ───────────────────────────────────────────────────────────

    private void fireIr(String key, int[][] patterns, String[] names, int freq) {
        int idx = patternIndex.containsKey(key) ? patternIndex.get(key) : 0;
        int[] pattern = patterns[idx % patterns.length];
        String name = names[idx % names.length];

        if (hasIr) {
            try {
                irManager.transmit(freq, pattern);
            } catch (Exception e) {
                Toast.makeText(this, "IR error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        // Update UI
        StringBuilder hex = new StringBuilder("0x");
        for (int v : pattern) hex.append(Integer.toHexString(v & 0xFFFF).toUpperCase());
        tvCode.setText(hex.length() > 20 ? hex.substring(0, 20) + "…" : hex.toString());
        tvProto.setText(name + " · " + freq/1000 + "kHz · " + pattern.length/2 + "-pulse frame");
        tvStatus.setText("✓ Sent " + name + " pattern");

        // Advance index
        patternIndex.put(key, (idx + 1) % patterns.length);
    }

    // ─── Auto-scan ───────────────────────────────────────────────────────────

    private void startAutoScan(String device) {
        scanning = true;
        Button scanBtn = (Button) layoutButtons.getChildAt(0);
        scanBtn.setText("⏹ Stop scan");

        int[][] patterns;
        String[] names;
        String key;
        int freq = IrPatterns.FREQ_38K;

        switch (device) {
            case "ac":        patterns = IrPatterns.AC_POWER_ALL;   names = IrPatterns.AC_POWER_NAMES;   key = "ac_power";   break;
            case "fan":       patterns = IrPatterns.FAN_POWER_ALL;  names = IrPatterns.FAN_POWER_NAMES;  key = "fan_power";  break;
            case "projector": patterns = IrPatterns.PROJ_POWER_ALL; names = IrPatterns.PROJ_POWER_NAMES; key = "proj_power"; break;
            case "dvd":       patterns = IrPatterns.DVD_POWER_ALL;  names = IrPatterns.DVD_POWER_NAMES;  key = "dvd_power";  break;
            case "audio":     patterns = IrPatterns.AUDIO_POWER_ALL;names = IrPatterns.AUDIO_POWER_NAMES;key = "audio_power";break;
            default:          patterns = IrPatterns.TV_POWER_ALL;   names = IrPatterns.TV_POWER_NAMES;   key = "tv_power";   break;
        }

        final int[] i = {0};
        final int[][] fPatterns = patterns;
        final String[] fNames = names;
        final int fFreq = freq;

        tvStatus.setText("Auto-scanning… watch your device");

        scanRunnable = new Runnable() {
            @Override
            public void run() {
                if (!scanning || i[0] >= fPatterns.length) {
                    stopScan();
                    tvStatus.setText(i[0] >= fPatterns.length ? "Scan complete — " + fPatterns.length + " patterns sent" : "Scan stopped");
                    return;
                }
                int[] pattern = fPatterns[i[0]];
                String name   = fNames[i[0] % fNames.length];
                if (hasIr) {
                    try { irManager.transmit(fFreq, pattern); } catch (Exception ignored) {}
                }
                tvStatus.setText("Scanning [" + (i[0]+1) + "/" + fPatterns.length + "] " + name);
                tvCode.setText(name);
                tvProto.setText(fFreq/1000 + "kHz · pattern " + (i[0]+1));
                i[0]++;
                handler.postDelayed(this, 600);
            }
        };
        handler.post(scanRunnable);
    }

    private void stopScan() {
        scanning = false;
        if (scanRunnable != null) handler.removeCallbacks(scanRunnable);
        View v = layoutButtons.getChildAt(0);
        if (v instanceof Button) ((Button) v).setText("↺  Auto-scan all patterns");
    }

    // ─── Button factories ────────────────────────────────────────────────────

    private Button makeIrBtn(String label, String key, int[][] patterns, String[] names, int freq) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextSize(13f);
        btn.setBackgroundColor(0xFFF0F0F0);
        btn.setTextColor(0xFF222222);
        btn.setPadding(4, 4, 4, 4);
        btn.setOnClickListener(v -> fireIr(key, patterns, names, freq));
        return btn;
    }

    private Button makeButton(String label, int bg, int fg) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextSize(14f);
        btn.setBackgroundColor(bg);
        btn.setTextColor(fg);
        return btn;
    }

    private void addRow(Button a, Button b, Button c) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 6, 0, 0);
        row.setLayoutParams(rowLp);

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, 120, 1f);
        btnLp.setMargins(4, 0, 4, 0);

        if (a != null) { a.setLayoutParams(btnLp); row.addView(a); }
        else { View spacer = new View(this); spacer.setLayoutParams(btnLp); row.addView(spacer); }

        if (b != null) { b.setLayoutParams(new LinearLayout.LayoutParams(btnLp)); row.addView(b); }
        else { View spacer = new View(this); spacer.setLayoutParams(new LinearLayout.LayoutParams(btnLp)); row.addView(spacer); }

        if (c != null) { c.setLayoutParams(new LinearLayout.LayoutParams(btnLp)); row.addView(c); }
        else { View spacer = new View(this); spacer.setLayoutParams(new LinearLayout.LayoutParams(btnLp)); row.addView(spacer); }

        layoutButtons.addView(row);
    }

    private void addDivider(String hint) {
        TextView tv = new TextView(this);
        tv.setText(hint);
        tv.setTextSize(11f);
        tv.setTextColor(0xFF888888);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(0, 8, 0, 4);
        layoutButtons.addView(tv);
    }
}
