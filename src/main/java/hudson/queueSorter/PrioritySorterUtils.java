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

import groovy.lang.Buildable;
import hudson.model.HealthReport;
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
     * Gets the average build duration
     * 
     * @param buildable
     *            Buildable Item
     * @return Average Duration if at least {@code minBuilds} present, otherwise
     *         -1
     */
    @SuppressWarnings("rawtypes")
    static long getAverageBuildDuration(BuildableItem buildable, int minBuilds) {
        if (!(buildable.task instanceof Job)) {
            // Assume this is rare, if this happens then average duration is -1
            // i.e. unknown
            return -1;
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

        // Don't calculate average for jobs with few builds because their
        // average build time is unlikely to be accurate since they likely
        // reflect new projects whose contents may be in flux
        if (runCount < minBuilds)
            return -1;

        return totalDuration / runCount;
    }

    /**
     * Computes the wait time for a buildable item
     * 
     * @param buildable
     *            Buildable Item
     * @return Wait Time in milliseconds
     */
    static long getWaitTime(BuildableItem buildable) {
        return System.currentTimeMillis() - buildable.buildableStartMilliseconds;
    }

    /**
     * Gets the priority boost based upon build duration
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
     * @param averageDuration
     *            Average Duration for a build
     * @param waitTime
     *            How long the build has been waiting
     * @param threshold
     *            Threshold for build duration above which a build is considered
     *            slow
     * @return Priority Boost
     */
    static double getPriorityBoostForBuildDuration(double averageDuration, double waitTime, double threshold) {
        // If average duration is unknown apply no boost
        if (averageDuration == -1)
            return 1.0d;

        // Compute the boost based on how the average duration corresponds to
        // the provided threshold. This also takes into account how long a build
        // has been waiting relative to its average duration so that builds that
        // are slow don't always get pushed down the priority list
        double factor = 1.0d;
        if (averageDuration >= threshold) {
            // This is a slow build per plugin configuration

            // If the wait time has exceeded the average duration then we will
            // not boost priority
            if (waitTime >= averageDuration) {
                // Is a slow build but has been waiting a long time so actually
                // boost priority
                factor = (waitTime / averageDuration);
            } else {
                // Decrease priority appropriately

                // The first component of this is the average duration relative
                // to the threshold
                factor = (threshold / averageDuration);

                // The second component of this is how long the job has been
                // waiting relative to its average duration
                if (waitTime > 0) {
                    factor = factor + (waitTime / (averageDuration * 2));
                }
            }
        } else {
            // This is a fast build per plugin configuration

            // Boost priority by a factor of the average duration relative to
            // the threshold
            double increaser = (threshold / averageDuration);

            // If we've been waiting longer than our average duration we should
            // apply an additional boost
            if (waitTime > averageDuration) {
                increaser = increaser * (waitTime / averageDuration);
            }

            factor = (double) increaser;
        }

        // Cap min/max factor appropriately so jobs don't get horrendously
        // boosted
        if (factor < 0.25) {
            factor = 0.25;
        } else if (factor > 4.0) {
            factor = 4.0;
        }

        return factor;
    }

    /**
     * Gets the health score of a buildable item
     * 
     * @param buildable
     *            Buildable Item
     * @return Health score in range 0-100
     */
    static double getHealth(BuildableItem buildable) {
        if (!(buildable.task instanceof Job)) {
            // Assume this is rare, if this happens then health is 100 i.e.
            // assumed healthy
            return 100;
        }

        Job<?, ?> job = (Job<?, ?>) buildable.task;
        return job.getBuildHealth().getScore();
    }

    /**
     * Gets the priority boost based upon the build health
     * <p>
     * The {@code positive} parameter controls whether unhealthy builds receive
     * a positive boost or whether they receive a negative boost. If set to true
     * an unhealthy build will be given increased priority i.e. if a build has
     * recently failed you want to try building it again sooner. If set to false
     * an unhealthy build will be given a reduced priority i.e. if a build has
     * recently failed you want it to wait longer before building it again.
     * </p>
     * 
     * @param health
     *            Build Health
     * @param positive
     *            Whether to adjust positively or not
     * @return Priority Boost in the range of 0.5 to 1.5
     */
    static double getPriorityBoostForBuildHealth(double health, boolean positive) {
        double adjust = ((100d - health) / 100d);
        adjust /= 2d;
        return positive ? 1.0d + adjust : 1.0d - adjust;
    }
}
