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
		Integer rhsPri = PrioritySorterUtils.getPriority(rhs);
		Integer lhsPri = PrioritySorterUtils.getPriority(lhs);
		int c = rhsPri.compareTo(lhsPri);
		if (c == 0) {
		    // Use default sort order
		    return super.compare(lhs, rhs);
		} else {
		    // Sorted by priority
		    return c;
		}
	}
}
