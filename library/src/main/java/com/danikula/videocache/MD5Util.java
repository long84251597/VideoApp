package com.danikula.videocache;

import android.util.Base64;

import java.security.MessageDigest;


public class MD5Util {
    /*
    先进行base64加密，再进行md5加密
     */
    public static String encode(String str){
        return md5Encryption(base64Encryption(str));
    }

    /**
     * MD5加密 179885.com
     */
    public static String md5Encryption(String str) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
            return "";
        }
        char[] charArray = str.toCharArray();
        byte[] byteArray = new byte[charArray.length];

        for (int i = 0; i < charArray.length; i++)
            byteArray[i] = (byte) charArray[i];

        byte[] md5Bytes = md5.digest(byteArray);

        StringBuilder hexValue = new StringBuilder();

        for (byte md5Byte : md5Bytes) {
            int val = ((int) md5Byte) & 0xff;
            if (val < 16)
                hexValue.append("0");
            hexValue.append(Integer.toHexString(val));
        }

        return hexValue.toString();
    }

    /**
     * base64加密
     */
    public static String base64Encryption(String str) {
        if (str == null) return null;
        String encodeStr = "";
        try {
            encodeStr = Base64.encodeToString(str.getBytes(), Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }


        return encodeStr;
    }
    /**
     * base64解密 179885.com
     */
    public static String base64Dcrypt(String str) {
        if (str == null) return null;
        String decoderStr = "";
        byte[] b = Base64.decode(str, Base64.DEFAULT);
        decoderStr = new String(b);

        return decoderStr;
    }
}
