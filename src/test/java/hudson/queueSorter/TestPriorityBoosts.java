/*
 * Copyright 2013 YarcData LLC All Rights Reserved.
 */

package hudson.queueSorter;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for queue sorting
 * 
 */
public class TestPriorityBoosts {
    
    private static final long MILLISECONDS_PER_MINUTE = 60 * 1000;
    private static final double DELTA = 0.00001d;

    @Test
    public void testPriorityBoostFastDuration01() {
        // Build averages 5 minutes, threshold for slow builds is 10 minutes and zero wait time
        // Thus build should have a boost of 2.0
        double boost = PrioritySorterUtils.getPriorityBoostForBuildDuration(5 * MILLISECONDS_PER_MINUTE, 0, 10 * MILLISECONDS_PER_MINUTE);
        Assert.assertEquals(2.0, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostFastDuration02() {
        // Build averages 5 minutes, threshold for slow builds is 10 minutes and 5 minutes wait time
        // Thus build should have a boost of 2.0
        // Note - For fast builds wait time has no effect until it exceeds average duration
        double boost = PrioritySorterUtils.getPriorityBoostForBuildDuration(5 * MILLISECONDS_PER_MINUTE, 5 * MILLISECONDS_PER_MINUTE, 10 * MILLISECONDS_PER_MINUTE);
        Assert.assertEquals(2.0, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostFastDuration03() {
        // Build averages 5 minutes, threshold for slow builds is 10 minutes and 7.5 minutes wait time
        // It has been waiting 50% more than its average duration so should get a 50% boost to the base boost
        // Thus build should have a boost of 3.0
        double boost = PrioritySorterUtils.getPriorityBoostForBuildDuration(5 * MILLISECONDS_PER_MINUTE, (long)(7.5d * MILLISECONDS_PER_MINUTE), 10 * MILLISECONDS_PER_MINUTE);
        Assert.assertEquals(3.0, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostFastDuration04() {
        // Build averages 5 minutes, threshold for slow builds is 10 minutes and 7.5 minutes wait time
        // It has been waiting twice as long over its average duration so should get a 50% boost to the base boost
        // Thus build should have a boost of 4.0
        double boost = PrioritySorterUtils.getPriorityBoostForBuildDuration(5 * MILLISECONDS_PER_MINUTE, 10 * MILLISECONDS_PER_MINUTE, 10 * MILLISECONDS_PER_MINUTE);
        Assert.assertEquals(4.0, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostFastDuration05() {
        // Build averages 5 minutes, threshold for slow builds is 10 minutes and 7.5 minutes wait time
        // It has been waiting four as long over its average duration so should get a 100% boost to the base boost
        // However boosts are capped at 4.0 so build should have a boost of 4.0
        double boost = PrioritySorterUtils.getPriorityBoostForBuildDuration(5 * MILLISECONDS_PER_MINUTE, 20 * MILLISECONDS_PER_MINUTE, 10 * MILLISECONDS_PER_MINUTE);
        Assert.assertEquals(4.0, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostSlowDuration01() {
        // Build averages 20 minutes, threshold for slow builds is 10 minutes and zero wait time
        // Thus build should have a boost of 0.5
        double boost = PrioritySorterUtils.getPriorityBoostForBuildDuration(20 * MILLISECONDS_PER_MINUTE, 0, 10 * MILLISECONDS_PER_MINUTE);
        Assert.assertEquals(0.5, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostSlowDuration02() {
        // Build averages 20 minutes, threshold for slow builds is 10 minutes and 10 minutes wait time
        // Since it has been waiting half its average duration the reduction should be half as much
        // Thus build should have a boost of 0.5
        double boost = PrioritySorterUtils.getPriorityBoostForBuildDuration(20 * MILLISECONDS_PER_MINUTE, 10 * MILLISECONDS_PER_MINUTE, 10 * MILLISECONDS_PER_MINUTE);
        Assert.assertEquals(0.75, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostSlowDuration03() {
        // Build averages 20 minutes, threshold for slow builds is 10 minutes and 30 minutes wait time
        // Since it has been waiting more than its average duration then we should actually get a positive boost
        // Thus build should have a boost of 1.5
        double boost = PrioritySorterUtils.getPriorityBoostForBuildDuration(20 * MILLISECONDS_PER_MINUTE, 30 * MILLISECONDS_PER_MINUTE, 10 * MILLISECONDS_PER_MINUTE);
        Assert.assertEquals(1.5, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostSlowDuration04() {
        // Build averages 20 minutes, threshold for slow builds is 5 minutes and zero wait time
        // Thus build should have a boost of 0.25
        double boost = PrioritySorterUtils.getPriorityBoostForBuildDuration(20 * MILLISECONDS_PER_MINUTE, 0, 5 * MILLISECONDS_PER_MINUTE);
        Assert.assertEquals(0.25, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostSlowDuration05() {
        // Build averages 40 minutes, threshold for slow builds is 5 minutes and zero wait time
        // However boosts are capped at 0.25 so build should have a boost of 0.25
        double boost = PrioritySorterUtils.getPriorityBoostForBuildDuration(20 * MILLISECONDS_PER_MINUTE, 0, 5 * MILLISECONDS_PER_MINUTE);
        Assert.assertEquals(0.25, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostUnknownDuration04() {
        // Build average unknown, threshold for slow builds is 10 minutes and zero wait time
        // Unknown average results in boost of 1.0
        double boost = PrioritySorterUtils.getPriorityBoostForBuildDuration(-1, 0, 10 * MILLISECONDS_PER_MINUTE);
        Assert.assertEquals(1.0, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostHealth01() {
        // 100% Healthy Build will have no boost
        double boost = PrioritySorterUtils.getPriorityBoostForBuildHealth(100, true);
        Assert.assertEquals(1.0, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostHealth02() {
        // 0% Healthy Build will have 1.5 boost
        double boost = PrioritySorterUtils.getPriorityBoostForBuildHealth(0, true);
        Assert.assertEquals(1.5, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostHealth03() {
        // 0% Healthy Build will have 0.5 boost when negative boosting
        double boost = PrioritySorterUtils.getPriorityBoostForBuildHealth(0, false);
        Assert.assertEquals(0.5, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostHealth04() {
        // 50% Healthy Build will have 1.25 boost
        double boost = PrioritySorterUtils.getPriorityBoostForBuildHealth(50, true);
        Assert.assertEquals(1.25, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostHealth05() {
        // 50% Healthy Build will have 0.75 boost when negative boosting
        double boost = PrioritySorterUtils.getPriorityBoostForBuildHealth(50, false);
        Assert.assertEquals(0.75, boost, DELTA);
    }
}
