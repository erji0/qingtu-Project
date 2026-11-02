package com.qingtu.security;

import com.qingtu.common.BusinessException;

public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(401, "登录状态已失效，请重新登录");
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
