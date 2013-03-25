/*
 * The MIT License
 *
 * Copyright (c) 2010, Brad Larson
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

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Queue.BuildableItem;
import hudson.model.queue.AbstractQueueSorterImpl;

/**
 * A queue sorter that uses priority based sorting, if no priorities are set or jobs have equal priorities then the default sort order (FIFO) is preserved
 *
 */
@Extension
public class PrioritySorterQueueSorter extends AbstractQueueSorterImpl {

    @Override
	public int compare(BuildableItem lhs, BuildableItem rhs) {
		// Note that we sort these backwards because we want to return
		// higher-numbered items first.
		Integer rhsPri = getPriority(rhs);
		Integer lhsPri = getPriority(lhs);
		int c = rhsPri.compareTo(lhsPri);
		if (c == 0) {
		    // Use default sort order
		    return super.compare(lhs, rhs);
		} else {
		    // Sorted by priority
		    return c;
		}
	}

	private static int getPriority(BuildableItem buildable) {
		if (!(buildable.task instanceof AbstractProject)) {
			// This shouldn't happen... but just in case, let's give this
			// task a really low priority so jobs with valid priorities
			// which do work will get built first.
			return 0;
		}
		AbstractProject<?, ?> project = (AbstractProject<?, ?>) buildable.task;
		PrioritySorterJobProperty priority = project
				.getProperty(PrioritySorterJobProperty.class);
		if (priority != null) {
			return priority.priority;
		} else {
			// No priority has been set for this job - use the
			// default
			return PrioritySorterDefaults.getDefault();
		}
	}
}
