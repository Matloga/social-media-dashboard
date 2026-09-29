package com.socialdashboard.adapter.mock;

import com.socialdashboard.entity.SocialAccount;
import com.socialdashboard.entity.SocialAccount.Platform;
import org.springframework.stereotype.Component;

@Component
public class YouTubeMockAdapter extends MockPlatformAdapter {

    @Override
    public Platform platform() {
        return Platform.YOUTUBE;
    }

    @Override
    public String displayName() {
        return "YouTube";
    }
}
