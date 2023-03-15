package com.cmbc.infras.health.util;

import com.alibaba.nacos.api.utils.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public class FileUtil {

    /**
     * 判断是否为图片
     */
    public static boolean isPicture(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String[] FILETYPES = new String[]{
                ".jpg", ".bmp", ".jpeg", ".png", ".gif",
                ".JPG", ".BMP", ".JPEG", ".PNG", ".GIF"
        };
        if (!StringUtils.isBlank(originalFilename)) {
            for (int i = 0; i < FILETYPES.length; i++) {
                String fileType = FILETYPES[i];
                if (originalFilename.endsWith(fileType)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 上传文件
     */
    public static boolean uploadDocument(MultipartFile file, String fileName, String sPath) {
        if (file.isEmpty()) return false;
        File dest = new File(sPath + "/" + fileName);
        if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();
        try {
            file.transferTo(dest);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 根据路径删除指定的目录或文件，无论存在与否
     *
     * @param sPath 要删除的目录或文件
     * @return 删除成功返回 true，否则返回 false。
     */
    public static boolean DeleteFolder(String sPath) {
        boolean flag = false;
        File file = new File(sPath);
        if (!file.exists()) return flag;
        if (file.isFile()) return deleteFile(sPath);
        return deleteDirectory(sPath);
    }

    /**
     * 删除单个文件
     *
     * @param sPath 被删除文件的文件名
     * @return 单个文件删除成功返回true，否则返回false
     */
    private static boolean deleteFile(String sPath) {
        boolean flag = false;
        File file = new File(sPath);
        // 路径为文件且不为空则进行删除
        if (file.isFile() && file.exists()) {
            file.delete();
            flag = true;
        }
        return flag;
    }

    /**
     * 删除目录（文件夹）以及目录下的文件
     *
     * @param sPath 被删除目录的文件路径
     * @return 目录删除成功返回true，否则返回false
     */
    private static boolean deleteDirectory(String sPath) {
        //如果sPath不以文件分隔符结尾，自动添加文件分隔符
        if (!sPath.endsWith(File.separator)) {
            sPath = sPath + File.separator;
        }
        File dirFile = new File(sPath);
        //如果dir对应的文件不存在，或者不是一个目录，则退出
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        boolean flag = true;
        //删除文件夹下的所有文件(包括子目录)
        File[] files = dirFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            //删除子文件
            if (files[i].isFile()) {
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag) break;
            } //删除子目录
            else {
                flag = deleteDirectory(files[i].getAbsolutePath());
                if (!flag) break;
            }
        }
        if (!flag) return false;
        //删除当前目录
        if (dirFile.delete()) return true;
        return false;
    }

}
