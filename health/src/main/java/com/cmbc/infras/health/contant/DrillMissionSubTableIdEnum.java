package com.cmbc.infras.health.contant;

import java.util.Arrays;
import java.util.List;

/**
 * 演练任务子表数据字段id
 */
public enum DrillMissionSubTableIdEnum {

    GROUP(Arrays.asList("Field_xxx_mniohsrxt", "Field_xxx_eovdpegq", "Field_xxx_auacaosdc", "Field_xxx_rcheovdm", "Field_xxx_pjcnczz")),//责任岗/组
    ORDER(Arrays.asList("Field_xxx_adxlbjqt", "Field_xxx_unyjj", "Field_xxx_vylorsjm", "Field_xxx_stgbfqb", "Field_xxx_fdhyahzzf")),//应急序列
    WORK_TIME(Arrays.asList("Field_xxx_byffhhk", "Field_xxx_abfhreo", "Field_xxx_vxbbrwam", "Field_xxx_tdfiqea", "Field_xxx_dbefwjozk")),//作业时间（分钟）
    IF_NORMAL(Arrays.asList("Field_xxx_eyicaaa", "Field_xxx_wufxzbfy", "Field_xxx_fsaitqwda", "Field_xxx_swlkmuod", "Field_xxx_htgctdmx")),//是否正常
    SITUATION(Arrays.asList("Field_xxx_kbydijq", "Field_xxx_dhxjymb", "Field_xxx_qrnxib", "Field_xxx_vpekvofkr", "Field_xxx_zdnzm"));//异常处理情况

    private List<String> ids;//id的集合

    DrillMissionSubTableIdEnum(List<String> ids) {
        this.ids = ids;
    }

    public List<String> getIds() {
        return ids;
    }

}
