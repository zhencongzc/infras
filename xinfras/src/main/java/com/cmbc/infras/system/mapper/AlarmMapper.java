package com.cmbc.infras.system.mapper;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.Device;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.StatementType;

import java.util.List;

@Mapper
public interface AlarmMapper {

    @Select("select id, account, type, content from fast_input where account=#{account} and type=#{type}")
    List<JSONObject> findFastInput(String account, int type);

    @Insert("insert into fast_input values(null, #{account}, #{type}, #{content})")
    @SelectKey(statement = "select last_insert_id()", keyProperty = "json.id", before = false, statementType = StatementType.STATEMENT,
            resultType = Integer.class)
    int addFastInput(String account, int type, String content,JSONObject json);

    @Update("update fast_input set content=#{content} where id=#{id}")
    void updateFastInput(int id, String content);

    @Delete("delete from fast_input where id=#{id}")
    void deleteFastInput(int id);
}
