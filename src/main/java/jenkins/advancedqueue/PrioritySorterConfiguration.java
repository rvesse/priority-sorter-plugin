/*
 * The MIT License
 *
 * Copyright (c) 2013, Magnus Sandberg
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
package jenkins.advancedqueue;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.TopLevelItem;
import hudson.queueSorter.PrioritySorterJobProperty;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import jenkins.advancedqueue.JobGroup.PriorityStrategyHolder;
import jenkins.advancedqueue.sorter.SorterStrategy;
import jenkins.advancedqueue.sorter.SorterStrategyDescriptor;
import jenkins.advancedqueue.sorter.strategy.AbsoluteStrategy;
import jenkins.advancedqueue.sorter.strategy.MultiBucketStrategy;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author Magnus Sandberg
 * @since 2.0
 */
@Extension
public class PrioritySorterConfiguration extends GlobalConfiguration {

	private final static Logger LOGGER = Logger.getLogger(PrioritySorterConfiguration.class.getName());
	private final static SorterStrategy DEFAULT_STRATEGY = new AbsoluteStrategy(
			MultiBucketStrategy.DEFAULT_PRIORITIES_NUMBER, MultiBucketStrategy.DEFAULT_PRIORITY);

	private boolean legacyMode = false;
	private Integer legacyMaxPriority = Integer.MAX_VALUE;
	private Integer legacyMinPriority = Integer.MIN_VALUE;

	private boolean allowPriorityOnJobs;

	private boolean onlyAdminsMayEditPriorityConfiguration = false;

	private SorterStrategy strategy;

	public PrioritySorterConfiguration() {
	}

	public static void init() {
		PrioritySorterConfiguration prioritySorterConfiguration = PrioritySorterConfiguration.get();
		// Make sure default is good for updating from legacy
		prioritySorterConfiguration.strategy = DEFAULT_STRATEGY; // TODO: replace with class ref
		prioritySorterConfiguration.allowPriorityOnJobs = true;
		// Check for legacy
		prioritySorterConfiguration.checkLegacy();
		if (!prioritySorterConfiguration.getLegacyMode()) {
			prioritySorterConfiguration.load();
		}
	}

	@Override
	public boolean configure(StaplerRequest req, JSONObject json) throws FormException {

		int prevNumberOfPriorities = strategy.getNumberOfPriorities();
		strategy = req.bindJSON(SorterStrategy.class, json.getJSONObject("strategy"));
		int newNumberOfPriorities = strategy.getNumberOfPriorities();

		FormValidation numberOfPrioritiesCheck = doCheckNumberOfPriorities(String.valueOf(newNumberOfPriorities));
		if (numberOfPrioritiesCheck.kind != FormValidation.Kind.OK) {
			throw new FormException(numberOfPrioritiesCheck.getMessage(), "numberOfPriorities");
		}
		//
		onlyAdminsMayEditPriorityConfiguration = json.getBoolean("onlyAdminsMayEditPriorityConfiguration");
		allowPriorityOnJobs = json.getBoolean("allowPriorityOnJobs");
		if (getLegacyMode()) {
			Boolean advanced = json.getBoolean("advanced");
			if (advanced) {
				convertFromLegacyToAdvanced();
			}
		} else {
			updatePriorities(prevNumberOfPriorities);
		}
		save();
		return true;
	}

	public final boolean getLegacyMode() {
		return legacyMode;
	}

	public boolean getAllowPriorityOnJobs() {
		return allowPriorityOnJobs;
	}

	public boolean getOnlyAdminsMayEditPriorityConfiguration() {
		return onlyAdminsMayEditPriorityConfiguration;
	}

	public SorterStrategy getStrategy() {
		return strategy;
	}

	public ListBoxModel doFillStrategyItems() {
		ListBoxModel strategies = new ListBoxModel();
		List<SorterStrategyDescriptor> values = SorterStrategy.getAllSorterStrategies();
		for (SorterStrategyDescriptor sorterStrategy : values) {
			strategies.add(sorterStrategy.getDisplayName(), sorterStrategy.getKey());
		}
		return strategies;
	}

	public ListBoxModel doGetPriorityItems() {
		ListBoxModel items = internalFillDefaultPriorityItems(strategy.getNumberOfPriorities());
		items.add(
				0,
				new ListBoxModel.Option("-- use default priority --", String.valueOf(PriorityCalculationsUtil
						.getUseDefaultPriorityPriority())));
		return items;
	}

	// TODO: move to helper class
	private ListBoxModel internalFillDefaultPriorityItems(int value) {
		ListBoxModel items = new ListBoxModel();
		for (int i = 1; i <= value; i++) {
			items.add(String.valueOf(i));
		}
		return items;
	}

	public FormValidation doCheckNumberOfPriorities(@QueryParameter String value) {
		if (value.length() == 0) {
			return FormValidation.error(Messages.PrioritySorterConfiguration_enterValueRequestMessage());
		}
		try {
			int intValue = Integer.parseInt(value);
			if (intValue <= 0) {
				return FormValidation.error(Messages.PrioritySorterConfiguration_enterValueRequestMessage());
			}
		} catch (NumberFormatException e) {
			return FormValidation.error(Messages.PrioritySorterConfiguration_enterValueRequestMessage());
		}
		return FormValidation.ok();
	}

