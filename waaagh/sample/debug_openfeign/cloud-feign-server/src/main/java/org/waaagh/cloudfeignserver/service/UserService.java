package org.waaagh.cloudfeignserver.service;

import org.springframework.stereotype.Service;
import org.waaagh.cloudfeignapi.User;

@Service
public class UserService {

    public User getUserById() {
        return new User(1L, "Waaagh!!!");
    }
}
