package com.ntaganira.heritier.ipos.backoffice;

import com.ntaganira.heritier.ipos.backoffice.entity.AppUser;
import com.ntaganira.heritier.ipos.backoffice.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AvatarFlowTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Test
    void uploadedAvatarIsStoredAndServed() throws Exception {
        byte[] png = pngBytes();
        MockMultipartFile avatar = new MockMultipartFile(
                "avatar", "avatar.png", "image/png", png);

        mockMvc.perform(multipart("/profile")
                        .file(avatar)
                        .param("fullName", "Administrator")
                        .param("email", "admin@example.com")
                        .param("locale", "en")
                        .param("removeAvatar", "false")
                        .with(csrf())
                        .with(user("admin")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        AppUser admin = userRepository.findByUsername("admin").orElseThrow();
        assertThat(admin.getAvatarPath()).startsWith("avatars/");

        mockMvc.perform(get("/files/" + admin.getAvatarPath()).with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    private byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}