package com.socialdashboard.adapter.mock;

import com.socialdashboard.entity.SocialAccount;
import com.socialdashboard.entity.SocialAccount.Platform;
import org.springframework.stereotype.Component;

@Component
public class InstagramMockAdapter extends MockPlatformAdapter {

    @Override
    public Platform platform() {
        return Platform.INSTAGRAM;
    }

    @Override
    public String displayName() {
        return "Instagram";
    }
}