	private void checkLegacy() {
		// Shouldn't really by a permission problem when getting here but
		// to be on the safe side
		SecurityContext saveCtx = ACL.impersonate(ACL.SYSTEM);
		try {
			legacyMode = false;
			legacyMaxPriority = Integer.MAX_VALUE;
			legacyMinPriority = Integer.MIN_VALUE;

			// getAllItems() doesn't return MatrixProject even if actually is a Project
			// since it also is a group of items (ItemGroup) in the tree being traversed
			List<TopLevelItem> allItems = Jenkins.getInstance().getItems();
			for (TopLevelItem item : allItems) {
				if (item instanceof AbstractProject) {
					AbstractProject<?, ?> project = (AbstractProject<?, ?>) item;
					PrioritySorterJobProperty priority = project.getProperty(PrioritySorterJobProperty.class);
					if (priority != null) {
						legacyMode = true;
						legacyMaxPriority = Math.max(legacyMaxPriority, priority.priority);
						legacyMinPriority = Math.min(legacyMinPriority, priority.priority);
					}
				}
			}
		} finally {
			SecurityContextHolder.setContext(saveCtx);
		}
	}

	private void updatePriorities(int prevNumberOfPriorities) {
		// Shouldn't really by a permission problem when getting here but
		// to be on the safe side
		SecurityContext saveCtx = ACL.impersonate(ACL.SYSTEM);
		try {
			@SuppressWarnings("rawtypes")
			List<AbstractProject> allProjects = Jenkins.getInstance().getAllItems(AbstractProject.class);
			for (AbstractProject<?, ?> project : allProjects) {
				try {
					// Scale any priority on the Job
					AdvancedQueueSorterJobProperty priorityProperty = project
							.getProperty(AdvancedQueueSorterJobProperty.class);
					if (priorityProperty != null) {
						int newPriority = PriorityCalculationsUtil.scale(prevNumberOfPriorities,
								strategy.getNumberOfPriorities(), priorityProperty.priority);
						project.removeProperty(priorityProperty);
						project.addProperty(new AdvancedQueueSorterJobProperty(priorityProperty.getUseJobPriority(),
								newPriority));
						project.save();
					}
					project.save();
				} catch (IOException e) {
					LOGGER.warning("Failed to update Advanced Job Priority To " + project.getName());
				}
			}
			//
			List<JobGroup> jobGroups = PriorityConfiguration.get().getJobGroups();
			for (JobGroup jobGroup : jobGroups) {
				jobGroup.setPriority(PriorityCalculationsUtil.scale(prevNumberOfPriorities,
						strategy.getNumberOfPriorities(), jobGroup.getPriority()));
				List<PriorityStrategyHolder> priorityStrategies = jobGroup.getPriorityStrategies();
				for (PriorityStrategyHolder priorityStrategyHolder : priorityStrategies) {
					priorityStrategyHolder.getPriorityStrategy().numberPrioritiesUpdates(prevNumberOfPriorities,
							strategy.getNumberOfPriorities());
				}
			}
			PriorityConfiguration.get().save();
		} finally {
			SecurityContextHolder.setContext(saveCtx);
		}
	}

	private void convertFromLegacyToAdvanced() {
		// Update legacy range first
		checkLegacy();
		// Shouldn't really by a permission problem when getting here but
		// to be on the safe side
		SecurityContext saveCtx = ACL.impersonate(ACL.SYSTEM);
		try {
			if (getLegacyMode()) {
				//
				@SuppressWarnings("rawtypes")
				List<AbstractProject> allProjects = Jenkins.getInstance().getAllItems(AbstractProject.class);
				for (AbstractProject<?, ?> project : allProjects) {
					PrioritySorterJobProperty legacyPriorityProperty = project
							.getProperty(PrioritySorterJobProperty.class);
					if (legacyPriorityProperty != null && getAllowPriorityOnJobs()) {
						int advancedPriority = legacyPriorityToAdvancedPriority(legacyMinPriority, legacyMaxPriority,
								strategy.getNumberOfPriorities(), legacyPriorityProperty.priority);
						AdvancedQueueSorterJobProperty advancedQueueSorterJobProperty = new AdvancedQueueSorterJobProperty(
								true, advancedPriority);
						try {
							project.addProperty(advancedQueueSorterJobProperty);
							project.save();
						} catch (IOException e) {
							LOGGER.warning("Failed to add Advanced Job Priority To " + project.getName());
						}
					}
					try {
						project.removeProperty(legacyPriorityProperty);
						project.save();
					} catch (IOException e) {
						LOGGER.warning("Failed to remove Legacy Job Priority From " + project.getName());
					}
				}

				// Finally, switch Legacy Mode
				legacyMode = false;
			}
		} finally {
			SecurityContextHolder.setContext(saveCtx);
		}
	}

	static int legacyPriorityToAdvancedPriority(int legacyMinPriority, int legacyMaxPriority, int numberOfPriorities,
			int priority) {
		int offset = normalizedOffset(legacyMinPriority);
		int normalized = inverseAndNormalize(legacyMinPriority, legacyMaxPriority, priority);
		int advancedPriority = PriorityCalculationsUtil.scale(legacyMaxPriority + offset, numberOfPriorities,
				normalized);
		return advancedPriority;
	}

	/**
	 * Calculates how much must be added to a legacy value to get into the positive numbers
	 */
	static int normalizedOffset(int min) {
		int offset = -min + 1;
		return offset;
	}

	static int inverseAndNormalize(int min, int max, int value) {
		int offset = normalizedOffset(min);
		max += offset;
		value += offset;
		// Inverse
		value = max - value + 1;
		return value;
	}

	static public PrioritySorterConfiguration get() {
		return (PrioritySorterConfiguration) Jenkins.getInstance().getDescriptor(PrioritySorterConfiguration.class);
	}

}
