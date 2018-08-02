package baritone.bot.pathing.actions;

public interface ActionCosts {

    //These costs are measured roughly in ticks btw
    double WALK_ONE_BLOCK_COST = 20 / 4.317;
    double WALK_ONE_IN_WATER_COST = 20 / 2.2;
    double JUMP_ONE_BLOCK_COST = 5.72854;//see below calculation for fall. 1.25 blocks
    double LADDER_UP_ONE_COST = 20 / 2.35;
    double LADDER_DOWN_ONE_COST = 20 / 3;
    double SNEAK_ONE_BLOCK_COST = 20 / 1.3;
    double SPRINT_ONE_BLOCK_COST = 20 / 5.612;
    /**
     * Doesn't include walking forwards, just the falling
     *
     * Based on a sketchy formula from minecraftwiki
     *
     * d(t) = 3.92 × (99 - 49.50×(0.98^t+1) - t)
     *
     * Solved in mathematica
     */
    double FALL_ONE_BLOCK_COST = 5.11354;
    double FALL_TWO_BLOCK_COST = 7.28283;
    double FALL_THREE_BLOCK_COST = 8.96862;
    /**
     * It doesn't actually take ten ticks to place a block, this cost is so high
     * because we want to generally conserve blocks which might be limited
     */
    double PLACE_ONE_BLOCK_COST = 20;
    /**
     * Add this to the cost of breaking any block. The cost of breaking any
     * block is calculated as the number of ticks that block takes to break with
     * the tools you have. You add this because there's always a little overhead
     * (e.g. looking at the block)
     */
    double BREAK_ONE_BLOCK_ADD = 4;
    double COST_INF = 1000000;
}
