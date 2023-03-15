package com.cmbc.infras.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Base64Utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * @description: <p> 说明 </p>
 * @author: LongQi
 * @date: 2020/12/21 13:54
 */

@Slf4j
public class AesUtil {

    /**
     * key算法
     */
    private static final String KEY_ALGORITHM = "AES";
    /**
     * 默认的加密算法
     */
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";
    /**
     * 字符串为UTF8
     */
    private static final String CHARSET = "UTF-8";
    /**
     * 密码
     */
    private static final String PASSWORD = "GongJi";


    /**
     * AES 加密操作
     * @param content  待加密内容
     * @return 返回Base64转码后的加密数据
     */
    public static String encrypt(String content) {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
            byte[] byteContent = content.getBytes(CHARSET);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(PASSWORD));
            byte[] result = cipher.doFinal(byteContent);
            return Base64Utils.encodeToString(result);
        } catch (Exception e) {
            log.error("AES加密异常，{}", e);
        }
        return null;
    }

    /**
     * AES 加密操作
     * @param content  待加密内容
     * @param password 加密密码
     * @return 返回Base64转码后的加密数据
     */
    public static String encrypt(String content, String password) {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
            byte[] byteContent = content.getBytes(CHARSET);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(password));
            byte[] result = cipher.doFinal(byteContent);
            return Base64Utils.encodeToString(result);
        } catch (Exception e) {
            log.error("AES加密异常，{}", e);
        }
        return null;
    }

    /**
     * AES 解密操作
     *
     * @param content
     * @return
     */
    public static String decrypt(String content) {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(PASSWORD));
            byte[] result = cipher.doFinal(Base64Utils.decodeFromString(content));
            return new String(result, CHARSET);
        } catch (Exception e) {
            log.error("AES解密异常，{}", e);
        }
        return null;
    }

    /**
     * AES 解密操作
     *
     * @param content
     * @param password
     * @return
     */
    public static String decrypt(String content, String password) {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(password));
            byte[] result = cipher.doFinal(Base64Utils.decodeFromString(content));
            return new String(result, CHARSET);
        } catch (Exception e) {
            log.error("AES解密异常，{}", e);
        }
        return null;
    }

    /**
     * 生成加密秘钥
     *
     * @return
     */
    private static SecretKeySpec getSecretKey(String password) {
        KeyGenerator kg = null;
        try {
            kg = KeyGenerator.getInstance(KEY_ALGORITHM);
            kg.init(128, new SecureRandom(password.getBytes()));
            SecretKey secretKey = kg.generateKey();
            return new SecretKeySpec(secretKey.getEncoded(), KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            log.error("AES秘钥异常，{}", e);
        }
        return null;
    }

//    public static void main(String[] args) throws Exception{
//        String content = "{\"account\":\"admin\",\"deptList\":[{\"children\":[],\"id\":1,\"name\":\"GU\",\"uid\":\"DEPT:1\",\"userList\":[]}],\"email\":\"admin@xxx.com\",\"gender\":0,\"id\":1,\"menuList\":[{\"authFlag\":\"appstore-edit\",\"id\":3,\"type\":3}],\"name\":\"系统管理员\",\"org\":{\"id\":1,\"name\":\"GU\",\"uid\":\"ORG:1\"},\"phone\":\"15899998888\",\"photo\":\"group1/M00/00/01/wKgEX1_bGp6ACs43AAiQfKHDaaQ042.jpg\",\"roleList\":[{\"id\":1,\"name\":\"系统管理员\",\"uid\":\"ROLE:1\"}],\"sessionId\":\"XSS_qvgHcWTNyiCln4VlnMPd4t0Zkj82wsLMC6nCgj-Z3nbYfHA\",\"uid\":\"USER:1\",\"userType\":1}";
//        System.out.println("原文："+content);
//        String passwordEn = encrypt(content);
//        System.out.println("AES加密后：" + passwordEn);
//        String outStr = Base64.encodeBase64String(passwordEn.getBytes(CHARSET));
//        System.out.println("Base64加密后：" + outStr);
//        String inStr = new String(Base64.decodeBase64(outStr.getBytes(CHARSET)));
//        System.out.println("Base64解密后：" + inStr);
//        String passwordDe = decrypt(inStr);
//        System.out.println("AES解密后：" + passwordDe);
//    }

}
