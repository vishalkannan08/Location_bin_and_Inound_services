package com.company.wms.locationbin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI locationBinOpenApi() {
        return new OpenAPI().info(new Info()
                .title("WMS location-bin-service API")
                .version("v1")
                .description("""
                        Location and Bin master data for WMS Increment 1.
                        Owns the location and bin tables only. warehouseId is a logical
                        reference to warehouse-service with no foreign key."""));
    }
}
