package com.cmbc.infras.util;

import com.alibaba.fastjson.JSONObject;
import org.springframework.util.Assert;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 纵向越权
 * 请求源PC_OA,MOBILE_OA
 * 加密判断时用
 * 参考-KeEncryptUtil
 */
public class AESCodeUtil {

    private IvParameterSpec ivSpec;
    private SecretKeySpec keySpec;

    public AESCodeUtil(String srckey) {
        String key = paddingKey(srckey);
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

    public String encode(String src) {
        try {
            byte[] bytes = src.getBytes();
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, this.keySpec, this.ivSpec);
            byte[] doBytes = cipher.doFinal(bytes);
            String result = new String(Base64.getEncoder().encode(doBytes));
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String decode(String src) {
        try {
            byte[] decode = Base64.getDecoder().decode(src);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, this.keySpec, this.ivSpec);
            byte[] doBytes = cipher.doFinal(decode);
            String result = new String(doBytes);
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }


    private String paddingKey(String key) {
        StringBuffer sb = new StringBuffer(key);
        for (int i = key.length(); i < 16; i++) {
            sb.append("0");
        }
        return sb.toString();
    }

    public static String getSrcEncode(String srcKey, String account, String src) {
        Assert.notNull(srcKey, "加密key不能为空！");
        Assert.notNull(account, "加密账号不能为空！");
        Assert.notNull(src, "加密访问源不能为空！");
        AESCodeUtil util = new AESCodeUtil(srcKey);
        JSONObject obj = new JSONObject();
        obj.put(CookieKey.ACCOUNT, account);
        obj.put(CookieKey.REQUEST_SOURCE, src);
        String time = DateTimeUtils.getCurrentTime("yyyy-MM-dd HH:mm:ss");
        obj.put("time", time);
        String encode = util.encode(obj.toJSONString());
        return encode;
    }

    public static String getSrcDecode(String srcKey, String code) {
        Assert.notNull(srcKey, "加密key不能为空！");
        Assert.notNull(code, "加密字符串不能为空！");
        AESCodeUtil util = new AESCodeUtil(srcKey);
        String decode = util.decode(code);
        return decode;
    }

    public static void main(String[] args) {

        String encode = AESCodeUtil.getSrcEncode("zhangxing", "admin", "MOBILE_OA");
        System.out.println("encode:" + encode);
        String de = AESCodeUtil.getSrcDecode("zhangxing", encode);
        System.out.println("decode:" + de);
    }


    /*public static void main(String[] args) {
        AESCodeUtil util = new AESCodeUtil("zhangxing");
        JSONObject obj = new JSONObject();
        obj.put("account", "zhaodi1");
        obj.put("REQ_SRC", "MOBILE_OA");
        obj.put("time", "202201041203");
        String encode = util.encode(obj.toJSONString());
        System.out.println("编码:" + encode);

        //String s = "ligzcKKUtir96ScTQ7ueatcvKgUSAYYt9uPZst8JMlRAzSoM4Yk3qP533EpajOUMCLcbkcJMPZ5kWg5mgD1glkwzlIClAaz+Ff6mpu3jhoo=";
        AESCodeUtil util1 = new AESCodeUtil("zhangxing");
        String decode = util1.decode(encode);
        System.out.println("解码:" + decode);
    }*/

}
