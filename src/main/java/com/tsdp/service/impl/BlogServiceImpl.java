package com.tsdp.service.impl;

import com.tsdp.entity.Blog;
import com.tsdp.mapper.BlogMapper;
import com.tsdp.service.IBlogService;
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
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

}
