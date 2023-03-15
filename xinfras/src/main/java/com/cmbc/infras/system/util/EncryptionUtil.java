package com.cmbc.infras.system.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

import lombok.extern.slf4j.Slf4j;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * 加密工具类
 * 包含MD5加密，Base64加解密
 *
 * @author FAN
 * @version 1.0
 */
@Slf4j
public class EncryptionUtil {

    private EncryptionUtil() {
    }

    /**
     * Base64加密
     */
    public static String encryption(String src, String key) {
        String requestValue = "";
        try {
            byte[] enKey = getEnKey(key);
            byte[] src2 = src.getBytes("UTF-16LE");
            byte[] encryptedData = Encrypt(src2, enKey);
            String base64String = getBase64Encode(encryptedData);
            String base64Encrypt = filter(base64String);
            requestValue = getURLEncode(base64Encrypt);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        return requestValue;
    }

    /**
     * Base64解密
     */
    public static String decryption(String src, String spkey) {
        String requestValue = "";
        try {
            String URLValue = getURLDecoderdecode(src);
            BASE64Decoder base64Decode = new BASE64Decoder();
            byte[] base64DValue = base64Decode.decodeBuffer(URLValue);
            requestValue = deCrypt(base64DValue, spkey);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        return requestValue;
    }

    private static byte[] md5(String strSrc) {
        byte[] returnByte = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            returnByte = md5.digest(strSrc.getBytes("GBK"));
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        return returnByte;
    }

    private static byte[] getEnKey(String spKey) {
        byte[] desKey = null;
        try {
            byte[] desKey1 = md5(spKey);
            desKey = new byte[24];
            int i = 0;
            while (i < desKey1.length && i < 24) {
                desKey[i] = desKey1[i];
                i++;
            }
            if (i < 24) {
                desKey[i] = 0;
                i++;
            }
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        return desKey;
    }

    private static byte[] Encrypt(byte[] src, byte[] enKey) {
        byte[] encryptedData = null;
        try {
            DESedeKeySpec dks = new DESedeKeySpec(enKey);
            SecretKeyFactory keyFactory = SecretKeyFactory
                    .getInstance("DESede");
            SecretKey key = keyFactory.generateSecret(dks);
            Cipher cipher = Cipher.getInstance("DESede");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            encryptedData = cipher.doFinal(src);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        return encryptedData;
    }

    private static String getBase64Encode(byte[] src) {
        String requestValue = "";
        try {
            BASE64Encoder base64en = new BASE64Encoder();
            requestValue = base64en.encode(src);
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        return requestValue;
    }

    private static String filter(String str) {
        String output;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            int asc = str.charAt(i);
            if (asc != 10 && asc != 13)
                sb.append(str.subSequence(i, i + 1));
        }
        output = new String(sb);
        return output;
    }

    private static String getURLEncode(String src) {
        String requestValue = "";
        try {
            requestValue = URLEncoder.encode(src);
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        return requestValue;
    }

    private static String getURLDecoderdecode(String src) {
        String requestValue = "";
        try {
            requestValue = URLDecoder.decode(src);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        return requestValue;
    }

    private static String deCrypt(byte[] debase64, String spKey) {
        String strDe;
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("DESede");
            byte[] key = getEnKey(spKey);
            DESedeKeySpec dks = new DESedeKeySpec(key);
            SecretKeyFactory keyFactory = SecretKeyFactory
                    .getInstance("DESede");
            SecretKey sKey = keyFactory.generateSecret(dks);
            cipher.init(Cipher.DECRYPT_MODE, sKey);
            byte ciphertext[] = cipher.doFinal(debase64);
            strDe = new String(ciphertext, "UTF-16LE");
        } catch (Exception ex) {
            strDe = "";
            log.warn(ex.getMessage());
            ;
        }
        return strDe;
    }

    //32位大写加密
//	public static String getBase32Encode(String str){
//		String result="";
//		try {
//			char hexDigets[]={'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
//			byte[] blinput=str.getBytes();
//			MessageDigest mdlnst=MessageDigest.getInstance("MD5");
//			mdlnst.update(blinput);
//			byte[] md=mdlnst.digest();
//			
//			int j=md.length;
//			char s[]=new char[j*2];
//			int k=0;
//			for (int i = 0; i < j; i++) {
//				byte byte0=md[i];
//			  s[k++]=hexDigets[byte0>>>4 & 0xf];
//		      s[k++]=hexDigets[byte0&0xf];
//			}
//			 result=new String(s);
//			 
//			 System.out.println(result+"================");
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		}
//		return result;
//	}

    //32位小写加密
    public static String getBase32Encode(String str) {
        String result = "";
        try {
            byte[] blinput = str.getBytes();
            MessageDigest mdlnst = MessageDigest.getInstance("MD5");
            mdlnst.update(blinput);
            byte[] md = mdlnst.digest();
            StringBuffer hef = new StringBuffer(md.length * 2);
            for (byte b : md) {
                if ((b & 0xFF) < 0x10) {
                    hef.append("0");
                }
                hef.append(Integer.toHexString(b & 0xFF));
            }
            result = hef.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void main(String[] asdf) {
        String ss = "zhaodi1";
        String encry = EncryptionUtil.encryption(ss, "ITPTL");
        String decry = EncryptionUtil.decryption(encry, "ITPTL");
        System.out.println(encry + "=========" + decry);
    }

}
