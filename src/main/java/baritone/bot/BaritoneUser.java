/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot;

import baritone.bot.spec.EntityBot;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.util.Session;

/**
 * Implementation of {@link IBaritoneUser}
 *
 * @author Brady
 * @since 11/6/2018
 */
class BaritoneUser implements IBaritoneUser {

    private final UserManager manager;
    private final NetworkManager networkManager;
    private final Session session;

    private GameProfile profile;
    private INetHandlerPlayClient netHandlerPlayClient;

    BaritoneUser(UserManager manager, NetworkManager networkManager, Session session) {
        this.manager = manager;
        this.networkManager = networkManager;
        this.session = session;
    }

    @Override
    public void onLoginSuccess(GameProfile profile, INetHandlerPlayClient netHandlerPlayClient) {
        this.profile = profile;
        this.netHandlerPlayClient = netHandlerPlayClient;
    }

    @Override
    public NetworkManager getNetworkManager() {
        return this.networkManager;
    }

    @Override
    public INetHandlerPlayClient getConnection() {
        return this.netHandlerPlayClient;
    }

    @Override
    public EntityBot getEntity() {
        // TODO
        return null;
    }

    @Override
    public Session getSession() {
        return this.session;
    }

    @Override
    public GameProfile getProfile() {
        return this.profile;
    }

    @Override
    public UserManager getManager() {
        return this.manager;
    }
}
