package xyz.eulix.space.did.presenter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.did.DIDUtils;
import xyz.eulix.space.did.bean.DIDDocument;
import xyz.eulix.space.did.bean.DIDReserveBean;
import xyz.eulix.space.did.bean.VerificationMethod;
import xyz.eulix.space.util.StringUtil;

public class CredentialInformationPresenter extends AbsPresenter<CredentialInformationPresenter.ICredentialInformation> {
    public interface ICredentialInformation extends IBaseView {}

    public List<VerificationMethod> getVerificationMethods() {
        List<VerificationMethod> verificationMethodList = null;
        DIDDocument didDocument = EulixSpaceDBUtil.getActiveDIDDocument(context);
        if (didDocument != null) {
            verificationMethodList = didDocument.getVerificationMethods();
        }
        return verificationMethodList;
    }

    public UserInfo getGranterUserInfo() {
        return EulixSpaceDBUtil.getActiveGranterUserInfo(context);
    }

    public String getPasswordEncryptPrivateKey() {
        String passwordEncryptPrivateKey = null;
        DIDReserveBean didReserveBean = EulixSpaceDBUtil.getActiveDIDReserveBean(context);
        if (didReserveBean != null) {
            passwordEncryptPrivateKey = didReserveBean.getPasswordEncryptedPriKeyBytes();
        }
        return passwordEncryptPrivateKey;
    }
}
