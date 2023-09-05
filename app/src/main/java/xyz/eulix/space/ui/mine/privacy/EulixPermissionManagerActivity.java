package xyz.eulix.space.ui.mine.privacy;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.mine.privacy.EulixPermissionAdapter;
import xyz.eulix.space.presenter.EulixPermissionManagerPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.PreferenceUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.SystemUtil;
import xyz.eulix.space.view.BottomDialog;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/12/13 18:32
 */
public class EulixPermissionManagerActivity extends AbsActivity<EulixPermissionManagerPresenter.IEulixPermissionManager, EulixPermissionManagerPresenter> implements EulixPermissionManagerPresenter.IEulixPermissionManager
        , View.OnClickListener, EulixPermissionAdapter.OnItemClickListener {
    private ImageButton back;
    private TextView title;
    private RecyclerView permissionList;
    private TextView goToSystemSettings;
    private Dialog eulixPermissionDialog;
    private ImageButton eulixPermissionDialogExit;
    private TextView eulixPermissionDialogTitle;
    private TextView eulixPermissionDialogContent;
    private LinearLayout eulixPermissionDialogPermissionContainer;
    private TextView eulixPermissionDialogPermissionHint;
    private ImageView eulixPermissionDialogPermissionIndicator;
    private String mPermissionGroup;
    private List<String> mPermissionGroupList;
    private EulixPermissionAdapter mAdapter;
    private boolean forResult;

    @Override
    public void initView() {
        setContentView(R.layout.activity_eulix_permission_manager);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        permissionList = findViewById(R.id.permission_list);
        goToSystemSettings = findViewById(R.id.go_to_system_settings);

        View eulixPermissionDialogView = LayoutInflater.from(this).inflate(R.layout.dialog_eulix_permission, null);
        eulixPermissionDialogExit = eulixPermissionDialogView.findViewById(R.id.dialog_exit);
        eulixPermissionDialogTitle = eulixPermissionDialogView.findViewById(R.id.dialog_title);
        eulixPermissionDialogContent = eulixPermissionDialogView.findViewById(R.id.dialog_content);
        eulixPermissionDialogPermissionContainer = eulixPermissionDialogView.findViewById(R.id.permission_container);
        eulixPermissionDialogPermissionHint = eulixPermissionDialogView.findViewById(R.id.permission_hint);
        eulixPermissionDialogPermissionIndicator = eulixPermissionDialogView.findViewById(R.id.permission_indicator);
        eulixPermissionDialog = new BottomDialog(this);
        eulixPermissionDialog.setCancelable(false);
        eulixPermissionDialog.setContentView(eulixPermissionDialogView);
    }

    @Override
    public void initData() {
        handleIntent(getIntent());
        mPermissionGroupList = new ArrayList<>();
    }

    @Override
    public void initViewData() {
        title.setText(R.string.android_permission_manager);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        goToSystemSettings.setOnClickListener(this);
        eulixPermissionDialogExit.setOnClickListener(v -> {
            mPermissionGroup = null;
            dismissPermissionDialog();
        });
        generateDataList();
        mAdapter = new EulixPermissionAdapter(this, mPermissionGroupList);
        mAdapter.setOnItemClickListener(this);
        permissionList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        permissionList.addItemDecoration(new EulixPermissionAdapter.ItemDecoration(RecyclerView.VERTICAL
                , Math.round(getResources().getDimension(R.dimen.dp_1)), getResources().getColor(R.color.white_fff7f7f9)));
        permissionList.setAdapter(mAdapter);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            intent = getIntent();
        }
        if (intent != null) {
            forResult = intent.getBooleanExtra(ConstantField.FOR_RESULT, false);
        }
    }

    private void generateDataList() {
        if (mPermissionGroupList == null) {
            mPermissionGroupList = new ArrayList<>();
        } else {
            mPermissionGroupList.clear();
        }
        mPermissionGroupList.add(Manifest.permission_group.STORAGE);
        mPermissionGroupList.add(Manifest.permission_group.CAMERA);
    }

    private void prepareEulixPermissionInformation() {
        boolean isHandle = false;
        String title = getString(R.string.unknown);
        String content = getString(R.string.unknown);
        String hint = getString(R.string.unknown);
        List<String> requestPermissionList = new ArrayList<>();
        int permissionRequestCode = 0;
        if (mPermissionGroup != null) {
            switch (mPermissionGroup) {
                case Manifest.permission_group.STORAGE:
                    isHandle = true;
                    title = getString(R.string.storage);
                    content = getString(R.string.storage_content);
                    hint = getString(R.string.storage_hint);
                    requestPermissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                    requestPermissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    permissionRequestCode = ConstantField.RequestCode.EXTERNAL_STORAGE_PERMISSION;
                    break;
                case Manifest.permission_group.LOCATION:
                    isHandle = true;
                    title = getString(R.string.location);
                    content = getString(R.string.location_content);
                    hint = getString(R.string.location_hint);
                    requestPermissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                    requestPermissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
                    permissionRequestCode = ConstantField.RequestCode.ACCESS_LOCATION_PERMISSION;
                    break;
                case Manifest.permission_group.CAMERA:
                    isHandle = true;
                    title = getString(R.string.camera);
                    content = getString(R.string.camera_content);
                    hint = getString(R.string.camera_hint);
                    requestPermissionList.add(Manifest.permission.CAMERA);
                    permissionRequestCode = ConstantField.RequestCode.CAMERA_PERMISSION;
                    break;
                default:
                    break;
            }
        }
        if (isHandle) {
            boolean isCheck = false;
            int permissionGrantedStatus = 1;
            for (String permission : requestPermissionList) {
                if (permission != null) {
                    isCheck = true;
                    permissionGrantedStatus = (permissionGrantedStatus * (SystemUtil.checkPermission(getApplicationContext(), permission) ? 1 : 0));
                }
            }
            if (!isCheck) {
                permissionGrantedStatus = 0;
            }
            final int finalPermissionGrantedStatus = permissionGrantedStatus;
            final int finalPermissionRequestCode = permissionRequestCode;
            eulixPermissionDialogTitle.setText(StringUtil.nullToEmpty(title));
            eulixPermissionDialogContent.setText(StringUtil.nullToEmpty(content));
            eulixPermissionDialogPermissionContainer.setOnClickListener(v -> {
                dismissPermissionDialog();
                if (finalPermissionGrantedStatus == 0) {
                    SystemUtil.requestPermission(EulixPermissionManagerActivity.this
                            , requestPermissionList.toArray(new String[0]), finalPermissionRequestCode);
                } else {
                    goToApplicationDetailsSettings();
                }
            });
            eulixPermissionDialogPermissionHint.setText(StringUtil.nullToEmpty(hint));
            eulixPermissionDialogPermissionIndicator.setImageResource(permissionGrantedStatus != 0
                    ? R.drawable.icon_checkbox_open : R.drawable.icon_checkbox_close);
        } else {
            eulixPermissionDialogTitle.setText("");
            eulixPermissionDialogContent.setText("");
            eulixPermissionDialogPermissionContainer.setOnClickListener(null);
            eulixPermissionDialogPermissionHint.setText("");
            eulixPermissionDialogPermissionIndicator.setImageDrawable(null);
        }
    }

    private void showEulixPermissionDialog() {
        prepareEulixPermissionInformation();
        if (eulixPermissionDialog != null && !eulixPermissionDialog.isShowing()) {
            eulixPermissionDialog.show();
            Window window = eulixPermissionDialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissPermissionDialog() {
        if (eulixPermissionDialog != null && eulixPermissionDialog.isShowing()) {
            eulixPermissionDialog.dismiss();
        }
        prepareEulixPermissionInformation();
    }

    private void goToApplicationDetailsSettings() {
        SystemUtil.goToApplicationDetailsSettings(EulixPermissionManagerActivity.this);
    }

    private void handleResult(boolean isOk) {
        Intent intent = new Intent();
        setResult(isOk ? Activity.RESULT_OK : Activity.RESULT_CANCELED, intent);
        finish();
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @NotNull
    @Override
    public EulixPermissionManagerPresenter createPresenter() {
        return new EulixPermissionManagerPresenter();
    }

    @Override
    public void onBackPressed() {
        if (forResult) {
            handleResult(false);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && forResult) {
            handleResult(false);
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int length = Math.min(permissions.length, grantResults.length);
        List<String> denyPermissions = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            String permission = permissions[i];
            boolean isGrant = (grantResults[i] == PackageManager.PERMISSION_GRANTED);
            if (permission != null) {
                PreferenceUtil.saveBaseKeyBoolean(EulixSpaceApplication.getContext(), permission, isGrant);
            }
            if (permission != null && !isGrant) {
                switch (requestCode) {
                    case ConstantField.RequestCode.EXTERNAL_STORAGE_PERMISSION:
                        switch (permission) {
                            case Manifest.permission.READ_EXTERNAL_STORAGE:
                            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                                denyPermissions.add(permission);
                                break;
                            default:
                                break;
                        }
                        break;
                    case ConstantField.RequestCode.ACCESS_LOCATION_PERMISSION:
                        switch (permission) {
                            case Manifest.permission.ACCESS_COARSE_LOCATION:
                            case Manifest.permission.ACCESS_FINE_LOCATION:
                                denyPermissions.add(permission);
                                break;
                            default:
                                break;
                        }
                        break;
                    case ConstantField.RequestCode.CAMERA_PERMISSION:
                        switch (permission) {
                            case Manifest.permission.CAMERA:
                                denyPermissions.add(permission);
                                break;
                            default:
                                break;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        if (!denyPermissions.isEmpty()) {
            boolean isRationale = false;
            for (String denyPermission : denyPermissions) {
                if (denyPermission != null && !ActivityCompat.shouldShowRequestPermissionRationale(this, denyPermission)) {
                    isRationale = true;
                    break;
                }
            }
            if (isRationale) {
                goToApplicationDetailsSettings();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    if (forResult) {
                        handleResult(true);
                    } else {
                        finish();
                    }
                    break;
                case R.id.go_to_system_settings:
                    goToApplicationDetailsSettings();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        if (mPermissionGroupList != null && position >= 0 && mPermissionGroupList.size() > position) {
            String permission = mPermissionGroupList.get(position);
            if (permission != null) {
                mPermissionGroup = permission;
                showEulixPermissionDialog();
            }
        }
    }
}
