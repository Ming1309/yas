package com.yas.order.viewmodel.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductCheckoutListVm {
    Long id;
    String name;
    String description;
    String shortDescription;
    String sku;
    Long parentId;
    Long brandId;
    Double price;
    Long taxClassId;
    String thumbnailUrl;
    ZonedDateTime createdOn;
    String createdBy;
    ZonedDateTime lastModifiedOn;
    String lastModifiedBy;
}
