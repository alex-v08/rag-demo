package com.atuhome.ragdemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test") 
class RagDemoApplicationTests {

	@Test
	void contextLoads() {
		// Test que verifica que el contexto de Spring Boot se carga correctamente
	}

}
