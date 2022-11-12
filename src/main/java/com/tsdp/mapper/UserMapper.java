package com.tsdp.mapper;

import com.tsdp.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 范大晨
 * @since 2022-11-9
 */

@Mapper
public interface UserMapper extends BaseMapper<User> {

}
