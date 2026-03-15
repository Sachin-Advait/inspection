package com.gissoft.inspection_backend.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(
                ObjectUtils.asMap(
                        "cloud_name", "dxv3dts8k",
                        "api_key", "422744849866437",
                        "api_secret", "G3im-z914VaIXMdpuIIIw8BK3vM"
                )
        );
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}