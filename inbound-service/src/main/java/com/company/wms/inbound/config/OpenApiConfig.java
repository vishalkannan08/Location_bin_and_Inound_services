package com.company.wms.inbound.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inboundOpenApi() {
        return new OpenAPI().info(new Info()
                .title("WMS inbound-service API")
                .version("v1")
                .description("""
                        Goods Receipt and receipt lines for WMS Increment 1.
                        Owns the goods_receipt and goods_receipt_line tables only.
                        warehouseId is a logical reference to warehouse-service; skuId is a
                        SKU code, as no product master exists in this increment."""));
    }
}
