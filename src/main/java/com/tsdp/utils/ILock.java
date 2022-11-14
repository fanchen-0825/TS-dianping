package com.tsdp.utils;

import org.springframework.stereotype.Component;

@Component
public interface ILock {

    boolean tryLock(long time);

    void unlock();
}
