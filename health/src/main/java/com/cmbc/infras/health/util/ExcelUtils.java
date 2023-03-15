package com.cmbc.infras.health.util;

import jxl.format.UnderlineStyle;
import jxl.write.*;

public class ExcelUtils {

    /**
     * 定义单元格样式
     * 0:title 1:head 2:table
     */
    public static WritableCellFormat getFormat(int n) throws WriteException {
        if (n == 0) {
            WritableFont wf = new WritableFont(
                    WritableFont.ARIAL,//格式
                    11,//字体
                    WritableFont.BOLD,//粗体
                    false,//斜体
                    UnderlineStyle.NO_UNDERLINE,//下划线
                    jxl.format.Colour.BLACK);//颜色
            WritableCellFormat wcf = new WritableCellFormat(wf); // 单元格定义
            wcf.setBackground(Colour.WHITE); // 设置单元格的背景颜色
            wcf.setAlignment(Alignment.CENTRE); // 设置对齐方式
            wcf.setBorder(Border.ALL, BorderLineStyle.THIN, Colour.BLACK); //设置边框
            return wcf;
        }
        if (n == 1) {
            WritableFont wf = new WritableFont(
                    WritableFont.ARIAL,
                    11,
                    WritableFont.NO_BOLD,
                    false,
                    UnderlineStyle.NO_UNDERLINE,
                    jxl.format.Colour.BLACK);
            WritableCellFormat wcf = new WritableCellFormat(wf);
            wcf.setBackground(Colour.WHITE);
            wcf.setAlignment(Alignment.CENTRE);
            wcf.setBorder(Border.ALL, BorderLineStyle.THIN, Colour.BLACK);
            return wcf;
        }
        if (n == 2) {
            WritableFont wf = new WritableFont(
                    WritableFont.ARIAL,
                    11,
                    WritableFont.NO_BOLD,
                    false,
                    UnderlineStyle.NO_UNDERLINE,
                    jxl.format.Colour.BLACK);
            WritableCellFormat wcf = new WritableCellFormat(wf);
            wcf.setBackground(Colour.LIGHT_GREEN);
            wcf.setAlignment(Alignment.CENTRE);
            wcf.setBorder(Border.ALL, BorderLineStyle.THIN, Colour.BLACK);
            return wcf;
        }
        return null;
    }

}
