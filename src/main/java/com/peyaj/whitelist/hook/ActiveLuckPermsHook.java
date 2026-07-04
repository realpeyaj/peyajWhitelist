package com.peyaj.whitelist.hook;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import java.util.UUID;

public class ActiveLuckPermsHook implements ILuckPermsHook {

    private final LuckPerms lp;

    public ActiveLuckPermsHook() {
        this.lp = LuckPermsProvider.get();
    }

    @Override
    public boolean hasBypassPermission(UUID uuid) {
        try {
            User user = lp.getUserManager().getUser(uuid);
            if (user == null) {
                // User may not be loaded during AsyncPlayerPreLoginEvent.
                // Since this runs asynchronously, we can safely block and load the user from storage.
                user = lp.getUserManager().loadUser(uuid).join();
            }
            if (user != null) {
                QueryOptions options = lp.getContextManager().getQueryOptions(user)
                        .orElse(lp.getContextManager().getStaticQueryOptions());
                return user.getCachedData().getPermissionData(options).checkPermission("peyajwhitelist.bypass").asBoolean();
            }
        } catch (Throwable t) {
            // Keep logins functioning if LuckPerms database is offline or errored
        }
        return false;
    }
}
