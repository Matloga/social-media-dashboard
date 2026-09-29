package com.socialdashboard.adapter.mock;

import com.socialdashboard.entity.SocialAccount;
import com.socialdashboard.entity.SocialAccount.Platform;
import org.springframework.stereotype.Component;

@Component
public class TikTokMockAdapter extends MockPlatformAdapter {

    @Override
    public Platform platform() {
        return Platform.TIKTOK;
    }

    @Override
    public String displayName() {
        return "TikTok";
    }
}
