package com.tsdp.service.impl;

import com.tsdp.entity.BlogComments;
import com.tsdp.mapper.BlogCommentsMapper;
import com.tsdp.service.IBlogCommentsService;
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
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
