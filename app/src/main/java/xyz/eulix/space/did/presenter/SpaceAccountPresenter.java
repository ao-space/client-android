package xyz.eulix.space.did.presenter;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.did.bean.DIDDocument;
import xyz.eulix.space.did.event.DIDDocumentRequestEvent;
import xyz.eulix.space.util.ClipboardUtil;
import xyz.eulix.space.util.EventBusUtil;

public class SpaceAccountPresenter extends AbsPresenter<SpaceAccountPresenter.ISpaceAccount> {
    public interface ISpaceAccount extends IBaseView {}

    public DIDDocument getDIDDocument() {
        return EulixSpaceDBUtil.getActiveDIDDocument(context);
    }

    public void refreshDIDDocument(String requestUuid) {
        EulixBoxBaseInfo eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context);
        if (eulixBoxBaseInfo != null) {
            EventBusUtil.post(new DIDDocumentRequestEvent(eulixBoxBaseInfo.getBoxUuid(), eulixBoxBaseInfo.getBoxBind(), requestUuid));
        }
    }

    public boolean copyDID(String did) {
        return ClipboardUtil.setClipData(context, did);
    }
}
