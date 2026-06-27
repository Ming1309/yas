package com.yas.order.viewmodel.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Builder(toBuilder = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductCheckoutListVm {
    Long id;
    String name;
    Double price;
    Long taxClassId;
}
