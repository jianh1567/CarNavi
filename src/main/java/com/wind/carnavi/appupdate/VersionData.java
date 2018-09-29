package com.wind.carnavi.appupdate;

import java.io.Serializable;

public class VersionData implements Serializable {
    public UpdateInfo updateinfo;
    public String updateTitle;
    public boolean status;

    public class UpdateInfo{
        public String apkVersionName;
        public String apkName;
        public String apkPackage;
        public String createTime;
        public int apkVersionCode;
        public String updateTime;
        public int apkId;
        public String downAddress;
        public int strategy;
        public String apkContent;
        public String isUsed;
        public String checkCode;
    }

}
