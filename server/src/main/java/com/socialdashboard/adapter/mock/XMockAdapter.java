package com.socialdashboard.adapter.mock;

import com.socialdashboard.entity.SocialAccount;
import com.socialdashboard.entity.SocialAccount.Platform;
import org.springframework.stereotype.Component;

@Component
public class XMockAdapter extends MockPlatformAdapter {

    @Override
    public Platform platform() {
        return Platform.X;
    }

    @Override
    public String displayName() {
        return "X";
    }
}
