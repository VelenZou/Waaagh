package org.waaagh.cloudfeignapi;

import java.io.Serializable;
import lombok.Data;


@Data
public class Order implements Serializable {
    private Long id;
    private String name;

    public Order() {}

    public Order(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
