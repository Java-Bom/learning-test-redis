package com.cys;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash(value = "testPoint", timeToLive = 60L)
public class Point {

    @Id
    private String id; // userId
    private Long point;

    public Point(String id, Long point) {
        this.id = id;
        this.point = point;
    }

    public String getId() {
        return id;
    }

    public Long getPoint() {
        return point;
    }
}
