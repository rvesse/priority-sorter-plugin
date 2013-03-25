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

import java.util.List;

import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.Queue.BuildableItem;
import hudson.model.Run;

/**
 * Static utility methods for the Priority Sorter plugin
 * 
 */
public class PrioritySorterUtils {

    /**
     * Private constructor prevents instantiation
     */
    private PrioritySorterUtils() {
    }

    /**
     * Gets the priority for a buildable item
     * <p>
     * This priority is provided by a job priority associated with each job.
     * Jobs which do not have a priority receive the default from
     * {@link PrioritySorterDefaults#getDefault()}. Buildable items not
     * associated with projects receive the lowest priority.
     * </p>
     * 
     * @param buildable
     *            Buildable Item
     * @return Priority
     */
    static int getPriority(BuildableItem buildable) {
        if (!(buildable.task instanceof AbstractProject)) {
            // This shouldn't happen... but just in case, let's give this
            // task a really low priority so jobs with valid priorities
            // which do work will get built first.
            return 0;
        }
        AbstractProject<?, ?> project = (AbstractProject<?, ?>) buildable.task;
        PrioritySorterJobProperty priority = project.getProperty(PrioritySorterJobProperty.class);
        if (priority != null) {
            return priority.priority;
        } else {
            // No priority has been set for this job - use the
            // default
            return PrioritySorterDefaults.getDefault();
        }
    }

    /**
     * Boost the priority based upon build duration
     * <p>
     * Computes the boosted priority for a buildable item, the boosted priority
     * includes one/more applicable boosts depending on the item. The primary
     * boost applied is that fast builds have their priority increased. However
     * the exact amount of boost may vary depending on the wait time of the
     * item. Items are boosted more if their wait time is below their average
     * duration or if they would usually be reduced in priority but have been
     * waiting more than their average duration.
     * </p>
     * 
     * @param buildable
     *            Buildable Item
     * @param basePriority
     *            Base Priority for the item
     * @param threshold
     *            Threshold for build duration
     * @return Boosted priority
     */
    static int boostPriorityByBuildDuration(BuildableItem buildable, int basePriority, long threshold) {
        if (!(buildable.task instanceof Job)) {
            // Assume this is rare, if this happens leave base priority alone
            return basePriority;
        }

        // Maybe there is a more elegant way of doing this but Java generics
        // are somewhat clunky in this regard
        Job<?, ?> job = (Job<?, ?>) buildable.task;
        List<?> builds = job.getBuilds();

        // Collect run durations
        long totalDuration = 0;
        long runCount = 0;
        for (Object build : builds) {
            if (build instanceof Run) {
                runCount++;
                totalDuration += ((Run) build).getDuration();
            }
        }

        // Don't boost priority for jobs with few builds because their average
        // build time is unlikely to be a good indicator of whether they should
        // be prioritized higher
        if (runCount < 10)
            return basePriority;

        // Now we can compute the average duration and wait time
        long averageDuration = totalDuration / runCount;
        long waitTime = System.currentTimeMillis() - buildable.buildableStartMilliseconds;

        // We then boost the base priority based on how the average duration
        // corresponds to
        // the provided threshold. This also takes into account how long a build
        // has
        // been waiting relative to its average duration so that builds that are
        // slow
        // don't always get pushed down the priority list
        double factor = 1.0d;
        if (averageDuration >= threshold) {
            // This is a slow build per plugin configuration

            // If the wait time has exceeded the average duration then we will
            // not boost priority
            if (waitTime >= averageDuration) {
                // TODO: This should likely positively boost the priority of a
                // job rather than leave
                // it untouched
                factor = 1.0d;
            } else {
                // Decrease priority appropriately

                // The first component of this is the average duration relative
                // to the threshold
                long reducer = (averageDuration / threshold);

                // The second component of this is how long the job has been
                // waiting relative
                // to its average duration
                reducer = reducer / (waitTime / averageDuration);

                // The 1 / is necessary because with a slow job the factor will
                // be a large number
                // and we want to reduce not increase the priority
                factor = 1 / reducer;
            }
        } else {
            // This is a fast build per plugin configuration

            // Boost priority by a factor of the average duration relative to
            // the threshold
            long increaser = (threshold / averageDuration);

            // If we've been waiting longer than our average duration we should
            // apply
            // an additional boost
            if (waitTime >= averageDuration) {
                increaser = increaser * (waitTime / averageDuration);
            }

            factor = (double) increaser;
        }

        // TODO: Cap min/max factor appropriately so jobs don't get horrendously
        // boosted

        return (int) (factor * basePriority);
    }

}
