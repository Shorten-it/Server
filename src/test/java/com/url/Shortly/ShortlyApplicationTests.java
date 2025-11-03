package com.url.Shortly;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.url.ShortenIt.ShortlyApplication;

@SpringBootTest(classes = ShortlyApplication.class)
@ActiveProfiles("test")
class ShortlyApplicationTests {

	@Test
	void contextLoads() {
	}

}
