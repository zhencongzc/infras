package com.cmbc.infras.util;

import cn.hutool.http.HttpUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;

public class KeEncryptUtil {
    private IvParameterSpec ivSpec;
    private SecretKeySpec keySpec;

    public KeEncryptUtil(String srckey) {
        String key = paddingkey(srckey);
        try {
            byte[] keyBytes = key.getBytes();
            byte[] buf = new byte[16];
            for (int i = 0; i < keyBytes.length && i < buf.length; i++) {
                buf[i] = keyBytes[i];
            }
            this.keySpec = new SecretKeySpec(buf, "AES");
            this.ivSpec = new IvParameterSpec(keyBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String encrypt(String src) {
        try {
            byte[] origData = src.getBytes();
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, this.keySpec, this.ivSpec);
            byte[] re = cipher.doFinal(origData);
            return encodeURIComponent(new String(Base64.getEncoder().encode(re)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String decrypt(String src) throws Exception {

        byte[] crypted = Base64.getDecoder().decode(src);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, this.keySpec, this.ivSpec);
        byte re[] = cipher.doFinal(crypted);
        return new String(re);
    }

    private static String paddingkey(String liu) {
        StringBuffer sb = new StringBuffer(liu);
        for (int i = liu.length(); i < 16; i++) {
            sb.append("0");
        }
        return sb.toString();

    }

    /**
     * encodeURIComponent 编码
     *
     * @param s 编码的字符串
     * @return 转码后的字符串
     */
    public static String encodeURIComponent(String s) {
        String result = null;

        try {
            result = URLEncoder.encode(s, "UTF-8")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%7E", "~");
        }

        // This exception should never occur.
        catch (UnsupportedEncodingException e) {
            result = s;
        }

        return result;
    }

}
