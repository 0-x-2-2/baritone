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

package baritone.utils;

import baritone.api.event.events.TickEvent;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.behavior.PathingBehavior;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

public class BaritoneAutoTest implements AbstractGameEventListener, Helper {

    public static final BaritoneAutoTest INSTANCE = new BaritoneAutoTest();

    private BaritoneAutoTest() {}

    public static final boolean ENABLE_AUTO_TEST = true;
    private static final long TEST_SEED = -928872506371745L;
    private static final BlockPos STARTING_POSITION = new BlockPos(50, 65, 50);
    private static final Goal GOAL = new GoalBlock(69, 121, 420);
    private static final int MAX_TICKS = 3200;

    /**
     * Called right after the {@link GameSettings} object is created in the {@link Minecraft} instance.
     */
    public void onPreInit() {
        System.out.println("Optimizing Game Settings");

        GameSettings s = mc.gameSettings;
        s.limitFramerate   = 20;
        s.mipmapLevels     = 0;
        s.particleSetting  = 2;
        s.overrideWidth    = 128;
        s.overrideHeight   = 128;
        s.heldItemTooltips = false;
        s.entityShadows    = false;
        s.chatScale        = 0.0F;
        s.ambientOcclusion = 0;
        s.clouds           = 0;
        s.fancyGraphics    = false;
        s.tutorialStep     = TutorialSteps.NONE;
        s.hideGUI          = true;
        s.fovSetting       = 30.0F;
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.currentScreen != null && mc.currentScreen instanceof GuiMainMenu) {
            System.out.println("Beginning Baritone automatic test routine");
            mc.displayGuiScreen(null);
            WorldSettings worldsettings = new WorldSettings(TEST_SEED, GameType.getByName("survival"), true, false, WorldType.DEFAULT);
            worldsettings.setGeneratorOptions("");
            mc.launchIntegratedServer("BaritoneAutoTest", "BaritoneAutoTest", worldsettings);
        }
        if (mc.getIntegratedServer() != null && mc.getIntegratedServer().worlds[0] != null) {
            mc.getIntegratedServer().worlds[0].setSpawnPoint(STARTING_POSITION);
        }
        if (event.getType() == TickEvent.Type.IN) {
            if (mc.isSingleplayer() && !mc.getIntegratedServer().getPublic()) {
                mc.getIntegratedServer().shareToLAN(GameType.getByName("survival"), false);
            }
            if (event.getCount() < 200) {
                System.out.println("Waiting for world to generate... " + event.getCount());
                return;
            }
            if (event.getCount() % 100 == 0) { // print only once every 5 seconds
                System.out.println(playerFeet() + " " + event.getCount());
            }
            PathingBehavior.INSTANCE.setGoal(GOAL);
            PathingBehavior.INSTANCE.path();
            if (GOAL.isInGoal(playerFeet())) {
                System.out.println("Successfully pathed to " + playerFeet() + " in " + event.getCount() + " ticks");
                mc.shutdown();
            }
            if (event.getCount() > MAX_TICKS) {
                throw new IllegalStateException("took too long");
            }
        }
    }
}
