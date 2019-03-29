package com.zxcvs.test.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/29 14:05
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Person {

    private String firstName;

    private String lastName;

}
