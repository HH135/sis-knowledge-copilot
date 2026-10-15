package io.github.hyun.sis_app_intelligence.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void homePageDisplaysTitleAndExplorerLinks() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("<title>SIS App Intelligence</title>")))
				.andExpect(content().string(containsString("href=\"/source\">Source Explorer</a>")))
				.andExpect(content().string(containsString("href=\"/metadata\">Metadata Explorer</a>")))
				.andExpect(content().string(containsString("href=\"/documents\">Document Explorer</a>")));
	}
}
