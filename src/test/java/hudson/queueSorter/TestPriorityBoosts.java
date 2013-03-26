/*
 * The MIT License
 *
 * Copyright (c) 2013, Rob Vesse
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
    public void testPriorityBoostUnhealthyBuilds01() {
        // 100% Healthy Build will have no boost
        double boost = PrioritySorterUtils.getPriorityBoostForUnhealthyBuilds(100, true);
        Assert.assertEquals(1.0, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostUnhealthyBuilds02() {
        // 0% Healthy Build will have 1.5 boost
        double boost = PrioritySorterUtils.getPriorityBoostForUnhealthyBuilds(0, true);
        Assert.assertEquals(1.5, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostUnhealthyBuilds03() {
        // 0% Healthy Build will have 0.5 boost when negative boosting
        double boost = PrioritySorterUtils.getPriorityBoostForUnhealthyBuilds(0, false);
        Assert.assertEquals(0.5, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostUnhealthyBuilds04() {
        // 50% Healthy Build will have 1.25 boost
        double boost = PrioritySorterUtils.getPriorityBoostForUnhealthyBuilds(50, true);
        Assert.assertEquals(1.25, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostUnhealthyBuilds05() {
        // 50% Healthy Build will have 0.75 boost when negative boosting
        double boost = PrioritySorterUtils.getPriorityBoostForUnhealthyBuilds(50, false);
        Assert.assertEquals(0.75, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostHealthyBuilds01() {
        // 100% Healthy Build will have 1.5 boost
        double boost = PrioritySorterUtils.getPriorityBoostForHealthyBuilds(100, true);
        Assert.assertEquals(1.5, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostHealthyBuilds02() {
        // 0% Healthy Build will have 1.0 boost
        double boost = PrioritySorterUtils.getPriorityBoostForHealthyBuilds(0, true);
        Assert.assertEquals(0.5, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostHealthyBuilds03() {
        // 50% Healthy Build will have 1.0 boost
        double boost = PrioritySorterUtils.getPriorityBoostForHealthyBuilds(50, true);
        Assert.assertEquals(1.0, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostHealthyBuilds04() {
        // 100% Healthy Build will have 0.5 boost when negative boosting
        double boost = PrioritySorterUtils.getPriorityBoostForHealthyBuilds(100, false);
        Assert.assertEquals(0.5, boost, DELTA);
    }
    
    @Test
    public void testPriorityBoostHealthyBuilds05() {
        // 0% Healthy Build will have 1.5 boost when negative boosting
        double boost = PrioritySorterUtils.getPriorityBoostForHealthyBuilds(0, false);
        Assert.assertEquals(1.5, boost, DELTA);
    }
}
