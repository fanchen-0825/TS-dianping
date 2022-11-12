package com.tsdp.service.impl;

import com.tsdp.entity.UserInfo;
import com.tsdp.mapper.UserInfoMapper;
import com.tsdp.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 范大晨
 * @since 2022-11-9
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
