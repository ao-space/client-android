/*
 * Copyright (c) 2022 Institute of Software, Chinese Academy of Sciences (ISCAS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.eulix.space.bean;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/10/4 21:57
 */
public class ApplicationLockInfo {
    protected boolean isFingerprintUnlock;
    protected boolean isFaceUnlock;

    public static boolean compare(ApplicationLockInfo applicationLockInfo1, ApplicationLockInfo applicationLockInfo2) {
        boolean isEqual = false;
        if (applicationLockInfo1 == null && applicationLockInfo2 == null) {
            isEqual = true;
        } else if (applicationLockInfo1 != null && applicationLockInfo2 != null) {
            boolean isFingerprintUnlock1 = applicationLockInfo1.isFingerprintUnlock;
            boolean isFaceUnlock1 = applicationLockInfo1.isFaceUnlock;
            boolean isFingerprintUnlock2 = applicationLockInfo2.isFingerprintUnlock;
            boolean isFaceUnlock2 = applicationLockInfo2.isFaceUnlock;
            if (isFingerprintUnlock1 == isFingerprintUnlock2 && isFaceUnlock1 == isFaceUnlock2) {
                isEqual = true;
            }
        }
        return isEqual;
    }

    public boolean isFingerprintUnlock() {
        return isFingerprintUnlock;
    }

    public void setFingerprintUnlock(boolean fingerprintUnlock) {
        isFingerprintUnlock = fingerprintUnlock;
    }

    public boolean isFaceUnlock() {
        return isFaceUnlock;
    }

    public void setFaceUnlock(boolean faceUnlock) {
        isFaceUnlock = faceUnlock;
    }

    @Override
    public String toString() {
        return "ApplicationLockInfo{" +
                "isFingerprintUnlock=" + isFingerprintUnlock +
                ", isFaceUnlock=" + isFaceUnlock +
                '}';
    }
}
