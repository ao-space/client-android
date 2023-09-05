package xyz.eulix.space.ui.mine.privacy;

import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.presenter.EulixPrivacyPresenter;
import xyz.eulix.space.util.StatusBarUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/12/13 17:08
 */
public class EulixPrivacyActivity extends AbsActivity<EulixPrivacyPresenter.IEulixPrivacy, EulixPrivacyPresenter> implements EulixPrivacyPresenter.IEulixPrivacy
        , View.OnClickListener {
    private ImageButton back;
    private TextView title;
    private LinearLayout permissionManageContainer;

    @Override
    public void initView() {
        setContentView(R.layout.activity_eulix_privacy);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        permissionManageContainer = findViewById(R.id.permission_manage_container);
    }

    @Override
    public void initData() {

    }

    @Override
    public void initViewData() {
        title.setText(R.string.privacy);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        permissionManageContainer.setOnClickListener(this);
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @NotNull
    @Override
    public EulixPrivacyPresenter createPresenter() {
        return new EulixPrivacyPresenter();
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    finish();
                    break;
                case R.id.permission_manage_container:
                    Intent permissionManageIntent = new Intent(EulixPrivacyActivity.this, EulixPermissionManagerActivity.class);
                    startActivity(permissionManageIntent);
                    break;
                default:
                    break;
            }
        }
    }
}
