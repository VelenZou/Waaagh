package org.waaagh.cloudfeignserver.controller;

import org.waaagh.cloudfeignapi.User;
import org.waaagh.cloudfeignapi.UserApiConst;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 演示 Controller，完整路径前缀 /hello/world/user（与 {@link UserServerController} 基本相同，另多一个 updateeeee）。
 * <p>
 * 预期匹配的 Feign 方法（路径 + HTTP 方法；下文 UserClient/2/4/5/6 表示对应客户端）：
 * <ul>
 *   <li>getUserById（DELETE /user/get/{id}）→ UserClient/2/4/5/6#getUserById + ListenerClient#delUsr、NullClient#deleUsr；</li>
 *   <li>update、del、getfather → UserClient/2/4/5/6 的同名方法（GET）；</li>
 *   <li>getmather、clipboard~clipboard4、optimizedCache~optimizedCache3、parallelScan~parallelScan5、parallelScan7
 *       → UserClient2/4/5/6 的同名方法（GET）；</li>
 *   <li>parallelScan8（路径 /user/parallelScan811/{id}）→ UserClient2/4/5/6#parallelScan8 + UserClient4/5/6#parallelScan811；</li>
 *   <li>parallelScan9（路径来自 UserApiConst.USER_CLIENT_PARALLEL_SCAN9_ID）→ UserClient3#parallelScan8；</li>
 *   <li>updateeeee（/user/updateeee/{id}）→ UserClient6#updateeeee。</li>
 * </ul>
 */
@RestController
public class UserServerController2 {

    /**
     *
     * @param id
     * @return
     */
    @DeleteMapping(value = "/user/get/{id}")
    public User getUserById(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }

    /**
     * hhh
     * @param id
     * @return
     */
    @GetMapping(value = "/user/update2/{id}")
    /**
     * 哈哈哈
     */
    public User update(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }
    /**
     * hhh
     * @param id
     * @return
     */
    @GetMapping(value = "/user/updateeee/{id}")
    /**
     * 哈哈哈
     */
    public User updateeeee(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }

    @GetMapping(value = "/user/del/{id}")
    public User del(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }

    /**
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/user/getfather/{id}")
    /**
     *
     */
    public User getfather(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }

    /**
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/user/getmather/{id}")
    /**
     *
     */
    public User getmather(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }

    /**
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/user/clipboard/{id}")
    public User clipboard(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }

    /**
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/user/clipboard2/{id}")
    public User clipboard2(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }
    @GetMapping(value = "/user/clipboard3/{id}")
    /**
     *
     */
    public User clipboard3(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }

    /**
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/user/clipboard4/{id}")
    /**
     *
     */
    @Deprecated
    /**
     *
     */
    public User clipboard4(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }

    /**
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/user/optimizedCache/{id}")
    @Deprecated
    /**
     *
     */
    public User optimizedCache(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }
    /**
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/user/optimizedCache2/{id}")
    @Deprecated
    /**
     *
     */
    public User optimizedCache2(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }

    /**
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/user/optimizedCache3/{id}")
    public User optimizedCache3(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }

    @GetMapping(value = "/user/parallelScan/{id}")
    public User parallelScan(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }

    @GetMapping(value = "/user/parallelScan2/{id}")
    public User parallelScan2(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }

    /**
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/user/parallelScan3/{id}")
    public User parallelScan3(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }

    /**
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/user/parallelScan4/{id}")
    public User parallelScan4(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }
    /**
     * parallelScan5. v5.2.1
     * @param id
     * @return
     */
    @GetMapping(value = "/user/parallelScan5/{id}")
    public User parallelScan5(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }
    @GetMapping(value = "/user/parallelScan7/{id}")
    public User parallelScan7(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }
    @GetMapping(value = "/user/parallelScan811/{id}")
    public User parallelScan8(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }

    @GetMapping(UserApiConst.USER_CLIENT_PARALLEL_SCAN9_ID)
    public User parallelScan9(@PathVariable("id") Long id)
    {
        return new User(id, "user");
    }











}