package xyz.eulix.space.did.ui;

import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.did.DIDUtils;
import xyz.eulix.space.did.bean.CredentialInformationBean;
import xyz.eulix.space.did.bean.DIDDocument;
import xyz.eulix.space.did.bean.VerificationMethod;
import xyz.eulix.space.did.event.DIDDocumentResponseEvent;
import xyz.eulix.space.did.network.DIDDocumentResult;
import xyz.eulix.space.did.presenter.SpaceAccountPresenter;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.ViewUtils;

public class SpaceAccountActivity extends AbsActivity<SpaceAccountPresenter.ISpaceAccount, SpaceAccountPresenter> implements SpaceAccountPresenter.ISpaceAccount, View.OnClickListener {
    private TextView title;
    private ImageButton back;
    private TextView didContent;
    private LinearLayout aoSpaceServerCredentialContainer;
    private LinearLayout bindPhoneCredentialContainer;
    private RelativeLayout securityPasswordCredentialContainer;
    private TextView securityPasswordCredentialNotSetText;
    private String mRequestId;
    private boolean isActivityStop;

    @Override
    public void initView() {
        setContentView(R.layout.activity_space_account);
        title = findViewById(R.id.title);
        back = findViewById(R.id.back);
        didContent = findViewById(R.id.did_content);
        aoSpaceServerCredentialContainer = findViewById(R.id.ao_space_server_credential_container);
        bindPhoneCredentialContainer = findViewById(R.id.bind_phone_credential_container);
        securityPasswordCredentialContainer = findViewById(R.id.security_password_credential_container);
        securityPasswordCredentialNotSetText = findViewById(R.id.security_password_credential_not_set_text);
    }

    @Override
    public void initData() {
        isActivityStop = false;
    }

    @Override
    public void initViewData() {
        title.setText(R.string.space_account);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        aoSpaceServerCredentialContainer.setOnClickListener(this);
        bindPhoneCredentialContainer.setOnClickListener(this);
        securityPasswordCredentialContainer.setOnClickListener(this);
        //didContent.setOnLongClickListener(this);
        if (presenter != null) {
            updateDIDInformation(presenter.getDIDDocument());
        }
    }

    private void updateDIDInformation(DIDDocument didDocument) {
        boolean hasAOSpaceServerCredential = false;
        boolean hasBindPhoneServerCredential = false;
        boolean hasSecurityPasswordCredential = false;
        if (didDocument != null) {
            String id = didDocument.getId();
            if (id != null) {
                int idLength = id.length();
                int splitIndex = id.indexOf(":");
                if (splitIndex >= 0 && splitIndex < idLength) {
                    if ((splitIndex + 1) >= idLength) {
                        id = "";
                    } else {
                        id = id.substring((splitIndex + 1));
                    }
                }
                int fragmentIndex = id.lastIndexOf("#");
                if (fragmentIndex >= 0 && fragmentIndex < id.length()) {
                    id = id.substring(0, fragmentIndex);
                }
                int queryIndex = id.indexOf("?");
                if (queryIndex >= 0 && queryIndex < id.length()) {
                    id = id.substring(0, queryIndex);
                }
            }
            int didContentRawWidth = ViewUtils.getScreenWidth(this) - 2 * getResources().getDimensionPixelSize(R.dimen.dp_10);
            didContent.setGravity(Gravity.START);
            didContent.setText(FormatUtil.autoWrap(StringUtil.nullToEmpty(id)
                    , (didContentRawWidth - (didContent.getPaddingStart() + didContent.getPaddingEnd()))
                    , didContent.getPaint()));
            List<VerificationMethod> verificationMethods = didDocument.getVerificationMethods();
            if (verificationMethods != null && !verificationMethods.isEmpty()) {
                for (VerificationMethod verificationMethod : verificationMethods) {
                    if (verificationMethod != null) {
                        Map<String, String> query = DIDUtils.getDIDQueryMap(verificationMethod.getId());
                        if (query != null && query.containsKey(VerificationMethod.QUERY_CREDENTIAL_TYPE)) {
                            String credentialType = query.get(VerificationMethod.QUERY_CREDENTIAL_TYPE);
                            if (credentialType != null) {
                                switch (credentialType) {
                                    case VerificationMethod.CREDENTIAL_TYPE_DEVICE:
                                        hasAOSpaceServerCredential = true;
                                        break;
                                    case VerificationMethod.CREDENTIAL_TYPE_BINDER:
                                        hasBindPhoneServerCredential = true;
                                        break;
                                    case VerificationMethod.CREDENTIAL_TYPE_PASSWORD_ON_DEVICE:
                                    case VerificationMethod.CREDENTIAL_TYPE_PASSWORD_ON_BINDER:
                                        hasSecurityPasswordCredential = true;
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            didContent.setGravity(Gravity.CENTER_HORIZONTAL);
            didContent.setText(R.string.none);
        }
        aoSpaceServerCredentialContainer.setClickable(hasAOSpaceServerCredential);
        bindPhoneCredentialContainer.setClickable(hasBindPhoneServerCredential);
        securityPasswordCredentialContainer.setClickable(hasSecurityPasswordCredential);
        securityPasswordCredentialNotSetText.setVisibility((hasSecurityPasswordCredential ? View.GONE : View.VISIBLE));
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (presenter != null) {
            if (isActivityStop) {
                isActivityStop = false;
                updateDIDInformation(presenter.getDIDDocument());
            }
            mRequestId = UUID.randomUUID().toString();
            presenter.refreshDIDDocument(mRequestId);
        }
    }

    @Override
    protected void onStop() {
        isActivityStop = true;
        super.onStop();
    }

    @NotNull
    @Override
    public SpaceAccountPresenter createPresenter() {
        return new SpaceAccountPresenter();
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    finish();
                    break;
                case R.id.ao_space_server_credential_container:
                    CredentialInformationActivity.startThisActivity(SpaceAccountActivity.this, CredentialInformationBean.CREDENTIAL_TYPE_AO_SPACE_SERVER);
                    break;
                case R.id.bind_phone_credential_container:
                    CredentialInformationActivity.startThisActivity(SpaceAccountActivity.this, CredentialInformationBean.CREDENTIAL_TYPE_BIND_PHONE);
                    break;
                case R.id.security_password_credential_container:
                    CredentialInformationActivity.startThisActivity(SpaceAccountActivity.this, CredentialInformationBean.CREDENTIAL_TYPE_SECURITY_PASSWORD);
                    break;
                default:
                    break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DIDDocumentResponseEvent event) {
        if (event != null) {
            String requestUuid = event.getRequestUuid();
            if (requestUuid != null && requestUuid.equals(mRequestId)) {
                mRequestId = null;
            }
            DIDDocumentResult didDocumentResult = event.getDidDocumentResult();
            if (didDocumentResult != null) {
                updateDIDInformation(DIDUtils.parseDIDDoc(didDocumentResult.getDidDoc()));
            }
        }
    }
}
