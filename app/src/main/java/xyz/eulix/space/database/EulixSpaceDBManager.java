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

package xyz.eulix.space.database;

import android.content.ContentResolver;
import android.net.Uri;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/7/27 16:04
 */
public class EulixSpaceDBManager {
    public static final String AUTHORITY = "space.ao.client.database";
    public static final String BASE_DIR_TYPE = "vnd.android.cursor.dir/vnd.eulixspace";
    public static final String BASE_ITEM_TYPE = "vnd.android.cursor.dir/vnd.eulixspace";

    public static final String BOX_TABLE_NAME = "box";
    public static final String BOX_CONTENT_TYPE = BASE_DIR_TYPE + "." + BOX_TABLE_NAME;
    public static final String BOX_CONTENT_ITEM_TYPE = BASE_ITEM_TYPE + "." + BOX_TABLE_NAME;

    public static final String PUSH_TABLE_NAME = "push";
    public static final String PUSH_CONTENT_TYPE = BASE_DIR_TYPE + "." + PUSH_TABLE_NAME;
    public static final String PUSH_CONTENT_ITEM_TYPE = BASE_ITEM_TYPE + "." + PUSH_TABLE_NAME;

    public static final String DID_TABLE_NAME = "did";
    public static final String DID_CONTENT_TYPE = BASE_DIR_TYPE + "." + DID_TABLE_NAME;
    public static final String DID_CONTENT_ITEM_TYPE = BASE_ITEM_TYPE + "." + DID_TABLE_NAME;

    public static final Uri BOX_URI = Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + AUTHORITY + "/" + BOX_TABLE_NAME);
    public static final Uri BOX_ID_URI_BASE = Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + AUTHORITY + "/" + BOX_TABLE_NAME + "/");
    public static final Uri PUSH_URI = Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + AUTHORITY + "/" + PUSH_TABLE_NAME);
    public static final Uri PUSH_ID_URI_BASE = Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + AUTHORITY + "/" + PUSH_TABLE_NAME + "/");
    public static final Uri DID_URI = Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + AUTHORITY + "/" + DID_TABLE_NAME);
    public static final Uri DID_ID_URI_BASE = Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + AUTHORITY + "/" + DID_TABLE_NAME + "/");

    public static final String FIELD_BOX_ID = "_id";
    // 盒子UUID
    public static final String FIELD_BOX_UUID = "uuid";
    // 盒子名字
    public static final String FIELD_BOX_NAME = "name";
    // 盒子信息，类EulixBoxInfo
    public static final String FIELD_BOX_INFO = "info";
    // 盒子RSA公钥（AES钥匙加解密）
    public static final String FIELD_BOX_PUBLIC_KEY = "publickey";
    // 盒子授权信息
    public static final String FIELD_BOX_AUTHORIZATION = "authorization";
    // 盒子注册信息
    public static final String FIELD_BOX_REGISTER = "register";
    // 盒子域名
    public static final String FIELD_BOX_DOMAIN = "domain";
    // 盒子绑定状态：1：绑定；0(后期是授权方aoId)：扫码；-1：邀请
    public static final String FIELD_BOX_BIND = "bind";
    // 盒子使用状态，见ConstantField.EulixDeviceStatus
    public static final String FIELD_BOX_STATUS = "status";
    // 盒子授权码更新时间，已绑定的盒子不用该功能，登录授权的设备超过一定使用时间失效
    public static final String FILED_BOX_UPDATE_TIME = "updatetime";
    // 盒子token信息，类EulixBoxToken
    public static final String FIELD_BOX_TOKEN = "token";
    // 盒子用户信息，Map<String, UserInfo>，键为client uuid
    public static final String FIELD_BOX_USER_INFO = "userinfo";
    // 盒子文件数据，Map<String, List<FileListItem>>，键为文件uuid
    public static final String FIELD_BOX_FILE_LIST = "filelist";
    // 盒子其它信息，类EulixBoxOtherInfo
    public static final String FIELD_BOX_OTHER_INFO = "otherinfo";


    public static final String FIELD_PUSH_ID = "_id";
    // 消息id
    public static final String FIELD_PUSH_MESSAGE_ID = "messageid";
    // 盒子uuid
    public static final String FIELD_PUSH_UUID = "boxuuid";
    // 盒子绑定状态
    public static final String FIELD_PUSH_BIND = "boxbind";
    // 消息类型
    public static final String FIELD_PUSH_TYPE = "type";
    // 消息优先级 1-8: 强提醒段，默认8；9-16：弱提醒段，默认16；17-24：软提醒段，默认24；25-32：虚提醒段，默认32
    public static final String FIELD_PUSH_PRIORITY = "priority";
    // 消息来源 0：本地消息；1：通知；2：长连接；3：消息接口中心；-1：消息中心缓存，该message id为{boxuuid}_{boxbind}
    public static final String FIELD_PUSH_SOURCE = "source";
    // 消息消费 1：存储；2：已经被展示；3：已经被跳过；4：已经被消费；-1：消息中心缓存
    public static final String FIELD_PUSH_CONSUME = "consume";
    // 消息标题
    public static final String FIELD_PUSH_TITLE = "title";
    // 消息内容
    public static final String FIELD_PUSH_CONTENT = "content";
    // 消息本身数据，通知是extra(Map)，长连接是JSON，消息中心缓存是List
    public static final String FIELD_PUSH_RAW_DATA = "rawdata";
    // 消息实际时间
    public static final String FIELD_PUSH_CREATE_TIME = "createtime";
    // 消息时间戳
    public static final String FIELD_PUSH_TIMESTAMP = "timestamp";
    // 保留字段，json格式，EulixPushReserveBean
    public static final String FIELD_PUSH_RESERVE = "reserve";


    public static final String FIELD_DID_ID = "_id";
    // 盒子UUID
    public static final String FIELD_DID_UUID = "diduuid";
    // 盒子绑定状态
    public static final String FIELD_DID_BIND = "didbind";
    // 空间标识符
    public static final String FIELD_DID_AO_ID = "didaoid";
    // didDoc原文，用于解析condition等多元数组
    public static final String FIELD_DID_DOC_ENCODE = "diddocencode";
    // didDoc文档，用于解析简单字符，存入的时候只放decode，取出的时候GSON化
    public static final String FIELD_DID_DOCUMENT = "diddocument";
    // did 凭证信息，DIDCredentialBean类
    public static final String FIELD_DID_CREDENTIAL = "didcredential";
    // 存入时间戳
    public static final String FIELD_DID_TIMESTAMP = "didtimestamp";
    // 保留字段，json格式，DIDReserveBean
    public static final String FIELD_DID_RESERVE = "didreserve";
}
