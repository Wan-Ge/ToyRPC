package com.zxcvs.test.client;

import java.util.List;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/29 14:06
 */

public interface PersonService {

    List<Person> getPerson(String name, int num);
}
