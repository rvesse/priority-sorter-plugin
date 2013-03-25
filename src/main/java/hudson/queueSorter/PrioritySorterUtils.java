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

import hudson.model.AbstractProject;
import hudson.model.Queue.BuildableItem;

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
     * @param buildable Buildable Item
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

}
