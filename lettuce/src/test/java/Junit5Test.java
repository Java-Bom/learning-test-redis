import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Junit5Test {

    @Test
    void name() {
        //given
        int a = 1;
        int b= 2;

        //when

        //then
        assertThat(a+b).isEqualTo(3);
    }
}
