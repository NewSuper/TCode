package com.qx.imui.util;

import android.text.TextUtils;

import java.io.Serializable;
import java.math.BigDecimal;

public class UserResponse implements Serializable {
    private static UserResponse mUserResponse;
    private String userId = "";
    private String token = "";
    private String account = "";//手機號 賬號
    private String nickname = "";//昵稱
    private boolean payPassword;// "string,是否设置交易密码               //支付？
    //  private boolean isPayPwd;// "string,是否设置交易密码               //交易  已废弃
    private boolean payPwd;// "string,是否设置交易密码               //新加字段--是否设置交易密码
    private String userSn = "";
    private String inviteCode;

    private String password = "";
    private String countryCode = "";//區號
    private String img = "";//頭像
    private String nationality = "";//國家
    private String signature = "";//簽名
    private String gender = "";//性別
    private String rongToken = "";  //融云token
    private String authState = "";  //實名狀態 1审核通过 2审核中 0未提交
    private String picToken = "";  //人脸登录成功的token
    private boolean faceFlag = false;  //是否人脸信息
    private boolean imgFlag = false;  //实名第三张
    private String userSig = "";
    private boolean officialInviteBln = false;//是否为官方邀请人

    private String mobile;
    private String email;
    private String imToken = "";  //新版IM 登录成功的imToken

    public String getImToken() {
        return imToken;
    }

    public void setImToken(String imToken) {
        this.imToken = imToken;
    }

    /**
     * 清除数据
     */
    public void clearData() {
        userId = "";
        token = "";
        account = "";
        nickname = "";
        userSn = "";
        inviteCode = "";
        password = "";
        countryCode = "";
        rongToken = "";
        img = "";
        nationality = "";
        signature = "";
        gender = "";
        rongToken = "";
        authState = "";
        picToken = "";
        mobile = "";
        email = "";
        //  isPayPwd=false;
        payPwd = false;
        faceFlag = false;
        imgFlag = false;
        userSig = "";
        imToken = "";
    }

    public static UserResponse getInstance() {
        if (mUserResponse == null) {
            mUserResponse = new UserResponse();
        }
        return mUserResponse;
    }

    public String getPicToken() {
        return picToken;
    }

    public void setPicToken(String picToken) {
        this.picToken = picToken;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public boolean isPayPwd() {
        return payPwd;
    }

    public void setPayPwd(boolean payPwd) {
        this.payPwd = payPwd;
    }

    public String getUserSn() {
        try {
            if (!TextUtils.isEmpty(userSn)) {
                BigDecimal bigDecimal = new BigDecimal(userSn);
                userSn = String.valueOf(bigDecimal.intValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userSn;
    }

    public void setUserSn(String userSn) {
        this.userSn = userSn;
    }

    public boolean isPayPassword() {
        return payPassword;
    }

    public void setPayPassword(boolean payPassword) {
        this.payPassword = payPassword;
    }

    public boolean isOfficialInviteBln() {
        return officialInviteBln;
    }

    public void setOfficialInviteBln(boolean officialInviteBln) {
        this.officialInviteBln = officialInviteBln;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public String getRongToken() {
        return rongToken;
    }

    public void setRongToken(String rongToken) {
        this.rongToken = rongToken;
    }

    public String getImg() {
        return img;
    }

    public boolean isFaceFlag() {
        return faceFlag;
    }

    public void setFaceFlag(boolean faceFlag) {
        this.faceFlag = faceFlag;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getGender() {
        try {
            if (!TextUtils.isEmpty(gender)) {
                BigDecimal bigDecimal = new BigDecimal(gender);
                gender = String.valueOf(bigDecimal.intValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAuthState() {
        try {
            if (!TextUtils.isEmpty(authState)) {
                BigDecimal bigDecimal = new BigDecimal(authState);
                authState = String.valueOf(bigDecimal.intValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return authState;
    }

    public void setAuthState(String authState) {
        this.authState = authState;
    }

    public boolean isImgFlag() {
        return imgFlag;
    }

    public void setImgFlag(boolean imgFlag) {
        this.imgFlag = imgFlag;
    }


    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public String getUserSig() {
        return userSig;
    }

    public void setUserSig(String userSig) {
        this.userSig = userSig;
    }


    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "UserResponse{" +
                "userId='" + userId + '\'' +
                ", token='" + token + '\'' +
                ", account='" + account + '\'' +
                ", nickname='" + nickname + '\'' +
                ", payPassword=" + payPassword +
                ", payPwd=" + payPwd +
                ", userSn='" + userSn + '\'' +
                ", password='" + password + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", img='" + img + '\'' +
                ", nationality='" + nationality + '\'' +
                ", signature='" + signature + '\'' +
                ", gender='" + gender + '\'' +
                ", rongToken='" + rongToken + '\'' +
                ", authState='" + authState + '\'' +
                ", picToken='" + picToken + '\'' +
                ", userSig='" + userSig + '\'' +
                ", imToken='" + imToken + '\'' +
                '}';
    }
}
