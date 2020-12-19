package com.cys;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = RedisConfig.class)
class PointTest {

    @Autowired
    private PointRepository pointRepository;

    @Test
    void name() {
        //given

        //when
        pointRepository.save(new Point("test1", 100L));
        pointRepository.save(new Point("test2", 100L));
//        pointRepository.findById("test1");
//        pointRepository.deleteById("test1");
        //then

    }
}