package com.socialdashboard.adapter;

import com.socialdashboard.entity.SocialAccount.Platform;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PlatformAdapterRegistry {

    private final Map<Platform, PlatformAdapter> adapters = new EnumMap<>(Platform.class);

    public PlatformAdapterRegistry(List<PlatformAdapter> discovered) {
        discovered.forEach(adapter -> adapters.put(adapter.platform(), adapter));
    }

    public PlatformAdapter get(Platform platform) {
        PlatformAdapter adapter = adapters.get(platform);
        if (adapter == null) {
            throw new IllegalStateException("No adapter registered for platform " + platform);
        }
        return adapter;
    }
}
