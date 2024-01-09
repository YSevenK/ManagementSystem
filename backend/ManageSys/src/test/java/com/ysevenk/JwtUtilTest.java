package com.ysevenk;

import com.ysevenk.common.utils.JwtUtil;
import com.ysevenk.sys.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class JwtUtilTest {
    @Autowired
    private JwtUtil jwtUtil;

    @Test
    public void testCreate() {
        User user = new User();
        user.setUsername("三");
        user.setPhone("123456");
        String token = jwtUtil.createToken(user);
        System.out.println(token);
    }

    @Test
    public void testParseJwt() {
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI1MzVlMWFhNi01MmRiLTRlY2UtYTJiYS1mZmVhMGJkYjU2MTQiLCJzdWIiOiJ7XCJwaG9uZVwiOlwiMTIzNDU2XCIsXCJ1c2VybmFtZVwiOlwi5LiJXCJ9IiwiaXNzIjoic3lzdGVtIiwiaWF0IjoxNzA0NzY3MDMxLCJleHAiOjE3MDQ3Njg4MzF9.PSX4ol0x9DngBKMwGI16eZEpLck-eLylTXo9s8EPrrw";
        Claims claims = jwtUtil.parseToken(token);
        System.out.println(claims);
    }
}
