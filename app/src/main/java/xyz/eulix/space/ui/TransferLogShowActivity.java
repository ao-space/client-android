package xyz.eulix.space.ui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import xyz.eulix.space.R;
import xyz.eulix.space.manager.TransferTaskManager;
import xyz.eulix.space.util.Logger;

public class TransferLogShowActivity extends AppCompatActivity {

    private TextView tvLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_log_show);

        tvLog = findViewById(R.id.tv_log);
        initViewData();

    }

    private void initViewData() {
        String logStr = TransferTaskManager.getInstance().printAllTransferLogs();
        Logger.d("TransferLog", logStr);
        tvLog.setText(logStr);
    }
}